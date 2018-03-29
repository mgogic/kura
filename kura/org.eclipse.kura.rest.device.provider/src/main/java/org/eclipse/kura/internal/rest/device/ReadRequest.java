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

import java.util.Set;

/**
 * ReadRequset represents payload that one needs to send
 * in POST request in order to successfully perform read
 * operation on device's components. ReadRequest implements
 * Validable interface to check is payload well formed
 * 
 * @see org.eclipse.kura.internal.rest.device.Validable
 */
public class ReadRequest implements Validable {

    /**
     * Set of channel names sent in payload on which
     * read command will be performed
     */
    private Set<String> channels;

    /**
     * Method returns set of channel names sent
     * in payload in POST request
     * 
     * @return set of channel names sent in payload
     */
    public Set<String> getChannelNames() {
        return channels;
    }

    /*
     * Implementation of isValid() method from Validable interface
     * 
     * @return boolean - true if channels are not equal to null
     * 
     * @see org.eclipse.kura.internal.rest.device.Validable#isValid()
     */
    @Override
    public boolean isValid() {
        return channels != null;
    }
}
