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

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.TypedValue;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

@Path("/publish")
public class PublisherRestService implements ConfigurableComponent {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class CloudPublisherServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudService, CloudService> {

        @Override
        public CloudService addingService(final ServiceReference<CloudService> reference) {
            PublisherRestService.this.cloudService = PublisherRestService.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error("Cloud Client setup failed!", e);
            }
            return PublisherRestService.this.cloudService;
        }

        @Override
        public void modifiedService(final ServiceReference<CloudService> reference, final CloudService service) {
            PublisherRestService.this.cloudService = PublisherRestService.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error("Cloud Client setup failed!", e);
            }
        }

        @Override
        public void removedService(final ServiceReference<CloudService> reference, final CloudService service) {
            PublisherRestService.this.cloudService = null;
        }
    }

    private static final Encoder BASE64_ENCODER = Base64.getEncoder();

    private static final Logger logger = LoggerFactory.getLogger(PublisherRestService.class);

    private ServiceTrackerCustomizer<CloudService, CloudService> cloudServiceTrackerCustomizer;
    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;
    private CloudService cloudService;
    private CloudClient cloudClient;
    private String oldSubscriptionTopic;

    private Map<String, Object> properties;

    private BundleContext bundleContext;

    private PublisherOptions publisherOptions;

    private Gson channelSerializer;

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating PublisherRestService...");

        this.properties = properties;

        this.bundleContext = componentContext.getBundleContext();

        this.publisherOptions = new PublisherOptions(properties);

        this.cloudServiceTrackerCustomizer = new CloudPublisherServiceTrackerCustomizer();
        initCloudServiceTracking();

        logger.info("Activating PublisherRestService... Done.");
    }

    private void closeCloudClient() {
        if (nonNull(this.cloudClient)) {
            this.cloudClient.release();
            this.cloudClient = null;
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating PublisherRestService...");

        // Releasing the CloudApplicationClient
        logger.info("Releasing CloudApplicationClient for {}...", this.publisherOptions.getAppId());
        // close the client
        closeCloudClient();
        oldSubscriptionTopic = null;

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }

        logger.debug("Deactivating PublisherRestService... Done.");
    }

    private Gson getChannelSerializer() {
        if (channelSerializer == null) {
            channelSerializer = new GsonBuilder().registerTypeAdapter(TypedValue.class,
                    (JsonSerializer<TypedValue<?>>) (typedValue, type, context) -> {
                        final Object value = typedValue.getValue();
                        if (value instanceof Number) {
                            return new JsonPrimitive((Number) value);
                        } else if (value instanceof String) {
                            return new JsonPrimitive((String) value);
                        } else if (value instanceof byte[]) {
                            return new JsonPrimitive(BASE64_ENCODER.encodeToString((byte[]) value));
                        } else if (value instanceof Boolean) {
                            return new JsonPrimitive((Boolean) value);
                        }
                        return null;
                    }).create();
        }
        return channelSerializer;
    }

    private void initCloudServiceTracking() {
        String selectedCloudServicePid = this.publisherOptions.getCloudServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    @POST
    @RolesAllowed("assets")
    @Path("/publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonElement read(PublishRequest publishRequest) throws KuraException {
        logger.debug("Request:" + publishRequest.toString());
        publishRequest.validate();

        // fetch the publishing configuration from the publishing properties
        String topic = "data/metrics";
        Integer qos = 0;
        Boolean retain = false;

        // Allocate a new payload
        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());

        // Add metrics to the payload
        for (Metric metric : publishRequest.getRequestItems()) {
            if (metric.getValue() instanceof Number) {
                if ("int".equals(metric.getType())) {
                    payload.addMetric(metric.getName(), ((Double) metric.getValue()).intValue());
                } else if ("long".equals(metric.getType())) {
                    payload.addMetric(metric.getName(), ((Double) metric.getValue()).longValue());
                } else if ("double".equals(metric.getType())) {
                    payload.addMetric(metric.getName(), (Double) metric.getValue());
                } else if ("float".equals(metric.getType())) {
                    payload.addMetric(metric.getName(), ((Double) metric.getValue()).floatValue());
                }
            } else if (metric.getValue() instanceof String) {
                if ("base64binary".equals(metric.getType())) {
                    payload.addMetric(metric.getName(), String.valueOf(metric.getValue()).getBytes());
                } else {
                    payload.addMetric(metric.getName(), metric.getValue());
                }
            } else if (metric.getValue() instanceof Boolean) {
                payload.addMetric(metric.getName(), metric.getValue());
            }
        }

        if (payload.metrics().isEmpty()) {
            logger.warn("Payload is empty.");
        }

        // Publish the message
        int messageId = 0;
        try {
            if (nonNull(this.cloudService) && nonNull(this.cloudClient)) {
                messageId = this.cloudClient.publish(topic, payload, qos, retain);
                logger.info("Published to {} message: {} with ID: {}", new Object[] { topic, payload, messageId });
            }
        } catch (Exception e) {
            logger.error("Cannot publish topic: {}", topic, e);
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build());
        }

        return getChannelSerializer().toJsonTree(messageId);
    }

    private void setupCloudClient() throws KuraException {
        closeCloudClient();
        // create the new CloudClient for the specified application
        final String appId = this.publisherOptions.getAppId();
        this.cloudClient = this.cloudService.newCloudClient(appId);
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated PublisherRestService...");

        // store the properties received
        this.properties = properties;

        this.publisherOptions = new PublisherOptions(properties);

        if (nonNull(this.cloudServiceTracker)) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();

        logger.info("Updated PublisherRestService... Done.");
    }

}
