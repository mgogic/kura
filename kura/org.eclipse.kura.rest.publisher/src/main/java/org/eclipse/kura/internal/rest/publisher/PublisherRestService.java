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
public class PublisherRestService {

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

    private static final String BAD_PUBLISH_REQUEST_ERROR_MESSAGE = "Bad request, expected request format:{\"metric_name1\":value, \"metric_name2\":value}";
    private static final String BAD_REQUEST_METRIC_VALUE_TYPE_ERROR_MESSAGE = "Bad request. Allowed metric values: String, Number and Boolean";
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

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    @POST
    @RolesAllowed("assets")
    @Path("/publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonElement read(Map<String, Object> publishRequest) throws KuraException {
        logger.debug("Request:" + publishRequest.toString());
        validateRequest(publishRequest);

        // fetch the publishing configuration from the publishing properties
        String topic = "data/metrics";
        Integer qos = 0;
        Boolean retain = false;

        // Allocate a new payload
        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());

        // Add metrics to the payload
        for (Map.Entry<String, Object> metric : publishRequest.entrySet()) {
            payload.addMetric(metric.getKey(), metric.getValue());
        }

        // Publish the message
        int messageId = 42;
        try {
            if (nonNull(this.cloudService) && nonNull(this.cloudClient)) {
                messageId = this.cloudClient.publish(topic, payload, qos, retain);
                logger.info("Published to {} message: {} with ID: {}", new Object[] { topic, payload, messageId });
            }
        } catch (Exception e) {
            logger.error("Cannot publish topic: {}", topic, e);
        }

        return getChannelSerializer().toJsonTree(messageId);
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

    private void closeCloudClient() {
        if (nonNull(this.cloudClient)) {
            this.cloudClient.release();
            this.cloudClient = null;
        }
    }

    private void setupCloudClient() throws KuraException {
        closeCloudClient();
        // create the new CloudClient for the specified application
        final String appId = this.publisherOptions.getAppId();
        this.cloudClient = this.cloudService.newCloudClient(appId);
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

    private void validateRequest(Map<String, Object> metrics) {
        // Check if request is empty or equals null
        if (metrics == null || metrics.isEmpty()) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity(BAD_PUBLISH_REQUEST_ERROR_MESSAGE).type(MediaType.TEXT_PLAIN).build());
        }

        // Check if metric values are of types String, Number, Boolean and are not Objects
        Object value;
        for (Map.Entry<String, Object> metric : metrics.entrySet()) {
            value = metric.getValue();
            if (!(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean)) {
                throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                        .entity(BAD_REQUEST_METRIC_VALUE_TYPE_ERROR_MESSAGE).type(MediaType.TEXT_PLAIN).build());
            }
        }
    }
}
