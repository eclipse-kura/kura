/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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

    private static final Logger s_logger = LoggerFactory.getLogger(DataServiceListenerS.class);

    private static final String DATA_SERVICE_LISTENER_REFERENCE = "DataServiceListener";

    private final ComponentContext m_ctx;
    private final List<DataServiceListener> m_listeners;

    public DataServiceListenerS(ComponentContext ctx) {
        this.m_ctx = ctx;
        // thread-safe list implementation
        this.m_listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onConnectionEstablished() {
        Object[] services = this.m_ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onConnectionEstablished();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onConnectionEstablished");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataServiceListener listener : this.m_listeners) {
                try {
                    listener.onConnectionEstablished();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onConnectionEstablished");
        }
    }

    @Override
    public void onDisconnecting() {
        Object[] services = this.m_ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onDisconnecting();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onDisconnecting");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataServiceListener listener : this.m_listeners) {
                try {
                    listener.onDisconnecting();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onDisconnecting");
        }
    }

    @Override
    public void onDisconnected() {
        Object[] services = this.m_ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onDisconnected();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onDisconnected");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataServiceListener listener : this.m_listeners) {
                try {
                    listener.onDisconnected();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onDisconnected");
        }
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        Object[] services = this.m_ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onConnectionLost(cause);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onConnectionLost");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataServiceListener listener : this.m_listeners) {
                try {
                    listener.onConnectionLost(cause);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onConnectionLost");
        }
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        Object[] services = this.m_ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onMessageArrived(topic, payload, qos,
                            retained);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onMessageArrived");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataServiceListener listener : this.m_listeners) {
                try {
                    listener.onMessageArrived(topic, payload, qos, retained);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onMessageArrived");
        }
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        Object[] services = this.m_ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onMessagePublished(messageId, topic);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onMessagePublished");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataServiceListener listener : this.m_listeners) {
                try {
                    listener.onMessagePublished(messageId, topic);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onMessagePublished");
        }
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        Object[] services = this.m_ctx.locateServices(DATA_SERVICE_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataServiceListener) service).onMessageConfirmed(messageId, topic);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onMessageConfirmed");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataServiceListener listener : this.m_listeners) {
                try {
                    listener.onMessageConfirmed(messageId, topic);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onMessageConfirmed");
        }
    }

    public void add(DataServiceListener listener) {
        this.m_listeners.add(listener);
    }

    public void remove(DataServiceListener listener) {
        this.m_listeners.remove(listener);
    }
}
