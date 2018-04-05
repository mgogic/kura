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
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;

import com.google.gson.Gson;

public class ProtocolRestServiceTest {

    @Test(expected = WebApplicationException.class)
    public void testConnectToDeviceAssetAndDriverNotFound() throws KuraException {
        // test unsuccessful retrieval of channels due to asset not being found

        ProtocolRestService svc = new ProtocolRestService();

        String protocolId = "pid1";
        String deviceId = "pid2";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        DriverService driverMock = mock(DriverService.class);
        svc.setDriverService(driverMock);

        Driver driver = null;
        when(driverMock.getDriver(protocolId)).thenReturn(driver);

        Asset asset = null;
        when(asMock.getAsset(deviceId)).thenReturn(asset);

        svc.connectToDevice(protocolId, deviceId);
    }

    @Test
    public void testConnectToDevice() {

        ProtocolRestService svc = new ProtocolRestService();

        String protocolId = "pid1";
        String deviceId = "pid2";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        DriverService driverMock = mock(DriverService.class);
        svc.setDriverService(driverMock);

        Driver driver = mock(Driver.class);
        when(driverMock.getDriver(protocolId)).thenReturn(driver);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(deviceId)).thenReturn(asset);

        Response mockResponse = Response.ok().entity("Connection initialized.").build();

        // test response entity and status
        assertEquals(mockResponse.getEntity(), svc.connectToDevice(protocolId, deviceId).getEntity());
        assertEquals(mockResponse.getStatus(), svc.connectToDevice(protocolId, deviceId).getStatus());

    }

    @Test
    public void testDisconnectFromDevice() {

        ProtocolRestService svc = new ProtocolRestService();

        String protocolId = "pid1";
        String deviceId = "pid2";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        DriverService driverMock = mock(DriverService.class);
        svc.setDriverService(driverMock);

        Driver driver = mock(Driver.class);
        when(driverMock.getDriver(protocolId)).thenReturn(driver);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(deviceId)).thenReturn(asset);

        Response mockResponse = Response.ok().entity("Disconnection completed.").build();

        // test response entity and status
        assertEquals(mockResponse.getEntity(), svc.disconnectFromDevice(protocolId, deviceId).getEntity());
        assertEquals(mockResponse.getStatus(), svc.disconnectFromDevice(protocolId, deviceId).getStatus());

    }

    @Test
    public void testWriteViaProtocol() {

    }

    @Test
    public void readViaProtocol() throws KuraException {

        ProtocolRestService svc = new ProtocolRestService() {

            @Override
            protected boolean isDeviceConnectedViaProtocol(Asset asset, String protocolId) {

                return true;
            }
        };

        String protocolId = "pid1";
        String deviceId = "pid2";
        String componentId = "pid3";
        Gson gson = new Gson();

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        DriverService driverMock = mock(DriverService.class);
        svc.setDriverService(driverMock);

        Driver driver = mock(Driver.class);
        when(driverMock.getDriver(protocolId)).thenReturn(driver);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(deviceId)).thenReturn(asset);

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

        for (ChannelRecord channelRecord : records) {

            String value = channelRecord.getValue().getValue().toString();
            mockDeviceList.add(new Device(asMock.getAssetPid(asset), channelRecord.getChannelName(), value.toString(),
                    "", Long.toString(channelRecord.getTimestamp()), ""));
        }

        String mockData = gson.toJson(mockDeviceList);

        Response testResponse = Response.ok(mockData, MediaType.APPLICATION_JSON).build();

        assertEquals(testResponse.getEntity(), svc.readViaProtocol(protocolId, deviceId).getEntity());
        assertEquals(testResponse.getStatus(), svc.readViaProtocol(protocolId, deviceId).getStatus());
    }

    @Test
    public void testReadViaProtocolBadDeviceAndProtocolCombination() throws KuraException {

        ProtocolRestService svc = new ProtocolRestService();

        String protocolId = "pid1";
        String deviceId = "pid2";

        AssetService asMock = mock(AssetService.class);
        svc.setAssetService(asMock);

        DriverService driverMock = mock(DriverService.class);
        svc.setDriverService(driverMock);

        Driver driver = mock(Driver.class);
        when(driverMock.getDriver(protocolId)).thenReturn(driver);

        Asset asset = mock(Asset.class);
        when(asMock.getAsset(deviceId)).thenReturn(asset);

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

        AssetConfiguration assetConfig = new AssetConfiguration("description", "device", mockChannelsList);

        when(asset.getAssetConfiguration()).thenReturn(assetConfig);

        Response testResponse = Response.status(Response.Status.BAD_REQUEST)
                .entity("This combination of device and protocol is not available.").build();

        assertEquals(testResponse.getEntity(), svc.readViaProtocol(protocolId, deviceId).getEntity());
        assertEquals(testResponse.getStatus(), svc.readViaProtocol(protocolId, deviceId).getStatus());

    }

}