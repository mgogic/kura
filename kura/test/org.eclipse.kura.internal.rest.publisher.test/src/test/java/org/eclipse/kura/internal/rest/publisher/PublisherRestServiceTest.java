/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.rest.publisher;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;

public class PublisherRestServiceTest {

    @Test(expected = WebApplicationException.class)
    public void testEmptyPublishRequestValidationException() throws KuraException, NoSuchFieldException {
        // test publish with empty list of metrics

        PublisherRestService prs = new PublisherRestService();
        PublishRequest request = new PublishRequest();
        List<Metric> metrics = new ArrayList<>();

        TestUtil.setFieldValue(request, "metrics", metrics);

        prs.read(request);
    }

    @Test(expected = WebApplicationException.class)
    public void testNullPublishRequestValidationException() throws KuraException {
        // test publish with empty request

        PublisherRestService prs = new PublisherRestService();
        PublishRequest request = null;

        prs.read(request);
    }

    @Test(expected = WebApplicationException.class)
    public void testIncorrectRequestFormValidationException() throws KuraException, NoSuchFieldException {
        // test publish with incorrect request form

        PublisherRestService prs = new PublisherRestService();
        PublishRequest request = new PublishRequest();
        List<Metric> metrics = new ArrayList<>();
        Metric metric = new Metric();

        TestUtil.setFieldValue(metric, "name", "temperature");
        TestUtil.setFieldValue(metric, "type", null);
        TestUtil.setFieldValue(metric, "value", 5);
        metrics.add(metric);

        TestUtil.setFieldValue(request, "metrics", metrics);

        prs.read(request);
    }

}
