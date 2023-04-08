/*******************************************************************************
 * Copyright (c) 2016, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.driver.descriptor.DriverDescriptor;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationList;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.server.servlet.DeviceSnapshotsServlet;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.validator.PasswordStrengthValidators;
import org.eclipse.kura.web.shared.validator.Validator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The Class GwtServerUtil is an utility class required for Kura Server
 * Components in GWT.
 */
public final class GwtServerUtil {

    private static final String JSON_FORMAT = "json";
    private static final String XML_FORMAT = "xml";

    public static final String PASSWORD_PLACEHOLDER = "Placeholder";

    /** The Constant to check if configuration policy is set to require. */
    public static final String PATTERN_CONFIGURATION_REQUIRE = "configuration-policy=\"require\"";

    /**
     * The Constant to check if the provided interface is a configurable component.
     */
    public static final String PATTERN_SERVICE_PROVIDE_CONFIGURABLE_COMP = "provide interface=\"org.eclipse.kura.configuration.ConfigurableComponent\"";

    /** The Constant to check if provided interface is Wire Emitter. */
    public static final String PATTERN_SERVICE_PROVIDE_EMITTER = "provide interface=\"org.eclipse.kura.wire.WireEmitter\"";

    /** The Constant to check if provided interface is Wire Receiver. */
    public static final String PATTERN_SERVICE_PROVIDE_RECEIVER = "provide interface=\"org.eclipse.kura.wire.WireReceiver\"";

    /**
     * The Constant to check if the provided interface is a self configuring
     * component.
     */
    public static final String PATTERN_SERVICE_PROVIDE_SELF_CONFIGURING_COMP = "provide interface=\"org.eclipse.kura.configuration.SelfConfiguringComponent\"";

    private static final String DRIVER_PID = "driver.pid";

    private static final Logger logger = LoggerFactory.getLogger(GwtServerUtil.class);

    public static Object getObjectValue(GwtConfigParameter param) {
        Object objValue = null;
        GwtConfigParameterType gwtType = param.getType();
        final String strValue = param.getValue();

        if (gwtType == GwtConfigParameterType.STRING) {
            objValue = strValue;
        } else if (strValue != null && !strValue.trim().isEmpty()) {
            final String trimmedValue = strValue.trim();
            switch (gwtType) {
                case LONG:
                    objValue = Long.parseLong(trimmedValue);
                    break;
                case DOUBLE:
                    objValue = Double.parseDouble(trimmedValue);
                    break;
                case FLOAT:
                    objValue = Float.parseFloat(trimmedValue);
                    break;
                case INTEGER:
                    objValue = Integer.parseInt(trimmedValue);
                    break;
                case SHORT:
                    objValue = Short.parseShort(trimmedValue);
                    break;
                case BYTE:
                    objValue = Byte.parseByte(trimmedValue);
                    break;
                case BOOLEAN:
                    objValue = Boolean.parseBoolean(trimmedValue);
                    break;
                case PASSWORD:
                    objValue = new Password(trimmedValue);
                    break;
                case CHAR:
                    objValue = Character.valueOf(trimmedValue.charAt(0));
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

        List<String> trimmedValues = Stream.of(defaultValues).map(String::trim).collect(Collectors.toList());

        switch (type) {
            case BOOLEAN:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(Boolean.valueOf(value));
                    }
                }
                return values.toArray(new Boolean[] {});

            case BYTE:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(Byte.valueOf(value));
                    }
                }
                return values.toArray(new Byte[] {});

            case CHAR:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(new Character(value.charAt(0)));
                    }
                }
                return values.toArray(new Character[] {});

            case DOUBLE:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(Double.valueOf(value));
                    }
                }
                return values.toArray(new Double[] {});

            case FLOAT:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(Float.valueOf(value));
                    }
                }
                return values.toArray(new Float[] {});

            case INTEGER:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(Integer.valueOf(value));
                    }
                }
                return values.toArray(new Integer[] {});

            case LONG:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(Long.valueOf(value));
                    }
                }
                return values.toArray(new Long[] {});

            case SHORT:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(Short.valueOf(value));
                    }
                }
                return values.toArray(new Short[] {});

            case PASSWORD:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
                        values.add(new Password(value));
                    }
                }
                return values.toArray(new Password[] {});

            case STRING:
                for (String value : trimmedValues) {
                    if (!value.isEmpty()) {
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
        if (pid == null) {
            return null;
        }

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
        final Map<String, Object> properties = new HashMap<>(config.getProperties());
        final ComponentConfiguration backupCC = currentCC;
        if (backupCC == null) {
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
        final String factoryPid = config.getFactoryId();
        if (factoryPid != null) {
            properties.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        }
        return properties;
    }

    private static List<GwtConfigParameter> getADProperties(ComponentConfiguration config) {
        List<GwtConfigParameter> gwtParams = new ArrayList<>();
        OCD ocd = config.getDefinition();
        for (AD ad : ocd.getAD()) {
            Object value = null;
            if (config.getConfigurationProperties() != null) {
                value = config.getConfigurationProperties().get(ad.getId());
            }
            gwtParams.add(toGwtConfigParameter(ad, value));
        }
        return gwtParams;
    }

    public static GwtConfigParameter toGwtConfigParameter(final AD ad, final Object value) {
        GwtConfigParameter gwtParam = new GwtConfigParameter();
        gwtParam.setId(ad.getId());
        gwtParam.setName(ad.getName());
        gwtParam.setDescription(ad.getDescription());
        gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
        gwtParam.setRequired(ad.isRequired());
        gwtParam.setCardinality(ad.getCardinality());
        gwtParam.setDefault(ad.getDefault());
        if (ad.getOption() != null && !ad.getOption().isEmpty()) {
            Map<String, String> options = new HashMap<>();
            for (Option option : ad.getOption()) {
                options.put(option.getLabel(), option.getValue());
            }
            gwtParam.setOptions(options);
        }
        gwtParam.setMin(ad.getMin());
        gwtParam.setMax(ad.getMax());

        // handle the value based on the cardinality of the attribute
        int cardinality = ad.getCardinality();

        if (value != null) {
            if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
                if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                    gwtParam.setValue(GwtServerUtil.PASSWORD_PLACEHOLDER);
                } else {
                    gwtParam.setValue(String.valueOf(value));
                }
            } else {
                // this could be an array value
                if (value instanceof Object[]) {
                    Object[] objValues = (Object[]) value;
                    List<String> strValues = new ArrayList<>();
                    for (Object v : objValues) {
                        if (v != null) {
                            if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                                strValues.add(GwtServerUtil.PASSWORD_PLACEHOLDER);
                            } else {
                                strValues.add(String.valueOf(v));
                            }
                        }
                    }
                    gwtParam.setValues(strValues.toArray(new String[] {}));
                }
            }
        }

        return gwtParam;
    }

    public static GwtConfigComponent toGwtConfigComponent(ComponentConfiguration config) {
        GwtConfigComponent gwtConfig = null;

        OCD ocd = config.getDefinition();
        if (ocd != null) {

            gwtConfig = new GwtConfigComponent();
            gwtConfig.setComponentId(config.getPid());

            Map<String, Object> props = config.getConfigurationProperties();
            if (props != null && props.get(DRIVER_PID) != null) {
                gwtConfig.set(DRIVER_PID, props.get(DRIVER_PID));
            }

            if (props != null && props.get(ConfigurationAdmin.SERVICE_FACTORYPID) != null) {
                gwtConfig.setFactoryComponent(true);
                gwtConfig.setFactoryPid(String.valueOf(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)));
            } else {
                gwtConfig.setFactoryComponent(false);
            }

            if (ocd.getName() != null) {
                gwtConfig.setComponentName(ocd.getName());
            }

            gwtConfig.setComponentDescription(ocd.getDescription());
            if (ocd.getIcon() != null && !ocd.getIcon().isEmpty()) {
                Icon icon = ocd.getIcon().get(0);
                gwtConfig.setComponentIcon(icon.getResource());
            }

            if (gwtConfig.getComponentName() == null) {
                // set a fallback name
                gwtConfig.setComponentName(stripPidPrefix(config.getPid()));
            }

            List<GwtConfigParameter> gwtParams = new ArrayList<>();
            gwtConfig.setParameters(gwtParams);

            gwtParams.addAll(getADProperties(config));
        }
        return gwtConfig;
    }

    public static GwtConfigComponent toGwtConfigComponent(String pid, Object descriptor) {
        if (!(descriptor instanceof List<?>)) {
            return null;
        }

        final List<?> ads = (List<?>) descriptor;

        final Tocd ocd = new Tocd();
        ocd.setId(pid);
        for (final Object ad : ads) {
            if (!(ad instanceof Tad)) {
                return null;
            }
            ocd.addAD((Tad) ad);
        }

        return GwtServerUtil.toGwtConfigComponent(new ComponentConfigurationImpl(pid, ocd, null));
    }

    public static ComponentConfiguration fromGwtConfigComponent(GwtConfigComponent gwtCompConfig,
            ComponentConfiguration currentCC) {
        if (currentCC == null) {
            final ComponentConfigurationImpl result = new ComponentConfigurationImpl();
            result.setPid(gwtCompConfig.getComponentId());
            result.setProperties(fillPropertiesFromConfiguration(gwtCompConfig, null));
            return result;
        }

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
        for (GwtConfigParameter gwtConfigParam : gwtCompConfig.getParameters()) {
            Object objValue;
            Object currentValue = currentConfigProp.get(gwtConfigParam.getId());

            boolean isReadOnly = gwtConfigParam.getMin() != null
                    && gwtConfigParam.getMin().equals(gwtConfigParam.getMax());
            if (isReadOnly) {
                objValue = currentValue;
            } else {
                objValue = GwtServerUtil.getUserDefinedObject(gwtConfigParam, currentValue);
            }
            properties.put(gwtConfigParam.getId(), objValue);
        }

        // Force kura.service.pid into properties, if originally present
        if (currentConfigProp.get(KURA_SERVICE_PID) != null) {
            properties.put(KURA_SERVICE_PID, currentConfigProp.get(KURA_SERVICE_PID));
        }
        final String factoryPid = gwtCompConfig.getFactoryId();
        if (factoryPid != null) {
            properties.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        }

        currentConfigProp.clear();
        currentConfigProp.putAll(properties);
        return currentCC;
    }

    public static GwtConfigComponent toGwtConfigComponent(DriverDescriptor descriptor) {
        final Object channelDescriptor = descriptor.getChannelDescriptor();
        if (channelDescriptor == null) {
            return null;
        }
        return toGwtConfigComponent(descriptor.getPid(), channelDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> getArrayProperty(final Object raw, final Class<T> elementValue) {
        if (!(raw instanceof Object[])) {
            return Collections.emptySet();
        }

        final Object[] asArray = (Object[]) raw;

        final Set<T> result = new HashSet<>();

        for (final Object o : asArray) {
            if (elementValue.isInstance(o)) {
                result.add((T) o);
            }
        }

        return result;
    }

    public static boolean isFactoryOf(final String factoryPid, final Predicate<Set<String>> filter) {

        try {
            return ServiceLocator.applyToServiceOptionally(ServiceComponentRuntime.class,
                    scr -> scr.getComponentDescriptionDTOs().stream().anyMatch(c -> {
                        if (!Objects.equals(factoryPid, c.name)) {
                            return false;
                        }

                        return providedInterfacesMatch(c.serviceInterfaces, filter);
                    }));
        } catch (final Exception e) {
            return false;
        }

    }

    public static boolean isFactoryOfAnyService(final String factoryPid, final Class<?>... interfaces) {
        return isFactoryOf(factoryPid, s -> {
            for (final Class<?> intf : interfaces) {
                if (s.contains(intf.getName())) {
                    return true;
                }
            }

            return false;
        });
    }

    public static Set<String> getServiceProviderComponentNames(final Predicate<Set<String>> filter)
            throws GwtKuraException {

        try {
            return ServiceLocator
                    .applyToServiceOptionally(ServiceComponentRuntime.class,
                            scr -> scr.getComponentDescriptionDTOs().stream()
                                    .filter(c -> providedInterfacesMatch(c.serviceInterfaces, filter)))
                    .map(c -> c.name).collect(Collectors.toSet());
        } catch (final Exception e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static Set<String> getServiceProviderFactoryPids(final Predicate<Set<String>> filter)
            throws GwtKuraException {
        final Set<String> names = getServiceProviderComponentNames(filter);

        final Set<String> factoryPids = ServiceLocator.applyToServiceOptionally(ConfigurationService.class,
                ConfigurationService::getFactoryComponentPids);

        names.removeIf(n -> !factoryPids.contains(n));

        return names;
    }

    public static List<GwtComponentInstanceInfo> getComponentInstances(final Predicate<Set<String>> filter)
            throws GwtKuraException {
        try {
            return ServiceLocator.applyToServiceOptionally(ServiceComponentRuntime.class,
                    scr -> scr.getComponentDescriptionDTOs().stream()
                            .filter(c -> providedInterfacesMatch(c.serviceInterfaces, filter))
                            .flatMap(c -> scr.getComponentConfigurationDTOs(c).stream()).map(c -> {
                                final Map<String, Object> properties = c.properties;

                                final Object kuraServicePid = properties.get(KURA_SERVICE_PID);
                                final Object rawFactoryPid = properties.get("service.factoryPid");

                                if (!(kuraServicePid instanceof String)) {
                                    return null;
                                }

                                final Optional<String> factoryPid;

                                if (rawFactoryPid instanceof String) {
                                    factoryPid = Optional.of((String) rawFactoryPid);
                                } else {
                                    factoryPid = Optional.empty();
                                }

                                return new GwtComponentInstanceInfo((String) kuraServicePid, factoryPid);

                            }).filter(Objects::nonNull).collect(Collectors.toList()));
        } catch (final Exception e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private static boolean providedInterfacesMatch(final String[] providedInterfaces,
            final Predicate<Set<String>> filter) {
        if (providedInterfaces == null) {
            return filter.test(Collections.emptySet());
        }

        return filter.test(Arrays.stream(providedInterfaces).collect(Collectors.toSet()));
    }

    public static boolean providesService(final String kuraServicePid, final Class<?> serviceInterface) {
        return providesService(kuraServicePid, s -> s.contains(serviceInterface.getName()));
    }

    public static boolean providesService(final String kuraServicePid, final Predicate<Set<String>> filter) {
        final BundleContext context = FrameworkUtil.getBundle(GwtServerUtil.class).getBundleContext();

        final String pidFilter = "(kura.service.pid=" + kuraServicePid + ")";

        try {
            final ServiceReference<?>[] refs = context.getAllServiceReferences(null, pidFilter);

            if (refs == null) {
                return false;
            }

            for (final ServiceReference<?> ref : refs) {
                final Object rawProvidedInterfaces = ref.getProperty("objectClass");

                final Set<String> providedInterfaces;

                if (rawProvidedInterfaces instanceof String) {
                    providedInterfaces = Collections.singleton((String) rawProvidedInterfaces);
                } else if (rawProvidedInterfaces instanceof String[]) {
                    providedInterfaces = Arrays.asList((String[]) rawProvidedInterfaces).stream()
                            .collect(Collectors.toSet());
                } else {
                    providedInterfaces = Collections.emptySet();
                }

                if (filter.test(providedInterfaces)) {
                    return true;
                }
            }

            return false;
        } catch (InvalidSyntaxException e) {
            return false;
        }
    }

    private static ServiceReference<Marshaller>[] getXmlMarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.xml.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(
                FrameworkUtil.getBundle(DeviceSnapshotsServlet.class).getBundleContext(), Marshaller.class,
                filterString);
    }

    private static void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(FrameworkUtil.getBundle(DeviceSnapshotsServlet.class).getBundleContext(),
                refs);
    }

    private static String marshal(Object object) {
        String result = null;
        ServiceReference<Marshaller>[] marshallerSRs = getXmlMarshallers();
        try {
            for (final ServiceReference<Marshaller> marshallerSR : marshallerSRs) {
                Marshaller marshaller = FrameworkUtil.getBundle(DeviceSnapshotsServlet.class).getBundleContext()
                        .getService(marshallerSR);
                result = marshaller.marshal(object);
                if (result != null) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to marshal configuration.");
        } finally {
            ungetServiceReferences(marshallerSRs);
        }
        return result;
    }

    public static void writeXmlSnapshot(HttpServletResponse response, PrintWriter writer, final String filename,
            List<ComponentConfiguration> configs) {
        // build a list of configuration which can be marshalled in XML
        List<ComponentConfiguration> configImpls = new ArrayList<>();
        for (ComponentConfiguration config : configs) {
            configImpls.add(config);
        }
        XmlComponentConfigurations xmlConfigs = new XmlComponentConfigurations();
        xmlConfigs.setConfigurations(configImpls);

        //
        // marshall the response and write it
        String result = marshal(xmlConfigs);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/xml");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setHeader("Cache-Control", "no-transform, max-age=0");

        writer.write(result);
    }

    public static void writeJsonSnapshot(HttpServletResponse response, PrintWriter writer, final String filename,
            List<ComponentConfiguration> configs) {

        final ComponentConfigurationList dto = DTOUtil.toComponentConfigurationList(configs, null, false);

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setHeader("Cache-Control", "no-transform, max-age=0");

        gson.toJson(dto, writer);
    }

    public static void writeSnapshot(final HttpServletRequest request, final HttpServletResponse response,
            final List<ComponentConfiguration> configs, final String filename) throws ServletException {

        String format = request.getParameter("format");

        if (format == null || format.isEmpty()) {
            format = XML_FORMAT;
        }

        try (PrintWriter writer = response.getWriter()) {

            if (XML_FORMAT.equalsIgnoreCase(format)) {
                GwtServerUtil.writeXmlSnapshot(response, writer, filename + ".xml", configs);
            } else if (JSON_FORMAT.equalsIgnoreCase(format)) {
                GwtServerUtil.writeJsonSnapshot(response, writer, filename + ".json", configs);
            }

        } catch (Exception e) {
            logger.error("Error exporting snapshot");
            throw new ServletException(e);
        }
    }

    public static List<GwtNetInterfaceConfig> replaceNetworkConfigListSensitivePasswordsWithPlaceholder(
            List<GwtNetInterfaceConfig> gwtNetworkConfigList) {
        for (GwtNetInterfaceConfig netConfig : gwtNetworkConfigList) {
            if (netConfig instanceof GwtWifiNetInterfaceConfig) {
                GwtWifiNetInterfaceConfig wifiConfig = (GwtWifiNetInterfaceConfig) netConfig;
                GwtWifiConfig gwtAPWifiConfig = wifiConfig.getAccessPointWifiConfig();
                if (gwtAPWifiConfig != null) {
                    gwtAPWifiConfig.setPassword(PASSWORD_PLACEHOLDER);
                }

                GwtWifiConfig gwtStationWifiConfig = wifiConfig.getStationWifiConfig();
                if (gwtStationWifiConfig != null) {
                    gwtStationWifiConfig.setPassword(PASSWORD_PLACEHOLDER);
                }
            } else if (netConfig instanceof GwtModemInterfaceConfig) {
                GwtModemInterfaceConfig modemConfig = (GwtModemInterfaceConfig) netConfig;
                modemConfig.setPassword(PASSWORD_PLACEHOLDER);
            }
        }

        return gwtNetworkConfigList;
    }

    public static void validateUserPassword(final String password) throws GwtKuraException {
        final List<Validator<String>> validators = PasswordStrengthValidators
                .fromConfig(Console.getConsoleOptions().getUserOptions());

        final List<String> errors = new ArrayList<>();

        for (final Validator<String> validator : validators) {
            validator.validate(password, errors::add);
        }

        if (!errors.isEmpty()) {
            logger.warn("password strenght requirements not satisfied: {}", errors);
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }
}
