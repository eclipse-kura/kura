/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.network.configuration;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
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
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationList;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.rest.configuration.api.FailureHandler;
import org.eclipse.kura.rest.configuration.api.PidSet;
import org.eclipse.kura.rest.configuration.api.UpdateComponentConfigurationRequest;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("networkConfiguration/v1")
public class NetworkConfigurationRestService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationRestService.class);

    private static final String APP_ID = "NETCONF-V1";

    private static final String KURA_PERMISSION_REST_CONFIGURATION_ROLE = "kura.permission.rest.network.configuration";

    private static final String NETWORK_CONF_SERVICE_PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";
    private static final String IP4_FIREWALL_CONF_SERVICE_PID = "org.eclipse.kura.net.admin.FirewallConfigurationService";
    private static final String IP6_FIREWALL_CONF_SERVICE_PID = "org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6";

    private static final List<String> NETWORK_CONFIGURATION_PIDS = Arrays.asList(NETWORK_CONF_SERVICE_PID,
            IP4_FIREWALL_CONF_SERVICE_PID, IP6_FIREWALL_CONF_SERVICE_PID);

    private static final String SNAPSHOT_SUBTASK_ID = "snapshot";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private ConfigurationService configurationService;
    private CryptoService cryptoService;

    public void setUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_CONFIGURATION_ROLE, Role.GROUP);
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
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
     * Lists the tracked network configurable component Pids
     *
     * @return a List of String objects representing the Pids of network factory components
     *         tracked by the
     *         {@link ConfigurationService}
     */
    @GET
    @RolesAllowed("network.configuration")
    @Path("/networkConfigurableComponents")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet listNetworkConfigurableComponentsPids() {

        Set<String> pids = this.configurationService.getConfigurableComponentPids().stream()
                .filter(this::isNetworkConfigurationPid).collect(Collectors.toSet());

        return new PidSet(pids);
    }

    /**
     * GET method.
     *
     * Lists all the network component configurations of all the ConfigurableComponents
     * tracked by the
     * {@link ConfigurationService}
     *
     * @return a list of {@link ComponentConfigurationDTO} that map all the
     *         configuration parameters tracked for the
     *         network configurable components tracked.
     */
    @GET
    @RolesAllowed("network.configuration")
    @Path("/networkConfigurableComponents/configurations")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList listNetworkConfiguration() {

        final List<ComponentConfiguration> ccs = new ArrayList<>();

        try {
            ccs.add(this.configurationService.getComponentConfiguration(NETWORK_CONF_SERVICE_PID));
            ccs.add(this.configurationService.getComponentConfiguration(IP4_FIREWALL_CONF_SERVICE_PID));
            ccs.add(this.configurationService.getComponentConfiguration(IP6_FIREWALL_CONF_SERVICE_PID));

        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return DTOUtil.toComponentConfigurationList(ccs, this.cryptoService, false).replacePasswordsWithPlaceholder();

    }

    /**
     * POST method.
     *
     * Lists the network component configurations of all the network ConfigurableComponents tracked
     * by the
     * {@link ConfigurationService} that match the filter specified
     *
     * @param filter
     *            A String representing an OSGi filter
     * @return a list of {@link ComponentConfigurationDTO}s for the components that
     *         match the specified filter
     */
    @POST
    @RolesAllowed("network.configuration")
    @Path("/networkConfigurableComponents/configurations/byPid")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList listNetworkComponentConfigurations(final PidSet pids) {
        pids.validate();

        final List<ComponentConfiguration> configs = new ArrayList<>();

        pids.getPids().forEach(pid -> {
            try {
                ComponentConfiguration config = this.configurationService.getComponentConfiguration(pid);
                if (!isNull(config) && isNetworkConfigurationPid(pid)) {
                    configs.add(config);
                }
            } catch (Exception e) {
                throw DefaultExceptionHandler.toWebApplicationException(e);
            }
        });

        return DTOUtil.toComponentConfigurationList(configs, this.cryptoService, false)
                .replacePasswordsWithPlaceholder();
    }

    /**
     * POST method.
     *
     * Provides the default network Component Configuration for the component identified by
     * the specified PID
     *
     * @param componentPid
     * @return The default {@link ComponentConfiguration} or a null object if the
     *         component is not tracked
     */
    @POST
    @RolesAllowed("network.configuration")
    @Path("/networkConfigurableComponents/configurations/byPid/_default")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentConfigurationList listDefaultNetworkComponentConfiguration(final PidSet pids) {
        pids.validate();

        final List<ComponentConfigurationDTO> result = new ArrayList<>();

        for (final String pid : pids.getPids()) {
            try {
                final ComponentConfiguration cc = this.configurationService.getDefaultComponentConfiguration(pid);

                if (!isNetworkConfigurationPid(pid) || cc == null || cc.getDefinition() == null) {
                    logger.warn("cannot find default configuration for {}", pid);
                    continue;
                }

                result.add(DTOUtil.toComponentConfigurationDTO(cc, this.cryptoService, false));
            } catch (final Exception e) {
                logger.warn("failed to get default configuration for {}", pid, e);
            }
        }

        return new ComponentConfigurationList(result);
    }

    /**
     * POST method.
     *
     * Allows to update the configuration of multiple network configurable components
     *
     * @param request
     */
    @PUT
    @RolesAllowed("network.configuration")
    @Path("/networkConfigurableComponents/configurations/_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateComponentConfigurations(UpdateComponentConfigurationRequest request) {
        request.validate();

        final FailureHandler handler = new FailureHandler();

        for (ComponentConfigurationDTO ccr : request.getComponentConfigurations()) {

            if (isNetworkConfigurationPid(ccr.getPid())) {
                handler.runFallibleSubtask("update:" + ccr.getPid(), () -> {
                    final Map<String, Object> configurationProperties = DTOUtil
                            .dtosToConfigurationProperties(ccr.getProperties());
                    this.configurationService.updateConfiguration(ccr.getPid(), configurationProperties, false);
                });
            }
        }

        if (request.isTakeSnapshot()) {
            handler.runFallibleSubtask(SNAPSHOT_SUBTASK_ID, () -> this.configurationService.snapshot());
        }

        handler.checkStatus();
        return Response.ok().build();
    }

    private boolean isNetworkConfigurationPid(String pid) {
        boolean result = false;
        for (String filter : NETWORK_CONFIGURATION_PIDS) {
            if (pid.equals(filter)) {
                result = true;
            }
        }
        return result;
    }

}
