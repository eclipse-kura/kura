/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.wire;

import static java.util.stream.Stream.concat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.descriptor.DriverDescriptor;
import org.eclipse.kura.driver.descriptor.DriverDescriptorService;
import org.eclipse.kura.internal.wire.asset.WireAssetChannelDescriptor;
import org.eclipse.kura.internal.wire.asset.WireAssetOCD;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.configuration.api.AdDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationList;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.rest.configuration.api.FailureHandler;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPid;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPidSet;
import org.eclipse.kura.rest.configuration.api.PidSet;
import org.eclipse.kura.rest.configuration.api.PropertyDTO;
import org.eclipse.kura.rest.wire.api.DriverDescriptorDTO;
import org.eclipse.kura.rest.wire.api.WireComponentDefinitionDTO;
import org.eclipse.kura.rest.wire.api.WireGraphMetadata;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireComponentDefinition;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/wire/v1")
public class WireRestService {

    private static final String WIRES_ADMIN_ROLE = "wires.admin";

    private static final String KURA_PERMISSION_REST_WIRES_ADMIN = "kura.permission.rest." + WIRES_ADMIN_ROLE;

    private static final Logger logger = LoggerFactory.getLogger(WireRestService.class);

    private static final String APP_ID = "WIRE-V1";
    private static final String WIRE_GRAPH_SERVICE_PID = "org.eclipse.kura.wire.graph.WireGraphService";

    private static final String WIRE_GRAPH_SERVICE_CONFIG_NOT_FOUND_MESSAGE = "Configuration for \""
            + WIRE_GRAPH_SERVICE_PID + "\" must be specified";
    private static final String WIRE_GRAPH_SERVICE_GRAPH_PROPERTY = "WireGraph";

    private static final String WIRE_GRAPH_PROPERTY_NOT_VALID = "\"" + WIRE_GRAPH_SERVICE_GRAPH_PROPERTY
            + "\" must be set and of STRING type";

    private static final String WIRE_GRAPH_PROPERTY_NOT_FOUND = "\"" + WIRE_GRAPH_SERVICE_PID
            + "\" configuration must contain the \"" + WIRE_GRAPH_SERVICE_GRAPH_PROPERTY + "\" property";

    private static final String WIRE_ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";

    private static final Class<?>[] ALLOWED_SERVICE_INTERFACES = new Class<?>[] { Driver.class, Asset.class,
            WireComponent.class, WireEmitter.class, WireReceiver.class };

    private WireGraphService wireGraphService;
    private Marshaller jsonMarshaller;
    private Unmarshaller jsonUnmarshaller;
    private CryptoService cryptoService;
    private ConfigurationService configurationService;
    private WireComponentDefinitionService wireComponentDefinitionService;
    private DriverDescriptorService driverDescriptorService;
    private OCDService ocdService;
    private BundleContext bundleContext;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    public void setWireGraphService(final WireGraphService wireGraphService) {
        this.wireGraphService = wireGraphService;
    }

    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setJsonMarshaller(final Marshaller marshaller) {
        this.jsonMarshaller = marshaller;
    }

    public void setJsonUnmarshaller(final Unmarshaller unmarshaller) {
        this.jsonUnmarshaller = unmarshaller;
    }

    public void setCryptoService(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void setWireComponentDefinifitionService(
            final WireComponentDefinitionService wireComponentDefinitionService) {
        this.wireComponentDefinitionService = wireComponentDefinitionService;
    }

    public void setDriverDescriptorService(final DriverDescriptorService driverDescriptorService) {
        this.driverDescriptorService = driverDescriptorService;
    }

    public void setOCDService(final OCDService ocdService) {
        this.ocdService = ocdService;
    }

    public void setRequestHandlerRegistry(final RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("failed to register request handler", e);
        }
    }

    public void unsetRequestHandlerRegistry(final RequestHandlerRegistry registry) {
        try {
            registry.unregister(APP_ID);
        } catch (final Exception e) {
            logger.warn("failed to unregister request handler", e);
        }
    }

    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_WIRES_ADMIN, Role.GROUP);
    }

    public void activate(final ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
    }

    @DELETE
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/graph")
    public Response deleteWireGraph() {

        try {
            this.wireGraphService.delete();
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/graph/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getWireGraphConfiguration() {
        try {

            final WireGraphConfiguration wireGraphConfiguation = this.wireGraphService.get();

            final String wireGraphServiceJsonGraph = this.jsonMarshaller.marshal(wireGraphConfiguation);

            final ComponentConfiguration wireGraphServiceConfig = buildWireGraphServiceConfig(
                    wireGraphServiceJsonGraph);

            final List<ComponentConfiguration> driverConfigs = this.configurationService.getComponentConfigurations(
                    bundleContext.createFilter("(objectClass=" + Driver.class.getName() + ")"));

            final List<ComponentConfiguration> resultConfigs = concat(concat(Stream.of(wireGraphServiceConfig),
                    wireGraphConfiguation.getWireComponentConfigurations().stream()
                            .map(WireComponentConfiguration::getConfiguration)),
                    driverConfigs.stream()).map(WireRestService::removeDefinition).collect(Collectors.toList());

            return DTOUtil.toComponentConfigurationList(resultConfigs, cryptoService, true);

        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @PUT
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/graph/snapshot")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWireGraphConfiguration(final ComponentConfigurationList configs) {
        try {

            configs.validate();

            for (final ComponentConfigurationDTO config : configs.getConfigs()) {
                validateReceivedConfig(config);
            }

            final Map<String, ComponentConfigurationDTO> configsByPid = configs.getConfigs().stream()
                    .collect(Collectors.toMap(ComponentConfigurationDTO::getPid, Function.identity()));

            final ComponentConfigurationDTO wireGraphServiceConfig = Optional
                    .ofNullable(configsByPid.remove(WIRE_GRAPH_SERVICE_PID))
                    .orElseThrow(() -> DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                            WIRE_GRAPH_SERVICE_CONFIG_NOT_FOUND_MESSAGE));

            final PropertyDTO marshalledWireGraphProperty = wireGraphServiceConfig.getProperties().entrySet().stream()
                    .filter(e -> WIRE_GRAPH_SERVICE_GRAPH_PROPERTY.equals(e.getKey())).findAny()
                    .orElseThrow(() -> DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                            WIRE_GRAPH_PROPERTY_NOT_FOUND))
                    .getValue();

            if (marshalledWireGraphProperty.getType() != Scalar.STRING
                    || !(marshalledWireGraphProperty.getValue() instanceof String)) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                        WIRE_GRAPH_PROPERTY_NOT_VALID);
            }

            final String marshalledWireGraph = (String) marshalledWireGraphProperty.getValue();

            final WireGraphConfiguration config = jsonUnmarshaller.unmarshal(marshalledWireGraph,
                    WireGraphConfiguration.class);

            final List<WireComponentConfiguration> resultConfigs = config.getWireComponentConfigurations().stream()
                    .map(c -> {
                        final Optional<ComponentConfigurationDTO> receivedConfig = Optional
                                .ofNullable(configsByPid.remove(c.getConfiguration().getPid()));

                        if (receivedConfig.isPresent()) {
                            return new WireComponentConfiguration(toComponentConfiguration(receivedConfig.get()),
                                    c.getProperties());
                        } else {
                            return c;
                        }
                    }).collect(Collectors.toList());

            final WireGraphConfiguration result = new WireGraphConfiguration(resultConfigs,
                    config.getWireConfigurations());

            for (final ComponentConfigurationDTO c : configsByPid.values()) {
                validateConfigForUpdate(result, c);
            }

            final FailureHandler failureHandler = new FailureHandler();

            updateGraphInternal(result, failureHandler);

            updateConfigurationsInternal(failureHandler, configsByPid.values());

            failureHandler.checkStatus();
            return Response.ok().build();

        } catch (final WebApplicationException e) {
            throw e;
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/drivers/pids")
    @Produces(MediaType.APPLICATION_JSON)
    public PidAndFactoryPidSet getDriverPids() {

        return getPidsWithFactory(Driver.class);
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/assets/pids")
    @Produces(MediaType.APPLICATION_JSON)
    public PidAndFactoryPidSet getAssetPids() {

        return getPidsWithFactory(Asset.class);
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/graph/topology")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWireGraphTopology() {
        try {

            final WireGraphConfiguration wireGraphConfiguation = this.wireGraphService.get();

            final String wireGraphServiceJsonGraph = this.jsonMarshaller.marshal(wireGraphConfiguation);

            return Response.status(200)
                    .entity(new ByteArrayInputStream(wireGraphServiceJsonGraph.getBytes(StandardCharsets.UTF_8)))
                    .type(MediaType.APPLICATION_JSON).build();

        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/configs/byPid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getWireConfigsByPid(final PidSet pidSet) {
        pidSet.validate();

        try {
            final List<ComponentConfiguration> result = new ArrayList<>();

            for (final String pid : pidSet.getPids()) {
                if (isAllowedPid(pid)) {
                    tryAddConfig(result, pid);
                }
            }

            return DTOUtil.toComponentConfigurationList(result, cryptoService, false).replacePasswordsWithPlaceholder();

        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @DELETE
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/configs/byPid")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteConfigsByPid(final PidSet pidSet) {
        pidSet.validate();

        try {
            final WireGraphConfiguration graph = this.wireGraphService.get();

            for (final String pid : pidSet.getPids()) {
                if (isReferencedByGraph(pid, graph)) {
                    throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                            "\"" + pid + "\" is referenced by the Wire Graph");
                }

                if (!isAllowedPid(pid)) {
                    throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                            "\"" + pid + "\" does not exist or it is not a Driver, Asset or Wire Component instance");
                }
            }

            final FailureHandler handler = new FailureHandler();

            for (final String pid : pidSet.getPids()) {
                handler.runFallibleSubtask("delete:" + pid,
                        () -> this.configurationService.deleteFactoryConfiguration(pid, false));
            }

            if (!pidSet.getPids().isEmpty()) {
                handler.runFallibleSubtask("snapshot", () -> this.configurationService.snapshot());
            }

            handler.checkStatus();
            return Response.ok().build();
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @PUT
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/configs")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConfigurations(final ComponentConfigurationList configs) {
        configs.validate();

        try {

            final WireGraphConfiguration graph = this.wireGraphService.get();

            for (final ComponentConfigurationDTO config : configs.getConfigs()) {
                validateReceivedConfig(config);
                validateConfigForUpdate(graph, config);
            }

            final FailureHandler failureHandler = new FailureHandler();

            updateConfigurationsInternal(failureHandler, configs.getConfigs());

            failureHandler.checkStatus();
            return Response.ok().build();

        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata")
    @Consumes(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getMetadata() {

        try {

            final List<WireComponentDefinitionDTO> wireComponentDefinitionDTOs = getWireComponentDefinitionsInternal(
                    c -> true);

            final List<DriverDescriptorDTO> driverDescriptors = getDriverDescriptorsInternal(c -> true);

            final List<ComponentConfigurationDTO> driverDefinitions = getDriverDefinitionsInternal(c -> true);

            final List<AdDTO> baseChannelDescriptor = getBaseChannelDescriptorInternal();

            return new WireGraphMetadata(wireComponentDefinitionDTOs, driverDefinitions, driverDescriptors,
                    baseChannelDescriptor);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/wireComponents/factoryPids")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet getWireComponentFactoryPids() {

        try {

            return new PidSet(this.wireComponentDefinitionService.getComponentDefinitions().stream()
                    .map(WireComponentDefinition::getFactoryPid).collect(Collectors.toSet()));
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/wireComponents/definitions")
    @Produces(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getWireComponentDefinitions() {

        try {

            final List<WireComponentDefinitionDTO> wireComponentDefinitionDTOs = getWireComponentDefinitionsInternal(
                    c -> true);

            return new WireGraphMetadata(wireComponentDefinitionDTOs, null, null, null);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/wireComponents/definitions/byFactoryPid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getWireComponentDefinitions(final PidSet pidSet) {

        pidSet.validate();

        try {

            final List<WireComponentDefinitionDTO> wireComponentDefinitionDTOs = getWireComponentDefinitionsInternal(
                    c -> pidSet.getPids().contains(c.getFactoryPid()));

            return new WireGraphMetadata(wireComponentDefinitionDTOs, null, null, null);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/drivers/factoryPids")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet getDriverFactoryPids() {

        try {

            return new PidSet(this.ocdService.getServiceProviderOCDs(Driver.class).stream()
                    .map(ComponentConfiguration::getPid).collect(Collectors.toSet()));
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/drivers/ocds")
    @Produces(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getDriverDefinitions() {

        try {
            final List<ComponentConfigurationDTO> result = getDriverDefinitionsInternal(c -> true);

            return new WireGraphMetadata(null, result, null, null);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/drivers/ocds/byFactoryPid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getDriverDefinitionsByPid(final PidSet pidSet) {
        pidSet.validate();

        try {
            final List<ComponentConfigurationDTO> result = getDriverDefinitionsInternal(
                    c -> pidSet.getPids().contains(c.getPid()));

            return new WireGraphMetadata(null, result, null, null);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/drivers/channelDescriptors")
    @Produces(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getDriverDescriptors() {
        try {
            final List<DriverDescriptorDTO> driverDescriptors = getDriverDescriptorsInternal(c -> true);

            return new WireGraphMetadata(null, null, driverDescriptors, null);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/drivers/channelDescriptors/byPid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getDriverDescriptorsByPid(final PidSet pidSet) {
        pidSet.validate();

        try {
            final List<DriverDescriptorDTO> driverDescriptors = getDriverDescriptorsInternal(
                    c -> pidSet.getPids().contains(c.getPid()));

            return new WireGraphMetadata(null, null, driverDescriptors, null);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(WIRES_ADMIN_ROLE)
    @Path("/metadata/assets/channelDescriptor")
    @Produces(MediaType.APPLICATION_JSON)
    public WireGraphMetadata getBaseChannelDescriptor() {
        try {
            final List<AdDTO> baseChannelDescriptor = getBaseChannelDescriptorInternal();

            return new WireGraphMetadata(null, null, null, baseChannelDescriptor);
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<AdDTO> getBaseChannelDescriptorInternal() {
        return ((List<AD>) WireAssetChannelDescriptor.get().getDescriptor()).stream().map(AdDTO::new)
                .collect(Collectors.toList());
    }

    private List<DriverDescriptorDTO> getDriverDescriptorsInternal(final Predicate<DriverDescriptor> filter) {
        return this.driverDescriptorService.listDriverDescriptors().stream().filter(filter)
                .map(DriverDescriptorDTO::new).collect(Collectors.toList());
    }

    private List<ComponentConfigurationDTO> getDriverDefinitionsInternal(
            final Predicate<ComponentConfiguration> filter) {
        return ocdService.getServiceProviderOCDs(Driver.class).stream().filter(filter)
                .map(WireRestService::removeProperties)
                .map(c -> DTOUtil.toComponentConfigurationDTO(c, cryptoService, false)).collect(Collectors.toList());
    }

    private List<WireComponentDefinitionDTO> getWireComponentDefinitionsInternal(
            final Predicate<WireComponentDefinition> filter) throws KuraException {
        return this.wireComponentDefinitionService.getComponentDefinitions().stream().filter(filter).map(c -> {
            if (WIRE_ASSET_FACTORY_PID.equals(c.getFactoryPid())) {
                setDefinition(c, (OCD) new WireAssetOCD());
            }
            return c;
        }).map(WireComponentDefinitionDTO::new).collect(Collectors.toCollection(ArrayList::new));
    }

    private static ComponentConfiguration removeDefinition(final ComponentConfiguration config) {
        return new ComponentConfiguration() {

            @Override
            public String getPid() {
                return config.getPid();
            }

            @Override
            public OCD getDefinition() {
                return null;
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return config.getConfigurationProperties();
            }
        };
    }

    private static ComponentConfiguration buildWireGraphServiceConfig(final String wireGraphServiceJsonGraph) {
        return new ComponentConfiguration() {

            @Override
            public String getPid() {
                return WIRE_GRAPH_SERVICE_PID;
            }

            @Override
            public OCD getDefinition() {
                return null;
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return Collections.singletonMap(WIRE_GRAPH_SERVICE_GRAPH_PROPERTY, wireGraphServiceJsonGraph);
            }
        };
    }

    private static ComponentConfiguration toComponentConfiguration(final ComponentConfigurationDTO dto) {
        final Map<String, Object> properties = DTOUtil.dtosToConfigurationProperties(dto.getProperties());

        return new ComponentConfiguration() {

            @Override
            public String getPid() {
                return dto.getPid();
            }

            @Override
            public OCD getDefinition() {
                return dto.getDefinition().orElse(null);
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return properties;
            }
        };

    }

    private static ComponentConfiguration removeProperties(final ComponentConfiguration config) {
        return new ComponentConfiguration() {

            @Override
            public String getPid() {
                return config.getPid();
            }

            @Override
            public OCD getDefinition() {
                return config.getDefinition();
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return null;
            }
        };
    }

    private static WireComponentDefinition setDefinition(final WireComponentDefinition definition, final OCD ocd) {
        definition.setComponentOCD(new ComponentConfiguration() {

            @Override
            public String getPid() {
                return definition.getFactoryPid();
            }

            @Override
            public OCD getDefinition() {
                return ocd;
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return null;
            }
        });

        return definition;
    }

    private void validateReceivedConfig(final ComponentConfigurationDTO config) {
        if (WIRE_GRAPH_SERVICE_PID.equals(config.getPid())) {
            return;
        }

        final Map<String, PropertyDTO> properties = config.getProperties();

        if (properties == null) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "\"" + config.getPid() + "\": configuration properties are not specified.");
        }

        final PropertyDTO rawFactoryPid = properties.get(ConfigurationAdmin.SERVICE_FACTORYPID);

        if (rawFactoryPid == null || rawFactoryPid.getType() != Scalar.STRING
                || !(rawFactoryPid.getValue() instanceof String)) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "\"" + config.getPid() + "\": \"" + ConfigurationAdmin.SERVICE_FACTORYPID
                            + "\" property is missing or it is not a string.");
        }

        final String factoryPid = (String) rawFactoryPid.getValue();

        if (!isAllowedFactoryPid(factoryPid)) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST, "\"" + config.getPid()
                    + "\": \"" + factoryPid + "\" is not a Wire Component, Asset or Driver factory pid.");
        }
    }

    private PidAndFactoryPidSet getPidsWithFactory(final Class<?> classz) {
        try {
            final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(bundleContext, classz, null);

            return new PidAndFactoryPidSet(Arrays.stream(refs).map(ref -> {
                final Object pid = ref.getProperty(ConfigurationService.KURA_SERVICE_PID);
                final Object factoryPid = ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);

                if (pid instanceof String && factoryPid instanceof String) {
                    return new PidAndFactoryPid((String) pid, (String) factoryPid);
                } else {
                    return null;
                }

            }).filter(Objects::nonNull).collect(Collectors.toSet()));
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private void tryAddConfig(final List<ComponentConfiguration> result, final String driverPid) {
        try {
            final ComponentConfiguration config = this.configurationService.getComponentConfiguration(driverPid);

            if (config != null) {
                result.add(config);
            }
        } catch (final Exception e) {
            logger.warn("failed to get component configuration", e);
        }
    }

    private boolean isAllowedFactoryPid(final String factoryPid) {
        return ServiceUtil.isFactoryOfAnyService(bundleContext, factoryPid, ALLOWED_SERVICE_INTERFACES);
    }

    private boolean isAllowedPid(final String pid) {
        return implementsAnyOf(pid, ALLOWED_SERVICE_INTERFACES);
    }

    private boolean isDriverOrAssetFactory(final String factoryPid) {
        return ServiceUtil.isFactoryOfAnyService(bundleContext, factoryPid, Driver.class, Asset.class);
    }

    private boolean isWireComponent(final String factoryPid) {
        return ServiceUtil.isFactoryOfAnyService(bundleContext, factoryPid, WireEmitter.class, WireReceiver.class,
                WireComponent.class);
    }

    private void updateGraphInternal(final WireGraphConfiguration result, final FailureHandler failureHandler)
            throws KuraException {
        try {
            this.wireGraphService.update(result);
        } catch (final Exception e) {
            if (e instanceof KuraException && ((KuraException) e).getCode() == KuraErrorCode.BAD_REQUEST) {
                throw e;
            } else {
                failureHandler.processFailure("updateGraph", e);
            }
        }
    }

    private void updateConfigurationsInternal(final FailureHandler handler,
            final Collection<ComponentConfigurationDTO> configs) {

        for (final ComponentConfigurationDTO config : configs) {
            final String factoryPid = getFactoryPid(config);

            if (WIRE_ASSET_FACTORY_PID.equals(factoryPid)) {
                handler.runFallibleSubtask("delete:" + config.getPid(),
                        () -> this.configurationService.deleteFactoryConfiguration(config.getPid(), false));
            }
        }

        if (!configs.isEmpty()) {
            for (final ComponentConfigurationDTO config : configs) {
                final String factoryPid = getFactoryPid(config);

                handler.runFallibleSubtask("update:" + config.getPid(), () -> {
                    final Map<String, Object> properties = WireRestService.toComponentConfiguration(config)
                            .getConfigurationProperties();

                    if (WIRE_ASSET_FACTORY_PID.equals(factoryPid)) {
                        this.configurationService.createFactoryConfiguration(factoryPid, config.getPid(), properties,
                                false);
                    } else {
                        this.configurationService.updateConfiguration(config.getPid(), properties, false);
                    }
                });
            }

            handler.runFallibleSubtask("snapshot", () -> this.configurationService.snapshot());
        }
    }

    private void validateConfigForUpdate(final WireGraphConfiguration graph, final ComponentConfigurationDTO config) {
        final String factoryPid = getFactoryPid(config);

        if (isDriverOrAssetFactory(factoryPid)) {
            return;
        }

        if (isWireComponent(factoryPid) && !isReferencedByGraph(config.getPid(), graph)) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "\"" + config.getPid() + "\" is a Wire Component but it is not referenced by the Wire Graph");
        }
    }

    private boolean isReferencedByGraph(final String pid, final WireGraphConfiguration graph) {
        return graph.getWireComponentConfigurations().stream().anyMatch(c -> c.getConfiguration().getPid().equals(pid));
    }

    private String getFactoryPid(final ComponentConfigurationDTO config) {
        return (String) config.getProperties().get(ConfigurationAdmin.SERVICE_FACTORYPID).getValue();
    }

    private boolean implementsAnyOf(final String pid, final Class<?>... classes) {
        try {
            return ServiceUtil.withService(bundleContext, s -> {
                if (!s.isPresent()) {
                    return false;
                }

                final Object o = s.get();

                for (final Class<?> classz : classes) {
                    if (classz.isInstance(o)) {
                        return true;
                    }
                }

                return false;
            }, "(kura.service.pid=" + pid + ")");
        } catch (InvalidSyntaxException e) {
            return false;
        }
    }

}
