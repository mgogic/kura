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

    // Publishing Property Names
    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.appTopic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    private static final String CLOUD_SERVICE_PROP_NAME = "cloud.service.pid";
    private static final String APP_ID_PROP_NAME = "app.id";

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
        Object app = this.properties.get(PUBLISH_TOPIC_PROP_NAME);
        if (nonNull(app) && app instanceof String) {
            appTopic = String.valueOf(app);
        }
        return appTopic;
    }

}