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
import com.flipkart.ranger.ServiceFinderBuilders;
import com.flipkart.ranger.ServiceProviderBuilders;
import com.flipkart.ranger.finder.unsharded.UnshardedClusterFinder;
import com.flipkart.ranger.finder.unsharded.UnshardedClusterInfo;
import com.flipkart.ranger.healthcheck.HealthcheckStatus;
import com.flipkart.ranger.model.ServiceNode;
import com.flipkart.ranger.serviceprovider.ServiceProvider;
import com.hazelcast.logging.ILogger;

import java.io.IOException;
import java.util.List;

public class RangerServiceDiscoveryHelper {

    private static ServiceProvider<UnshardedClusterInfo> serviceProvider;

    private static UnshardedClusterFinder serviceFinder;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void start(final String zkConnectionString, final String namespace, final String serviceName, final String hostname, final int port, final ILogger logger) throws Exception {
        if(serviceProvider == null && hostname != null) {
            serviceProvider = ServiceProviderBuilders
                    .unshardedServiceProviderBuilder()
                    .withConnectionString(zkConnectionString)
                    .withNamespace(namespace)
                    .withServiceName(serviceName)
                    .withSerializer(serviceNode -> {
                        try {
                            return objectMapper.writeValueAsBytes(serviceNode);
                        } catch (JsonProcessingException e) {
                            logger.severe("Cannot serialize data: " +serviceNode.representation(), e);
                        }
                        return null;
                    })
                    .withHostname(hostname)
                    .withPort(port)
                    .withHealthcheck(() -> HealthcheckStatus.healthy)
                    .buildServiceDiscovery();
            serviceProvider.start();
        }
        if(serviceFinder == null) {
            serviceFinder = ServiceFinderBuilders.unshardedFinderBuilder()
                    .withConnectionString(zkConnectionString)
                    .withNamespace(namespace)
                    .withServiceName(serviceName)
                    .withDeserializer(data -> {
                        try {
                            return objectMapper.readValue(data,
                                    new TypeReference<ServiceNode<UnshardedClusterInfo>>() {
                                    });
                        } catch (IOException e) {
                            logger.severe("Error staring service discovery!", e);
                        }
                        return null;
                    })
                    .build();
            serviceFinder.start();
        }
    }

    public static List<ServiceNode<UnshardedClusterInfo>> getAllNodes() {
        return serviceFinder.getAll(null);
    }

    public static void stop() throws Exception {
        if(serviceProvider != null) {
            serviceProvider.stop();
        }
        if(serviceFinder != null) {
            serviceFinder.stop();
        }
    }
}
