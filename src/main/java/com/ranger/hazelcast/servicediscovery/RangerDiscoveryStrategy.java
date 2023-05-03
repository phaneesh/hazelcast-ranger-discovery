/*
 * Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ranger.hazelcast.servicediscovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.hazelcast.cluster.Address;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.ranger.hazelcast.servicediscovery.config.ServiceDiscoveryConfiguration;
import com.ranger.hazelcast.servicediscovery.healthchecks.InitialDelayChecker;
import com.ranger.hazelcast.servicediscovery.resolvers.DefaultNodeInfoResolver;
import com.ranger.hazelcast.servicediscovery.selectors.HierarchicalEnvironmentAwareShardSelector;
import io.appform.ranger.client.RangerClient;
import io.appform.ranger.client.zk.SimpleRangerZKClient;
import io.appform.ranger.common.server.ShardInfo;
import io.appform.ranger.core.finder.serviceregistry.MapBasedServiceRegistry;
import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.model.ShardSelector;
import io.appform.ranger.core.serviceprovider.ServiceProvider;
import io.appform.ranger.zookeeper.ServiceProviderBuilders;
import io.appform.ranger.zookeeper.serde.ZkNodeDataSerializer;
import lombok.Getter;
import lombok.val;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author phaneesh
 */
public class RangerDiscoveryStrategy extends AbstractDiscoveryStrategy {

    private ObjectMapper objectMapper;

    private ILogger logger;
      private ServiceDiscoveryConfiguration serviceDiscoveryConfiguration;

    @Getter
    private CuratorFramework curator;

    private ServiceProvider<ShardInfo, ZkNodeDataSerializer<ShardInfo>> serviceProvider;

    private RangerClient<ShardInfo, MapBasedServiceRegistry<ShardInfo>> rangerClient;
    
    public RangerDiscoveryStrategy(final DiscoveryNode discoveryNode, final ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        String zkConnectionString = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.ZK_CONNECTION_STRING);
        String namespace = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.NAMESPACE);
        String serviceName = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.SERVICE_NAME);
        this.logger = logger;
        this.objectMapper = new ObjectMapper();
        try {
            String host = discoveryNode != null ? discoveryNode.getPublicAddress().getHost() : null;
            if(!InetAddress.getLocalHost().getHostAddress().equals(host) && Strings.isNullOrEmpty(host))
                host = InetAddress.getLocalHost().getHostAddress();
            int port = discoveryNode != null ? discoveryNode.getPublicAddress().getPort() : 0;
            this.serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                    .namespace(namespace)
                    .disableWatchers(true)
                    .environment(System.getenv("CONFIG_ENV"))
                    .publishedHost(host)
                    .publishedPort(port)
                    .zookeeper(zkConnectionString)
                    .initialRotationStatus(true)
                    .build();
            initializeCurator();
            val initialCriteria = getInitialCriteria();
            val shardSelector = getShardSelector();
            this.serviceProvider = buildServiceProvider(objectMapper, namespace, serviceName, host, port, null);
            this.serviceProvider.start();
            logger.info("Service provider started with configuration: " +serviceDiscoveryConfiguration.toString());
            this.rangerClient = buildDiscoveryClient(namespace, serviceName, initialCriteria, false, shardSelector);
            this.rangerClient.start();
            logger.info("Ranger client started with configuration: " +serviceDiscoveryConfiguration.toString());
        } catch (Exception e) {
           logger.severe("Failed to start service discovery!", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serviceProvider.stop();
                rangerClient.stop();
                curator.close();
            } catch (Exception e) {
                logger.severe("Error adding shutdown hook!", e);
            }
        }));
    }

    public Iterable<DiscoveryNode> discoverNodes() {
        return rangerClient.getAllNodes().stream().map( n -> {
            Map<String, String> attributes = Collections.singletonMap("hostname", n.getHost());
            try {
                return new SimpleDiscoveryNode(new Address(n.getHost(), n.getPort()), attributes);
            } catch (UnknownHostException e) {
                logger.severe("Error adding discovered member", e);
                return null;
            }
        }).collect(Collectors.toList());
    }

    private void initializeCurator() {
        this.curator = CuratorFrameworkFactory.builder()
            .connectString(serviceDiscoveryConfiguration.getZookeeper())
            .namespace(serviceDiscoveryConfiguration.getNamespace())
            .retryPolicy(new RetryForever(serviceDiscoveryConfiguration.getConnectionRetryIntervalMillis()))
            .build();
    }

    private ServiceProvider<ShardInfo, ZkNodeDataSerializer<ShardInfo>> buildServiceProvider(ObjectMapper objectMapper,
                                                                                             String namespace,
                                                                                             String serviceName,
                                                                                             String hostname,
                                                                                             int port,
                                                                                             String portScheme) {
        val nodeInfoResolver = new DefaultNodeInfoResolver();
        val nodeInfo = nodeInfoResolver.resolve(serviceDiscoveryConfiguration);
        val serviceProviderBuilder = ServiceProviderBuilders.<ShardInfo>unshardedServiceProviderBuilder()
                .withCuratorFramework(curator)
                .withNamespace(namespace)
                .withServiceName(serviceName)
                .withSerializer(data -> {
                    try {
                        return objectMapper.writeValueAsBytes(data);
                    } catch (Exception e) {
                        logger.warning("Could not parse node data", e);
                    }
                    return null;
                })
                .withPortScheme(portScheme)
                .withNodeData(nodeInfo)
                .withHostname(hostname)
                .withPort(port)
                .withHealthcheck(() -> HealthcheckStatus.healthy)
//                .withHealthcheck(new InitialDelayChecker(serviceDiscoveryConfiguration.getInitialDelaySeconds()))
                .withHealthUpdateIntervalMs(serviceDiscoveryConfiguration.getRefreshTimeMs())
                .withStaleUpdateThresholdMs(10000);
        return serviceProviderBuilder.build();
    }

    private RangerClient<ShardInfo, MapBasedServiceRegistry<ShardInfo>> buildDiscoveryClient(String namespace,
                                                                                             String serviceName,
                                                                                             Predicate<ShardInfo> initialCriteria,
                                                                                             boolean mergeWithInitialCriteria,
                                                                                             ShardSelector<ShardInfo, MapBasedServiceRegistry<ShardInfo>> shardSelector) {
        return SimpleRangerZKClient.<ShardInfo>builder()
                .curatorFramework(curator)
                .namespace(namespace)
                .serviceName(serviceName)
                .mapper(objectMapper)
                .nodeRefreshIntervalMs(serviceDiscoveryConfiguration.getRefreshTimeMs())
                .disableWatchers(serviceDiscoveryConfiguration.isDisableWatchers())
                .deserializer(data -> {
                    try {
                        return objectMapper.readValue(data, new TypeReference<ServiceNode<ShardInfo>>() {});
                    } catch (IOException e) {
                        logger.warning("Error parsing node data with value " +new String(data), e);
                    }
                    return null;
                })
                .initialCriteria(initialCriteria)
                .alwaysUseInitialCriteria(mergeWithInitialCriteria)
                .shardSelector(shardSelector)
                .build();
    }

    protected ShardSelector<ShardInfo, MapBasedServiceRegistry<ShardInfo>> getShardSelector() {
        return new HierarchicalEnvironmentAwareShardSelector(serviceDiscoveryConfiguration.getEnvironment());
    }

    protected Predicate<ShardInfo> getInitialCriteria() {
        return shardInfo -> true;
    }
}
