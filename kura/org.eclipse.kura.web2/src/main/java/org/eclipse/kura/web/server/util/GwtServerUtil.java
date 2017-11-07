/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Class GwtServerUtil is an utility class required for Kura Server
 * Components in GWT.
 */
public final class GwtServerUtil {

    public static final String PASSWORD_PLACEHOLDER = "Placeholder";

    /** The Constant to check if configuration policy is set to require. */
    public static final String PATTERN_CONFIGURATION_REQUIRE = "configuration-policy=\"require\"";

    /** The Constant to check if the provided interface is a configurable component. */
    public static final String PATTERN_SERVICE_PROVIDE_CONFIGURABLE_COMP = "provide interface=\"org.eclipse.kura.configuration.ConfigurableComponent\"";

    /** The Constant to check if provided interface is Wire Emitter. */
    public static final String PATTERN_SERVICE_PROVIDE_EMITTER = "provide interface=\"org.eclipse.kura.wire.WireEmitter\"";

    /** The Constant to check if provided interface is Wire Receiver. */
    public static final String PATTERN_SERVICE_PROVIDE_RECEIVER = "provide interface=\"org.eclipse.kura.wire.WireReceiver\"";

    /** The Constant to check if the provided interface is a self configuring component. */
    public static final String PATTERN_SERVICE_PROVIDE_SELF_CONFIGURING_COMP = "provide interface=\"org.eclipse.kura.configuration.SelfConfiguringComponent\"";

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(GwtServerUtil.class);

    /**
     * Fills the provided lists with the proper factory IDs of the available
     * configurable or self configuring components.
     *
     * @param emitters
     *            the emitters factory PID list
     * @param receivers
     *            the receivers factory PID list
     * @throws GwtKuraException
     *             if any exception is encountered
     */
    public static void fillFactoriesLists(final List<String> emitters, final List<String> receivers)
            throws GwtKuraException {
        final Bundle[] bundles = FrameworkUtil.getBundle(GwtWireService.class).getBundleContext().getBundles();
        for (final Bundle bundle : bundles) {
            final Enumeration<URL> enumeration = bundle.findEntries("OSGI-INF", "*.xml", false);
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    final URL entry = enumeration.nextElement();
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(entry.openConnection().getInputStream()));
                        final StringBuilder contents = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            contents.append(line);
                        }
                        // Configruation Policy=Require and
                        // SelfConfiguringComponent or ConfigurableComponent
                        if ((contents.toString().contains(PATTERN_SERVICE_PROVIDE_SELF_CONFIGURING_COMP)
                                || contents.toString().contains(PATTERN_SERVICE_PROVIDE_CONFIGURABLE_COMP))
                                && contents.toString().contains(PATTERN_CONFIGURATION_REQUIRE)) {
                            final Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                                    .parse(entry.openConnection().getInputStream());
                            final NodeList nl = dom.getElementsByTagName("property");
                            for (int i = 0; i < nl.getLength(); i++) {
                                final Node n = nl.item(i);
                                if (n instanceof Element) {
                                    final String name = ((Element) n).getAttribute("name");
                                    if ("service.pid".equals(name)) {
                                        final String factoryPid = ((Element) n).getAttribute("value");
                                        if (contents.toString().contains(PATTERN_SERVICE_PROVIDE_EMITTER)) {
                                            emitters.add(factoryPid);
                                        }
                                        if (contents.toString().contains(PATTERN_SERVICE_PROVIDE_RECEIVER)) {
                                            receivers.add(factoryPid);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (final Exception ex) {
                        logger.error("Error while reading Component Definition file {}", entry.getPath());
                    } finally {
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (final IOException e) {
                            logger.error("Error closing File Reader!" + e);
                        }
                    }
                }
            }
        }
    }

    public static Object getObjectValue(GwtConfigParameter param) {
        Object objValue = null;
        GwtConfigParameterType gwtType = param.getType();
        final String strValue = param.getValue();

        if (gwtType == GwtConfigParameterType.STRING) {
            objValue = strValue;
        } else if (strValue != null && !strValue.trim().isEmpty()) {
            switch (gwtType) {
            case LONG:
                objValue = Long.parseLong(strValue);
                break;
            case DOUBLE:
                objValue = Double.parseDouble(strValue);
                break;
            case FLOAT:
                objValue = Float.parseFloat(strValue);
                break;
            case INTEGER:
                objValue = Integer.parseInt(strValue);
                break;
            case SHORT:
                objValue = Short.parseShort(strValue);
                break;
            case BYTE:
                objValue = Byte.parseByte(strValue);
                break;
            case BOOLEAN:
                objValue = Boolean.parseBoolean(strValue);
                break;
            case PASSWORD:
                objValue = new Password(strValue);
                break;
            case CHAR:
                objValue = Character.valueOf(strValue.charAt(0));
                break;
            default:
                break;
            }
        }
        return objValue;
    }

    public static Object[] getObjectValues(GwtConfigParameter param, String[] defaultValues) {
        final List<Object> values = new ArrayList<>();
        final GwtConfigParameterType type = param.getType();
        switch (type) {
        case BOOLEAN:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Boolean.valueOf(value));
                }
            }
            return values.toArray(new Boolean[] {});

        case BYTE:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Byte.valueOf(value));
                }
            }
            return values.toArray(new Byte[] {});

        case CHAR:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(new Character(value.charAt(0)));
                }
            }
            return values.toArray(new Character[] {});

        case DOUBLE:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Double.valueOf(value));
                }
            }
            return values.toArray(new Double[] {});

        case FLOAT:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Float.valueOf(value));
                }
            }
            return values.toArray(new Float[] {});

        case INTEGER:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Integer.valueOf(value));
                }
            }
            return values.toArray(new Integer[] {});

        case LONG:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Long.valueOf(value));
                }
            }
            return values.toArray(new Long[] {});

        case SHORT:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Short.valueOf(value));
                }
            }
            return values.toArray(new Short[] {});

        case PASSWORD:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(new Password(value));
                }
            }
            return values.toArray(new Password[] {});

        case STRING:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(value);
                }
            }
            return values.toArray(new String[] {});
        default:
            return null;
        }
    }

    public static Object getUserDefinedObject(GwtConfigParameter param, Object currentObjValue) {
        Object objValue;

        final int cardinality = param.getCardinality();
        if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
            String strValue = param.getValue();

            if (currentObjValue instanceof Password && PASSWORD_PLACEHOLDER.equals(strValue)) {
                objValue = currentObjValue;
            } else {
                objValue = getObjectValue(param);
            }
        } else {
            String[] strValues = param.getValues();

            if (currentObjValue instanceof Password[]) {
                Password[] currentPasswordValue = (Password[]) currentObjValue;
                for (int i = 0; i < strValues.length; i++) {
                    if (PASSWORD_PLACEHOLDER.equals(strValues[i])) {
                        strValues[i] = new String(currentPasswordValue[i].getPassword());
                    }
                }
            }

            objValue = getObjectValues(param, strValues);
        }
        return objValue;
    }

    /**
     * Strip PID prefix.
     *
     * @param pid
     *            the PID
     * @return the string
     */
    public static String stripPidPrefix(final String pid) {
        final int start = pid.lastIndexOf('.');
        if (start < 0) {
            return pid;
        } else {
            final int begin = start + 1;
            if (begin < pid.length()) {
                return pid.substring(begin);
            } else {
                return pid;
            }
        }
    }

    /**
     * Instantiates a new gwt server util.
     */
    private GwtServerUtil() {
        // No need to instantiate
    }

    public static Map<String, Object> fillPropertiesFromConfiguration(final GwtConfigComponent config,
            final ComponentConfiguration currentCC) {
        // Build the new properties
        final Map<String, Object> properties = new HashMap<>();
        final ComponentConfiguration backupCC = currentCC;
        if (backupCC == null) {
            properties.putAll(config.getProperties());
            for (final GwtConfigParameter gwtConfigParam : config.getParameters()) {
                properties.put(gwtConfigParam.getId(), getUserDefinedObject(gwtConfigParam, null));
            }
        } else {
            final Map<String, Object> backupConfigProp = backupCC.getConfigurationProperties();
            for (final GwtConfigParameter gwtConfigParam : config.getParameters()) {
                final Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
                properties.put(gwtConfigParam.getId(),
                        getUserDefinedObject(gwtConfigParam, currentConfigProp.get(gwtConfigParam.getName())));
            }

            // Force kura.service.pid into properties, if originally present
            if (backupConfigProp.get(KURA_SERVICE_PID) != null) {
                properties.put(KURA_SERVICE_PID, backupConfigProp.get(KURA_SERVICE_PID));
            }
        }
        return properties;
    }

}
