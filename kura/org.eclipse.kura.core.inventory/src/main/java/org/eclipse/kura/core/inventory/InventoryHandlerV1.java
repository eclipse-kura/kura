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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.inventory;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.inventory.resources.SystemPackage;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryHandlerV1 implements ConfigurableComponent, RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryHandlerV1.class);
    public static final String APP_ID = "INVENTORY-V1";

    public static final String RESOURCE_PACKAGES = "packages";
    public static final String RESOURCE_BUNDLES = "bundles";
    public static final String RESOURCE_SYSTEM_PACKAGES = "system.packages";
    public static final String INVENTORY = "inventory";

    private static final String CANNOT_FIND_RESOURCE_MESSAGE = "Cannot find resource with name: {}";
    private static final String NONE_RESOURCE_FOUND_MESSAGE = "Expected one resource but found none";
    private static final String BAD_REQUEST_TOPIC_MESSAGE = "Bad request topic: {}";
    private static final String ERROR_GETTING_RESOURCE = "Error getting resource {}";

    private DeploymentAdmin deploymentAdmin;
    private SystemService systemService;
    private BundleContext bundleContext;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    protected void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    protected void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        if (this.deploymentAdmin == deploymentAdmin) {
            this.deploymentAdmin = null;
        }
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        if (this.systemService == systemService) {
            this.systemService = null;
        }
    }

    public void setRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.registerRequestHandler(APP_ID, this);
        } catch (KuraException e) {
            logger.info("Unable to register cloudlet {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    public void unsetRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.unregister(APP_ID);
        } catch (KuraException e) {
            logger.info("Unable to register cloudlet {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        logger.info("Inventory v1 is starting");
        this.bundleContext = componentContext.getBundleContext();
    }

    protected void deactivate() {
        logger.info("Bundle {} is deactivating!", APP_ID);
        this.bundleContext = null;
    }

    // ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public KuraMessage doGet(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {

        Object requestObject = reqMessage.getProperties().get(ARGS_KEY.value());
        List<String> resources;
        if (requestObject instanceof List) {
            resources = (List<String>) requestObject;
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        if (resources.isEmpty()) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(NONE_RESOURCE_FOUND_MESSAGE);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        KuraPayload resPayload;
        if (resources.get(0).equals(INVENTORY)) {
            resPayload = doGetInventory();
        } else if (resources.get(0).equals(RESOURCE_PACKAGES)) {
            resPayload = doGetPackages();
        } else if (resources.get(0).equals(RESOURCE_BUNDLES)) {
            resPayload = doGetBundles();
        } else if (resources.get(0).equals(RESOURCE_SYSTEM_PACKAGES)) {
            resPayload = doGetSystemPackages();
        } else {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(CANNOT_FIND_RESOURCE_MESSAGE, resources.get(0));
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        return new KuraMessage(resPayload);
    }

    @Override
    public KuraMessage doExec(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {
        throw new KuraException(KuraErrorCode.NOT_FOUND);
    }

    @Override
    public KuraMessage doDel(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {
        throw new KuraException(KuraErrorCode.NOT_FOUND);
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private KuraPayload doGetPackages() {
        DeploymentPackage[] dps = this.deploymentAdmin.listDeploymentPackages();
        SystemDeploymentPackages xdps = new SystemDeploymentPackages();
        SystemDeploymentPackage[] axdp = new SystemDeploymentPackage[dps.length];

        for (int i = 0; i < dps.length; i++) {
            DeploymentPackage dp = dps[i];

            SystemDeploymentPackage xdp = new SystemDeploymentPackage(dp.getName(), dp.getVersion().toString());

            BundleInfo[] bis = dp.getBundleInfos();
            SystemBundle[] axbi = new SystemBundle[bis.length];

            for (int j = 0; j < bis.length; j++) {

                BundleInfo bi = bis[j];
                SystemBundle xb = new SystemBundle(bi.getSymbolicName(), bi.getVersion().toString());

                axbi[j] = xb;
            }

            xdp.setBundleInfos(axbi);

            axdp[i] = xdp;
        }

        xdps.setDeploymentPackages(axdp);

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        try {
            String s = marshal(xdps);
            respPayload.setTimestamp(new Date());
            respPayload.setBody(s.getBytes(Charsets.UTF_8));
        } catch (Exception e) {
            logger.error("Error getting resource {}: {}", RESOURCE_PACKAGES, e);
        }
        return respPayload;
    }

    private KuraPayload doGetBundles() {
        Bundle[] bundles = this.bundleContext.getBundles();
        SystemBundles systemBundles = new SystemBundles();
        SystemBundle[] axb = new SystemBundle[bundles.length];

        for (int i = 0; i < bundles.length; i++) {

            Bundle bundle = bundles[i];
            SystemBundle systemBundle = new SystemBundle(bundle.getSymbolicName(), bundle.getVersion().toString());

            systemBundle.setId(bundle.getBundleId());

            int state = bundle.getState();

            switch (state) {
            case Bundle.UNINSTALLED:
                systemBundle.setState("UNINSTALLED");
                break;

            case Bundle.INSTALLED:
                systemBundle.setState("INSTALLED");
                break;

            case Bundle.RESOLVED:
                systemBundle.setState("RESOLVED");
                break;

            case Bundle.STARTING:
                systemBundle.setState("STARTING");
                break;

            case Bundle.STOPPING:
                systemBundle.setState("STOPPING");
                break;

            case Bundle.ACTIVE:
                systemBundle.setState("ACTIVE");
                break;

            default:
                systemBundle.setState(String.valueOf(state));
            }

            axb[i] = systemBundle;
        }

        systemBundles.setBundles(axb);

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        try {
            String s = marshal(systemBundles);
            respPayload.setTimestamp(new Date());
            respPayload.setBody(s.getBytes(Charsets.UTF_8));
        } catch (Exception e) {
            logger.error(ERROR_GETTING_RESOURCE, RESOURCE_BUNDLES, e);
        }
        return respPayload;
    }

    private KuraPayload doGetInventory() {
        List<SystemResourceInfo> inventory = new ArrayList<>();

        // get System Packages
        try {
            inventory.addAll(this.systemService.getSystemPackages());
        } catch (KuraProcessExecutionErrorException e) {
            logger.error(ERROR_GETTING_RESOURCE, RESOURCE_SYSTEM_PACKAGES, e);
        }

        // get Bundles
        Bundle[] bundles = this.bundleContext.getBundles();
        Arrays.asList(bundles).stream().forEach(b -> inventory.add(
                new SystemResourceInfo(b.getSymbolicName(), b.getVersion().toString(), SystemResourceType.BUNDLE)));

        // get Deployment Packages
        DeploymentPackage[] dps = this.deploymentAdmin.listDeploymentPackages();
        Arrays.asList(dps).stream().forEach(dp -> inventory
                .add(new SystemResourceInfo(dp.getName(), dp.getVersion().toString(), SystemResourceType.DP)));

        // get Containers
        // to be defined...

        inventory.sort(Comparator.comparing(SystemResourceInfo::getName));
        SystemResourcesInfo systemResourcesInfo = new SystemResourcesInfo(inventory);

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        try {
            String s = marshal(systemResourcesInfo);
            respPayload.setTimestamp(new Date());
            respPayload.setBody(s.getBytes(Charsets.UTF_8));
        } catch (Exception e1) {
            logger.error("Error getting inventory", e1);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
        }
        return respPayload;
    }

    private KuraPayload doGetSystemPackages() {
        List<SystemResourceInfo> systemResourceList;
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        try {
            systemResourceList = this.systemService.getSystemPackages();
            List<SystemPackage> systemPackageList = new ArrayList<>();
            systemResourceList.stream()
                    .forEach(p -> systemPackageList.add(new SystemPackage(p.getName(), p.getVersion(), p.getType())));
            SystemPackages systemPackages = new SystemPackages(systemPackageList);

            String s = marshal(systemPackages);
            respPayload.setTimestamp(new Date());
            respPayload.setBody(s.getBytes(Charsets.UTF_8));
        } catch (Exception e) {
            logger.error(ERROR_GETTING_RESOURCE, RESOURCE_SYSTEM_PACKAGES, e);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
        }

        return respPayload;
    }

    private ServiceReference<Marshaller>[] getJsonMarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.json.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(this.bundleContext, Marshaller.class, filterString);
    }

    private void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
    }

    protected String marshal(Object object) {
        String result = null;
        ServiceReference<Marshaller>[] marshallerSRs = getJsonMarshallers();
        try {
            for (final ServiceReference<Marshaller> marshallerSR : marshallerSRs) {
                Marshaller marshaller = this.bundleContext.getService(marshallerSR);
                result = marshaller.marshal(object);
                if (result != null) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to marshal configuration.");
        } finally {
            ungetServiceReferences(marshallerSRs);
        }
        return result;
    }
}
