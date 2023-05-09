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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hazelcast.cluster.Address;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import io.appform.ranger.client.RangerClient;
import io.appform.ranger.client.zk.ShardedRangerZKHubClient;
import io.appform.ranger.client.zk.SimpleRangerZKClient;
import io.appform.ranger.client.zk.UnshardedRangerZKHubClient;
import io.appform.ranger.common.server.ShardInfo;
import io.appform.ranger.core.finder.serviceregistry.ListBasedServiceRegistry;
import io.appform.ranger.core.finder.serviceregistry.MapBasedServiceRegistry;
import io.appform.ranger.core.healthcheck.Healthchecks;
import io.appform.ranger.core.model.Service;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.serviceprovider.ServiceProvider;
import io.appform.ranger.zookeeper.ServiceProviderBuilders;
import io.appform.ranger.zookeeper.serde.ZkNodeDataSerializer;
import lombok.Getter;
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

    public static final String CONFIG_PREFIX = "discovery.ranger";
    private static final int UNUSED_PORT = 65535;
    private final ObjectMapper objectMapper;

    private final ILogger logger;
    @Getter
    private CuratorFramework curator;

    private ServiceProvider<ShardInfo, ZkNodeDataSerializer<ShardInfo>> serviceProvider;

    private RangerClient<ShardInfo, MapBasedServiceRegistry<ShardInfo>> rangerClient;

    private final Service service;
    
    public RangerDiscoveryStrategy(final DiscoveryNode discoveryNode, final ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        String zkConnectionString = getOrNull(CONFIG_PREFIX, RangerDiscoveryConfiguration.ZK_CONNECTION_STRING);
        String namespace = getOrNull(CONFIG_PREFIX, RangerDiscoveryConfiguration.NAMESPACE);
        String serviceName = getOrNull(CONFIG_PREFIX, RangerDiscoveryConfiguration.SERVICE_NAME);
        this.service = Service.builder()
                .namespace(namespace)
                .serviceName(serviceName)
                .build();
        this.logger = logger;
        this.objectMapper = new ObjectMapper();
        try {
            String host = discoveryNode != null ? discoveryNode.getPublicAddress().getHost() : null;
            if(!InetAddress.getLocalHost().getHostAddress().equals(host) && Strings.isNullOrEmpty(host))
                host = InetAddress.getLocalHost().getHostAddress();
            int port = discoveryNode != null ? discoveryNode.getPublicAddress().getPort() : UNUSED_PORT;
            this.curator = buildCurator(zkConnectionString, namespace);
            this.serviceProvider = buildServiceProvider(objectMapper, namespace, serviceName, host, port);
            this.rangerClient = buildDiscoveryClient();
        } catch (Exception e) {
           logger.severe("Failed to start service discovery!", e);
        }
    }

    public Iterable<DiscoveryNode> discoverNodes() {
        try {
            if( rangerClient != null && rangerClient.getAllNodes() != null) {
                return rangerClient.getAllNodes().stream().map(n -> {
                    Map<String, String> attributes = Collections.singletonMap("hostname", n.getHost());
                    try {
                        return new SimpleDiscoveryNode(new Address(n.getHost(), n.getPort()), attributes);
                    } catch (UnknownHostException e) {
                        logger.severe("Error adding discovered member", e);
                        return null;
                    }
                }).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } catch(Exception e) {
            logger.severe("Error discovering nodes", e);
            return Collections.emptyList();
        }
    }

    private CuratorFramework buildCurator(final String connectionString, final String namespace) {
        return CuratorFrameworkFactory.builder()
            .connectString(connectionString)
            .namespace(namespace)
            .retryPolicy(new RetryForever(1000))
            .build();
    }

    private ServiceProvider<ShardInfo, ZkNodeDataSerializer<ShardInfo>> buildServiceProvider(ObjectMapper objectMapper,
                                                                                             String namespace,
                                                                                             String serviceName,
                                                                                             String hostname,
                                                                                             int port) {
        return ServiceProviderBuilders.<ShardInfo>unshardedServiceProviderBuilder()
                .withCuratorFramework(curator)
                .withNamespace(namespace)
                .withServiceName(serviceName)
                .withSerializer(data -> {
                    try {
                        return objectMapper.writeValueAsBytes(data);
                    } catch (JsonProcessingException e) {
                       logger.severe("Error serializing data", e);
                    }
                    return null;
                })
                .withHostname(hostname)
                .withPort(port)
                .withNodeData(ShardInfo.builder().build())
                .withHealthcheck(Healthchecks.defaultHealthyCheck())
                .withHealthUpdateIntervalMs(5000)
                .withStaleUpdateThresholdMs(15000)
                .build();
    }

    private RangerClient<ShardInfo, MapBasedServiceRegistry<ShardInfo>> buildDiscoveryClient() {
        return SimpleRangerZKClient.<ShardInfo>builder()
                .curatorFramework(curator)
                .deserializer(bytes -> {
                    try {
                        return objectMapper.readValue(bytes, new TypeReference<>() {
                        });
                    } catch (IOException e) {
                        logger.severe("Error de-serializing data", e);
                    }
                    return null;
                })
                .namespace(service.getNamespace())
                .serviceName(service.getServiceName())
                .mapper(objectMapper)
                .nodeRefreshIntervalMs(5000)
                .build();
    }

    @Override
    public void start() {
        curator.start();
        serviceProvider.start();
        rangerClient.start();
        logger.info("Ranger discovery initialized successfully");
    }

    @Override
    public void destroy() {
        rangerClient.stop();
        serviceProvider.stop();
        curator.close();
    }
}
