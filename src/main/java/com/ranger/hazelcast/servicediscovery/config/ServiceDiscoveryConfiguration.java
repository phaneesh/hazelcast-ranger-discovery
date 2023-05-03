package com.ranger.hazelcast.servicediscovery.config;

import com.google.common.base.Strings;
import lombok.*;

/**
 * Ranger configuration.
 */
@Data
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class ServiceDiscoveryConfiguration {

    private String namespace = Constants.DEFAULT_NAMESPACE;

    private String environment;

    private String zookeeper;

    private int connectionRetryIntervalMillis = Constants.DEFAULT_RETRY_CONN_INTERVAL;

    private String publishedHost = Constants.DEFAULT_HOST;

    private int publishedPort = Constants.DEFAULT_PORT;

    private int refreshTimeMs;

    private boolean disableWatchers;

    private long initialDelaySeconds;

    private boolean initialRotationStatus = true;

    private int dropwizardCheckStaleness;

    @Builder
    public ServiceDiscoveryConfiguration(
            String namespace,
            String environment,
            String zookeeper,
            int connectionRetryIntervalMillis,
            String publishedHost,
            int publishedPort,
            int refreshTimeMs,
            boolean disableWatchers,
            long initialDelaySeconds,
            boolean initialRotationStatus) {
        this.namespace = Strings.isNullOrEmpty(namespace)
                ? Constants.DEFAULT_NAMESPACE
                : namespace;
        this.environment = environment;
        this.zookeeper = zookeeper;
        this.connectionRetryIntervalMillis = connectionRetryIntervalMillis == 0
                ? Constants.DEFAULT_RETRY_CONN_INTERVAL
                : connectionRetryIntervalMillis;
        this.publishedHost = Strings.isNullOrEmpty(publishedHost)
                ? Constants.DEFAULT_HOST
                : publishedHost;
        this.publishedPort = publishedPort == 0
                ? Constants.DEFAULT_PORT
                : publishedPort;
        this.refreshTimeMs = refreshTimeMs == 0 ? Constants.DEFAULT_REFRESH_TIME : refreshTimeMs;
        this.disableWatchers = disableWatchers;
        this.initialDelaySeconds = initialDelaySeconds;
        this.initialRotationStatus = initialRotationStatus;
    }
}