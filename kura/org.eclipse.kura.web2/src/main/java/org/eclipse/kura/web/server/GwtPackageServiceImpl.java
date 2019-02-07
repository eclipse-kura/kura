/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.client.util.GwtSafeHtmlUtils;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtBundleInfo;
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtMarketplacePackageDescriptor;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtPackageService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GwtPackageServiceImpl extends OsgiRemoteServiceServlet implements GwtPackageService {

    private static final long serialVersionUID = -3422518194598042896L;
    private static final Logger logger = LoggerFactory.getLogger(GwtPackageServiceImpl.class);
    private static final int MARKETPLACE_FEEDBACK_REQUEST_TIMEOUT = 20 * 1000;

    @Override
    public List<GwtDeploymentPackage> findDeviceDeploymentPackages(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        DeploymentAdmin deploymentAdmin = ServiceLocator.getInstance().getService(DeploymentAdmin.class);

        List<GwtDeploymentPackage> gwtDeploymentPackages = new ArrayList<>();
        DeploymentPackage[] deploymentPackages = deploymentAdmin.listDeploymentPackages();

        if (deploymentPackages != null) {
            for (DeploymentPackage deploymentPackage : deploymentPackages) {
                GwtDeploymentPackage gwtDeploymentPackage = new GwtDeploymentPackage();
                gwtDeploymentPackage.setName(GwtSafeHtmlUtils.htmlEscape(deploymentPackage.getName()));
                gwtDeploymentPackage.setVersion(GwtSafeHtmlUtils.htmlEscape(deploymentPackage.getVersion().toString()));

                List<GwtBundleInfo> gwtBundleInfos = new ArrayList<>();
                BundleInfo[] bundleInfos = deploymentPackage.getBundleInfos();
                if (bundleInfos != null) {
                    for (BundleInfo bundleInfo : bundleInfos) {
                        GwtBundleInfo gwtBundleInfo = new GwtBundleInfo();
                        gwtBundleInfo.setName(GwtSafeHtmlUtils.htmlEscape(bundleInfo.getSymbolicName()));
                        gwtBundleInfo.setVersion(GwtSafeHtmlUtils.htmlEscape(bundleInfo.getVersion().toString()));

                        gwtBundleInfos.add(gwtBundleInfo);
                    }
                }

                gwtDeploymentPackage.setBundleInfos(gwtBundleInfos);

                gwtDeploymentPackages.add(gwtDeploymentPackage);
            }
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

    private Node getFirstNode(final Document doc, final String tagName) {
        final NodeList elements = doc.getElementsByTagName(tagName);
        if (elements.getLength() == 0) {
            return null;
        }
        return elements.item(0);
    }

    private String getAttributeValue(NamedNodeMap attributes, String attribute) {
        final Node node = attributes.getNamedItem(attribute);
        if (node == null) {
            return null;
        }
        return node.getNodeValue();
    }

    @Override
    public GwtMarketplacePackageDescriptor getMarketplacePackageDescriptor(GwtXSRFToken xsrfToken, String nodeId)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        GwtMarketplacePackageDescriptor descriptor = null;
        URL mpUrl = null;
        HttpURLConnection connection = null;

        try {
            mpUrl = new URL("http://marketplace.eclipse.org/node/" + nodeId + "/api/p");
            connection = (HttpURLConnection) mpUrl.openConnection();

            connection.setRequestMethod("GET");
            connection.connect();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(connection.getInputStream());

            descriptor = new GwtMarketplacePackageDescriptor();

            final Node updateUrl = getFirstNode(doc, "updateurl");

            if (updateUrl == null) {
                throw new GwtKuraException("Unable to find dp install URL");
            }

            descriptor.setDpUrl(updateUrl.getTextContent());

            final Node node = getFirstNode(doc, "node");

            if (node != null) {
                final NamedNodeMap nodeAttributes = node.getAttributes();
                descriptor.setNodeId(getAttributeValue(nodeAttributes, "id"));
                descriptor.setUrl(getAttributeValue(nodeAttributes, "url"));
            }

            Node versionCompatibility = getFirstNode(doc, "versioncompatibility");

            if (versionCompatibility != null) {
                NodeList children = versionCompatibility.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node n = children.item(i);
                    String nodeName = n.getNodeName();
                    if ("from".equalsIgnoreCase(nodeName)) {
                        descriptor.setMinKuraVersion(n.getTextContent());
                    } else if ("to".equalsIgnoreCase(nodeName)) {
                        descriptor.setMaxKuraVersion(n.getTextContent());
                    }
                }
            }

            String kuraPropertyCompatibilityVersion = getMarketplaceCompatibilityVersionString();
            Version kuraVersion = getMarketplaceCompatibilityVersion(kuraPropertyCompatibilityVersion);
            if (kuraVersion != null) {
                kuraPropertyCompatibilityVersion = kuraVersion.toString();
            }

            descriptor.setCurrentKuraVersion(kuraPropertyCompatibilityVersion);
            checkCompatibility(descriptor, kuraVersion);

        } catch (Exception e) {
            logger.warn("failed to get deployment package descriptior from Eclipse Marketplace", e);
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return descriptor;
    }

    private Version getMarketplaceCompatibilityVersion(String marketplaceCompatibilityVersion) {
        try {
            return new Version(marketplaceCompatibilityVersion);
        } catch (Exception e) {
            return null;
        }
    }

    private String getMarketplaceCompatibilityVersionString() throws GwtKuraException {
        return ServiceLocator.applyToServiceOptionally(SystemService.class,
                systemService -> systemService.getKuraMarketplaceCompatibilityVersion());
    }

    private void checkCompatibility(GwtMarketplacePackageDescriptor descriptor, Version currentProductVersion) {
        final String minKuraVersionString = descriptor.getMinKuraVersion();
        final String maxKuraVersionString = descriptor.getMaxKuraVersion();

        try {
            boolean haveMinKuraVersion = minKuraVersionString != null && !minKuraVersionString.isEmpty();
            boolean haveMaxKuraVersion = maxKuraVersionString != null && !maxKuraVersionString.isEmpty();

            if (haveMinKuraVersion && currentProductVersion.compareTo(new Version(minKuraVersionString)) < 0) {
                throw new GwtKuraException(GwtKuraErrorCode.MARKETPLACE_COMPATIBILITY_VERSION_UNSUPPORTED);
            }
            if (haveMaxKuraVersion && currentProductVersion.compareTo(new Version(maxKuraVersionString)) > 0) {
                throw new GwtKuraException(GwtKuraErrorCode.MARKETPLACE_COMPATIBILITY_VERSION_UNSUPPORTED);
            }

            descriptor.setCompatible(haveMinKuraVersion || haveMaxKuraVersion);

        } catch (Exception e) {
            descriptor.setCompatible(false);
        }
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
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
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
