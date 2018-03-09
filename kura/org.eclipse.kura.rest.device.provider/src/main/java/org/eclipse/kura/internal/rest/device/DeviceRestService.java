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

import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
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
import org.eclipse.kura.type.TypedValue;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

@Path("/device")
public class DeviceRestService {

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: {\"channels\": [{\"name\": \"channel-1\", \"type\": \"INTEGER\", \"value\": 10 }]}";
    // private static final String BAD_READ_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: {
    // \"channels\": [ \"channel-1\", \"channel-2\"]}";
    private static final Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final Logger logger = LoggerFactory.getLogger(DeviceRestService.class);
    private AssetService assetService;
    private Gson channelSerializer;

    protected void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    protected Collection<ServiceReference<Asset>> getDeviceServiceReferences() throws InvalidSyntaxException {
        return FrameworkUtil.getBundle(DeviceRestService.class).getBundleContext().getServiceReferences(Asset.class,
                null);
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceChannels(@PathParam("deviceId") String deviceId)
            throws InvalidSyntaxException, KuraException {
        Asset asset = getAsset(deviceId);
        String data = null;
        data = getDeviceResponse(asset);
        if (data.isEmpty()) {
            return Response.status(204).entity("No data available.").build();
        }
        return Response.ok(data, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceStatus(@PathParam("deviceId") String deviceId) throws InvalidSyntaxException {

        String status = null;
        JsonObject statusObject = new JsonObject();
        final Asset asset = getAsset(deviceId);
        if (asset != null) {
            status = "CONNECTED";
        } else {
            return Response.noContent().build();
        }
        statusObject.addProperty("status", status);

        return Response.ok(statusObject, MediaType.APPLICATION_JSON).build();
    }

    // Need to return 204 - no content
    @POST
    @RolesAllowed("assets")
    @Path("{deviceId}/connection")
    @Produces(MediaType.APPLICATION_JSON)
    public Response connectDevice(@PathParam("deviceId") String deviceId) throws KuraException {
        final Asset asset = getAsset(deviceId);
        if (asset == null) {
            return Response.status(204).entity("No such device!").build();
        }
        JsonObject statusObject = new JsonObject();
        statusObject.addProperty("status", "connected");
        return Response.ok(statusObject, MediaType.APPLICATION_JSON).build();
    }

    // Need to return 204 - no content
    @DELETE
    @RolesAllowed("assets")
    @Path("{deviceId}/connection")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnectDevice(@PathParam("deviceId") String deviceId) throws KuraException {
        final Asset asset = getAsset(deviceId);
        if (asset == null) {
            return Response.status(204).entity("No such device!").build();
        }
        JsonObject statusObject = new JsonObject();
        statusObject.addProperty("status", "disconnected");
        return Response.ok(statusObject, MediaType.APPLICATION_JSON).build();
    }

    // @POST
    // @RolesAllowed("assets")
    // @Path("/{deviceId}/_read")
    // @Produces(MediaType.APPLICATION_JSON)
    // public JsonElement read(@PathParam("deviceId") String deviceId, ReadRequest readRequest) throws KuraException {
    // final Asset asset = getAsset(deviceId);
    // validate(readRequest, BAD_READ_REQUEST_ERROR_MESSAGE);
    // return getChannelSerializer().toJsonTree(asset.read(readRequest.getChannelNames()));
    // }

    // @POST
    // @RolesAllowed("assets")
    // @Path("/{deviceId}/execute/{command}")
    // @Produces(MediaType.APPLICATION_JSON)
    // public Response write(@PathParam("deviceId") String deviceId, @PathParam("command") String command)
    // throws KuraException {
    // if ("read".equals(command)) {
    // return Response.status(204).entity("Read is )
    // } else if ("write".equals(command)) {
    //
    // } else {
    // return Response.status(204).entity("Action sent, no response received").build();
    // }
    // final Asset asset = getAsset(deviceId);
    // final List<ChannelRecord> records = requests.getRequests().stream().map(request -> request.toChannelRecord())
    // .collect(Collectors.toList());
    // asset.write(records);
    // return getChannelSerializer().toJsonTree(records);
    // }

    @POST
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}/subscribe")
    @Produces(MediaType.TEXT_PLAIN)
    public String subscribeToComponent(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) {
        throw new WebApplicationException(Response.status(Response.Status.NOT_IMPLEMENTED).type(MediaType.TEXT_PLAIN)
                .entity("Subscribe is not implemented. ").build());
    }

    @DELETE
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}/subscribe")
    @Produces(MediaType.TEXT_PLAIN)
    public String unsubscribeFromComponent(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) {
        throw new WebApplicationException(Response.status(Response.Status.NOT_IMPLEMENTED).type(MediaType.TEXT_PLAIN)
                .entity("Unsubscribe is not implemented. ").build());
    }

    // If channel doesn't exist return 404
    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readDataFromChannel(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) throws KuraException {
        final Asset asset = getAsset(deviceId);
        Device device = null;
        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();
        List<ChannelRecord> records = asset.read(channelsList.keySet());
        for (ChannelRecord channelRecord : records) {
            if (channelRecord.getChannelName().equals(componentId)) {
                String value = channelRecord.getValue().getValue().toString();
                device = new Device(assetService.getAssetPid(asset), channelRecord.getChannelName(), value.toString(),
                        "", "", Long.toString(channelRecord.getTimestamp()));
                return Response.ok(getChannelSerializer().toJsonTree(device), MediaType.APPLICATION_JSON).build();
            }
        }
        return Response.noContent().build();
    }

    // Need to add WriteRequest to method
    @POST
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeDataToChannel(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) throws KuraException {
        final Asset asset = getAsset(deviceId);

        List<ChannelRecord> records = getDataFromSpecificChannel(asset, deviceId, componentId);
        if (records.size() == 0) {
            return Response.noContent().entity("Write sent, no results returned").build();
        }
        logger.trace("TRACE : " + records.toString());
        asset.write(records);
        return Response.ok("Write sent", MediaType.TEXT_PLAIN).build();
    }

    // Need to add 404 for non existing channel
    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}/lastUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readLastDataFromChannel(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) throws KuraException {
        final Asset asset = getAsset(deviceId);
        Device device = null;
        if (asset == null) {
            return Response.status(404).entity("Device not available").build();
        }

        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();
        List<ChannelRecord> records = asset.read(channelsList.keySet());
        for (ChannelRecord channelRecord : records) {
            if (channelRecord.getChannelName().equals(componentId)) {
                String value = channelRecord.getValue().getValue().toString();
                device = new Device(assetService.getAssetPid(asset), channelRecord.getChannelName(), value, "", "",
                        Long.toString(channelRecord.getTimestamp()));
                return Response.ok(getChannelSerializer().toJsonTree(device), MediaType.APPLICATION_JSON).build();
            }
        }
        return Response.noContent().entity("No data available").build();
    }

    // need to fix output
    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/lastUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readLastDataFromDevice(@PathParam("deviceId") String deviceId) throws KuraException {
        final Asset asset = getAsset(deviceId);

        if (asset == null) {
            return Response.status(404).entity("Device not available").build();
        }

        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();
        List<ChannelRecord> records = asset.read(channelsList.keySet());
        Device[] devices = new Device[records.size()];
        for (int i = 0; i < records.size(); i++) {
            String value = records.get(i).getValue().getValue().toString();
            devices[i] = new Device(assetService.getAssetPid(asset), records.get(i).getChannelName(), value, "", "",
                    Long.toString(records.get(i).getTimestamp()));

        }
        if (devices.length == 0) {
            return Response.noContent().entity("No data available").build();
        }
        return Response.ok(getChannelSerializer().toJsonTree(devices), MediaType.APPLICATION_JSON).build();

    }

    private String getDeviceResponse(final Asset asset) throws KuraException, InvalidSyntaxException {
        String data = null;
        Gson gson = new Gson();
        List<Device> deviceList = new ArrayList<Device>();
        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();
        List<String> channelNames = new ArrayList<String>(channelsList.keySet());
        logger.trace("NAMES : " + channelsList.keySet());
        logger.trace("CHANNELS LIST : " + channelsList.values());
        // Set<String> assetName = getAssetPid();
        List<ChannelRecord> records = asset.read(channelsList.keySet());

        for (int i = 0; i < channelsList.size(); i++) {
            String value = records.get(i).getValue().getValue().toString();
            deviceList.add(new Device(assetService.getAssetPid(asset), channelNames.get(i), value, "", "",
                    Long.toString(records.get(i).getTimestamp())));
        }
        data = gson.toJson(deviceList);
        return data;
    }

    // private Response executeCommand(String deviceId, String command) {
    // final Asset asset = getAsset(deviceId);
    // // return getChannelSerializer().toJsonTree(asset.read(readRequest.getChannelNames()));
    // List<ChannelRecord> records = asset.read(asset.getAssetConfiguration().getAssetChannels().keySet());
    // if("read".equals(command)) {
    // return Response.ok(getChannelSerializer().toJsonTree(getDeviceResponse(asset)),
    // MediaType.APPLICATION_JSON).build();
    // } else if("write".equals(command)) {
    //
    // }
    // return
    // }

    private List<ChannelRecord> getDataFromSpecificChannel(Asset asset, String deviceId, String componentId)
            throws KuraException {
        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();
        List<ChannelRecord> records = asset.read(channelsList.keySet());
        for (int i = 0; i < records.size(); i++) {
            if (!records.get(i).getChannelName().equals(componentId)) {
                records.remove(records.get(i));
            }
        }
        return records;
    }

    private Asset getAsset(String deviceId) {
        final Asset asset = assetService.getAsset(deviceId);
        if (asset == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                    .entity("Device not found: " + deviceId).build());
        }
        return asset;
    }

    private Gson getChannelSerializer() {
        if (channelSerializer == null) {
            channelSerializer = new GsonBuilder().registerTypeAdapter(TypedValue.class,
                    (JsonSerializer<TypedValue<?>>) (typedValue, type, context) -> {
                        final Object value = typedValue.getValue();
                        if (value instanceof Number) {
                            return new JsonPrimitive((Number) value);
                        } else if (value instanceof String) {
                            return new JsonPrimitive((String) value);
                        } else if (value instanceof byte[]) {
                            return new JsonPrimitive(BASE64_ENCODER.encodeToString((byte[]) value));
                        } else if (value instanceof Boolean) {
                            return new JsonPrimitive((Boolean) value);
                        }
                        return null;
                    }).create();
        }
        return channelSerializer;
    }
}
