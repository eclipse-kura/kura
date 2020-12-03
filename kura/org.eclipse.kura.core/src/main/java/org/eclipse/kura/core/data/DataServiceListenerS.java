/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.data.listener.DataServiceListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The following represents an exception to Semantic Versioning conventions.
 * Though the class implements the org.eclipse.kura.data.listener.DataServiceListener API,
 * it is actually an API consumer (it calls into the API implementors).
 */
class DataServiceListenerS implements DataServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(DataServiceListenerS.class);

    private static final String DATA_SERVICE_LISTENER_REFERENCE = "DataServiceListener";

    private final ComponentContext ctx;
    private final List<DataServiceListener> listeners;

    public DataServiceListenerS(ComponentContext ctx) {
        this.ctx = ctx;
        // thread-safe list implementation
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onConnectionEstablished() {
        Object[] services = this.ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onConnectionEstablished();
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.debug("No registered listener services. Ignoring onConnectionEstablished");
        }

        if (!this.listeners.isEmpty()) {
            for (DataServiceListener listener : this.listeners) {
                try {
                    listener.onConnectionEstablished();
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.warn("No registered listeners. Ignoring onConnectionEstablished");
        }
    }

    @Override
    public void onDisconnecting() {
        Object[] services = this.ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onDisconnecting();
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.debug("No registered listener services. Ignoring onDisconnecting");
        }

        if (!this.listeners.isEmpty()) {
            for (DataServiceListener listener : this.listeners) {
                try {
                    listener.onDisconnecting();
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.warn("No registered listeners. Ignoring onDisconnecting");
        }
    }

    @Override
    public void onDisconnected() {
        Object[] services = this.ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onDisconnected();
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.debug("No registered listener services. Ignoring onDisconnected");
        }

        if (!this.listeners.isEmpty()) {
            for (DataServiceListener listener : this.listeners) {
                try {
                    listener.onDisconnected();
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.warn("No registered listeners. Ignoring onDisconnected");
        }
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        Object[] services = this.ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onConnectionLost(cause);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.debug("No registered listener services. Ignoring onConnectionLost");
        }

        if (!this.listeners.isEmpty()) {
            for (DataServiceListener listener : this.listeners) {
                try {
                    listener.onConnectionLost(cause);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.warn("No registered listeners. Ignoring onConnectionLost");
        }
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        Object[] services = this.ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onMessageArrived(topic, payload, qos,
                            retained);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.debug("No registered listener services. Ignoring onMessageArrived");
        }

        if (!this.listeners.isEmpty()) {
            for (DataServiceListener listener : this.listeners) {
                try {
                    listener.onMessageArrived(topic, payload, qos, retained);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.warn("No registered listeners. Ignoring onMessageArrived");
        }
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        Object[] services = this.ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onMessagePublished(messageId, topic);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.debug("No registered listener services. Ignoring onMessagePublished");
        }

        if (!this.listeners.isEmpty()) {
            for (DataServiceListener listener : this.listeners) {
                try {
                    listener.onMessagePublished(messageId, topic);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.warn("No registered listeners. Ignoring onMessagePublished");
        }
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        Object[] services = this.ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onMessageConfirmed(messageId, topic);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.debug("No registered listener services. Ignoring onMessageConfirmed");
        }

        if (!this.listeners.isEmpty()) {
            for (DataServiceListener listener : this.listeners) {
                try {
                    listener.onMessageConfirmed(messageId, topic);
                } catch (Throwable t) {
                    logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            logger.warn("No registered listeners. Ignoring onMessageConfirmed");
        }
    }

    public void add(DataServiceListener listener) {
        this.listeners.add(listener);
    }

    public void remove(DataServiceListener listener) {
        this.listeners.remove(listener);
    }
}
