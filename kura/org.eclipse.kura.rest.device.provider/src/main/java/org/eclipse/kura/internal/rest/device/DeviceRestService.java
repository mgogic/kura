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
import java.util.Base64;
import java.util.Base64.Encoder;
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
import javax.ws.rs.core.Response.Status;

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

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: {\"channels\": [{\"name\": \"channel-1\", \"type\": \"BOOLEAN\", \"value\": true }]}";
    private static final String BAD_READ_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: {\"channels\": [ \"channel-1\", \"channel-2\"]}";
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
    public Response getDeviceChannels(@PathParam("deviceId") String deviceId) throws KuraException {

        Asset asset = getAsset(deviceId);

        return getDeviceResponse(asset);
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceStatus(@PathParam("deviceId") String deviceId) {
        JsonObject statusObject = new JsonObject();
        checkIfDeviceExists(deviceId);

        statusObject.addProperty("status", "CONNECTED");

        return Response.ok(statusObject, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @RolesAllowed("assets")
    @Path("{deviceId}/connection")
    @Produces(MediaType.TEXT_PLAIN)
    public Response connectDevice(@PathParam("deviceId") String deviceId) {
        checkIfDeviceExists(deviceId);

        return Response.ok("Device connected.").build();
    }

    @DELETE
    @RolesAllowed("assets")
    @Path("{deviceId}/connection")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnectDevice(@PathParam("deviceId") String deviceId) {
        checkIfDeviceExists(deviceId);

        return Response.ok("Device disconnected").build();
    }

    @POST
    @RolesAllowed("assets")
    @Path("/{deviceId}/execute/{command}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeSpecificCommand(@PathParam("deviceId") String deviceId, @PathParam("command") String command,
            String request) throws KuraException {

        Response response = executeCommand(deviceId, command, request);

        return response;
    }

    @POST
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}/subscribe")
    @Produces(MediaType.TEXT_PLAIN)
    public String subscribeToComponent(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) {

        throw new WebApplicationException(Response.status(Response.Status.NOT_IMPLEMENTED).type(MediaType.TEXT_PLAIN)
                .entity("Subscribe is not implemented.").build());
    }

    @DELETE
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}/subscribe")
    @Produces(MediaType.TEXT_PLAIN)
    public String unsubscribeFromComponent(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) {

        throw new WebApplicationException(Response.status(Response.Status.NOT_IMPLEMENTED).type(MediaType.TEXT_PLAIN)
                .entity("Unsubscribe is not implemented.").build());
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response readDataFromChannel(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) throws KuraException {

        return readChannelRecord(deviceId, componentId);
    }

    @POST
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeDataToChannel(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId, String writeRequest) {

        final Asset asset = getAsset(deviceId);
        Response notFound = Response.status(Response.Status.NOT_FOUND).entity("Component not found.").build();

        try {
            WriteRequestList writeRequestList = new Gson().fromJson(writeRequest, WriteRequestList.class);
            validate(writeRequestList, BAD_WRITE_REQUEST_ERROR_MESSAGE);

            final List<ChannelRecord> records = writeRequestList.getRequests().stream()
                    .map(request -> request.toChannelRecord()).collect(Collectors.toList());

            if (!asset.getAssetConfiguration().getAssetChannels().containsKey(componentId)) {
                return notFound;
            }
            if (!records.get(0).getChannelName().equals(componentId)) {
                return notFound;
            }
            if (records.size() > 1) {
                return Response.noContent().build();
            }
            asset.write(records);
        } catch (Exception e) {
            logger.error("Exception : ", e);

            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity(BAD_WRITE_REQUEST_ERROR_MESSAGE).type(MediaType.TEXT_PLAIN).build());
        }
        return Response.ok("Write sent", MediaType.TEXT_PLAIN).build();
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/{componentId}/lastUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readLastDataFromChannel(@PathParam("deviceId") String deviceId,
            @PathParam("componentId") String componentId) throws KuraException {

        return readChannelRecord(deviceId, componentId);
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/lastUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readLastDataFromDevice(@PathParam("deviceId") String deviceId) throws KuraException {

        final Asset asset = getAsset(deviceId);

        return getDeviceResponse(asset);
    }

    private Response getDeviceResponse(final Asset asset) throws KuraException {

        List<Device> deviceList = new ArrayList<>();
        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();
        List<ChannelRecord> records = asset.read(channelsList.keySet());

        if (records.isEmpty()) {
            return Response.noContent().build();
        }

        for (ChannelRecord record : records) {
            String value = record.getValue().getValue().toString();
            deviceList.add(new Device(assetService.getAssetPid(asset), record.getChannelName(), value, "",
                    Long.toString(record.getTimestamp()), ""));
        }

        return Response.ok(getChannelSerializer().toJsonTree(deviceList), MediaType.APPLICATION_JSON).build();
    }

    private Response executeCommand(String deviceId, String command, String request) throws KuraException {

        final Asset asset = getAsset(deviceId);
        List<String> existingChannelNames = new ArrayList<>(asset.getAssetConfiguration().getAssetChannels().keySet());
        Response noChannelFound = Response.status(404).entity("Specified channel(s) not found.").build();

        WebApplicationException badReadRequest = new WebApplicationException(Response.status(Status.BAD_REQUEST)
                .entity(BAD_READ_REQUEST_ERROR_MESSAGE).type(MediaType.TEXT_PLAIN).build());
        WebApplicationException badWriteRequest = new WebApplicationException(Response.status(Status.BAD_REQUEST)
                .entity(BAD_WRITE_REQUEST_ERROR_MESSAGE).type(MediaType.TEXT_PLAIN).build());

        Response responseOk = Response.status(Response.Status.OK).entity("Action sent.").build();
        Gson gson = new Gson();

        if ("read".equals(command)) {

            if (request.isEmpty()) {
                throw badReadRequest;
            }

            try {
                ReadRequest readRequest = gson.fromJson(request, ReadRequest.class);
                validate(readRequest, BAD_READ_REQUEST_ERROR_MESSAGE);

                List<String> channelNamesFromRequest = new ArrayList<>(readRequest.getChannelNames());

                if (existingChannelNames.containsAll(channelNamesFromRequest)) {

                    return responseOk;
                }
            } catch (Exception e) {
                // Handle com.google.gson.JsonSyntaxException
                logger.error("Exception  : ", e);

                throw badReadRequest;

            }
            return noChannelFound;

        } else if ("write".equals(command)) {

            if (request.isEmpty()) {
                throw badWriteRequest;
            }

            try {
                WriteRequestList writeRequestlist = gson.fromJson(request, WriteRequestList.class);

                validate(writeRequestlist, BAD_WRITE_REQUEST_ERROR_MESSAGE);

                final List<ChannelRecord> records = writeRequestlist.getRequests().stream()
                        .map(req -> req.toChannelRecord()).collect(Collectors.toList());
                List<String> channelNamesFromRequest = new ArrayList<>();
                for (ChannelRecord record : records) {
                    channelNamesFromRequest.add(record.getChannelName());
                }

                if (existingChannelNames.containsAll(channelNamesFromRequest)) {
                    asset.write(records);

                    return responseOk;
                }
            } catch (Exception e) {
                // Handle com.google.gson.JsonSyntaxException
                logger.error("Exception is : ", e);

                throw badWriteRequest;
            }
            return noChannelFound;

        }

        return Response.noContent().build();
    }

    // Method for check if specific device exists
    private void checkIfDeviceExists(String deviceId) {
        getAsset(deviceId);
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

    private Response readChannelRecord(String deviceId, String componentId) throws KuraException {

        final Asset asset = getAsset(deviceId);
        Device device = null;
        Map<String, Channel> channelsList = asset.getAssetConfiguration().getAssetChannels();

        if (channelsList.isEmpty()) {
            return Response.noContent().build();
        }

        if (!channelsList.containsKey(componentId)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Component not found.").build();
        }

        List<ChannelRecord> records = asset.read(channelsList.keySet());
        for (ChannelRecord channelRecord : records) {

            if (channelRecord.getChannelName().equals(componentId)) {

                String value = channelRecord.getValue().getValue().toString();
                device = new Device(assetService.getAssetPid(asset), channelRecord.getChannelName(), value, "",
                        Long.toString(channelRecord.getTimestamp()), "");

                return Response.ok(getChannelSerializer().toJsonTree(device), MediaType.APPLICATION_JSON).build();
            }
        }

        return Response.noContent().build();
    }
}
