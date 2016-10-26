/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.web.server.util;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GwtWireServiceUtil {

    /** The Logger instance. */
    private static final Logger s_logger = LoggerFactory.getLogger(GwtWireServiceUtil.class);

    private GwtWireServiceUtil() {
        // static factory methods container
    }

    public static String getDriverByPid(final String pid) throws GwtKuraException {
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

    public static String getFactoryPid(final String pid) throws GwtKuraException {
        final BundleContext context = FrameworkUtil.getBundle(GwtWireServiceUtil.class).getBundleContext();
        final ServiceReference<?>[] refs = getServiceReferences(context, WireComponent.class.getName(), null);
        for (final ServiceReference<?> ref : refs) {
            if (ref.getProperty(KURA_SERVICE_PID).equals(pid)) {
                return (String) ref.getProperty("service.factoryPid");
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

    public static String getType(final String pid) throws GwtKuraException {
        final BundleContext context = FrameworkUtil.getBundle(GwtWireServiceUtil.class).getBundleContext();
        final ServiceReference<?>[] refs = getServiceReferences(context, WireComponent.class.getName(), null);
        for (final ServiceReference<?> ref : refs) {
            WireComponent wc = null;
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

    public static String getWireComponentsJson(final List<GwtWireComponentConfiguration> list) throws GwtKuraException {
        final JSONObject wireCompConfig = new JSONObject();
        int i = 0;
        for (final GwtWireComponentConfiguration wcConf : list) {
            final JSONObject wireConf = new JSONObject();
            try {
                wireConf.put("fPid", wcConf.getFactoryPid());
                wireConf.put("pid", wcConf.getPid());
                wireConf.put("name", wcConf.getPid());
                wireConf.put("type", wcConf.getType());
                wireConf.put("driver", wcConf.getDriverPid());
                wireCompConfig.put(String.valueOf(i++), wireConf);
            } catch (final JSONException exception) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
            }
        }
        try {
            wireCompConfig.put("length", String.valueOf(i));
        } catch (final JSONException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
        return wireCompConfig.toString();
    }

    public static Set<WireConfiguration> getWireConfigurations() throws GwtKuraException {
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
        return wireService.getWireConfigurations();
    }

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

    public static List<GwtWireConfiguration> getWireConfigurationsFromJson(final JSONObject json) throws JSONException {
        final List<GwtWireConfiguration> list = new ArrayList<GwtWireConfiguration>();
        for (int i = 0; i < json.length(); i++) {
            final JSONObject jsonObject = json.getJSONObject(String.valueOf(i));
            final String emitter = jsonObject.getString("producer");
            final String receiver = jsonObject.getString("consumer");
            final GwtWireConfiguration configuration = new GwtWireConfiguration();
            configuration.setEmitterPid(emitter);
            configuration.setReceiverPid(receiver);
            list.add(configuration);
        }
        return list;
    }

    public static String getWireConfigurationsJson(final List<GwtWireConfiguration> list) throws GwtKuraException {
        final JSONObject wireConfigs = new JSONObject();
        int i = 0;
        for (final GwtWireConfiguration wcConf : list) {
            final JSONObject wireConf = new JSONObject();
            try {
                wireConf.put("emitter", wcConf.getEmitterPid());
                wireConf.put("receiver", wcConf.getReceiverPid());
                wireConfigs.put(String.valueOf(i++), wireConf);
            } catch (final JSONException exception) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
            }
        }
        try {
            wireConfigs.put("length", String.valueOf(i));
        } catch (final JSONException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
        return wireConfigs.toString();
    }

}
