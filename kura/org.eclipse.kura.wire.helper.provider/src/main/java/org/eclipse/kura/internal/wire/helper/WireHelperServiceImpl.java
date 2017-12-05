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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireComponentDefinition;
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

    /** {@inheritDoc} */
    @Override
    public WireSupport newWireSupport(final WireComponent wireComponent) {
        return new WireSupportImpl(wireComponent, this, this.eventAdmin);
    }

    @Override
    public List<WireComponentDefinition> getComponentDefinitions() throws KuraException {
        List<WireComponentDefinition> wireComponentDefinitions = new ArrayList<>();

        final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
        ServiceReference<ScrService> scrServiceRef = context.getServiceReference(ScrService.class);
        ServiceReference<ConfigurationService> configServiceRef = context
                .getServiceReference(ConfigurationService.class);

        try {
            ScrService scrService = context.getService(scrServiceRef);

            List<Component> components = Arrays.stream(scrService.getComponents())
                    .filter(component -> implementsAnyService(component,
                            new String[] { "org.eclipse.kura.wire.WireComponent", "org.eclipse.kura.wire.WireReceiver",
                                    "org.eclipse.kura.wire.WireEmitter" }))
                    .collect(Collectors.toList());

            List<String> unprocessedPids = new ArrayList<>();
            for (Component component : components) {
                Dictionary componentProperties = component.getProperties();
                List<String> componentPropsKeys = Collections.list(component.getProperties().keys());
                if (!componentPropsKeys.contains("input.cardinality.minimum")) {
                    unprocessedPids.add(component.getName());
                } else {
                    WireComponentDefinition wireComponentDefinition = new WireComponentDefinition();
                    String factoryPid = component.getName();
                    int minInputPorts = (int) componentProperties.get("input.cardinality.minimum");
                    int maxInputPorts = (int) componentProperties.get("input.cardinality.maximum");
                    int defaultInputPorts = (int) componentProperties.get("input.cardinality.default");
                    int minOutputPorts = (int) componentProperties.get("output.cardinality.minimum");
                    int maxOutputPorts = (int) componentProperties.get("output.cardinality.maximum");
                    int defaultOutputPorts = (int) componentProperties.get("output.cardinality.default");

                    wireComponentDefinition.setFactoryPid(factoryPid);
                    wireComponentDefinition.setMinInputPorts(minInputPorts);
                    wireComponentDefinition.setMaxInputPorts(maxInputPorts);
                    wireComponentDefinition.setDefaultInputPorts(defaultInputPorts);
                    wireComponentDefinition.setMinOutputPorts(minOutputPorts);
                    wireComponentDefinition.setMaxOutputPorts(maxOutputPorts);
                    wireComponentDefinition.setDefaultOutputPorts(defaultOutputPorts);

                    wireComponentDefinitions.add(wireComponentDefinition);
                }
            }

            List<String> receiverPids = new ArrayList<>();
            ConfigurationService configurationService = context.getService(configServiceRef);
            for (ComponentConfiguration receiver : configurationService
                    .getServiceProviderOCDs("org.eclipse.kura.wire.WireReceiver")) {
                WireComponentDefinition wireComponentDefinition = new WireComponentDefinition();
                wireComponentDefinition.setFactoryPid(receiver.getPid());
                wireComponentDefinition.setMinInputPorts(1);
                wireComponentDefinition.setMaxInputPorts(1);
                wireComponentDefinition.setDefaultInputPorts(1);
                wireComponentDefinition.setMinOutputPorts(0);
                wireComponentDefinition.setMaxOutputPorts(0);
                wireComponentDefinition.setDefaultOutputPorts(0);

                wireComponentDefinitions.add(wireComponentDefinition);
                receiverPids.add(receiver.getPid());

            }
            for (ComponentConfiguration emitter : configurationService
                    .getServiceProviderOCDs("org.eclipse.kura.wire.WireEmitter")) {

                WireComponentDefinition wireComponentDefinition = new WireComponentDefinition();
                wireComponentDefinition.setFactoryPid(emitter.getPid());
                if (!receiverPids.contains(emitter.getPid())) {
                    wireComponentDefinition.setMinInputPorts(0);
                    wireComponentDefinition.setMaxInputPorts(0);
                    wireComponentDefinition.setDefaultInputPorts(0);
                }
                wireComponentDefinition.setMinOutputPorts(1);
                wireComponentDefinition.setMaxOutputPorts(1);
                wireComponentDefinition.setDefaultOutputPorts(1);

                wireComponentDefinitions.add(wireComponentDefinition);
            }

        } finally {
            context.ungetService(scrServiceRef);
            context.ungetService(configServiceRef);
        }

        return wireComponentDefinitions;
    }

    private static boolean implementsAnyService(Component component, String[] classes) {
        final String[] services = component.getServices();
        if (services == null) {
            return false;
        }
        for (String className : classes) {
            for (String s : services) {
                if (s.equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }
}
