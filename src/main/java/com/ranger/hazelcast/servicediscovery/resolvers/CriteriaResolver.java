package com.ranger.hazelcast.servicediscovery.resolvers;

@FunctionalInterface
public interface CriteriaResolver<T, A> {

    T resolve(A args);

}
