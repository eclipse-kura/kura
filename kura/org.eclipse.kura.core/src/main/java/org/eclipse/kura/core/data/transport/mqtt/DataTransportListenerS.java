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
package org.eclipse.kura.core.data.transport.mqtt;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The following represents an exception to Semantic Versioning conventions.
 * Though the class implements the org.eclipse.kura.data.transport.listener.DataTransportListener API,
 * it is actually an API consumer (it calls into the API implementors).
 */
class DataTransportListenerS implements DataTransportListener {

    private static final Logger s_logger = LoggerFactory.getLogger(DataTransportListenerS.class);

    private static final String DATA_TRANSPORT_LISTENER_REFERENCE = "DataTransportListener";

    private final ComponentContext m_ctx;
    private final List<DataTransportListener> m_listeners;

    public DataTransportListenerS(ComponentContext ctx) {
        this.m_ctx = ctx;
        this.m_listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onConnectionEstablished(boolean newSession) {
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onConnectionEstablished(newSession);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onConnectionEstablished");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
                try {
                    listener.onConnectionEstablished(newSession);
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
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onDisconnecting();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onDisconnecting");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
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
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onDisconnected();
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onDisconnected");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
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
    public void onConfigurationUpdating(boolean wasConnected) {
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onConfigurationUpdating(wasConnected);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onConfigurationUpdating");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
                try {
                    listener.onConfigurationUpdating(wasConnected);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onConfigurationUpdating");
        }
    }

    @Override
    public void onConfigurationUpdated(boolean wasConnected) {
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onConfigurationUpdated(wasConnected);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onConfigurationUpdated");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
                try {
                    listener.onConfigurationUpdated(wasConnected);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onConfigurationUpdated");
        }
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onConnectionLost(cause);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onConnectionLost");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
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
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onMessageArrived(topic, payload, qos,
                            retained);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onMessageArrived");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
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
    public void onMessageConfirmed(DataTransportToken token) {
        Object[] services = this.m_ctx.locateServices(DATA_TRANSPORT_LISTENER_REFERENCE);
        if (services != null) {
            for (Object service : services) {
                try {
                    ((org.eclipse.kura.data.DataTransportListener) service).onMessageConfirmed(token);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.debug("No registered listener services. Ignoring onMessageConfirmed");
        }

        if (!this.m_listeners.isEmpty()) {
            for (DataTransportListener listener : this.m_listeners) {
                try {
                    listener.onMessageConfirmed(token);
                } catch (Throwable t) {
                    s_logger.warn("Unexpected Throwable", t);
                }
            }
        } else {
            s_logger.warn("No registered listeners. Ignoring onMessageConfirmed");
        }
    }

    public void add(DataTransportListener listener) {
        this.m_listeners.add(listener);
    }

    public void remove(DataTransportListener listener) {
        this.m_listeners.remove(listener);
    }
}
