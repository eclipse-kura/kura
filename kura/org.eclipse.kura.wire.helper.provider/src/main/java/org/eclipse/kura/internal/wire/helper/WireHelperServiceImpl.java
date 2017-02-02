/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.helper;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

/**
 * The Class WireHelperServiceImpl is the implementation of
 * {@link WireHelperService}
 */
public final class WireHelperServiceImpl implements WireHelperService {

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Event Admin Service */
    private volatile EventAdmin eventAdmin;

    /**
     * Binds the Event Admin Service.
     *
     * @param eventAdmin
     *            the new Event Admin Service
     */
    public synchronized void bindEventAdmin(final EventAdmin eventAdmin) {
        if (this.eventAdmin == null) {
            this.eventAdmin = eventAdmin;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPid(final WireComponent wireComponent) {
        requireNonNull(wireComponent, message.wireComponentNonNull());
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
        try {
            for (final ServiceReference<?> ref : refs) {
                final WireComponent wc = (WireComponent) context.getService(ref);
                if (wc == wireComponent) {
                    return String.valueOf(ref.getProperty(KURA_SERVICE_PID));
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getServicePid(final String wireComponentPid) {
        requireNonNull(wireComponentPid, message.wireComponentPidNonNull());
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
                    return String.valueOf(ref.getProperty(SERVICE_PID));
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getServicePid(final WireComponent wireComponent) {
        requireNonNull(wireComponent, message.wireComponentNonNull());
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
        try {
            for (final ServiceReference<?> ref : refs) {
                final WireComponent wc = (WireComponent) context.getService(ref);
                if (wc == wireComponent) {
                    return String.valueOf(ref.getProperty(SERVICE_PID));
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmitter(final String wireComponentPid) {
        requireNonNull(wireComponentPid, message.wireComponentPidNonNull());
        final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
                    final WireComponent wc = (WireComponent) context.getService(ref);
                    if (wc instanceof WireEmitter) {
                        return true;
                    }
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReceiver(final String wireComponentPid) {
        requireNonNull(wireComponentPid, message.wireComponentPidNonNull());
        final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
                    final WireComponent wc = (WireComponent) context.getService(ref);
                    if (wc instanceof WireReceiver) {
                        return true;
                    }
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public WireSupport newWireSupport(final WireComponent wireComponent) {
        return new WireSupportImpl(wireComponent, this, this.eventAdmin);
    }

    /**
     * Unbinds the Event Admin Service.
     *
     * @param eventAdmin
     *            the new Event Admin Service
     */
    public synchronized void unbindEventAdmin(final EventAdmin eventAdmin) {
        if (this.eventAdmin == eventAdmin) {
            this.eventAdmin = null;
        }
    }

}
