/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import static org.eclipse.kura.cloudconnection.request.RequestHandlerConstants.ARGS_KEY;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConfigurationHandler implements RequestHandler {

    private static final String EXPECTED_ONE_RESOURCE_BUT_FOUND_NONE_MESSAGE = "Expected one resource but found none";

    private static final String EXPECTED_AT_MOST_TWO_RESOURCES_BUT_FOUND_MESSAGE = "Expected at most two resource(s) but found {}";

    private static final String CANNOT_FIND_RESOURCE_WITH_NAME_MESSAGE = "Cannot find resource with name: {}";

    private static final String BAD_REQUEST_TOPIC_MESSAGE = "Bad request topic: {}";

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

    protected void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    protected void deactivate(ComponentContext componentContext) {
        this.executor.shutdownNow();
    }

    @Override
    public KuraMessage doGet(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {

        List<String> resources = getRequestResources(reqMessage);

        if (resources.isEmpty()) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(EXPECTED_ONE_RESOURCE_BUT_FOUND_NONE_MESSAGE);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        KuraPayload payload;
        if (resources.get(0).equals(RESOURCE_CONFIGURATIONS)) {
            payload = doGetConfigurations(resources);
        } else if (resources.get(0).equals(RESOURCE_SNAPSHOTS)) {
            payload = doGetSnapshots(resources);
        } else {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(CANNOT_FIND_RESOURCE_WITH_NAME_MESSAGE, resources.get(0));
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        return new KuraMessage(payload);
    }

    @Override
    public KuraMessage doPut(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {

        List<String> resources = getRequestResources(reqMessage);

        if (resources.isEmpty()) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(EXPECTED_ONE_RESOURCE_BUT_FOUND_NONE_MESSAGE);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        KuraPayload payload;
        if (resources.get(0).equals(RESOURCE_CONFIGURATIONS)) {
            payload = doPutConfigurations(resources, reqMessage.getPayload());
        } else {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(CANNOT_FIND_RESOURCE_WITH_NAME_MESSAGE, resources.get(0));
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        return new KuraMessage(payload);
    }

    @Override
    public KuraMessage doExec(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {

        List<String> resources = getRequestResources(reqMessage);

        if (resources.isEmpty()) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(EXPECTED_ONE_RESOURCE_BUT_FOUND_NONE_MESSAGE);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        KuraPayload payload;
        if (resources.get(0).equals(RESOURCE_SNAPSHOT)) {
            payload = doExecSnapshot(resources);
        } else if (resources.get(0).equals(RESOURCE_ROLLBACK)) {
            payload = doExecRollback(resources);
        } else {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(CANNOT_FIND_RESOURCE_WITH_NAME_MESSAGE, resources.get(0));
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        return new KuraMessage(payload);
    }

    private KuraPayload doGetSnapshots(List<String> resources) throws KuraException {

        KuraResponsePayload responsePayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        if (resources.size() > 2) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error("Expected one or two resource(s) but found {}", resources.size());
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        String snapshotId = resources.size() == 2 ? resources.get(1) : null;

        if (snapshotId != null) {
            long sid = Long.parseLong(snapshotId);
            XmlComponentConfigurations xmlConfigs = ((ConfigurationServiceImpl) this.configurationService)
                    .loadEncryptedSnapshotFileContent(sid);
            //
            // marshall the response

            List<ComponentConfiguration> configs = xmlConfigs.getConfigurations();
            configs.forEach(config -> ((ConfigurationServiceImpl) this.configurationService)
                    .decryptConfigurationProperties(config.getConfigurationProperties()));

            byte[] body = toResponseBody(xmlConfigs);

            //
            // Build payload
            responsePayload.setBody(body);
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
            responsePayload.setBody(body);
        }
        return responsePayload;
    }

    @SuppressWarnings("unchecked")
    private List<String> getRequestResources(KuraMessage reqMessage) throws KuraException {
        Object requestObject = reqMessage.getProperties().get(ARGS_KEY.value());
        List<String> resources;
        if (requestObject instanceof List) {
            resources = (List<String>) requestObject;
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
        return resources;
    }

    private KuraPayload doGetConfigurations(List<String> resources) throws KuraException {
        if (resources.size() > 2) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(EXPECTED_AT_MOST_TWO_RESOURCES_BUT_FOUND_MESSAGE, resources.size());
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
        String pid = resources.size() == 2 ? resources.get(1) : null;

        //
        // get current configuration with descriptors
        List<ComponentConfiguration> configs = new ArrayList<>();
        try {
            if (pid == null) {
                configs = getAllConfigurations();
            } else {
                configs = getConfiguration(pid);
            }
        } catch (KuraException e) {
            logger.error("Error getting component configurations: {}", e);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        XmlComponentConfigurations xmlConfigs = new XmlComponentConfigurations();
        xmlConfigs.setConfigurations(configs);

        //
        // marshall
        byte[] body = toResponseBody(xmlConfigs);

        //
        // Build response payload
        KuraResponsePayload response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        response.setBody(body);
        return response;
    }

    private List<ComponentConfiguration> getConfiguration(String pid) throws KuraException {
        List<ComponentConfiguration> configs = new ArrayList<>();
        ComponentConfiguration cc = this.configurationService.getComponentConfiguration(pid);
        if (cc != null) {
            configs.add(cc);
        }
        return configs;
    }

    private List<ComponentConfiguration> getAllConfigurations() {

        List<ComponentConfiguration> configs = new ArrayList<>();
        List<String> pidsToIgnore = this.systemService.getDeviceManagementServiceIgnore();

        // the configuration for all components has been requested
        Set<String> componentPids = this.configurationService.getConfigurableComponentPids();
        if (pidsToIgnore != null) {
            Set<String> filteredComponentPids = componentPids.stream()
                    .filter(((Predicate<String>) pidsToIgnore::contains).negate()).collect(Collectors.toSet());
            filteredComponentPids.forEach(componentPid -> {
                ComponentConfiguration cc;
                try {
                    cc = this.configurationService.getComponentConfiguration(componentPid);

                    // TODO: define a validate method for ComponentConfiguration
                    if (cc == null) {
                        logger.error("null ComponentConfiguration");
                        return;
                    }
                    if (cc.getPid() == null || cc.getPid().isEmpty()) {
                        logger.error("null or empty ComponentConfiguration PID");
                        return;
                    }
                    if (cc.getDefinition() == null) {
                        logger.error("null OCD for ComponentConfiguration PID {}", cc.getPid());
                        return;
                    }
                    if (cc.getDefinition().getId() == null || cc.getDefinition().getId().isEmpty()) {

                        logger.error("null or empty OCD ID for ComponentConfiguration PID {}. OCD ID: {}", cc.getPid(),
                                cc.getDefinition().getId());
                        return;
                    }
                    configs.add(cc);
                } catch (KuraException e) {
                    // Nothing needed here
                }

            });
        }
        return configs;
    }

    private KuraPayload doPutConfigurations(List<String> resources, KuraPayload reqPayload) throws KuraException {

        if (resources.size() > 2) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(EXPECTED_AT_MOST_TWO_RESOURCES_BUT_FOUND_MESSAGE, resources.size());
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        String pid = resources.size() == 2 ? resources.get(1) : null;

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
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        this.executor.schedule(new UpdateConfigurationsCallable(pid, xmlConfigs, this.configurationService), 1000,
                TimeUnit.MILLISECONDS);

        return new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
    }

    private KuraPayload doExecRollback(List<String> resources) throws KuraException {
        if (resources.size() > 2) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error(EXPECTED_AT_MOST_TWO_RESOURCES_BUT_FOUND_MESSAGE, resources.size());
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        String snapshotId = resources.size() == 2 ? resources.get(1) : null;
        Long sid;
        try {
            sid = snapshotId != null ? Long.parseLong(snapshotId) : null;
        } catch (NumberFormatException e) {
            logger.error("Bad numeric numeric format for snapshot ID: {}", snapshotId);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        this.executor.schedule(new RollbackCallable(sid, this.configurationService), 1000, TimeUnit.MILLISECONDS);

        return new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
    }

    private KuraPayload doExecSnapshot(List<String> resources) throws KuraException {

        if (resources.size() > 1) {
            logger.error(BAD_REQUEST_TOPIC_MESSAGE, resources);
            logger.error("Expected one resource(s) but found {}", resources.size());
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
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

        KuraResponsePayload responsePayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        responsePayload.setBody(body);
        return responsePayload;
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
                    ? this.xmlConfigurations.getConfigurations()
                    : null;
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
                        this.configurationService.updateConfiguration(this.pid, config.getConfigurationProperties());
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
