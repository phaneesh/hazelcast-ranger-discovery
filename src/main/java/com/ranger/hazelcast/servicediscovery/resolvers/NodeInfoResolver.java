package com.ranger.hazelcast.servicediscovery.resolvers;

import com.ranger.hazelcast.servicediscovery.config.ServiceDiscoveryConfiguration;
import io.appform.ranger.common.server.ShardInfo;

/**
 * NodeInfoResolver.java
 * Interface to help build a node to be saved in the discovery backend while building the serviceProvider.
 * To define your custom nodeData {@link ShardInfo},
 */
@FunctionalInterface
public interface NodeInfoResolver extends CriteriaResolver<ShardInfo, ServiceDiscoveryConfiguration> {

}
