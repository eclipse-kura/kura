/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * The Class GwtWireServiceUtil.
 */
public final class GwtWireServiceUtil {

    /**
     * Instantiates a new gwt wire service util.
     */
    private GwtWireServiceUtil() {
        // static factory methods container
    }

    /**
     * Gets the driver by PID.
     *
     * @param pid
     *            the PID
     * @return the driver by PID
     */
    public static String getDriverByPid(final String pid) {
        final BundleContext context = FrameworkUtil.getBundle(GwtWireServiceUtil.class).getBundleContext();
        final ServiceReference<?>[] refs = getServiceReferences(context, WireComponent.class.getName(), null);
        for (final ServiceReference<?> ref : refs) {
            if (ref.getProperty(KURA_SERVICE_PID).equals(pid)) {
                final String driver = String.valueOf(ref.getProperty("driver.pid"));
                context.ungetService(ref);
                return driver;
            }
        }
        return null;
    }

    /**
     * Gets the factory PID.
     *
     * @param pid
     *            the PID
     * @return the factory PID
     */
    public static String getFactoryPid(final String pid) {
        final BundleContext context = FrameworkUtil.getBundle(GwtWireServiceUtil.class).getBundleContext();
        final ServiceReference<?>[] refs = getServiceReferences(context, WireComponent.class.getName(), null);
        for (final ServiceReference<?> ref : refs) {
            if (ref.getProperty(KURA_SERVICE_PID).equals(pid)) {
                return (String) ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
            }
        }
        return null;
    }

    /**
     * Returns references to <em>all</em> services matching the given class name
     * and OSGi filter.
     *
     * @param bundleContext
     *            OSGi bundle context
     * @param clazz
     *            fully qualified class name (can be <code>null</code>)
     * @param filter
     *            valid OSGi filter (can be <code>null</code>)
     * @return non-<code>null</code> array of references to matching services
     */
    public static ServiceReference<?>[] getServiceReferences(final BundleContext bundleContext, final String clazz,
            final String filter) {
        try {
            final ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz, filter);
            return refs == null ? new ServiceReference[0] : refs;
        } catch (final InvalidSyntaxException ise) {
            throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ise);
        }
    }

    /**
     * Gets the type.
     *
     * @param pid
     *            the PID
     * @return the type
     */
    public static String getType(final String pid) {
        final BundleContext context = FrameworkUtil.getBundle(GwtWireServiceUtil.class).getBundleContext();
        final ServiceReference<?>[] refs = getServiceReferences(context, WireComponent.class.getName(), null);
        for (final ServiceReference<?> ref : refs) {
            WireComponent wc;
            if (ref.getProperty(KURA_SERVICE_PID).equals(pid)) {
                wc = (WireComponent) context.getService(ref);
            } else {
                continue;
            }
            boolean isEmitter = false, isReceiver = false;
            if (wc instanceof WireEmitter) {
                isEmitter = true;
            }
            if (wc instanceof WireReceiver) {
                isReceiver = true;
            }
            if (isEmitter && isReceiver) {
                return "both";
            }
            if (isEmitter) {
                return "producer";
            }
            if (isReceiver) {
                return "consumer";
            }
            context.ungetService(ref);
        }
        return "";
    }

    /**
     * Gets the wire components.
     *
     * @return the wire components
     * @throws GwtKuraException
     *             the gwt kura exception
     */
    public static List<String> getWireComponents() throws GwtKuraException {
        final WireHelperService helperService = ServiceLocator.getInstance().getService(WireHelperService.class);
        final List<String> list = new ArrayList<String>();
        final BundleContext context = FrameworkUtil.getBundle(GwtWireServiceUtil.class).getBundleContext();
        final ServiceReference<?>[] refs = getServiceReferences(context, WireComponent.class.getName(), null);
        for (final ServiceReference<?> ref : refs) {
            final WireComponent wc = (WireComponent) context.getService(ref);
            final String pid = helperService.getPid(wc);
            list.add(pid);
            context.ungetService(ref);
        }
        return list;
    }

    /**
     * Gets the wire components JSON.
     *
     * @param list
     *            the list containing the Wire Component Configuration
     * @return the wire components JSON
     */
    public static String getWireComponentsJson(final List<GwtWireComponentConfiguration> list) {
        final JsonObject wireCompConfig = Json.object();
        int i = 0;
        for (final GwtWireComponentConfiguration wcConf : list) {
            final JsonObject wireConf = Json.object().add("fPid", wcConf.getFactoryPid()).add("pid", wcConf.getPid())
                    .add("name", wcConf.getPid()).add("type", wcConf.getType()).add("driver", wcConf.getDriverPid());
            wireCompConfig.add(String.valueOf(i++), wireConf);
        }
        wireCompConfig.add("length", String.valueOf(i));
        return wireCompConfig.toString();
    }

    /**
     * Gets the wire configurations.
     *
     * @return the wire configurations
     * @throws GwtKuraException
     *             the gwt kura exception
     */
    public static Set<WireConfiguration> getWireConfigurations() throws GwtKuraException {
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
        return wireService.getWireConfigurations();
    }

    /**
     * Gets the wire configurations by emitter PID.
     *
     * @param pid
     *            the PID
     * @return the wire configurations by emitter PID
     * @throws GwtKuraException
     *             the gwt kura exception
     */
    public static List<WireConfiguration> getWireConfigurationsByEmitterPid(final String pid) throws GwtKuraException {
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
        final List<WireConfiguration> wireConfs = new ArrayList<WireConfiguration>();
        for (final WireConfiguration wireConf : wireService.getWireConfigurations()) {
            final String emitterPid = wireConf.getEmitterPid();
            if (emitterPid.equalsIgnoreCase(pid)) {
                wireConfs.add(wireConf);
            }
        }
        return wireConfs;
    }

    /**
     * Gets the wire configurations by receiver PID.
     *
     * @param pid
     *            the PID
     * @return the wire configurations by receiver PID
     * @throws GwtKuraException
     *             the gwt kura exception
     */
    public static List<WireConfiguration> getWireConfigurationsByReceiverPid(final String pid) throws GwtKuraException {
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
        final List<WireConfiguration> wireConfs = new ArrayList<WireConfiguration>();
        for (final WireConfiguration wireConf : wireService.getWireConfigurations()) {
            final String receiverPid = wireConf.getReceiverPid();
            if (receiverPid.equalsIgnoreCase(pid)) {
                wireConfs.add(wireConf);
            }
        }
        return wireConfs;
    }

    /**
     * Gets the wire configurations from JSON.
     *
     * @param json
     *            the JSON
     * @return the wire configurations from JSON
     */
    public static List<GwtWireConfiguration> getWireConfigurationsFromJson(final JsonObject json) {
        final List<GwtWireConfiguration> list = new ArrayList<GwtWireConfiguration>();
        for (int i = 0; i < json.size(); i++) {
            final JsonObject jsonObject = json.get(String.valueOf(i)).asObject();
            final String emitter = jsonObject.getString("producer", null);
            final String receiver = jsonObject.getString("consumer", null);
            final GwtWireConfiguration configuration = new GwtWireConfiguration();
            configuration.setEmitterPid(emitter);
            configuration.setReceiverPid(receiver);
            list.add(configuration);
        }
        return list;
    }

    /**
     * Gets the wire configurations JSON.
     *
     * @param list
     *            the list containing Wire Configurations
     * @return the wire configurations JSON
     */
    public static String getWireConfigurationsJson(final List<GwtWireConfiguration> list) {
        final JsonObject wireConfigs = Json.object();
        int i = 0;
        for (final GwtWireConfiguration wcConf : list) {
            final JsonObject wireConf = Json.object();
            wireConf.add("emitter", wcConf.getEmitterPid()).add("receiver", wcConf.getReceiverPid());
            wireConfigs.add(String.valueOf(i++), wireConf);
        }
        wireConfigs.add("length", String.valueOf(i));
        return wireConfigs.toString();
    }

}
