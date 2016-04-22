/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.cloud;

import static org.eclipse.kura.camel.cloud.KuraCloudComponent.clientCache;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.eclipse.kura.camel.utils.KuraServiceFactory;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;

@UriEndpoint(scheme = "kura-cloud", title = "Kura Cloud", label = "iot,kura,cloud", syntax = "kura-cloud:applicationId/appTopic")
public class KuraCloudEndpoint extends DefaultEndpoint {

    @UriParam(defaultValue = "")
    private String applicationId = "";

    @UriParam(defaultValue = "")
    private String topic = "";

    @UriParam(defaultValue = "0")
    private int qos;

    @UriParam(defaultValue = "false")
    private boolean retain = false;

    @UriParam(defaultValue = "5")
    private int priority = 5;

    @UriParam(defaultValue = "false")
    private boolean control = false;

    @UriParam(defaultValue = "")
    private String deviceId;

    private CloudService cloudService;

    public KuraCloudEndpoint(String uri, KuraCloudComponent kuraCloudComponent, CloudService cloudService) {
        super(uri, kuraCloudComponent);
        this.cloudService = cloudService;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        CloudClient cloudClient = clientCache().getOrCreate(applicationId, cloudService);
        return new KuraCloudConsumer(this, processor, cloudClient);
    }

    @Override
    public KuraCloudProducer createProducer() throws Exception {
        CloudClient cloudClient = clientCache().getOrCreate(applicationId, cloudService);
        return new KuraCloudProducer(this, cloudClient);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public KuraCloudComponent getComponent() {
        return (KuraCloudComponent) super.getComponent();
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isControl() {
        return control;
    }

    public void setControl(boolean control) {
        this.control = control;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public CloudService getCloudService() {
        if(cloudService != null) {
            return cloudService;
        }

        if(getComponent().getCloudService() != null) {
            cloudService = getComponent().getCloudService();
        } else {
            cloudService = KuraServiceFactory.retrieveService(CloudService.class, this.getCamelContext().getRegistry());
        }

        return cloudService;
    }

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

}
