/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.deployment.agent.MarketplacePackageDescriptor;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtMarketplacePackageDescriptor;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtPackageService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtPackageServiceImpl extends OsgiRemoteServiceServlet implements GwtPackageService {

    private static final long serialVersionUID = -3422518194598042896L;

    private static final Logger logger = LoggerFactory.getLogger(GwtPackageServiceImpl.class);

    private static final int MARKETPLACE_FEEDBACK_REQUEST_TIMEOUT = 20 * 1000;

    private static final String MARKETPLACE_URL = "https://marketplace.eclipse.org/node/%s/api/p";

    private final SslManagerService sslManagerService;

    public GwtPackageServiceImpl(SslManagerService sslManagerService) {
        this.sslManagerService = sslManagerService;
    }

    @Override
    public List<GwtDeploymentPackage> findDeviceDeploymentPackages(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        DeploymentAdmin deploymentAdmin = ServiceLocator.getInstance().getService(DeploymentAdmin.class);

        List<GwtDeploymentPackage> gwtDeploymentPackages = new ArrayList<>();
        DeploymentPackage[] deploymentPackages = deploymentAdmin.listDeploymentPackages();
        BundleContext bc = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        List<Bundle> bundles = Arrays.asList(bc.getBundles());

        if (isNull(deploymentPackages)) {
            return gwtDeploymentPackages;
        }
        for (DeploymentPackage deploymentPackage : deploymentPackages) {
            GwtDeploymentPackage gwtDeploymentPackage = new GwtDeploymentPackage();
            gwtDeploymentPackage.setName(GwtSafeHtmlUtils.htmlEscape(deploymentPackage.getName()));
            gwtDeploymentPackage.setVersion(GwtSafeHtmlUtils.htmlEscape(deploymentPackage.getVersion().toString()));
            gwtDeploymentPackage.setSigned(true);

            BundleInfo[] bundleInfos = deploymentPackage.getBundleInfos();
            if (bundleInfos != null) {
                for (BundleInfo bundleInfo : bundleInfos) {
                    Optional<Bundle> bundle = bundles.stream()
                            .filter(b -> b.getSymbolicName().equals(bundleInfo.getSymbolicName())
                                    && b.getVersion().toString().equals(bundleInfo.getVersion().toString()))
                            .findFirst();

                    if (bundle.isPresent() && bundle.get().getSignerCertificates(Bundle.SIGNERS_ALL).isEmpty()) {
                        gwtDeploymentPackage.setSigned(false);
                        break;
                    }
                }
            }

            gwtDeploymentPackages.add(gwtDeploymentPackage);
        }

        return gwtDeploymentPackages;
    }

    @Override
    public void uninstallDeploymentPackage(GwtXSRFToken xsrfToken, String packageName) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        DeploymentAgentService deploymentAgentService = ServiceLocator.getInstance()
                .getService(DeploymentAgentService.class);
        try {
            deploymentAgentService.uninstallDeploymentPackageAsync(GwtSafeHtmlUtils.htmlEscape(packageName));
        } catch (Exception e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public GwtMarketplacePackageDescriptor getMarketplacePackageDescriptor(GwtXSRFToken xsrfToken, String nodeId)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        GwtMarketplacePackageDescriptor descriptor = new GwtMarketplacePackageDescriptor();
        try {
            String url = String.format(MARKETPLACE_URL, nodeId);
            DeploymentAgentService deploymentAgentService = ServiceLocator.getInstance()
                    .getService(DeploymentAgentService.class);
            MarketplacePackageDescriptor marketplacePackageDescriptor = deploymentAgentService
                    .getMarketplacePackageDescriptor(url, this.sslManagerService);

            descriptor.setCompatible(marketplacePackageDescriptor.isCompatible());
            descriptor.setDpUrl(marketplacePackageDescriptor.getDpUrl());
            descriptor.setMinKuraVersion(marketplacePackageDescriptor.getMinKuraVersion());
            descriptor.setMaxKuraVersion(marketplacePackageDescriptor.getMaxKuraVersion());
            descriptor.setCurrentKuraVersion(marketplacePackageDescriptor.getCurrentKuraVersion());
            descriptor.setNodeId(marketplacePackageDescriptor.getNodeId());
            descriptor.setUrl(marketplacePackageDescriptor.getUrl());
        } catch (Exception e) {
            logger.warn("failed to get deployment package descriptor from Eclipse Marketplace", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

        return descriptor;
    }

    @Override
    public void installPackageFromMarketplace(GwtXSRFToken xsrfToken, GwtMarketplacePackageDescriptor descriptor)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            ServiceLocator.applyToServiceOptionally(DeploymentAgentService.class, deploymentAgentService -> {
                if (deploymentAgentService == null) {
                    throw new IllegalStateException("Deployment Agent Service not running");
                }

                final String dpUrl = descriptor.getDpUrl();
                requireNonNull(dpUrl);
                logger.info("Installing deployment package from Eclipse Marketplace, URL {}...", dpUrl);
                if (descriptor.getUrl() != null) {
                    new MarketplaceFeedbackEventHandler(descriptor);
                }
                deploymentAgentService.installDeploymentPackageAsync(dpUrl);

                return (Void) null;
            });

        } catch (Exception e) {
            logger.warn("failed to start package install from Eclipse Marketplace", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    private static class MarketplaceFeedbackEventHandler implements EventHandler {

        private final GwtMarketplacePackageDescriptor descriptor;
        private final ServiceRegistration<EventHandler> registration;
        private static Hashtable<String, Object> serviceProperties;

        static {
            final String[] topics = new String[] { DeploymentAgentService.EVENT_INSTALLED_TOPIC };
            serviceProperties = new Hashtable<>();
            serviceProperties.put(EventConstants.EVENT_TOPIC, topics);
        }

        private MarketplaceFeedbackEventHandler(GwtMarketplacePackageDescriptor descriptor) {
            this.descriptor = descriptor;
            this.registration = FrameworkUtil.getBundle(GwtPackageServiceImpl.class).getBundleContext()
                    .registerService(EventHandler.class, this, serviceProperties);
        }

        private void sendSuccessFeedbackToMarketplace() {
            HttpURLConnection connection = null;

            try {
                logger.info("Sending successful install feedback to Eclipse Marketplace...");
                String feedbackUrl = this.descriptor.getUrl();
                feedbackUrl += feedbackUrl.endsWith("/") ? "success" : "/success";
                connection = (HttpURLConnection) new URL(feedbackUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(MARKETPLACE_FEEDBACK_REQUEST_TIMEOUT);
                connection.connect();

                if (connection.getResponseCode() / 200 == 1) {
                    logger.info("Sending successful install feedback to Eclipse Marketplace...done");
                } else {
                    throw new IOException("got status: " + connection.getResponseCode());
                }

            } catch (Exception e) {
                logger.warn("Sending successful install feedback to Eclipse Marketplace...failure", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        public void handleEvent(Event event) {
            final String eventUrl = (String) event.getProperty(DeploymentAgentService.EVENT_PACKAGE_URL);
            final String dpUrl = this.descriptor.getDpUrl();
            if (eventUrl != null && eventUrl.equals(this.descriptor.getDpUrl())) {
                try {
                    Boolean successful = (Boolean) event.getProperty(DeploymentAgentService.EVENT_SUCCESSFUL);
                    if (successful != null && successful) {
                        logger.info("Installing deployment package from Eclipse Marketplace, URL {}...success", dpUrl);
                        sendSuccessFeedbackToMarketplace();
                    } else {
                        logger.info("Installing deployment package from Eclipse Marketplace, URL {}...failure", dpUrl);
                    }
                } finally {
                    this.registration.unregister();
                }
            }

        }
    }

}
