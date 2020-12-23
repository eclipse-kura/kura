/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.internal.rest.asset;

import static org.eclipse.kura.rest.utils.Validable.validate;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

@Path("/assets")
public class AssetRestService {

    private static final String BAD_WRITE_REQUEST_ERROR_MESSAGE = "Bad request, "
            + "expected request format: {\"channels\": [{\"name\": \"channel-1\", \"type\": \"INTEGER\", \"value\": 10 }]}";
    private static final String BAD_READ_REQUEST_ERROR_MESSAGE = "Bad request, "
            + "expected request format: { \"channels\": [ \"channel-1\", \"channel-2\"]}";
    private static final Encoder BASE64_ENCODER = Base64.getEncoder();

    private AssetService assetService;
    private Gson channelSerializer;

    private UserAdmin userAdmin;
    private Thread userAdminUpdateThread = new Thread(() -> {
        waitUserAdminReady();

        userAdmin.createRole("kura.permission.rest.assets", Role.GROUP);
    });

    protected void setUserAdmin(UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    protected void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    public void activate() {
        this.userAdminUpdateThread.start();
    }

    public void deactivate() {
        try {
            this.userAdminUpdateThread.join(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @GET
    @RolesAllowed("assets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listAssetPids() throws InvalidSyntaxException {
        return getAssetServiceReferences().stream().map(reference -> (String) reference.getProperty("kura.service.pid"))
                .collect(Collectors.toList());
    }

    protected Collection<ServiceReference<Asset>> getAssetServiceReferences() throws InvalidSyntaxException {
        return FrameworkUtil.getBundle(AssetRestService.class).getBundleContext().getServiceReferences(Asset.class,
                null);
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{pid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Channel> getAssetChannels(@PathParam("pid") String assetPid) {
        final Asset asset = getAsset(assetPid);
        return asset.getAssetConfiguration().getAssetChannels().values();
    }

    @GET
    @RolesAllowed("assets")
    @Path("/{pid}/_read")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement read(@PathParam("pid") String assetPid) throws KuraException {
        final Asset asset = getAsset(assetPid);
        return getChannelSerializer().toJsonTree(asset.readAllChannels());
    }

    @POST
    @RolesAllowed("assets")
    @Path("/{pid}/_read")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement read(@PathParam("pid") String assetPid, ReadRequest readRequest) throws KuraException {
        final Asset asset = getAsset(assetPid);
        validate(readRequest, BAD_READ_REQUEST_ERROR_MESSAGE);
        return getChannelSerializer().toJsonTree(asset.read(readRequest.getChannelNames()));
    }

    @POST
    @RolesAllowed("assets")
    @Path("/{pid}/_write")
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonElement write(@PathParam("pid") String assetPid, WriteRequestList requests) throws KuraException {
        final Asset asset = getAsset(assetPid);
        validate(requests, BAD_WRITE_REQUEST_ERROR_MESSAGE);
        final List<ChannelRecord> records = requests.getRequests().stream().map(request -> request.toChannelRecord())
                .collect(Collectors.toList());
        asset.write(records);
        return getChannelSerializer().toJsonTree(records);
    }

    private Asset getAsset(String assetPid) {
        final Asset asset = assetService.getAsset(assetPid);
        if (asset == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                    .entity("Asset not found: " + assetPid).build());
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

    private void waitUserAdminReady() {
        final BundleContext context = FrameworkUtil.getBundle(AssetRestService.class).getBundleContext();

        while (true) {

            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            final ServiceReference<?> ref = context
                    .getServiceReference("org.apache.felix.useradmin.RoleRepositoryStore");

            final Bundle[] usingBundles = ref.getUsingBundles();

            if (usingBundles != null && usingBundles.length > 0) {
                break;
            }

        }
    }
}
