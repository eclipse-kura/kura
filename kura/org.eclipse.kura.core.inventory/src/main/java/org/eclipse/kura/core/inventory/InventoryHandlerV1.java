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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import org.eclipse.kura.core.inventory.resources.SystemBundleRef;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.inventory.resources.SystemPackage;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
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

    public static final String RESOURCE_DEPLOYMENT_PACKAGES = "deploymentPackages";
    public static final String RESOURCE_BUNDLES = "bundles";
    public static final String RESOURCE_SYSTEM_PACKAGES = "systemPackages";
    public static final String INVENTORY = "inventory";
    private static final String START = "_start";
    private static final String STOP = "_stop";

    private static final List<String> START_BUNDLE = Arrays.asList(RESOURCE_BUNDLES, START);
    private static final List<String> STOP_BUNDLE = Arrays.asList(RESOURCE_BUNDLES, STOP);

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

    @Override
    public KuraMessage doGet(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {

        List<String> resources = extractResources(reqMessage);

        KuraPayload resPayload;
        if (resources.get(0).equals(INVENTORY)) {
            resPayload = doGetInventory();
        } else if (resources.get(0).equals(RESOURCE_DEPLOYMENT_PACKAGES)) {
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

    @SuppressWarnings("unchecked")
    private List<String> extractResources(KuraMessage reqMessage) throws KuraException {
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
        return resources;
    }

    @Override
    public KuraMessage doExec(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {

        final List<String> resources = extractResources(reqMessage);

        try {
            if (START_BUNDLE.equals(resources)) {
                findFirstMatchingBundle(extractBundleRef(reqMessage)).start();
                return success();
            } else if (STOP_BUNDLE.equals(resources)) {
                findFirstMatchingBundle(extractBundleRef(reqMessage)).stop();
                return success();
            }
        } catch (final KuraException e) {
            throw e;
        } catch (final Exception e) {
            logger.debug("unexpected exception dispatcing call", e);
            // this should result in response code 500
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE);
        }

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

                Bundle[] bundles = this.bundleContext.getBundles();
                Optional<Bundle> bundle = Arrays.asList(bundles).stream()
                        .filter(b -> b.getSymbolicName().equals(bi.getSymbolicName())).findFirst();
                if (bundle.isPresent()) {
                    xb.setId(bundle.get().getBundleId());
                    xb.setState(bundleStateToString(bundle.get().getState()));
                }

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
            logger.error("Error getting resource {}: {}", RESOURCE_DEPLOYMENT_PACKAGES, e);
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
            systemBundle.setState(bundleStateToString(state));

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

    private <T> ServiceReference<T>[] getJsonMarshallers(final Class<T> classz) {
        String filterString = String.format("(kura.service.pid=%s)",
                "org.eclipse.kura.json.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(this.bundleContext, classz, filterString);
    }

    private void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
    }

    protected String marshal(Object object) {
        String result = null;
        ServiceReference<Marshaller>[] marshallerSRs = getJsonMarshallers(Marshaller.class);
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

    private <T> T unmarshal(final String str, final Class<T> classz) throws KuraException {
        T result = null;
        ServiceReference<Unmarshaller>[] unmarshallerSRs = getJsonMarshallers(Unmarshaller.class);
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                Unmarshaller unmarshaller = this.bundleContext.getService(unmarshallerSR);
                result = unmarshaller.unmarshal(str, classz);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to unmarshal request.");
        } finally {
            ungetServiceReferences(unmarshallerSRs);
        }

        throw new KuraException(KuraErrorCode.BAD_REQUEST);
    }

    private String bundleStateToString(int state) {
        String stateString;

        switch (state) {
        case Bundle.UNINSTALLED:
            stateString = "UNINSTALLED";
            break;

        case Bundle.INSTALLED:
            stateString = "INSTALLED";
            break;

        case Bundle.RESOLVED:
            stateString = "RESOLVED";
            break;

        case Bundle.STARTING:
            stateString = "STARTING";
            break;

        case Bundle.STOPPING:
            stateString = "STOPPING";
            break;

        case Bundle.ACTIVE:
            stateString = "ACTIVE";
            break;

        default:
            stateString = String.valueOf(state);
        }

        return stateString;
    }

    private SystemBundleRef extractBundleRef(final KuraMessage message) throws KuraException {
        final KuraPayload payload = message.getPayload();

        final byte[] body = payload.getBody();

        if (body == null) {
            logger.warn("missing message body");
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        return unmarshal(new String(message.getPayload().getBody(), StandardCharsets.UTF_8), SystemBundleRef.class);
    }

    private Bundle findFirstMatchingBundle(final SystemBundleRef ref) throws KuraException {
        for (final Bundle bundle : bundleContext.getBundles()) {
            if (!bundle.getSymbolicName().equals(ref.getName())) {
                continue;
            }

            final Optional<String> version = ref.getVersion();

            if (!version.isPresent() || version.get().equals(bundle.getVersion().toString())) {
                return bundle;
            }
        }

        throw new KuraException(KuraErrorCode.NOT_FOUND);
    }

    private static KuraMessage success() {
        final KuraPayload response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        response.setTimestamp(new Date());
        return new KuraMessage(response);
    }
}
