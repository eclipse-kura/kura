/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationList;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.rest.configuration.api.PropertyDTO;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/wire/v1")
public class WireRestService {

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

    private WireGraphService wireGraphService;
    private Marshaller jsonMarshaller;
    private Unmarshaller jsonUnmarshaller;
    private CryptoService cryptoService;
    private ConfigurationService configurationService;

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

    @DELETE
    @RolesAllowed("configuration")
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
    @RolesAllowed("configuration")
    @Path("/graph")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getWireGraphConfiguration() {
        try {

            final WireGraphConfiguration wireGraphConfiguation = this.wireGraphService.get();

            final String wireGraphServiceJsonGraph = this.jsonMarshaller.marshal(wireGraphConfiguation);

            final ComponentConfiguration wireGraphServiceConfig = buildWireGraphServiceConfig(
                    wireGraphServiceJsonGraph);

            final List<ComponentConfiguration> resultConfigs = Stream
                    .concat(Stream.of(wireGraphServiceConfig),
                            wireGraphConfiguation.getWireComponentConfigurations().stream()
                                    .map(WireComponentConfiguration::getConfiguration))
                    .map(WireRestService::removeDefinition).collect(Collectors.toList());

            return DTOUtil.toComponentConfigurationList(resultConfigs, cryptoService, true);

        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @PUT
    @RolesAllowed("configuration")
    @Path("/graph")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWireGraphConfiguration(final ComponentConfigurationList configs) {
        try {

            configs.validate();

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

            this.wireGraphService.update(new WireGraphConfiguration(resultConfigs, config.getWireConfigurations()));

            if (!configsByPid.values().isEmpty()) {
                this.configurationService.updateConfigurations(configsByPid.values().stream()
                        .map(WireRestService::toComponentConfiguration).collect(Collectors.toList()), true);
            }

            return Response.ok().build();

        } catch (final WebApplicationException e) {
            throw e;
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
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

}
