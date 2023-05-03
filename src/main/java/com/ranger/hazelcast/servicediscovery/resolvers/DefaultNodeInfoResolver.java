package com.ranger.hazelcast.servicediscovery.resolvers;

import com.ranger.hazelcast.servicediscovery.config.ServiceDiscoveryConfiguration;
import io.appform.ranger.common.server.ShardInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Collections;


@NoArgsConstructor
@Slf4j
public class DefaultNodeInfoResolver implements NodeInfoResolver {

    private static final String FARM_ID = "FARM_ID";

    @Override
    public ShardInfo resolve(ServiceDiscoveryConfiguration configuration) {
        val region = System.getenv(FARM_ID);
        log.debug("The region received from the env variable FARM_ID is {}. Setting the same in nodeInfo", region);
        return ShardInfo.builder()
                .environment(configuration.getEnvironment())
                .region(region)
                .tags(Collections.emptySet())
                .build();
    }
}