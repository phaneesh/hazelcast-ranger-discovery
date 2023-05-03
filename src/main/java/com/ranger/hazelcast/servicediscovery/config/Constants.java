package com.ranger.hazelcast.servicediscovery.config;

import lombok.experimental.UtilityClass;

/**
 * Constants
 */
@UtilityClass
public class Constants {
    public static final String DEFAULT_NAMESPACE = "default";
    public static final String DEFAULT_HOST = "__DEFAULT_SERVICE_HOST";
    public static final int DEFAULT_PORT = -1;
    public static final int DEFAULT_DW_CHECK_INTERVAL = 15;
    public static final int DEFAULT_RETRY_CONN_INTERVAL = 5000;
    public static final int DEFAULT_REFRESH_TIME = 5000;
}
