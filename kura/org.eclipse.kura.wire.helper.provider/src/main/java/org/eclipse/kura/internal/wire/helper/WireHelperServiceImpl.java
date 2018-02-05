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
import static org.eclipse.kura.wire.graph.Constants.EMITTER_PORT_COUNT_PROP_NAME;
import static org.eclipse.kura.wire.graph.Constants.RECEIVER_PORT_COUNT_PROP_NAME;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.Arrays;

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

    private static final WireMessages wireMessages = LocalizationAdapter.adapt(WireMessages.class);

    private volatile EventAdmin eventAdmin;

    public void bindEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unbindEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    /** {@inheritDoc} */
    @Override
    public String getPid(final WireComponent wireComponent) {
        requireNonNull(wireComponent, wireMessages.wireComponentNonNull());
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
        requireNonNull(wireComponentPid, wireMessages.wireComponentPidNonNull());
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
        requireNonNull(wireComponent, wireMessages.wireComponentNonNull());
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
        requireNonNull(wireComponentPid, wireMessages.wireComponentPidNonNull());
        final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)
                        && context.getService(ref) instanceof WireEmitter) {
                    return true;
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
        requireNonNull(wireComponentPid, wireMessages.wireComponentPidNonNull());
        final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)
                        && context.getService(ref) instanceof WireReceiver) {
                    return true;
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return false;
    }

    private int getIntOrDefault(Object portCount, int defaultValue) {
        if (portCount instanceof Integer) {
            return (Integer) portCount;
        }
        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public WireSupport newWireSupport(final WireComponent wireComponent) {
        requireNonNull(wireComponent, wireMessages.wireComponentNonNull());
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        return Arrays.stream(ServiceUtil.getServiceReferences(context, WireComponent.class, null)).filter(ref -> {
            final boolean matches = context.getService(ref) == wireComponent;
            context.ungetService(ref);
            return matches;
        }).map(ref -> {
            final String servicePid = (String) ref.getProperty(SERVICE_PID);
            final String kuraServicePid = (String) ref.getProperty(KURA_SERVICE_PID);
            int receiverPortCount = getIntOrDefault(ref.getProperty(RECEIVER_PORT_COUNT_PROP_NAME.value()),
                    wireComponent instanceof WireReceiver ? 1 : 0);
            int emitterPortCount = getIntOrDefault(ref.getProperty(EMITTER_PORT_COUNT_PROP_NAME.value()),
                    wireComponent instanceof WireEmitter ? 1 : 0);

            return new WireSupportImpl(wireComponent, servicePid, kuraServicePid, this.eventAdmin, receiverPortCount,
                    emitterPortCount);
        }).findAny().orElse(null);
    }
}
