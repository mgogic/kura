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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

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
    public void testGetDeviceChannels() throws KuraException {

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        List<Device> mockDeviceList = new ArrayList<>();
        Map<String, Channel> mockChannelsList = new TreeMap<>();
        Map<String, Object> channelConfig = new HashMap<>();

        Channel ch1 = new Channel("id1", ChannelType.READ, DataType.BOOLEAN, channelConfig);
        Channel ch2 = new Channel("id2", ChannelType.READ, DataType.BOOLEAN, channelConfig);

        mockChannelsList.put(ch1.getName(), ch1);
        mockChannelsList.put(ch2.getName(), ch2);

        List<ChannelRecord> records = new ArrayList<>();

        ChannelRecord record1 = ChannelRecord.createReadRecord(ch1.getName(), ch1.getValueType());
        record1.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record1.setValue(TypedValues.newBooleanValue(true));

        ChannelRecord record2 = ChannelRecord.createReadRecord(ch2.getName(), ch2.getValueType());
        record2.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record2.setValue(TypedValues.newBooleanValue(false));

        records.add(record1);
        records.add(record2);

        AssetConfiguration assetConfig = new AssetConfiguration("description", "driverPid", mockChannelsList);

        when(asset.getAssetConfiguration()).thenReturn(assetConfig);

        when(asset.read(mockChannelsList.keySet())).thenReturn(records);

        List<ChannelRecord> mockRecords = asset.read(mockChannelsList.keySet());

        for (ChannelRecord channelRecord : mockRecords) {

            channelRecord.setValue(TypedValues.newBooleanValue(true));
            channelRecord.setTimestamp(123456789);
            String value = channelRecord.getValue().getValue().toString();
            mockDeviceList.add(new Device(asMock.getAssetPid(asset), channelRecord.getChannelName(), value.toString(),
                    "", Long.toString(channelRecord.getTimestamp()), ""));

        }

        Response testResponse = Response
                .ok(getChannelSerializer().toJsonTree(mockDeviceList), MediaType.APPLICATION_JSON).build();

        Response expectedResponse = svc.getDeviceChannels(pid);

        assertEquals(testResponse.getEntity(), expectedResponse.getEntity());
        assertEquals(testResponse.getStatus(), expectedResponse.getStatus());

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
        assertEquals(200, testRes.getStatus());
    }

    @Test
    public void testDisconnectDevice() {

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        Response testRes = svc.disconnectDevice(pid);
        String data = (String) testRes.getEntity();

        assertEquals("Device disconnected", data);
        assertEquals(200, testRes.getStatus());
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

    @Test
    public void testReadDataFromChannel() throws KuraException {
        // test for successful read (Response code 200) from channel/component,
        // and for lastChannelUpdate

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";
        String componentId = "cid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Device mockDevice = null;

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);
        when(asMock.getAssetPid(asset)).thenReturn(pid);

        Map<String, Channel> mockChannelsList = new TreeMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel ch1 = new Channel(componentId, ChannelType.READ, DataType.BOOLEAN, channelConfig);

        mockChannelsList.put(ch1.getName(), ch1);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord(ch1.getName(), ch1.getValueType());
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newBooleanValue(true));
        records.add(record);
        AssetConfiguration assetConfig = new AssetConfiguration("description", "driverPid", mockChannelsList);

        when(asset.getAssetConfiguration()).thenReturn(assetConfig);

        when(asset.read(mockChannelsList.keySet())).thenReturn(records);

        List<ChannelRecord> mockRecords = asset.read(mockChannelsList.keySet());
        for (ChannelRecord channelRecord : mockRecords) {
            if (channelRecord.getChannelName().equals(componentId)) {
                channelRecord.setValue(TypedValues.newBooleanValue(true));
                channelRecord.setTimestamp(123456789);
                String value = channelRecord.getValue().getValue().toString();
                mockDevice = new Device(asMock.getAssetPid(asset), channelRecord.getChannelName(), value.toString(), "",
                        Long.toString(channelRecord.getTimestamp()), "");
            }
        }

        Response testResponse = Response.ok(getChannelSerializer().toJsonTree(mockDevice), MediaType.APPLICATION_JSON)
                .build();
        Response expectedResponse = svc.readDataFromChannel(pid, componentId);

        assertEquals(testResponse.getEntity(), expectedResponse.getEntity());
        assertEquals(testResponse.getStatus(), expectedResponse.getStatus());

        Response notFoundResponse = svc.readDataFromChannel(pid, "mockedId");

        // testForNotFound
        assertEquals(404, notFoundResponse.getStatus());

    }

    @Test
    public void testWriteDataToChannel() throws KuraException, NoSuchFieldException {

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";
        String componentId = "cid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        Map<String, Channel> mockChannelsList = new TreeMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel ch1 = new Channel("ch1", ChannelType.READ, DataType.BOOLEAN, channelConfig);

        mockChannelsList.put(ch1.getName(), ch1);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord(ch1.getName(), ch1.getValueType());
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newBooleanValue(true));
        records.add(record);

        AssetConfiguration assetConfig = new AssetConfiguration("description", "driverPid", mockChannelsList);

        when(asset.getAssetConfiguration()).thenReturn(assetConfig);

        List<WriteRequest> requests = new ArrayList<>();
        addRequest(requests, "ch1", DataType.BOOLEAN, "true");

        List<String> mockExistingChannelNames = new ArrayList<>(
                asset.getAssetConfiguration().getAssetChannels().keySet());

        WriteRequestList requestsMock = new WriteRequestList() {

            @Override
            public List<WriteRequest> getRequests() {
                return requests;
            }

            @Override
            public boolean isValid() {
                return true;
            }
        };
        String mockData = "{\"channels\": [{\"name\": \"ch1\", \"type\": \"BOOLEAN\", \"value\": true }]}";
        Response testResponse = Response.ok().entity("Action sent.").build();

        assertEquals(testResponse.getEntity(), svc.executeSpecificCommand(pid, "write", mockData).getEntity());

    }

    @Test
    public void testExecuteSpecificCommandRead() throws KuraException {
        // test selective channel read with invalid request

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        Map<String, Channel> mockChannelsList = new TreeMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel ch1 = new Channel("ch1", ChannelType.READ, DataType.BOOLEAN, channelConfig);

        mockChannelsList.put(ch1.getName(), ch1);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord(ch1.getName(), ch1.getValueType());
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newBooleanValue(true));
        records.add(record);

        AssetConfiguration assetConfig = new AssetConfiguration("description", "driverPid", mockChannelsList);

        when(asset.getAssetConfiguration()).thenReturn(assetConfig);

        when(asset.read(mockChannelsList.keySet())).thenReturn(records);

        String mockData = "{\"channels\": [ \"ch1\"]}";

        Response mockResponse = Response.status(Response.Status.OK).entity("Action sent.").build();
        Response expectedResponse = svc.executeSpecificCommand(pid, "read", mockData);

        assertEquals(mockResponse.getEntity(), expectedResponse.getEntity());
    }

    @Test
    public void testReadSelectedChannelsValidationException() throws KuraException {
        // test selective channel read with invalid request

        DeviceRestService svc = new DeviceRestService();

        String pid = "pid1";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(pid)).thenReturn(asset);

        Map<String, Channel> mockChannelsList = new TreeMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel ch1 = new Channel(pid, ChannelType.READ, DataType.BOOLEAN, channelConfig);

        mockChannelsList.put(ch1.getName(), ch1);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord(ch1.getName(), ch1.getValueType());
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setValue(TypedValues.newBooleanValue(true));
        records.add(record);

        AssetConfiguration assetConfig = new AssetConfiguration("description", "driverPid", mockChannelsList);

        when(asset.getAssetConfiguration()).thenReturn(assetConfig);

        when(asset.read(mockChannelsList.keySet())).thenReturn(records);
        List<String> existingChannelNames = new ArrayList<>(mockChannelsList.keySet());

        ReadRequest requestMock = mock(ReadRequest.class);
        try {
            svc.executeSpecificCommand(pid, "read", requestMock.toString());
            fail("Expected an exception.");
        } catch (WebApplicationException e) {
            // OK
        }

        // verify(requestMock).isValid();
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

    private Gson getChannelSerializer() {
        Gson channelSerializer = null;
        if (channelSerializer == null) {
            channelSerializer = new GsonBuilder().registerTypeAdapter(TypedValue.class,
                    (JsonSerializer<TypedValue<?>>) (typedValue, type, context) -> {
                        final Object value = typedValue.getValue();
                        if (value instanceof Number) {
                            return new JsonPrimitive((Number) value);
                        } else if (value instanceof String) {
                            return new JsonPrimitive((String) value);
                        } else if (value instanceof Boolean) {
                            return new JsonPrimitive((Boolean) value);
                        }
                        return null;
                    }).create();
        }
        return channelSerializer;
    }

}
