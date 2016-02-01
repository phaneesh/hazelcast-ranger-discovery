package com.ranger.hazelcast.servicediscovery;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by phaneesh on 01/02/16.
 */
public class RangerDiscoveryStrategy extends AbstractDiscoveryStrategy {

    private String zkConnectionString;

    private String namespace;

    private String serviceName;

    private int port;

    private ILogger logger;

    public RangerDiscoveryStrategy(final ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        this.zkConnectionString = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.ZK_CONNECTION_STRING);
        this.namespace = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.NAMESPACE);
        this.serviceName = getOrNull("discovery.ranger", RangerDiscoveryConfiguration.SERVICE_NAME);
        this.port = Integer.parseInt(getOrNull("discovery.ranger", RangerDiscoveryConfiguration.PORT));
        this.logger = logger;
        try {
            RangerServiceDiscoveryHelper.start(zkConnectionString, namespace, serviceName, InetAddress.getLocalHost().getHostAddress(), port, logger);
        } catch (Exception e) {
           logger.severe("Failed to start service discovery!", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    RangerServiceDiscoveryHelper.stop();
                } catch (Exception e) {
                    logger.severe("Error adding shutdown hook!", e);
                }
            }
        });
    }

    public Iterable<DiscoveryNode> discoverNodes() {
        return RangerServiceDiscoveryHelper.getAllNodes().stream().map( n -> {
            Map<String, Object> attributes = Collections.<String, Object>singletonMap("hostname", n.getHost());
            try {
                return new SimpleDiscoveryNode(new Address(n.getHost(), n.getPort()), attributes);
            } catch (UnknownHostException e) {
                logger.severe("Error adding discovered member", e);
                return null;
            }
        }).collect(Collectors.toList());
    }
}
