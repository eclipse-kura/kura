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
import static java.util.stream.Collectors.toSet;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class WireHelperServiceImpl is the implementation of
 * {@link WireHelperService}
 */
public final class WireHelperServiceImpl implements WireHelperService {

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);
    private static final Logger logger = LoggerFactory.getLogger(WireHelperServiceImpl.class);

    private BundleContext bundleContext;
    private volatile EventAdmin eventAdmin;
    private final AtomicReference<WireService> wireServiceRef = new AtomicReference<>(null);

    private ServiceTracker<WireService, WireService> serviceTracker;

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
     * OSGi service component activation callback
     *
     * @param bundleContext
     *            the {@link BundleContext} instance
     */
    protected void activate(final BundleContext bundleContext) {
        logger.debug(message.activatingHelperService());
        this.bundleContext = bundleContext;
        this.serviceTracker = createTracker();
        this.serviceTracker.open();
        logger.debug(message.activatingHelperServiceDone());
    }

    /**
     * OSGi service component deactivation callback
     */
    protected void deactivate() {
        logger.debug(message.deactivatingHelperService());
        if (this.serviceTracker != null) {
            this.serviceTracker.close();
            this.serviceTracker = null;
        }
        logger.debug(message.deactivatingHelperServiceDone());
    }

    /** {@inheritDoc} */
    @Override
    public String getPid(final WireComponent wireComponent) {
        requireNonNull(wireComponent, message.wireComponentNonNull());
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(this.bundleContext, WireComponent.class,
                null);
        try {
            for (final ServiceReference<?> ref : refs) {
                final WireComponent wc = (WireComponent) this.bundleContext.getService(ref);
                if (wc == wireComponent) {
                    return String.valueOf(ref.getProperty(KURA_SERVICE_PID));
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getServicePid(final String wireComponentPid) {
        requireNonNull(wireComponentPid, message.wireComponentPidNonNull());
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(this.bundleContext, WireComponent.class,
                null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
                    return String.valueOf(ref.getProperty(SERVICE_PID));
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getServicePid(final WireComponent wireComponent) {
        requireNonNull(wireComponent, message.wireComponentNonNull());
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(this.bundleContext, WireComponent.class,
                null);
        try {
            for (final ServiceReference<?> ref : refs) {
                final WireComponent wc = (WireComponent) this.bundleContext.getService(ref);
                if (wc == wireComponent) {
                    return String.valueOf(ref.getProperty(SERVICE_PID));
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<WireConfiguration> getWireConfiguration(final String emitterPid, final String receiverPid) {
        requireNonNull(emitterPid, message.emitterPidNonNull());
        requireNonNull(receiverPid, message.receiverPidNonNull());

        final WireConfiguration wireConfiguration = new WireConfiguration(emitterPid, receiverPid);
        final WireService wireService = this.wireServiceRef.get();
        if (wireService == null) {
            return Optional.empty();
        }
        final Set<WireConfiguration> wireConfs = wireService.getWireConfigurations();
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
        requireNonNull(emitterPid, message.emitterPidNonNull());
        final WireService wireService = this.wireServiceRef.get();
        if (wireService == null) {
            return Collections.emptySet();
        }
        final Set<WireConfiguration> wireConfs = wireService.getWireConfigurations();
        return wireConfs.stream().filter(wc -> wc.getEmitterPid().equalsIgnoreCase(emitterPid)).collect(toSet());
    }

    /** {@inheritDoc} */
    @Override
    public Set<WireConfiguration> getWireConfigurationsByReceiverPid(final String receiverPid) {
        requireNonNull(receiverPid, message.receiverPidNonNull());
        final WireService wireService = this.wireServiceRef.get();
        if (wireService == null) {
            return Collections.emptySet();
        }
        final Set<WireConfiguration> wireConfs = wireService.getWireConfigurations();
        return wireConfs.stream().filter(wc -> wc.getReceiverPid().equalsIgnoreCase(receiverPid)).collect(toSet());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmitter(final String wireComponentPid) {
        requireNonNull(wireComponentPid, message.wireComponentPidNonNull());
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(this.bundleContext, WireComponent.class,
                null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)
                        && this.bundleContext.getService(ref) instanceof WireEmitter) {
                    return true;
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReceiver(final String wireComponentPid) {
        requireNonNull(wireComponentPid, message.wireComponentPidNonNull());
        final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(this.bundleContext, WireComponent.class,
                null);
        try {
            for (final ServiceReference<?> ref : refs) {
                if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)
                        && this.bundleContext.getService(ref) instanceof WireReceiver) {
                    return true;
                }
            }
        } finally {
            ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public WireSupport newWireSupport(final WireComponent wireComponent) {
        return new WireSupportImpl(wireComponent, this, this.eventAdmin);
    }

    /**
     * Creates a {@link ServiceTracker} to track {@link WireService} instances
     *
     * @return {@link ServiceTracker} instance
     */
    private ServiceTracker<WireService, WireService> createTracker() {
        return new ServiceTracker<WireService, WireService>(this.bundleContext, WireService.class, null) {

            @Override
            public WireService addingService(final ServiceReference<WireService> reference) {
                final WireService wireService = super.addingService(reference);
                WireHelperServiceImpl.this.wireServiceRef.compareAndSet(null, wireService);
                return wireService;
            }

            @Override
            public void removedService(final ServiceReference<WireService> reference, final WireService service) {
                WireHelperServiceImpl.this.wireServiceRef.compareAndSet(service, null);
                super.removedService(reference, service);
            }

            @Override
            public void modifiedService(final ServiceReference<WireService> reference, final WireService service) {
                WireHelperServiceImpl.this.wireServiceRef
                        .set(WireHelperServiceImpl.this.bundleContext.getService(reference));
            }
        };
    }
}
