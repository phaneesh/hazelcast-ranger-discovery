package com.ranger.hazelcast.servicediscovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.zookeeper.serde.ZkNodeDataSerializer;

public class JacksonDataSerializer implements ZkNodeDataSerializer<UnshardedClusterInfo> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public byte[] serialize(ServiceNode<UnshardedClusterInfo> serviceNode) {
        try {
            return objectMapper.writeValueAsBytes(serviceNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
