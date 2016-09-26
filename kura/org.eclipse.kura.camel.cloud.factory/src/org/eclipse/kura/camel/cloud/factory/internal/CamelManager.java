/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.cloud.factory.internal;

import static org.eclipse.kura.camel.cloud.factory.internal.CamelFactory.FACTORY_ID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.camel.component.Configuration;
import org.eclipse.kura.cloud.factory.CloudServiceFactory;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelManager implements SelfConfiguringComponent, CloudServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(CamelManager.class);

    private static final String PID = "org.eclipse.kura.camel.cloud.factory.CamelManager";

    private ConfigurationService configurationService;

    private static OCD makeModel() {
        final ObjectFactory objectFactory = new ObjectFactory();
        final Tocd tocd = objectFactory.createTocd();

        tocd.setName("Camel Cloud Factory");
        tocd.setId(PID);
        tocd.setDescription("Create a new Apache Camel™ cloud service");

        {
            final Tad tad = objectFactory.createTad();
            tad.setId("add");
            tad.setName("PID to add");
            tad.setType(Tscalar.STRING);
            tad.setCardinality(0);
            tad.setRequired(Boolean.FALSE);
            tad.setDescription("PID for the new configuration");
            tocd.addAD(tad);
        }

        {
            final Tad tad = objectFactory.createTad();
            tad.setId("xml");
            tad.setName("Router XML");
            tad.setType(Tscalar.STRING);
            tad.setCardinality(0);
            tad.setRequired(Boolean.FALSE);
            tad.setDescription("Initial XML configuration|TextArea");
            tocd.addAD(tad);
        }

        {
            final Tad tad = objectFactory.createTad();
            tad.setId("serviceRanking");
            tad.setName("Service Ranking");
            tad.setType(Tscalar.INTEGER);
            tad.setCardinality(0);
            tad.setRequired(Boolean.FALSE);
            tad.setDescription("The initial service ranking of the new cloud service. A higher number will have more priority.");
            tocd.addAD(tad);
        }

        return tocd;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void modified(Map<String, Object> properties) throws KuraException {
        final String add = asString(properties.get("add"));

        if (add != null && !add.isEmpty()) {
            add(add, properties);
        }
    }

    /**
     * Add a new CamelFactory
     *
     * @param userPid
     *            the PID as entered by the user
     * @param properties
     *            the provided configuration properties
     * @throws KuraException
     *             if anything goes wrong
     */
    protected void add(final String pid, final Map<String, Object> properties) throws KuraException {
        logger.info("Add: {}", pid);

        final Map<String, Object> props = new HashMap<>();

        String xml = Configuration.asString(properties, "xml");
        if (xml == null || xml.trim().isEmpty()) {
            xml = "<routes xmlns=\"http://camel.apache.org/schema/spring\"></routes>";
        }

        props.put("xml", xml);

        final Integer serviceRanking = Configuration.asInteger(properties, "serviceRanking");
        if (serviceRanking != null) {
            props.put("serviceRanking", serviceRanking);
        }

        props.put("cloud.service.pid", pid);

        this.configurationService.createFactoryConfiguration(FACTORY_ID, fromUserPid(pid), props, true);
    }

    private static String fromUserPid(String pid) {
        return pid + "-CloudFactory";
    }

    private static String asString(Object object) {
        if (object instanceof String) {
            return ((String) object).trim();
        }
        return null;
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        return new ComponentConfiguration() {

            @Override
            public String getPid() {
                return PID;
            }

            @Override
            public OCD getDefinition() {
                return makeModel();
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return makeConfiguration();
            }
        };
    }

    protected Map<String, Object> makeConfiguration() {
        final Map<String, Object> result = new HashMap<>();
        return result;
    }

    /**
     * Enumerate all registered CamelFactory instances
     *
     * @return a PID (<code>kura.service.pid</code>) set of all registered CamelFactory instances
     */
    public static Set<String> lookupIds() {
        final Set<String> ids = new TreeSet<>();
        try {

            final Collection<ServiceReference<CamelFactory>> refs = FrameworkUtil.getBundle(CamelManager.class).getBundleContext().getServiceReferences(CamelFactory.class, null);
            if (refs != null) {
                for (ServiceReference<CamelFactory> ref : refs) {
                    addService(ref, ids);
                }
            }
        } catch (InvalidSyntaxException e) {
        }
        return ids;
    }

    private static void addService(ServiceReference<CamelFactory> ref, Set<String> ids) {
        final Object kpid = ref.getProperty("kura.service.pid");
        if (kpid instanceof String) {
            ids.add((String) kpid);
        }
    }

    /**
     * Provide a common way to delete camel factory configurations
     * <p>
     * Right now this is a rather slim implementation used by CamelFactory and the CamelManager
     * </p>
     *
     * @param configurationService
     *            the configuration service to use
     * @param pid
     *            the PID to delete
     */
    static void delete(ConfigurationService configurationService, String pid) {
        try {
            configurationService.deleteFactoryConfiguration(pid, true);
        } catch (KuraException e) {
            logger.warn("Failed to delete: {}", pid, e);
        }
    }

    @Override
    public void createConfiguration(String pid) throws KuraException {
        add(pid, Collections.<String, Object> emptyMap());
    }

    @Override
    public void deleteConfiguration(String pid) throws KuraException {
        delete(this.configurationService, fromUserPid(pid));
    }

    @Override
    public String getFactoryPid() {
        return FACTORY_ID;
    }

}
