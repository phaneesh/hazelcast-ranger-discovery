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

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.curator.test.TestingCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;


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
    public void testSingleMemberDiscovery() throws IOException {
        HazelcastInstance hazelcast = getHazelcastInstance();
        assertTrue(hazelcast.getCluster().getMembers().size() > 0);
        hazelcast.shutdown();
    }

    @Test
    public void testMultiMemberDiscovery() throws UnknownHostException {
        HazelcastInstance hazelcast1 = getHazelcastInstance();
        HazelcastInstance hazelcast2 = getHazelcastInstance();
        HazelcastInstance hazelcast3 = getHazelcastInstance();
        assertTrue(hazelcast3.getCluster().getMembers().size() > 0);
        assertTrue(hazelcast3.getCluster().getMembers().size() == 3);
        hazelcast1.shutdown();
        hazelcast2.shutdown();
        hazelcast3.shutdown();
    }

    @Test
    public void testClientDiscovery() throws UnknownHostException {
        HazelcastInstance hazelcast1 = getHazelcastInstance();
        HazelcastInstance hazelcast2 = getHazelcastInstance();
        HazelcastInstance hazelcast3 = getHazelcastInstance();
        HazelcastInstance hazelcastInstance = getHazelcastClientInstance();
        assertTrue(hazelcastInstance.getCluster().getMembers().size() > 0);
        assertTrue(hazelcastInstance.getCluster().getMembers().size() == 3);
    }


    private HazelcastInstance getHazelcastInstance() throws UnknownHostException {
        Config config = new Config();
        config.setProperty("hazelcast.discovery.enabled", "true");
        config.setProperty("hazelcast.discovery.public.ip.enabled", "true");
        config.setProperty("hazelcast.socket.client.bind.any", "true");
        config.setProperty("hazelcast.socket.bind.any", "true");
        NetworkConfig networkConfig = config.getNetworkConfig();
        //networkConfig.setPublicAddress(InetAddress.getLocalHost().getHostAddress() + ":" + port);
        networkConfig.getInterfaces().addInterface(InetAddress.getLocalHost().getHostAddress()).setEnabled(true);
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getTcpIpConfig().setEnabled(false);
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);
        DiscoveryConfig discoveryConfig = joinConfig.getDiscoveryConfig();
        DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(new RangerDiscoveryStrategyFactory());
        discoveryStrategyConfig.addProperty("zk-connection-string", testingCluster.getConnectString());
        discoveryStrategyConfig.addProperty("namespace", "hz_disco");
        discoveryStrategyConfig.addProperty("service-name", "hz_disco_test");
        discoveryConfig.addDiscoveryStrategyConfig(discoveryStrategyConfig);
        return Hazelcast.newHazelcastInstance(config);
    }


    private HazelcastInstance getHazelcastClientInstance() throws UnknownHostException {
        ClientConfig config = new ClientConfig();
        config.setProperty("hazelcast.discovery.enabled", "true");
        config.setProperty("hazelcast.discovery.public.ip.enabled", "true");
        config.setProperty("hazelcast.socket.client.bind.any", "true");
        config.setProperty("hazelcast.socket.bind.any", "true");
        DiscoveryConfig discoveryConfig = config.getNetworkConfig().getDiscoveryConfig();
        DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(new RangerDiscoveryStrategyFactory());
        discoveryStrategyConfig.addProperty("zk-connection-string", testingCluster.getConnectString());
        discoveryStrategyConfig.addProperty("namespace", "hz_disco");
        discoveryStrategyConfig.addProperty("service-name", "hz_disco_test");
        discoveryConfig.addDiscoveryStrategyConfig(discoveryStrategyConfig);
        return HazelcastClient.newHazelcastClient(config);
    }
}
