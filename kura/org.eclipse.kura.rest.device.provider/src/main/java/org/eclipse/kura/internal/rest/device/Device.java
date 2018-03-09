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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Device {

    @SerializedName("deviceID")
    @Expose
    private String deviceID;
    @SerializedName("componentID")
    @Expose
    private String componentID;
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("unit")
    @Expose
    private String unit;
    @SerializedName("lastUpdate")
    @Expose
    private String lastUpdate;
    @SerializedName("format")
    @Expose
    private String format;

    public Device(String deviceID, String componentID, String value, String unit, String format, String lastUpdate) {
        super();
        this.deviceID = deviceID;
        this.componentID = componentID;
        this.value = value;
        this.unit = unit;
        this.format = format;
        this.lastUpdate = lastUpdate;
    }

    public String getComponentID() {
        return componentID;
    }

    public void setComponentID(String componentID) {
        this.componentID = componentID;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

}
