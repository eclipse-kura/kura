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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
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
    private volatile WireService wireService;

    /**
     * Binds the Event Admin Service.
     *
     * @param eventAdmin
     *            the new Event Admin Service
     */
    public void bindEventAdmin(final EventAdmin eventAdmin) {
        if (this.eventAdmin == null) {
            this.eventAdmin = eventAdmin;
        }
    }

    /**
     * Unbinds the Event Admin Service.
     *
     * @param eventAdmin
     *            the new Event Admin Service
     */
    public void unbindEventAdmin(final EventAdmin eventAdmin) {
        if (this.eventAdmin == eventAdmin) {
            this.eventAdmin = null;
        }
    }

    /**
     * Binds the {@link WireService} instance
     *
     * @param wireService
     *            the new {@link WireService} instance
     */
    public void bindWireService(final WireService wireService) {
        if (this.wireService == null) {
            this.wireService = wireService;
        }
    }

    /**
     * Unbinds the {@link WireService} instance
     *
     * @param wireService
     *            the new {@link WireService} instance
     */
    public void unbindWireService(final WireService wireService) {
        if (this.wireService == wireService) {
            this.wireService = null;
        }
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
    public Optional<WireConfiguration> getWireConfiguration(final String emitterPid, final String receiverPid) {
        requireNonNull(emitterPid, wireMessages.emitterPidNonNull());
        requireNonNull(receiverPid, wireMessages.receiverPidNonNull());

        final WireConfiguration wireConfiguration = new WireConfiguration(emitterPid, receiverPid);
        final Set<WireConfiguration> wireConfs = this.wireService.getWireConfigurations();

        for (final WireConfiguration wc : wireConfs) {
            if (wc.equals(wireConfiguration)) {
                return Optional.of(wc);
            }
        }
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public Set<WireConfiguration> getWireConfigurationsByEmitterPid(final String emitterPid) {
        requireNonNull(emitterPid, wireMessages.emitterPidNonNull());
        final Set<WireConfiguration> wireConfs = this.wireService.getWireConfigurations();
        return wireConfs.stream().filter(wc -> wc.getEmitterPid().equalsIgnoreCase(emitterPid))
                .collect(Collectors.toSet());
    }

    /** {@inheritDoc} */
    @Override
    public Set<WireConfiguration> getWireConfigurationsByReceiverPid(final String receiverPid) {
        requireNonNull(receiverPid, wireMessages.receiverPidNonNull());
        final Set<WireConfiguration> wireConfs = this.wireService.getWireConfigurations();
        return wireConfs.stream().filter(wc -> wc.getReceiverPid().equalsIgnoreCase(receiverPid))
                .collect(Collectors.toSet());
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

    /** {@inheritDoc} */
    @Override
    public WireSupport newWireSupport(final WireComponent wireComponent) {
        return new WireSupportImpl(wireComponent, this, this.eventAdmin);
    }
}
