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

import com.google.common.base.Strings;
import com.hazelcast.cluster.Address;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author phaneesh
 */
public class RangerDiscoveryStrategy extends AbstractDiscoveryStrategy {


    private ILogger logger;
    private RangerServiceDiscoveryHelper rangerServiceDiscoveryHelper;

    public RangerDiscoveryStrategy(final DiscoveryNode discoveryNode, final ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        String zkConnectionString = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.ZK_CONNECTION_STRING);
        String namespace = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.NAMESPACE);
        String serviceName = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.SERVICE_NAME);
        this.rangerServiceDiscoveryHelper = new RangerServiceDiscoveryHelper();
        this.logger = logger;
        try {
            String host = discoveryNode != null ? discoveryNode.getPublicAddress().getHost() : null;
            if(!Inet4Address.getLocalHost().getHostAddress().equals(host) && Strings.isNullOrEmpty(host))
                host = Inet4Address.getLocalHost().getHostAddress();
            int port = discoveryNode != null ? discoveryNode.getPublicAddress().getPort() : 0;
            rangerServiceDiscoveryHelper.start(zkConnectionString, namespace, serviceName, host, port, logger);
        } catch (Exception e) {
           logger.severe("Failed to start service discovery!", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                rangerServiceDiscoveryHelper.stop();
            } catch (Exception e) {
                logger.severe("Error adding shutdown hook!", e);
            }
        }));
    }

    public Iterable<DiscoveryNode> discoverNodes() {
        return rangerServiceDiscoveryHelper.getAllNodes().stream().map( n -> {
            Map<String, String> attributes = Collections.singletonMap("hostname", n.getHost());
            try {
                return new SimpleDiscoveryNode(new Address(n.getHost(), n.getPort()), attributes);
            } catch (UnknownHostException e) {
                logger.severe("Error adding discovered member", e);
                return null;
            }
        }).collect(Collectors.toList());
    }
}
