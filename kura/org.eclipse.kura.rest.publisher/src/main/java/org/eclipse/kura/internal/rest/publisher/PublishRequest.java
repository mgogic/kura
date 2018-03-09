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

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class PublishRequest {

    private static final String BAD_PUBLISH_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: { \"metrics\": [ { \"name\" : \"...\", \"type\" : \"...\", \"value\" : \"...\" }, ... ] }";

    private List<Metric> metrics;

    public List<Metric> getRequestItems() {
        return this.metrics;
    }

    public void validate() {
        // Check if request is empty or equals null
        if (this.metrics == null || this.metrics.isEmpty()) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity(BAD_PUBLISH_REQUEST_ERROR_MESSAGE).type(MediaType.TEXT_PLAIN).build());
        }

        for (Metric metric : this.metrics) {
            if (metric.getName() == null || metric.getType() == null || metric.getValue() == null) {
                throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                        .entity(BAD_PUBLISH_REQUEST_ERROR_MESSAGE).type(MediaType.TEXT_PLAIN).build());
            }
        }
    }
}
