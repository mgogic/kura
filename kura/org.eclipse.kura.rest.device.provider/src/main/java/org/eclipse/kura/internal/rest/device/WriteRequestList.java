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

import java.util.List;

/**
 * WriteRequestList class represents payload with list of device components
 * for performing write operation
 * 
 * @see org.eclipse.kura.internal.rest.device.WriteRequest
 */
public class WriteRequestList implements Validable {

    /**
     * List of WriteRequests sent in payload in POST request
     */
    private List<WriteRequest> channels;

    /**
     * Method returns list of WriteRequests objects sent in payload
     * 
     * @return list of WriteRequests sent in payload
     */
    public List<WriteRequest> getRequests() {
        return channels;
    }

    /**
     * Overrode isValid() method from Validable interface
     * Method checks if WriteRequests from list sent in payload are valid
     * 
     * @return method returns true only if name, type and value are not null
     * 
     * @see org.eclipse.kura.internal.rest.device.Validable#isValid()
     * @see org.eclipse.kura.internal.rest.device.WriteRequest#isValid()
     */
    @Override
    public boolean isValid() {
        if (channels == null) {
            return false;
        }
        for (WriteRequest request : channels) {
            if (!request.isValid()) {
                return false;
            }
        }
        return true;
    }
}
