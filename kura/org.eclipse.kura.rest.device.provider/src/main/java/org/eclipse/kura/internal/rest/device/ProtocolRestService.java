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

import static org.eclipse.kura.internal.rest.device.Validable.validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.DriverService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.google.gson.Gson;

@Path("/protocol")
public class ProtocolRestService {

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: {\"channels\": [{\"name\": \"channel-1\", \"type\": \"BOOLEAN\", \"value\": true }]}";

    private DriverService driverService;
    private AssetService assetService;
    private Response noContentResponse = Response.noContent().build();

    protected void setDriverService(DriverService driverService) {
        this.driverService = driverService;
    }

    protected void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    protected Collection<ServiceReference<Driver>> getDeviceServiceReferences() throws InvalidSyntaxException {

        return FrameworkUtil.getBundle(ProtocolRestService.class).getBundleContext().getServiceReferences(Driver.class,
                null);
    }

    @POST
    @RolesAllowed("assets")
    @Path("/{protocolId}/connection/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response connectToDevice(@PathParam("protocolId") String protocolId,
            @PathParam("deviceId") String deviceId) {

        // Check if device and protocol actually exists
        checkIfDeviceAndProtocolExists(protocolId, deviceId);

        return Response.ok().entity("Connection initialized.").build();
    }

    @DELETE
    @RolesAllowed("assets")
    @Path("/{protocolId}/connection/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnectFromDevice(@PathParam("protocolId") String protocolId,
            @PathParam("deviceId") String deviceId) {

        // Check if device and protocol actually exists
        checkIfDeviceAndProtocolExists(protocolId, deviceId);

        return Response.ok().entity("Disconnection completed.").build();
    }

    @POST
    @RolesAllowed("assets")
    @Path("/{protocolId}/{deviceId}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response writeViaProtocol(@PathParam("protocolId") String protocolId, @PathParam("deviceId") String deviceId,
            WriteRequestList writeRequestList) throws ConnectionException {

        final Driver driver = getDriver(protocolId);
        final Asset asset = getAsset(deviceId);
        boolean isConnected = isDeviceConnectedViaProtocol(asset, protocolId);
        List<String> channelNamesFromRequest = new ArrayList<>();

        if (!isConnected) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("This combination of device and protocol is not available.").build();
        }
        List<String> existingChannelNames = new ArrayList<>(asset.getAssetConfiguration().getAssetChannels().keySet());
        validate(writeRequestList, BAD_WRITE_REQUEST_ERROR_MESSAGE);

        final List<ChannelRecord> records = writeRequestList.getRequests().stream().map(req -> req.toChannelRecord())
                .collect(Collectors.toList());
        for (ChannelRecord record : records) {
            channelNamesFromRequest.add(record.getChannelName());
        }

        if (existingChannelNames.containsAll(channelNamesFromRequest)) {
            driver.write(records);

            return Response.ok("Write succeeded").build();
        }

        return noContentResponse;
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{protocolId}/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readViaProtocol(@PathParam("protocolId") String protocolId, @PathParam("deviceId") String deviceId)
            throws KuraException {

        String data = null;
        Gson gson = new Gson();

        checkIfDeviceAndProtocolExists(protocolId, deviceId);
        final Asset asset = getAsset(deviceId);
        if (!isDeviceConnectedViaProtocol(asset, protocolId)) {

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("This combination of device and protocol is not available.").build();
        }
        List<Device> deviceList = new ArrayList<>();
        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();
        List<ChannelRecord> records = asset.read(channelsList.keySet());

        for (ChannelRecord channelRecord : records) {

            String value = channelRecord.getValue().getValue().toString();
            deviceList.add(new Device(assetService.getAssetPid(asset), channelRecord.getChannelName(), value.toString(),
                    "", Long.toString(channelRecord.getTimestamp()), ""));
        }
        data = gson.toJson(deviceList);

        return Response.ok(data, MediaType.APPLICATION_JSON).build();
    }

    private Driver getDriver(String protocolId) {
        final Driver driver = driverService.getDriver(protocolId);

        if (driver == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                    .entity("Protocol not found: " + protocolId).build());
        }

        return driver;
    }

    private Asset getAsset(String deviceId) {
        final Asset asset = assetService.getAsset(deviceId);

        if (asset == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                    .entity("Device not found: " + deviceId).build());
        }

        return asset;
    }

    protected boolean isDeviceConnectedViaProtocol(Asset asset, String protocolId) {
        boolean isConnected = false;

        if (asset.getAssetConfiguration().getDriverPid().equals(protocolId)) {
            isConnected = true;
        }

        return isConnected;
    }

    private void checkIfDeviceAndProtocolExists(String protocolId, String deviceId) {
        getDriver(protocolId);
        getAsset(deviceId);
    }

}
