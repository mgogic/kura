/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.rest.publisher;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class PublisherRestServiceTest {

    @Test(expected = WebApplicationException.class)
    public void testEmptyPublishRequestValidationException() throws KuraException, NoSuchFieldException {
        // test publish with empty list of metrics

        PublisherRestService prs = new PublisherRestService();
        PublishRequest request = new PublishRequest();
        List<Metric> metrics = new ArrayList<>();

        request.setMetrics(metrics);

        prs.publish(request);
    }

    @Test(expected = WebApplicationException.class)
    public void testNullPublishRequestValidationException() throws KuraException {
        // test publish with empty request

        PublisherRestService prs = new PublisherRestService();
        PublishRequest request = null;

        prs.publish(request);
    }

    @Test(expected = WebApplicationException.class)
    public void testIncorrectRequestFormValidationException() throws KuraException, NoSuchFieldException {
        // test publish with incorrect request form

        PublisherRestService prs = new PublisherRestService();
        PublishRequest request = new PublishRequest();
        List<Metric> metrics = new ArrayList<>();
        Metric metric = new Metric();

        metric.setName("temperature");
        metric.setType(null);
        metric.setValue(5);
        metrics.add(metric);

        request.setMetrics(metrics);

        prs.publish(request);
    }

    @Test
    public void testSuccessfulPublish() throws KuraException, NoSuchFieldException {
        // test successful publish

        PublisherRestService prs = new PublisherRestService();

        CloudService csMock = mock(CloudService.class);
        CloudClient ccMock = mock(CloudClient.class);

        Map<String, Object> properties = new HashMap<>();
        properties.put("publish.appTopic", "data/metrics");
        properties.put("publish.qos", 0);
        properties.put("publish.retain", false);

        PublisherOptions po = new PublisherOptions(properties);

        TestUtil.setFieldValue(prs, "cloudService", csMock);
        TestUtil.setFieldValue(prs, "cloudClient", ccMock);
        TestUtil.setFieldValue(prs, "publisherOptions", po);

        PublishRequest request = new PublishRequest();
        List<Metric> metrics = new ArrayList<>();
        Metric metric = new Metric();

        metric.setName("temperature");
        metric.setType("int");
        metric.setValue(8.0);
        metrics.add(metric);

        request.setMetrics(metrics);

        when(ccMock.publish(eq("data/metrics"), anyObject(), eq(0), eq(false))).thenReturn(5);

        JsonElement json = prs.publish(request);

        ArgumentCaptor<KuraPayload> argument = ArgumentCaptor.forClass(KuraPayload.class);
        verify(ccMock).publish(eq("data/metrics"), argument.capture(), eq(0), eq(false));
        assertEquals(8, argument.getValue().getMetric("temperature"));

        assertEquals(new JsonPrimitive(5).getAsString(), json.getAsString());
    }

}
