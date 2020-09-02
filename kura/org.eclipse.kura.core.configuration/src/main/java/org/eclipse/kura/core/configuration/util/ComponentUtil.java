/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix service leak
 *         - fix default value lookup
 *******************************************************************************/
package org.eclipse.kura.core.configuration.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Designate;
import org.eclipse.kura.configuration.metatype.MetaData;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.metatype.Tmetadata;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to handle the loading of the meta-information
 * of a bundle or one of its Services/Components.
 */
public class ComponentUtil {

    private static final Logger logger = LoggerFactory.getLogger(ComponentUtil.class);

    /**
     * Returns a Map with all the MetaType Object Class Definitions contained in the bundle.
     *
     * @param ctx
     * @param bnd
     * @return
     */
    public static Map<String, Tmetadata> getMetadata(BundleContext ctx, Bundle bnd) {
        final Map<String, Tmetadata> bundleMetadata = new HashMap<>();

        final ServiceReference<MetaTypeService> ref = ctx.getServiceReference(MetaTypeService.class);
        final MetaTypeService metaTypeService = ctx.getService(ref);

        try {
            final MetaTypeInformation mti = metaTypeService.getMetaTypeInformation(bnd);
            if (mti != null) {

                final List<String> pids = new ArrayList<>();
                pids.addAll(Arrays.asList(mti.getPids()));
                pids.addAll(Arrays.asList(mti.getFactoryPids()));
                if (pids != null) {
                    for (String pid : pids) {

                        final Tmetadata metadata;
                        try {
                            metadata = readMetadata(bnd, pid);
                            if (metadata != null) {
                                bundleMetadata.put(pid, metadata);
                            }
                        } catch (Exception e) {
                            // ignore: Metadata for the specified pid is not found
                            logger.warn("Error loading Metadata for pid " + pid, e);
                        }
                    }
                }
            }
        } finally {
            ctx.ungetService(ref);
        }

        return bundleMetadata;
    }

    /**
     * Returns the Designate for the given pid
     */
    public static Designate getDesignate(final Tmetadata metadata, final String pid) {

        if (metadata == null || pid == null) {
            return null;
        }

        final List<Designate> designates = metadata.getDesignate();
        if (designates == null) {
            return null;
        }

        for (final Designate designate : designates) {
            if (pid.equals(designate.getPid())) {
                return designate;
            }
            if (pid.equals(designate.getFactoryPid())) {
                return designate;
            }
        }

        return null;
    }

    /**
     * Returns the Designate for the given pid
     */
    public static OCD getOCD(Tmetadata metadata, String pid) {
        if (metadata.getOCD() != null && !metadata.getOCD().isEmpty()) {
            for (OCD ocd : metadata.getOCD()) {
                if (ocd.getId() != null && ocd.getId().equals(pid)) {
                    return ocd;
                }
            }
        }
        return null;
    }

    /**
     * Returns a Map with all the MetaType Object Class Definitions contained in the bundle.
     *
     * @param ctx
     * @param bnd
     * @return
     */
    public static Map<String, OCD> getObjectClassDefinition(BundleContext ctx, Bundle bnd) {
        Map<String, OCD> bundleDefaults = new HashMap<>();

        ServiceReference<MetaTypeService> ref = ctx.getServiceReference(MetaTypeService.class);
        MetaTypeService metaTypeService = ctx.getService(ref);
        MetaTypeInformation mti = metaTypeService.getMetaTypeInformation(bnd);
        if (mti != null) {

            String[] pids = mti.getPids();
            if (pids != null) {
                for (String pid : pids) {
                    OCD ocd = null;
                    try {
                        ocd = readObjectClassDefinition(bnd, pid);
                        if (ocd != null) {
                            bundleDefaults.put(pid, ocd);
                        }
                    } catch (Exception e) {
                        // ignore: OCD for the specified pid is not found
                        logger.warn("Error loading OCD for pid " + pid, e);
                    }
                }
            }
        }
        return bundleDefaults;
    }

    /**
     * Returns the ObjectClassDefinition for the provided Service pid
     * as returned by the native OSGi MetaTypeService.
     * The returned ObjectClassDefinition is a thick object tied to
     * OSGi framework which masks certain attributes of the OCD XML
     * file - like required and min/max values - but which exposes
     * business logic like the validate method for each attribute.
     *
     * @param ctx
     * @param pid
     * @return
     */
    public static ObjectClassDefinition getObjectClassDefinition(BundleContext ctx, String pid) {
        //
        ServiceReference<MetaTypeService> ref = ctx.getServiceReference(MetaTypeService.class);
        MetaTypeService metaTypeService = ctx.getService(ref);
        MetaTypeInformation mti = metaTypeService.getMetaTypeInformation(ctx.getBundle());

        ObjectClassDefinition ocd = null;
        try {
            if (mti != null) {
                ocd = mti.getObjectClassDefinition(pid, null); // default locale
            }
        } catch (IllegalArgumentException iae) {
            // ignore: OCD for the specified pid is not found
        }
        return ocd;
    }

    /**
     * Returned the Tmetadata as parsed from the XML file.
     * The returned Tmetadata is just an object representation of the Metadata
     * element contained in the XML MetaData file and it does not
     * contain any extra post-processing of the loaded information.
     *
     * @param ctx
     * @param pid
     *            ID of the service whose OCD should be loaded
     * @return
     * @throws IOException
     * @throws XMLStreamException
     * @throws FactoryConfigurationError
     */
    public static Tmetadata readMetadata(Bundle bundle, String pid)
            throws IOException, Exception, XMLStreamException, FactoryConfigurationError {
        Tmetadata metaData = null;
        StringBuilder sbMetatypeXmlName = new StringBuilder();
        sbMetatypeXmlName.append("OSGI-INF/metatype/").append(pid).append(".xml");

        String metatypeXmlName = sbMetatypeXmlName.toString();
        String metatypeXml = IOUtil.readResource(bundle, metatypeXmlName);
        if (metatypeXml != null) {
            metaData = unmarshal(metatypeXml, Tmetadata.class);
        }
        return metaData;
    }

    /**
     * Returned the ObjectClassDefinition as parsed from the XML file.
     * The returned OCD is just an object representation of the OCD
     * element contained in the XML MetaData file and it does not
     * contain any extra post-processing of the loaded information.
     *
     * @param resourceUrl
     *            Url of the MetaData XML file which needs to be loaded
     * @return
     * @throws IOException
     * @throws XMLStreamException
     * @throws FactoryConfigurationError
     */
    public static OCD readObjectClassDefinition(URL resourceUrl)
            throws IOException, XMLStreamException, FactoryConfigurationError, Exception {
        OCD ocd = null;
        String metatypeXml = IOUtil.readResource(resourceUrl);
        if (metatypeXml != null) {
            MetaData metaData = unmarshal(metatypeXml, MetaData.class);
            if (metaData.getOCD() != null && !metaData.getOCD().isEmpty()) {
                ocd = metaData.getOCD().get(0);
            } else {
                logger.warn("Cannot find OCD for component with url: {}", resourceUrl.toString());
            }
        }
        return ocd;
    }

    /**
     * Returned the ObjectClassDefinition as parsed from the XML file.
     * The returned OCD is just an object representation of the OCD
     * element contained in the XML MetaData file and it does not
     * contain any extra post-processing of the loaded information.
     *
     * @param pid
     *            ID of the service whose OCD should be loaded
     * @return
     * @throws IOException
     * @throws XMLStreamException
     * @throws FactoryConfigurationError
     */
    public static Tocd readObjectClassDefinition(String pid)
            throws IOException, Exception, XMLStreamException, FactoryConfigurationError {
        Tocd ocd = null;
        StringBuilder sbMetatypeXmlName = new StringBuilder();
        sbMetatypeXmlName.append("OSGI-INF/metatype/").append(pid).append(".xml");

        String metatypeXmlName = sbMetatypeXmlName.toString();
        String metatypeXml = IOUtil.readResource(metatypeXmlName);
        if (metatypeXml != null) {
            Tmetadata metaData = unmarshal(metatypeXml, Tmetadata.class);
            if (metaData.getOCD() != null && !metaData.getOCD().isEmpty()) {
                ocd = (Tocd) metaData.getOCD().get(0);
            } else {
                logger.warn("Cannot find OCD for component with pid: {}", pid);
            }
        }
        return ocd;
    }

    /**
     * Returned the ObjectClassDefinition as parsed from the XML file.
     * The returned OCD is just an object representation of the OCD
     * element contained in the XML MetaData file and it does not
     * contain any extra post-processing of the loaded information.
     *
     * @param ctx
     * @param pid
     *            ID of the service whose OCD should be loaded
     * @return
     * @throws IOException
     * @throws XMLStreamException
     * @throws FactoryConfigurationError
     */
    public static Tocd readObjectClassDefinition(Bundle bundle, String pid)
            throws IOException, Exception, XMLStreamException, FactoryConfigurationError {
        Tocd ocd = null;
        StringBuilder sbMetatypeXmlName = new StringBuilder();
        sbMetatypeXmlName.append("OSGI-INF/metatype/").append(pid).append(".xml");

        String metatypeXmlName = sbMetatypeXmlName.toString();
        String metatypeXml = IOUtil.readResource(bundle, metatypeXmlName);
        if (metatypeXml != null) {
            Tmetadata metaData = unmarshal(metatypeXml, Tmetadata.class);
            if (metaData.getOCD() != null && !metaData.getOCD().isEmpty()) {
                ocd = (Tocd) metaData.getOCD().get(0);
            }
        }
        return ocd;
    }

    public static Map<String, Object> getDefaultProperties(OCD ocd, ComponentContext ctx) {
        //
        // reconcile by looping through the ocd properties
        Map<String, Object> defaults = null;
        defaults = new HashMap<>();
        if (ocd != null) {

            List<AD> attrDefs = ocd.getAD();
            if (attrDefs != null) {

                for (AD attrDef : attrDefs) {

                    String id = attrDef.getId();
                    Object defaultValue = getDefaultValue(attrDef, ctx);
                    if (defaults.get(id) == null && defaultValue != null) {
                        defaults.put(id, defaultValue);
                    }
                }
            }
        }
        return defaults;
    }

    private static Object getDefaultValue(AD attrDef, ComponentContext ctx) {
        // get the default value string from the AD
        // then split it by comma-separate list
        // keeping in mind the possible escape sequence "\,"
        String defaultValue = attrDef.getDefault();
        if (defaultValue == null || defaultValue.length() == 0) {
            return null;
        }

        Object[] objectValues = null;
        Scalar type = attrDef.getType();
        if (type != null) {

            // split the default value in separate string
            // if cardinality is greater than 0 or abs(1)
            String[] defaultValues = new String[] { defaultValue };
            int cardinality = attrDef.getCardinality();
            if (cardinality != 0 || cardinality != 1 || cardinality != -1) {
                defaultValues = StringUtil.splitValues(defaultValue);
            }

            // convert string values into object values
            objectValues = getObjectValue(type, defaultValues, ctx);
            if (objectValues != null && (cardinality == 0 || cardinality == 1 || cardinality == -1)) {

                // return one single object
                // if cardinality is 0 or abs(1)
                return objectValues[0];
            }
        } else {
            logger.warn("Unknown type for attribute {}", attrDef.getId());
        }
        return objectValues;
    }

    private static Object[] getObjectValue(Scalar type, String[] defaultValues, ComponentContext ctx) {
        List<Object> values = new ArrayList<>();
        switch (type) {
        case BOOLEAN:
            for (String value : defaultValues) {
                values.add(Boolean.valueOf(value));
            }
            return values.toArray(new Boolean[] {});

        case BYTE:
            for (String value : defaultValues) {
                values.add(Byte.valueOf(value));
            }
            return values.toArray(new Byte[] {});

        case CHAR:
            for (String value : defaultValues) {
                values.add(Character.valueOf(value.charAt(0)));
            }
            return values.toArray(new Character[] {});

        case DOUBLE:
            for (String value : defaultValues) {
                values.add(Double.valueOf(value));
            }
            return values.toArray(new Double[] {});

        case FLOAT:
            for (String value : defaultValues) {
                values.add(Float.valueOf(value));
            }
            return values.toArray(new Float[] {});

        case INTEGER:
            for (String value : defaultValues) {
                values.add(Integer.valueOf(value));
            }
            return values.toArray(new Integer[] {});

        case LONG:
            for (String value : defaultValues) {
                values.add(Long.valueOf(value));
            }
            return values.toArray(new Long[] {});

        case SHORT:
            for (String value : defaultValues) {
                values.add(Short.valueOf(value));
            }
            return values.toArray(new Short[] {});

        case PASSWORD:
            ServiceReference<CryptoService> sr = ctx.getBundleContext().getServiceReference(CryptoService.class);
            CryptoService cryptoService = ctx.getBundleContext().getService(sr);
            for (String value : defaultValues) {
                try {
                    values.add(new Password(cryptoService.encryptAes(value.toCharArray())));
                } catch (Exception e) {
                    values.add(new Password(value));
                }
            }
            return values.toArray(new Password[] {});

        case STRING:
            return defaultValues;
        }

        return null;
    }

    private static ServiceReference<Unmarshaller>[] getXmlUnmarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.xml.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(FrameworkUtil.getBundle(ComponentUtil.class).getBundleContext(),
                Unmarshaller.class, filterString);
    }

    private static void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(FrameworkUtil.getBundle(ComponentUtil.class).getBundleContext(), refs);
    }

    protected static <T> T unmarshal(String string, Class<T> clazz) throws KuraException {
        T result = null;
        ServiceReference<Unmarshaller>[] unmarshallerSRs = getXmlUnmarshallers();
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                Unmarshaller unmarshaller = FrameworkUtil.getBundle(ComponentUtil.class).getBundleContext()
                        .getService(unmarshallerSR);
                result = unmarshaller.unmarshal(string, clazz);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract persisted configuration.");
        } finally {
            ungetServiceReferences(unmarshallerSRs);
        }
        if (result == null) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, "value");
        }
        return result;
    }
}
