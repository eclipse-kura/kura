/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConfigurationHandler extends Cloudlet {

    private static Logger logger = LoggerFactory.getLogger(CloudConfigurationHandler.class);

    public static final String APP_ID = "CONF-V1";

    /* GET or PUT */
    public static final String RESOURCE_CONFIGURATIONS = "configurations";
    /* GET */
    public static final String RESOURCE_SNAPSHOTS = "snapshots";
    /* EXEC */
    public static final String RESOURCE_SNAPSHOT = "snapshot";
    public static final String RESOURCE_ROLLBACK = "rollback";

    private SystemService systemService;
    private ConfigurationService configurationService;

    private ComponentContext ctx;
    private BundleContext bundleContext;

    private ScheduledExecutorService executor;

    protected void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    protected void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    protected void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    // The dependency on the CloudService is optional so we might be activated
    // before we have the CloudService.
    @Override
    public void setCloudService(CloudService cloudService) {
        super.setCloudService(cloudService);
        super.activate(this.ctx);
    }

    @Override
    public void unsetCloudService(CloudService cloudService) {
        super.deactivate(this.ctx);
        super.unsetCloudService(cloudService);
    }

    public CloudConfigurationHandler() {
        super(APP_ID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        this.ctx = componentContext;
        this.bundleContext = componentContext.getBundleContext();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        this.executor.shutdownNow();
    }

    @Override
    protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {

        String resources[] = reqTopic.getResources();

        if (resources == null || resources.length == 0) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (resources[0].equals(RESOURCE_CONFIGURATIONS)) {
            doGetConfigurations(reqTopic, reqPayload, respPayload);
        } else if (resources[0].equals(RESOURCE_SNAPSHOTS)) {
            doGetSnapshots(reqTopic, reqPayload, respPayload);
        } else {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Cannot find resource with name: {}", resources[0]);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }
    }

    @Override
    protected void doPut(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {

        String resources[] = reqTopic.getResources();

        if (resources == null || resources.length == 0) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (resources[0].equals(RESOURCE_CONFIGURATIONS)) {
            doPutConfigurations(reqTopic, reqPayload, respPayload);
        } else {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Cannot find resource with name: {}", resources[0]);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }
    }

    @Override
    protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources == null || resources.length == 0) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (resources[0].equals(RESOURCE_SNAPSHOT)) {
            doExecSnapshot(reqTopic, reqPayload, respPayload);
        } else if (resources[0].equals(RESOURCE_ROLLBACK)) {
            doExecRollback(reqTopic, reqPayload, respPayload);
        } else {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Cannot find resource with name: {}", resources[0]);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }
    }

    private void doGetSnapshots(CloudletTopic reqTopic, KuraPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources.length > 2) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected one or two resource(s) but found {}", resources.length);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        String snapshotId = resources.length == 2 ? resources[1] : null;

        if (snapshotId != null) {
            long sid = Long.parseLong(snapshotId);
            XmlComponentConfigurations xmlConfigs = ((ConfigurationServiceImpl) this.configurationService)
                    .loadEncryptedSnapshotFileContent(sid);
            //
            // marshall the response

            List<ComponentConfiguration> configs = xmlConfigs.getConfigurations();
            for (ComponentConfiguration config : configs) {
                if (config != null) {
                    try {
                        ((ConfigurationServiceImpl) this.configurationService)
                                .decryptConfigurationProperties(config.getConfigurationProperties());
                    } catch (Throwable t) {
                        logger.warn("Error during snapshot password decryption");
                    }
                }
            }

            byte[] body = toResponseBody(xmlConfigs);

            //
            // Build payload
            respPayload.setBody(body);
        } else {
            // get the list of snapshot IDs and put them into a response object
            Set<Long> sids = null;
            try {
                sids = this.configurationService.getSnapshots();
            } catch (KuraException e) {
                logger.error("Error listing snapshots: {}", e);
                throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_LISTING, e);
            }
            List<Long> snapshotIds = new ArrayList<>(sids);
            XmlSnapshotIdResult xmlResult = new XmlSnapshotIdResult();
            xmlResult.setSnapshotIds(snapshotIds);

            //
            // marshall the response
            byte[] body = toResponseBody(xmlResult);

            //
            // Build payload
            respPayload.setBody(body);
        }
    }

    private void doGetConfigurations(CloudletTopic reqTopic, KuraPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {
        String[] resources = reqTopic.getResources();
        if (resources.length > 2) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected at most two resource(s) but found {}", resources.length);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }
        String pid = resources.length == 2 ? resources[1] : null;

        //
        // get current configuration with descriptors
        List<ComponentConfiguration> configs = new ArrayList<>();
        try {

            if (pid == null) {
                List<String> pidsToIgnore = this.systemService.getDeviceManagementServiceIgnore();

                // the configuration for all components has been requested
                Set<String> componentPids = this.configurationService.getConfigurableComponentPids();
                for (String componentPid : componentPids) {
                    boolean skip = false;
                    if (pidsToIgnore != null && !pidsToIgnore.isEmpty()) {
                        for (String pidToIgnore : pidsToIgnore) {
                            if (componentPid.equals(pidToIgnore)) {
                                skip = true;
                                break;
                            }
                        }
                    }
                    if (skip) {
                        continue;
                    }

                    ComponentConfiguration cc = this.configurationService.getComponentConfiguration(componentPid);

                    // TODO: define a validate method for ComponentConfiguration
                    if (cc == null) {
                        logger.error("null ComponentConfiguration");
                        continue;
                    }
                    if (cc.getPid() == null || cc.getPid().isEmpty()) {
                        logger.error("null or empty ComponentConfiguration PID");
                        continue;
                    }
                    if (cc.getDefinition() == null) {
                        logger.error("null OCD for ComponentConfiguration PID {}", cc.getPid());
                        continue;
                    }
                    if (cc.getDefinition().getId() == null || cc.getDefinition().getId().isEmpty()) {

                        logger.error("null or empty OCD ID for ComponentConfiguration PID {}. OCD ID: {}", cc.getPid(),
                                cc.getDefinition().getId());
                        continue;
                    }
                    configs.add(cc);
                }
            } else {

                // the configuration for a specific component has been requested.
                ComponentConfiguration cc = this.configurationService.getComponentConfiguration(pid);
                if (cc != null) {
                    configs.add(cc);
                }
            }
        } catch (KuraException e) {
            logger.error("Error getting component configurations: {}", e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Error getting component configurations");
        }

        XmlComponentConfigurations xmlConfigs = new XmlComponentConfigurations();
        xmlConfigs.setConfigurations(configs);

        //
        // marshall
        byte[] body = toResponseBody(xmlConfigs);

        //
        // Build response payload
        respPayload.setBody(body);
    }

    private void doPutConfigurations(CloudletTopic reqTopic, KuraPayload reqPayload, KuraResponsePayload respPayload) {
        String[] resources = reqTopic.getResources();

        if (resources.length > 2) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected at most two resource(s) but found {}", resources.length);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        String pid = resources.length == 2 ? resources[1] : null;

        XmlComponentConfigurations xmlConfigs = null;
        try {

            // unmarshall the response
            if (reqPayload.getBody() == null || reqPayload.getBody().length == 0) {
                throw new IllegalArgumentException("body");
            }

            String s = new String(reqPayload.getBody(), "UTF-8");
            logger.info("Received new Configuration");

            xmlConfigs = unmarshal(s, XmlComponentConfigurations.class);
        } catch (Exception e) {
            logger.error("Error unmarshalling the request body: {}", e);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            respPayload.setException(e);
            return;
        }

        this.executor.schedule(new UpdateConfigurationsCallable(pid, xmlConfigs, this.configurationService), 1000,
                TimeUnit.MILLISECONDS);
    }

    private void doExecRollback(CloudletTopic reqTopic, KuraPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources.length > 2) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected at most two resource(s) but found {}", resources.length);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        String snapshotId = resources.length == 2 ? resources[1] : null;
        Long sid;
        try {
            sid = snapshotId != null ? Long.parseLong(snapshotId) : null;
        } catch (NumberFormatException e) {
            logger.error("Bad numeric numeric format for snapshot ID: {}", snapshotId);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        this.executor.schedule(new RollbackCallable(sid, this.configurationService), 1000, TimeUnit.MILLISECONDS);
    }

    private void doExecSnapshot(CloudletTopic reqTopic, KuraPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources.length > 1) {
            logger.error("Bad request topic: {}", reqTopic.toString());
            logger.error("Expected one resource(s) but found {}", resources.length);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        // take a new snapshot and get the id
        long snapshotId;
        try {
            snapshotId = this.configurationService.snapshot();
        } catch (KuraException e) {
            logger.error("Error taking snapshot: {}", e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_TAKING, e);
        }
        List<Long> snapshotIds = new ArrayList<>();
        snapshotIds.add(snapshotId);
        XmlSnapshotIdResult xmlResult = new XmlSnapshotIdResult();
        xmlResult.setSnapshotIds(snapshotIds);

        byte[] body = toResponseBody(xmlResult);

        respPayload.setBody(body);
    }

    private byte[] toResponseBody(Object o) throws KuraException {
        //
        // marshall the response
        String result = null;
        try {
            result = marshal(o);
        } catch (Exception e) {
            logger.error("Error marshalling snapshots: {}", e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_LOADING, e);
        }

        byte[] body = null;
        try {
            body = result.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding response body: {}", e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_LOADING, e);
        }

        return body;
    }

    private ServiceReference<Marshaller>[] getXmlMarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.xml.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(this.bundleContext, Marshaller.class, filterString);
    }

    private ServiceReference<Unmarshaller>[] getXmlUnmarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.xml.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(this.bundleContext, Unmarshaller.class, filterString);
    }

    private void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
    }

    protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
        T result = null;
        ServiceReference<Unmarshaller>[] unmarshallerSRs = getXmlUnmarshallers();
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                Unmarshaller unmarshaller = this.bundleContext.getService(unmarshallerSR);
                result = unmarshaller.unmarshal(xmlString, clazz);
                if (result != null) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract persisted configuration.");
        } finally {
            ungetServiceReferences(unmarshallerSRs);
        }
        if (result == null) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
        }
        return result;
    }

    protected String marshal(Object object) {
        String result = null;
        ServiceReference<Marshaller>[] marshallerSRs = getXmlMarshallers();
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

class UpdateConfigurationsCallable implements Callable<Void> {

    private static Logger logger = LoggerFactory.getLogger(UpdateConfigurationsCallable.class);

    private final String pid;
    private final XmlComponentConfigurations xmlConfigurations;
    private final ConfigurationService configurationService;

    public UpdateConfigurationsCallable(String pid, XmlComponentConfigurations xmlConfigurations,
            ConfigurationService configurationService) {
        this.pid = pid;
        this.xmlConfigurations = xmlConfigurations;
        this.configurationService = configurationService;
    }

    @Override
    public Void call() throws Exception {

        logger.info("Updating configurations");
        Thread.currentThread().setName(getClass().getSimpleName());
        //
        // update the configuration
        try {
            List<ComponentConfiguration> configImpls = this.xmlConfigurations != null
                    ? this.xmlConfigurations.getConfigurations() : null;
            if (configImpls == null) {
                return null;
            }

            List<ComponentConfiguration> configs = new ArrayList<>();
            configs.addAll(configImpls);

            if (this.pid == null) {
                // update all the configurations provided
                this.configurationService.updateConfigurations(configs);
            } else {
                // update only the configuration with the provided id
                for (ComponentConfiguration config : configs) {
                    if (this.pid.equals(config.getPid())) {
                        this.configurationService.updateConfiguration(this.pid,
                                config.getConfigurationProperties());
                    }
                }
            }
        } catch (KuraException e) {
            logger.error("Error updating configurations: {}", e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e);
        }

        return null;
    }
}

class RollbackCallable implements Callable<Void> {

    private static Logger logger = LoggerFactory.getLogger(RollbackCallable.class);

    private final Long snapshotId;
    private final ConfigurationService configurationService;

    public RollbackCallable(Long snapshotId, ConfigurationService configurationService) {
        super();
        this.snapshotId = snapshotId;
        this.configurationService = configurationService;
    }

    @Override
    public Void call() throws Exception {
        Thread.currentThread().setName(getClass().getSimpleName());
        // rollback to the specified snapshot if any
        try {
            if (this.snapshotId == null) {
                this.configurationService.rollback();
            } else {
                this.configurationService.rollback(this.snapshotId);
            }
        } catch (KuraException e) {
            logger.error("Error rolling back to snapshot: {}", e);
            throw new KuraException(KuraErrorCode.CONFIGURATION_ROLLBACK, e);
        }

        return null;
    }
}
