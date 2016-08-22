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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.kura.KuraException;
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

public class CamelManager implements SelfConfiguringComponent {
    private static final Logger logger = LoggerFactory.getLogger(CamelManager.class);

    private static final String PID = "org.eclipse.kura.camel.cloud.factory.CamelManager";

    private ConfigurationService configurationService;

    private OCD makeModel() {
        final ObjectFactory objectFactory = new ObjectFactory();
        final Tocd tocd = objectFactory.createTocd();

        tocd.setName("Camel Cloud Factory");
        tocd.setId(PID);
        tocd.setDescription("Camel Cloud Factory Manager");

        {
            final Tad tad = objectFactory.createTad();
            tad.setId("add");
            tad.setName("add");
            tad.setType(Tscalar.STRING);
            tad.setCardinality(0);
            tad.setRequired(Boolean.FALSE);
            tad.setDescription("New configuration");
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
            add(add);
        }
    }

    protected void add(String pid) throws KuraException {
        logger.info("Add: {}", pid);

        final Map<String, Object> props = new HashMap<>();
        props.put("xml", "<routes xmlns=\"http://camel.apache.org/schema/spring\"></routes>");
        this.configurationService.createFactoryConfiguration(FACTORY_ID, FACTORY_ID + "-" + pid, props, true);
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
}
