/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.publisher;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

final class PublisherOptions {

    // Cloud Application identifier
    private static final String DEFAULT_APP_ID = "REST_PUBLISHER";
    private static final String DEFAULT_CLOUD_SERVICE_PID = "org.eclipse.kura.cloud.CloudService";
    private static final int DEFAULT_PUBLISH_QOS = 0;
    private static final boolean DEFAULT_PUBLISH_RETAIN = false;
    private static final String DEFAULT_APP_TOPIC = "data/metrics";
    private static final int DEFAULT_PUBLISH_RATE = 1000;
    private static final int DEFAULT_TEMP_INIT = 10;
    private static final double DEFAULT_TEMP_INC = 0.1;

    // Publishing Property Names
    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.appTopic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    private static final String CLOUD_SERVICE_PROP_NAME = "cloud.service.pid";
    private static final String APP_ID_PROP_NAME = "app.id";
    private static final String PUBLISH_RATE_NAME = "publish.rate";
    private static final String TEMPARATURE_INITIAL_NAME = "metric.temperature.initial";
    private static final String TEMPARATURE_INCREMENT_NAME = "metric.temperature.increment";

    private final Map<String, Object> properties;

    PublisherOptions(final Map<String, Object> properties) {
        requireNonNull(properties);
        this.properties = properties;
    }

    String getCloudServicePid() {
        String cloudServicePid = DEFAULT_CLOUD_SERVICE_PID;
        Object configCloudServicePid = this.properties.get(CLOUD_SERVICE_PROP_NAME);
        if (nonNull(configCloudServicePid) && configCloudServicePid instanceof String) {
            cloudServicePid = (String) configCloudServicePid;
        }
        return cloudServicePid;
    }

    String getAppId() {
        String appId = DEFAULT_APP_ID;
        Object app = this.properties.get(APP_ID_PROP_NAME);
        if (nonNull(app) && app instanceof String) {
            appId = String.valueOf(app);
        }
        return appId;
    }

    int getPublishQos() {
        int publishQos = DEFAULT_PUBLISH_QOS;
        Object qos = this.properties.get(PUBLISH_QOS_PROP_NAME);
        if (nonNull(qos) && qos instanceof Integer) {
            publishQos = (int) qos;
        }
        return publishQos;
    }

    boolean getPublishRetain() {
        boolean publishRetain = DEFAULT_PUBLISH_RETAIN;
        Object retain = this.properties.get(PUBLISH_RETAIN_PROP_NAME);
        if (nonNull(retain) && retain instanceof Boolean) {
            publishRetain = (boolean) retain;
        }
        return publishRetain;
    }

    String getAppTopic() {
        String appTopic = DEFAULT_APP_TOPIC;
        Object topic = this.properties.get(PUBLISH_TOPIC_PROP_NAME);
        if (nonNull(topic) && topic instanceof String) {
            appTopic = String.valueOf(topic);
        }
        return appTopic;
    }
    
    int getPublishRate() {
        int publishRate = DEFAULT_PUBLISH_RATE;
        Object rate = this.properties.get(PUBLISH_RATE_NAME);
        if (nonNull(rate) && rate instanceof String) {
            publishRate = (int) rate;
        }
        return publishRate;
    }
    
    int getTemperatureInit() {
        int tempInit = DEFAULT_TEMP_INIT;
        Object init = this.properties.get(TEMPARATURE_INITIAL_NAME);
        if (nonNull(init) && init instanceof String) {
            tempInit = (int) init;
        }
        return tempInit;
    }
    
    double getTemperatureInc() {
        double tempInc = DEFAULT_TEMP_INC;
        Object inc = this.properties.get(TEMPARATURE_INCREMENT_NAME);
        if (nonNull(inc) && inc instanceof String) {
            tempInc = (double) inc;
        }
        return tempInc;
    }

}