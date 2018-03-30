/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.rest.device;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class DeviceRestServiceTest {

    Logger logger = LoggerFactory.getLogger(DeviceRestServiceTest.class);

    @Test(expected = WebApplicationException.class)
    public void testGetAssetChannelsAssetNotFound() throws KuraException {
        // test unsuccessful retrieval of channels due to asset not being found

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = null;
        when(asMock.getAsset(pid)).thenReturn(asset);

        svc.getDeviceChannels(pid);
    }

    @Test
    public void testGetDeviceStatus() {
        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";
        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        Response testRes = svc.getDeviceStatus(pid);
        JsonObject json = (JsonObject) testRes.getEntity();
        JsonObject testJson = new JsonObject();
        testJson.addProperty("status", "CONNECTED");
        assertEquals(testJson.toString(), json.toString());
    }

    @Test
    public void testConnectDevice() {

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        Response testRes = svc.connectDevice(pid);
        String data = (String) testRes.getEntity();

        assertEquals("Device connected.", data);
    }

    @Test(expected = WebApplicationException.class)
    public void testSubscribeToComponent() throws KuraException {
        // test unsuccessful retrieval of channels due to asset not being found

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";
        String componentId = "cid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        svc.subscribeToComponent(pid, componentId);
    }

    private void assertChannelWrite(List<ChannelRecord> records, int idx, String channel, TypedValue value) {
        ChannelRecord channelRecord = records.get(idx);
        assertEquals(channel, channelRecord.getChannelName());
        assertEquals(value, channelRecord.getValue());

        channelRecord.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
    }

    private void addRequest(List<WriteRequest> requests, String name, DataType type, String value)
            throws NoSuchFieldException {

        WriteRequest request = new WriteRequest();
        TestUtil.setFieldValue(request, "name", name);
        TestUtil.setFieldValue(request, "type", type);
        TestUtil.setFieldValue(request, "value", value);
        requests.add(request);
    }

}
