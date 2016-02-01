# Hazelcast Ranger Discovery [![Travis build status](https://travis-ci.org/phaneesh/hazelcast-ranger-discovery.svg?branch=master)](https://travis-ci.org/phaneesh/hazelcast-ranger-discovery)

This is a discovery strategy extension for Hazelcast to make discovery work on [ranger](https://github.com/flipkart-incubator/ranger).
This library compiles only on Java 8.
 
## Dependencies
* ranger 0.2.1  

## Usage
Hazelcast Ranger Discovery provides a easy way to enable member discovery with elastic applications on docker & DCOS 
like environment where using a static host list or using multicast based discovery is not possible.
 
### Build instructions
  - Clone the source:

        git clone github.com/phaneesh/hazelcast-ranger-discovery

  - Build

        mvn install

### Maven Dependency
Use the following repository:
```xml
<repository>
    <id>clojars</id>
    <name>Clojars repository</name>
    <url>https://clojars.org/repo</url>
</repository>
```
Use the following maven dependency:
```xml
<dependency>
    <groupId>com.ranger.hazelcast.servicediscovery</groupId>
    <artifactId>hazelcast-ranger-discovery</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Using Hazelcast Ranger Discovery
```java
Config config = new Config();
//This is important to enable the discovery strategy
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
```

LICENSE
-------

Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.