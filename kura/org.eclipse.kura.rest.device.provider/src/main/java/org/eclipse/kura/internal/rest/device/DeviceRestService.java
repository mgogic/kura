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

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
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
import org.eclipse.kura.type.TypedValue;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

@Path("/device")
public class DeviceRestService {

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: {\"channels\": [{\"name\": \"channel-1\", \"type\": \"INTEGER\", \"value\": 10 }]}";
    private static final String BAD_READ_REQUEST_ERROR_MESSAGE = "Bad request, expected request format: { \"channels\": [ \"channel-1\", \"channel-2\"]}";
    private static final Encoder BASE64_ENCODER = Base64.getEncoder();

    private AssetService assetService;
    private Gson channelSerializer;

    protected void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    @GET
    @RolesAllowed("assets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listDevicePids() throws InvalidSyntaxException {
        return getDeviceServiceReferences().stream()
                .map(reference -> (String) reference.getProperty("kura.service.pid")).collect(Collectors.toList());
    }

    protected Collection<ServiceReference<Asset>> getDeviceServiceReferences() throws InvalidSyntaxException {
        return FrameworkUtil.getBundle(DeviceRestService.class).getBundleContext().getServiceReferences(Asset.class,
                null);
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Channel> getDeviceChannels(@PathParam("deviceId") String deviceId) {
        final Asset asset = getAsset(deviceId);
        return asset.getAssetConfiguration().getAssetChannels().values();
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/_read")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement read(@PathParam("deviceId") String assetPid) throws KuraException {
        final Asset asset = getAsset(assetPid);
        return getChannelSerializer().toJsonTree(asset.readAllChannels());
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{deviceId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceStatus(@PathParam("deviceId") String deviceId) {
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

    @POST
    @RolesAllowed("assets")
    @Path("{deviceId}/connection")
    @Produces(MediaType.APPLICATION_JSON)
    public Response connectDevice(@PathParam("deviceId") String deviceId, ReadRequest readRequest)
            throws KuraException {
        final Asset asset = getAsset(deviceId);
        JsonObject statusObject = new JsonObject();
        statusObject.addProperty("status", "connected");
        return Response.ok(statusObject, MediaType.APPLICATION_JSON).build();
    }

    private Asset getAsset(String assetPid) {
        final Asset asset = assetService.getAsset(assetPid);
        if (asset == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                    .entity("Device not found: " + assetPid).build());
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
