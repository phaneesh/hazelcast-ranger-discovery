package com.ranger.hazelcast.servicediscovery;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.curator.test.TestingCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class DiscoveryTest {

    private TestingCluster testingCluster;

    @Before
    public void startTestCluster() throws Exception {
        testingCluster = new TestingCluster(3);
        testingCluster.start();
    }

    @After
    public void stopTestCluster() throws Exception {
        if(null != testingCluster) {
            testingCluster.close();
        }
    }

    @Test
    public void testDiscovery() throws IOException {
        Config config = new Config();
        config.setProperty("hazelcast.discovery.enabled", "true");
        NetworkConfig networkConfig = config.getNetworkConfig();
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getTcpIpConfig().setEnabled(false);
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);
        DiscoveryConfig discoveryConfig = joinConfig.getDiscoveryConfig();
        DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(new RangerDiscoveryStrategyFactory());
        discoveryStrategyConfig.addProperty("zk-connection-string", testingCluster.getConnectString());
        discoveryStrategyConfig.addProperty("namespace", "hz_disco");
        discoveryStrategyConfig.addProperty("service-name", "hz_disco_test");
        discoveryStrategyConfig.addProperty("port", "5701");
        discoveryConfig.addDiscoveryStrategyConfig(discoveryStrategyConfig);
        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);
        assertTrue(hazelcast.getCluster().getMembers().size() > 0);
    }
}
