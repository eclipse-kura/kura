/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.request.handler.jaxrs.annotation.EXEC;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationList;
import org.eclipse.kura.rest.configuration.api.CreateFactoryComponentConfigurationsRequest;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.rest.configuration.api.DeleteFactoryComponentRequest;
import org.eclipse.kura.rest.configuration.api.FactoryComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPid;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPidSet;
import org.eclipse.kura.rest.configuration.api.PidSet;
import org.eclipse.kura.rest.configuration.api.SnapshotId;
import org.eclipse.kura.rest.configuration.api.SnapshotIdSet;
import org.eclipse.kura.rest.configuration.api.UpdateComponentConfigurationRequest;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/configuration/v2")
public class ConfigurationRestService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationRestService.class);

    private static final String APP_ID = "CONF-V2";

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.configuration";
    private static final String SNAPSHOT_SUBTASK_ID = "snapshot";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private ConfigurationService configurationService;
    private OCDService ocdService;
    private CryptoService cryptoService;

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setOCDService(OCDService ocdService) {
        this.ocdService = ocdService;
    }

    public void setCryptoService(CryptoService cryptoService) {
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
        } catch (KuraException e) {
            logger.warn("failed to unregister request handler", e);
        }
    }

    /**
     * GET method.
     *
     * Lists all the available snapshots in the framework
     *
     * @return a list of long that represents the list of snapshots managed by the framework.
     */
    @GET
    @RolesAllowed("configuration")
    @Path("/snapshots")
    @Produces(MediaType.APPLICATION_JSON)
    public SnapshotIdSet listSnapshots() {
        try {
            return new SnapshotIdSet(
                    this.configurationService.getSnapshots().stream().collect(Collectors.toCollection(TreeSet::new)));
        } catch (KuraException e) {
            throw FailureHandler.toWebApplicationException(e);
        }
    }

    /**
     * GET method.
     *
     * The method lists all the FactoryComponents Pids tracked by {@link ConfigurationService}
     *
     * @return a list of String representing the tracked FactoryComponents
     */
    @GET
    @RolesAllowed("configuration")
    @Path("/factoryComponents")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet listFactoryComponentsPids() {
        return new PidSet(this.configurationService.getFactoryComponentPids().stream().collect(Collectors.toSet()));
    }

    /**
     * POST method.
     *
     * Creates a new ConfigurableComponent instance by creating a new configuration from a
     * Configuration Admin factory.
     * The {@link FactoryComponentConfigurationDTO} will provide all the information needed to generate the instance: it
     * links the factory Pid to be used, the target instance Pid, the properties to be used when creating the instance
     * and if the request should be persisted with a snapshot.
     * In case of a request error, an exception is thrown.
     *
     * @param factoryComponentConfiguration
     *            provides all the parameters needed to generate a new instance from a Factory Component
     *
     */
    @POST
    @RolesAllowed("configuration")
    @Path("/factoryComponents")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFactoryComponents(CreateFactoryComponentConfigurationsRequest configs) {
        configs.validate();

        final FailureHandler handler = new FailureHandler();

        for (final FactoryComponentConfigurationDTO config : configs.getConfigs()) {
            handler.runFallibleSubtask("create:" + config.getPid(), () -> {
                final Map<String, Object> castedProperties = DTOUtil
                        .dtosToConfigurationProperties(config.getProperties());

                this.configurationService.createFactoryConfiguration(config.getFactoryPid(), config.getPid(),
                        castedProperties, false);

            });
        }

        if (configs.isTakeSnapshot()) {
            handler.runFallibleSubtask(SNAPSHOT_SUBTASK_ID, () -> this.configurationService.snapshot());
        }

        handler.checkStatus();
        return Response.ok().build();
    }

    /**
     * DELETE method.
     *
     * For the specified Pid and {@link FactoryComponentDeleteRequest}, the {@link ConfigurationService} instance
     * deletes the corresponding ConfigurableComponent instance.
     *
     * @param pid
     *            A String representing the pid of the instance generated by a Factory Component that needs to be
     *            deleted
     * @param factoryComponentDeleteRequest
     *            A {@link FactoryComponentDeleteRequest} containing additional information to ease the process of
     *            instance delete
     */
    @DELETE
    @RolesAllowed("configuration")
    @Path("/factoryComponents/byPid")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteFactoryConfigurations(final DeleteFactoryComponentRequest request) {
        request.validate();

        final FailureHandler handler = new FailureHandler();

        for (final String pid : request.getPids()) {
            handler.runFallibleSubtask("delete:" + pid,
                    () -> this.configurationService.deleteFactoryConfiguration(pid, false));
        }

        if (request.isTakeSnapshot()) {
            handler.runFallibleSubtask(SNAPSHOT_SUBTASK_ID, () -> this.configurationService.snapshot());
        }

        handler.checkStatus();
        return Response.ok().build();
    }

    @GET
    @RolesAllowed("configuration")
    @Path("/factoryComponents/ocd")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getFactoryComponentOcds() {
        final List<ComponentConfiguration> ocds;

        try {
            ocds = this.ocdService.getFactoryComponentOCDs();
        } catch (final Exception e) {
            throw FailureHandler.toWebApplicationException(e);
        }

        return DTOUtil.toComponentConfigurationList(ocds, cryptoService, false);
    }

    @POST
    @RolesAllowed("configuration")
    @Path("/factoryComponents/ocd/byFactoryPid")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getFactoryComponentOcdsByPid(final PidSet factoryPids) {
        factoryPids.validate();

        final List<ComponentConfiguration> ocds;

        try {
            ocds = this.ocdService.getFactoryComponentOCDs().stream()
                    .filter(c -> factoryPids.getPids().contains(c.getPid())).collect(Collectors.toList());
        } catch (final Exception e) {
            throw FailureHandler.toWebApplicationException(e);
        }

        return DTOUtil.toComponentConfigurationList(ocds, cryptoService, false);
    }

    /**
     * GET method.
     *
     * Lists the tracked configurable component Pids
     *
     * @return a List of String objects representing the Pids of factory components tracked by the
     *         {@link ConfigurationService}
     */
    @GET
    @RolesAllowed("configuration")
    @Path("/configurableComponents/pidsWithFactory")
    @Produces(MediaType.APPLICATION_JSON)
    public PidAndFactoryPidSet listConfigurableComponentsPidAndFactoryPid() {

        final List<ComponentConfiguration> ccs;

        try {
            ccs = this.configurationService.getComponentConfigurations();
        } catch (final Exception e) {
            throw FailureHandler.toWebApplicationException(e);
        }

        final Set<PidAndFactoryPid> result = ccs.stream().map(c -> {
            final String pid = c.getPid();

            final Optional<String> factoryPid = Optional.ofNullable(c.getConfigurationProperties())
                    .map(p -> p.get(ConfigurationAdmin.SERVICE_FACTORYPID)).flatMap(o -> {
                        if (o instanceof String) {
                            return Optional.of((String) o);
                        } else {
                            return Optional.empty();
                        }
                    });

            if (factoryPid.isPresent()) {
                return new PidAndFactoryPid(pid, factoryPid.get());
            } else {
                return new PidAndFactoryPid(pid);
            }

        }).collect(Collectors.toSet());

        return new PidAndFactoryPidSet(result);
    }

    /**
     * GET method.
     *
     * Lists the tracked configurable component Pids
     *
     * @return a List of String objects representing the Pids of factory components tracked by the
     *         {@link ConfigurationService}
     */
    @GET
    @RolesAllowed("configuration")
    @Path("/configurableComponents")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet listConfigurableComponentsPids() {
        return new PidSet(
                this.configurationService.getConfigurableComponentPids().stream().collect(Collectors.toSet()));
    }

    /**
     * GET method.
     *
     * Lists all the component configurations of all the ConfigurableComponents tracked by the
     * {@link ConfigurationService}
     *
     * @return a list of {@link ComponentConfigurationDTO} that map all the configuration parameters tracked for the
     *         configurable components tracked.
     */
    @GET
    @RolesAllowed("configuration")
    @Path("/configurableComponents/configurations")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList listComponentConfigurations() {

        final List<ComponentConfiguration> ccs;

        try {
            ccs = this.configurationService.getComponentConfigurations();
        } catch (final Exception e) {
            throw FailureHandler.toWebApplicationException(e);
        }

        return DTOUtil.toComponentConfigurationList(ccs, cryptoService, true);

    }

    /**
     * GET method.
     *
     * Lists the component configurations of all the ConfigurableComponents tracked by the
     * {@link ConfigurationService} that match the filter specified
     *
     * @param filter
     *            A String representing an OSGi filter
     * @return a list of {@link ComponentConfigurationDTO}s for the components that match the specified filter
     */
    @POST
    @RolesAllowed("configuration")
    @Path("/configurableComponents/configurations/byPid")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList listComponentConfigurations(final PidSet pids) {
        pids.validate();

        final List<ComponentConfiguration> configs;

        try {
            configs = this.configurationService.getComponentConfigurations().stream()
                    .filter(c -> pids.getPids().contains(c.getPid())).collect(Collectors.toList());
        } catch (final Exception e) {
            throw FailureHandler.toWebApplicationException(e);
        }

        return DTOUtil.toComponentConfigurationList(configs, cryptoService, true);
    }

    /**
     * GET method.
     *
     * Provides the default Component Configuration for the component identified by the specified PID
     *
     * @param componentPid
     * @return The default {@link ComponentConfiguration} or a null object if the component is not tracked
     */
    @POST
    @RolesAllowed("configuration")
    @Path("/configurableComponents/configurations/byPid/_default")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList listDefaultComponentConfiguration(final PidSet pids) {
        pids.validate();

        final List<ComponentConfigurationDTO> result = new ArrayList<>();

        for (final String pid : pids.getPids()) {
            try {
                final ComponentConfiguration cc = this.configurationService.getDefaultComponentConfiguration(pid);

                if (cc == null || cc.getDefinition() == null) {
                    logger.warn("cannot find default configuration for {}", pid);
                    continue;
                }

                result.add(DTOUtil.toComponentConfigurationDTO(cc, cryptoService, true));
            } catch (final Exception e) {
                logger.warn("failed to get default configuration for {}", pid, e);
            }
        }

        return new ComponentConfigurationList(result);
    }

    /**
     * POST method.
     *
     * Allows to update the configuration of multiple configurable components
     *
     * @param request
     */
    @PUT
    @RolesAllowed("configuration")
    @Path("/configurableComponents/configurations/_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateComponentConfigurations(UpdateComponentConfigurationRequest request) {
        request.validate();

        final FailureHandler handler = new FailureHandler();

        for (ComponentConfigurationDTO ccr : request.getComponentConfigurations()) {

            handler.runFallibleSubtask("update:" + ccr.getPid(), () -> {
                final Map<String, Object> configurationProperties = DTOUtil
                        .dtosToConfigurationProperties(ccr.getProperties());
                this.configurationService.updateConfiguration(ccr.getPid(), configurationProperties, false);
            });
        }

        if (request.isTakeSnapshot()) {
            handler.runFallibleSubtask(SNAPSHOT_SUBTASK_ID, () -> this.configurationService.snapshot());
        }

        handler.checkStatus();
        return Response.ok().build();
    }

    /**
     * GET method.
     *
     * Returns the content of a given snapshot tracked by the framework.
     *
     * @param snapshotId
     * @return a List of {@link ComponentConfiguration}
     */
    @POST
    @RolesAllowed("configuration")
    @Path("/snapshots/byId")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList getSnapshot(final SnapshotId id) {
        id.validate();

        try {
            List<ComponentConfiguration> configs = this.configurationService.getSnapshot(id.getId());

            return DTOUtil.toComponentConfigurationList(configs, cryptoService, false);
        } catch (KuraException e) {
            throw FailureHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Triggers the framework to take and persist a snapshot.
     *
     * @return a long representing the id of the generated snapshot
     */
    @EXEC
    @POST
    @RolesAllowed("configuration")
    @Path("/snapshots/_write")
    @Produces(MediaType.APPLICATION_JSON)
    public SnapshotId takeSnapshot() {
        try {
            return new SnapshotId(this.configurationService.snapshot());
        } catch (KuraException e) {
            throw FailureHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method
     *
     * Rollbacks the framework to the last saved snapshot if available.
     *
     * @return the ID of the snapshot it rolled back to
     */
    @EXEC
    @POST
    @RolesAllowed("configuration")
    @Path("/snapshots/_rollback")
    @Produces(MediaType.APPLICATION_JSON)
    public SnapshotId rollbackSnapshot() {
        try {
            return new SnapshotId(this.configurationService.rollback());
        } catch (KuraException e) {
            throw FailureHandler.toWebApplicationException(e);
        }
    }

    /**
     * POST method.
     *
     * Rollbacks the framework to the snapshot identified by the provided ID
     *
     * @param snapshotId
     */
    @EXEC
    @POST
    @RolesAllowed("configuration")
    @Path("/snapshots/byId/_rollback")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response rollbackSnapshot(final SnapshotId id) {
        id.validate();

        try {
            this.configurationService.rollback(id.getId());
        } catch (KuraException e) {
            throw FailureHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }
}
