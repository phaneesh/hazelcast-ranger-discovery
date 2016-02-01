package com.ranger.hazelcast.servicediscovery;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.PropertyTypeConverter;
import com.hazelcast.config.properties.SimplePropertyDefinition;

public class RangerDiscoveryConfiguration {

    public static final PropertyDefinition ZK_CONNECTION_STRING = new SimplePropertyDefinition("zk-connection-string", PropertyTypeConverter.STRING);

    public static final PropertyDefinition NAMESPACE = new SimplePropertyDefinition("namespace", PropertyTypeConverter.STRING);

    public static final PropertyDefinition SERVICE_NAME = new SimplePropertyDefinition("service-name", PropertyTypeConverter.STRING);

    public static final PropertyDefinition PORT = new SimplePropertyDefinition("port", PropertyTypeConverter.STRING);

    private RangerDiscoveryConfiguration() {

    }
}
