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

/**
 * Class represents Kura device
 * 
 * Data from Kura devices are stored in objects of Device class
 *
 */

public class Device {

    /**
     * deviceID field represents Device name
     */
    private String deviceID;
    /**
     * componentID field represents device's channel ID
     */
    private String componentID;
    /**
     * value field represents data / value of the channel's record
     */
    private String value;
    /**
     * unit field represents unit of measure of specific channel
     */
    private String unit;
    /**
     * lastUpdate field represents time stamp of last record fetched from component
     */
    private String lastUpdate;
    /**
     * format field represents format of records on component
     */
    private String format;

    /**
     * Constructor - Method for creating new Device object
     * that represents Kura device
     * 
     * @param deviceID
     * @param componentID
     * @param value
     * @param unit
     * @param format
     * @param lastUpdate
     */
    public Device(String deviceID, String componentID, String value, String unit, String lastUpdate, String format) {
        this.deviceID = deviceID;
        this.componentID = componentID;
        this.value = value;
        this.unit = unit;
        this.lastUpdate = lastUpdate;
        this.format = format;
    }

    /**
     * Method returns device name
     * 
     * @return
     */
    public String getDeviceID() {
        return deviceID;
    }

    /**
     * Method sets device name value
     * 
     * @param deviceID
     */
    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    /**
     * Method returns component name
     * 
     * @return
     */
    public String getComponentID() {
        return componentID;
    }

    /**
     * Method sets component name value
     * 
     * @param componentID
     */
    public void setComponentID(String componentID) {
        this.componentID = componentID;
    }

    /**
     * Method returns data from device component object
     * 
     * @return
     */
    public String getValue() {
        return value;
    }

    /**
     * Method sets data on device component object
     * 
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Method returns unit of measure for device channel
     * 
     * @return
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Method sets unit of measure for device channel
     * 
     * @param unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Method returns time stamp of last record fetched from component
     * 
     * @return
     */
    public String getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Method sets time stamp of last record fetched from component
     * 
     * @param lastUpdate
     */
    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Method returns format of records from component
     * 
     * @return
     */
    public String getFormat() {
        return format;
    }

    /**
     * Method sets format of records on component
     * 
     * @param format
     */
    public void setFormat(String format) {
        this.format = format;
    }

}
