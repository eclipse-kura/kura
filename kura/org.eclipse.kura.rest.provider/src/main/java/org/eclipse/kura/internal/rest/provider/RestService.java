/*******************************************************************************
 * Copyright (c) 2017, 2025 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.internal.rest.provider;

import static org.eclipse.kura.internal.rest.auth.SessionRestServiceConstants.BASE_PATH;
import static org.eclipse.kura.internal.rest.auth.SessionRestServiceConstants.CHANGE_PASSWORD_PATH;
import static org.eclipse.kura.internal.rest.auth.SessionRestServiceConstants.XSRF_TOKEN_PATH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.identity.LoginBannerService;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.eclipse.kura.internal.rest.auth.BasicAuthenticationProvider;
import org.eclipse.kura.internal.rest.auth.CertificateAuthenticationProvider;
import org.eclipse.kura.internal.rest.auth.RestSessionHelper;
import org.eclipse.kura.internal.rest.auth.SessionAuthProvider;
import org.eclipse.kura.internal.rest.auth.SessionRestService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

@SuppressWarnings("restriction")
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestService implements ConfigurableComponent {

    private static final String KURA_DEFAULT_JAKARTARS_WHITEBOARD_NAME = "KuraDefaultJakartarsWhiteboard";

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);

    private CryptoService cryptoService;
    private UserAdmin userAdmin;
    private ConfigurationAdmin configurationAdmin;

    private RestServiceOptions options;

    private final List<ServiceRegistration<?>> registeredServices = new ArrayList<>();

    private AuthenticationProvider basicAuthProvider;
    private AuthenticationProvider certificateAuthProvider;

    private SessionAuthProvider sessionAuthenticationProvider;
    private SessionRestService authRestService;

    private final IncomingPortCheckFilter incomingPortCheckFilter = new IncomingPortCheckFilter();
    private final AuthenticationFilter authenticationFilter = new AuthenticationFilter();
    private PasswordStrengthVerificationService passwordStrengthVerificationService;
    private LoginBannerService loginBannerService;

    @Reference
    public void setUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
        this.authenticationFilter.setUserAdmin(userAdmin);
    }

    @Reference
    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Reference
    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Reference
    public void setPasswordStrengthVerificationService(
            PasswordStrengthVerificationService passwordStrengthVerificationService) {
        this.passwordStrengthVerificationService = passwordStrengthVerificationService;
    }

    @Reference
    public void setLoginBannerService(LoginBannerService loginBannerService) {
        this.loginBannerService = loginBannerService;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindAuthenticationProvider(final AuthenticationProvider provider) {
        this.authenticationFilter.registerAuthenticationProvider(provider);
    }

    public void unbindAuthenticationProvider(final AuthenticationProvider provider) {
        this.authenticationFilter.unregisterAuthenticationProvider(provider);
    }

    @Activate
    public void activate(final Map<String, Object> properties) {
        UserAdminHelper userAdminHelper;
        logger.info("activating...");

        final BundleContext bundleContext = FrameworkUtil.getBundle(RestService.class).getBundleContext();

        userAdminHelper = new UserAdminHelper(this.userAdmin, this.cryptoService);
        final RestSessionHelper restSessionHelper = new RestSessionHelper(userAdminHelper);
        final Dictionary<String, Object> serviceProperties = RestServiceUtils.extensionProperties();

        registeredServices.add(bundleContext.registerService(ContainerRequestFilter.class, this.incomingPortCheckFilter,
                serviceProperties));
        registeredServices.add(bundleContext.registerService(ContainerRequestFilter.class, this.authenticationFilter,
                serviceProperties));
        registeredServices.add(bundleContext.registerService(ContainerRequestFilter.class, new AuthorizationFilter(),
                serviceProperties));
        registeredServices.add(
                bundleContext.registerService(ContainerResponseFilter.class, new AuditFilter(), serviceProperties));
        registeredServices.add(bundleContext.registerService(
                new String[] { MessageBodyReader.class.getName(), MessageBodyWriter.class.getName() },
                new GsonSerializer<Object>(), serviceProperties));

        this.basicAuthProvider = new BasicAuthenticationProvider(bundleContext, userAdminHelper);
        this.certificateAuthProvider = new CertificateAuthenticationProvider(userAdminHelper);
        this.sessionAuthenticationProvider = new SessionAuthProvider(//
                restSessionHelper,
                new HashSet<>(Arrays.asList(BASE_PATH + CHANGE_PASSWORD_PATH, BASE_PATH + XSRF_TOKEN_PATH)),
                Collections.singleton(BASE_PATH + XSRF_TOKEN_PATH));

        this.authRestService = new SessionRestService(userAdminHelper, restSessionHelper, this.configurationAdmin,
                this.passwordStrengthVerificationService, this.loginBannerService);

        this.registeredServices.add(bundleContext.registerService(SessionRestService.class, this.authRestService,
                RestServiceUtils.resourceProperties()));

        update(properties);

        try {
            configureDefaultWhiteboard();
        } catch (final Exception e) {
            logger.error("failed to configure JakartarsServletWhiteboard whiteboard", e);
        }

        logger.info("activating...done");
    }

    private void configureDefaultWhiteboard() throws IOException, InvalidSyntaxException {

        final Configuration[] configs = this.configurationAdmin.listConfigurations(
                "(jersey.jakartars.whiteboard.name=" + KURA_DEFAULT_JAKARTARS_WHITEBOARD_NAME + ")");

        if (configs == null) {
            final Dictionary<String, Object> properties = new Hashtable<>();

            properties.put("jersey.context.path", "/services");
            properties.put("jersey.jakartars.whiteboard.name", KURA_DEFAULT_JAKARTARS_WHITEBOARD_NAME);

            final Configuration newConfiguration = this.configurationAdmin
                    .createFactoryConfiguration("JakartarsServletWhiteboardRuntimeComponent", null);

            newConfiguration.update(properties);

        }

    }

    @Modified
    public void update(final Map<String, Object> properties) {
        logger.info("updating...");

        final RestServiceOptions newOptions = new RestServiceOptions(properties);

        if (!Objects.equals(this.options, newOptions)) {
            this.options = newOptions;
            this.authRestService.setOptions(newOptions);
            this.sessionAuthenticationProvider.setOptions(newOptions);
            this.incomingPortCheckFilter.setAllowedPorts(newOptions.getAllowedPorts());

            updateBuiltinAuthenticationProviders(newOptions);
        }

        logger.info("updating...done");
    }

    @Deactivate
    public void deactivate() {
        logger.info("deactivating...");

        for (final ServiceRegistration<?> reg : registeredServices) {
            reg.unregister();
        }

        this.authenticationFilter.close();

        logger.info("deactivating...done");
    }

    private void updateBuiltinAuthenticationProviders(final RestServiceOptions options) {
        if (options.isPasswordAuthEnabled() && options.isBasicAuthEnabled()) {
            bindAuthenticationProvider(this.basicAuthProvider);
        } else {
            unbindAuthenticationProvider(basicAuthProvider);
        }

        if (options.isCertificateAuthEnabled() && options.isStatelessCertificateAuthEnabled()) {
            bindAuthenticationProvider(this.certificateAuthProvider);
        } else {
            unbindAuthenticationProvider(this.certificateAuthProvider);
        }

        if (options.isSessionManagementEnabled()) {
            bindAuthenticationProvider(this.sessionAuthenticationProvider);
        }
    }

}
