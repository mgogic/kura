/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.device;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This interface Validable provides methods for
 * validation of payloads sent in POST requests.
 * Checks is payload sent in request formed properly
 */
public interface Validable {

    /**
     * Checks is payload sent in request formed properly
     * 
     * @return boolean is request valid
     */
    public boolean isValid();

    /**
     * Method for checking is requests sent for validation null. If
     * request is not null method calls isValid() method in order to
     * check is request well formed
     * 
     * @param validable
     *            Object for validation.
     * @return if validable is null method returns false
     *         else, method calls isValid() to check is request well formed
     */
    public static boolean isValid(Validable validable) {
        if (validable == null) {
            return false;
        }
        return validable.isValid();
    }

    /**
     * Validate request. Method calls isValid() method, and if request is malformed
     * returns BAD_REQUEST status code with exception message
     * 
     * @param validable
     *            Object for validation
     * @param exceptionMessage
     *            Message explaining bad request
     */
    public static void validate(Validable validable, String exceptionMessage) {
        if (!isValid(validable)) {
            throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(exceptionMessage).type(MediaType.TEXT_PLAIN).build());
        }
    }
}
