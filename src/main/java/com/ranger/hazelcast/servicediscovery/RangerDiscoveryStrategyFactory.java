package com.ranger.hazelcast.servicediscovery;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by phaneesh on 01/02/16.
 */
public class RangerDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

    private static Collection<PropertyDefinition> properties;

    public RangerDiscoveryStrategyFactory() {
        properties = new ArrayList<>();
        properties.add(RangerDiscoveryConfiguration.NAMESPACE);
        properties.add(RangerDiscoveryConfiguration.PORT);
        properties.add(RangerDiscoveryConfiguration.SERVICE_NAME);
        properties.add(RangerDiscoveryConfiguration.ZK_CONNECTION_STRING);
    }

    @Override
    public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
        return RangerDiscoveryStrategy.class;
    }

    @Override
    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode, ILogger logger, Map<String, Comparable> properties) {
        return new RangerDiscoveryStrategy(logger, properties);
    }

    @Override
    public Collection<PropertyDefinition> getConfigurationProperties() {
        return properties;
    }
}
