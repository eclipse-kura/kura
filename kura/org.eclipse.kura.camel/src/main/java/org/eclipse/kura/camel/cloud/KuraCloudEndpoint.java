/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.cloud;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.eclipse.kura.camel.internal.cloud.CloudClientCache;
import org.eclipse.kura.camel.internal.cloud.CloudClientCache.CloudClientHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint implementation for {@link KuraCloudComponent}
 */
@UriEndpoint(scheme = "kura-cloud", title = "Kura Cloud", label = "iot,kura,cloud", syntax = "kura-cloud:applicationId/appTopic")
public class KuraCloudEndpoint extends DefaultEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(KuraCloudEndpoint.class);

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

    private CloudClientHandle cloudClientHandle;

    private final CloudClientCache cache;

    public KuraCloudEndpoint(String uri, KuraCloudComponent kuraCloudComponent, CloudClientCache cache) {
        super(uri, kuraCloudComponent);
        this.cache = cache;
    }

    @Override
    protected void doStart() throws Exception {
        synchronized (this) {
            this.cloudClientHandle = this.cache.getOrCreate(this.applicationId);
            logger.debug("CloudClient {} -> {}", this.applicationId, this.cloudClientHandle.getClient());
        }
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        synchronized (this) {
            if (this.cloudClientHandle != null) {
                this.cloudClientHandle.close();
                this.cloudClientHandle = null;
            }
        }
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new KuraCloudConsumer(this, processor, this.cloudClientHandle.getClient());
    }

    @Override
    public KuraCloudProducer createProducer() throws Exception {
        return new KuraCloudProducer(this, this.cloudClientHandle.getClient());
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
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getQos() {
        return this.qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public boolean isRetain() {
        return this.retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isControl() {
        return this.control;
    }

    public void setControl(boolean control) {
        this.control = control;
    }

    public String getApplicationId() {
        return this.applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
