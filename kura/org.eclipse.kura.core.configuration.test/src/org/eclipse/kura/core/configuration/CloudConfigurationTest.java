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
package org.eclipse.kura.core.configuration;

import static org.eclipse.kura.cloud.CloudletTopic.Method.GET;
import static org.eclipse.kura.core.configuration.CloudConfigurationHandler.RESOURCE_CONFIGURATIONS;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

public class CloudConfigurationTest {

    private final List<ServiceRegistration<?>> registrations = new LinkedList<>();

    private BundleContext context;

    @Before
    public void setup() {
        this.context = FrameworkUtil.getBundle(CloudConfigurationTest.class).getBundleContext();
    }

    @After
    public void cleanup() {
        for (ServiceRegistration<?> reg : this.registrations) {
            reg.unregister();
        }
        this.registrations.clear();
    }

    @Test
    public void testGet() throws Exception {

        // set up

        {
            final Hashtable<String, Object> props = new Hashtable<>();
            props.put(Constants.SERVICE_PID, "pid1");
            remember(this.context.registerService(SelfConfiguringComponent.class, makeTest1Service(), props));
        }

        final MockSystemService systemService = new MockSystemService();
        final MockCryptoService cryptoService = new MockCryptoService();

        remember(this.context.registerService(CryptoService.class, cryptoService, null));

        final ConfigurationServiceImpl cfgService = new ConfigurationServiceImpl();
        cfgService.setConfigurationAdmin(get(ConfigurationAdmin.class));
        cfgService.setSystemService(systemService);
        cfgService.setCryptoService(cryptoService);

        cfgService.activate();

        remember(this.context.registerService(ConfigurationService.class, cfgService, null));

        final CloudConfigurationHandler cfgHandler = new CloudConfigurationHandler();
        cfgHandler.setSystemService(systemService);
        cfgHandler.setConfigurationService(cfgService);
        cfgHandler.setCryptoService(cryptoService);
        cfgHandler.activate();

        // execute

        final CloudletTopic ct = CloudletTopic.parseAppTopic(GET + "/" + RESOURCE_CONFIGURATIONS);
        final KuraRequestPayload request = new KuraRequestPayload();
        final KuraResponsePayload response = new KuraResponsePayload(0);

        cfgHandler.doGet(ct, request, response);

        final String data = new String(response.getBody(), StandardCharsets.UTF_8);
        final XmlComponentConfigurations xmlResult = XmlUtil.unmarshal(data, XmlComponentConfigurations.class);

        // dump

        dump(xmlResult);

        // tear down

        cfgHandler.deactivate();
        cfgService.deactivate();

        // assert

        Assert.assertEquals(1, xmlResult.getConfigurations().size());
    }

    private SelfConfiguringComponent makeTest1Service() {
        return new SelfConfiguringComponent() {

            @Override
            public ComponentConfiguration getConfiguration() throws KuraException {
                return new ComponentConfiguration() {

                    @Override
                    public String getPid() {
                        return "pid1";
                    }

                    @Override
                    public OCD getDefinition() {
                        final Tocd result = new Tocd();
                        result.setId("pid1");
                        result.setName("PID 1");
                        result.setDescription("Foo bar");

                        final Tad ad1 = new Tad();
                        ad1.setId("foo");
                        ad1.setDefault("baz");
                        ad1.setName("FOO");
                        ad1.setType(Tscalar.STRING);
                        ad1.setCardinality(1);

                        result.getAD().add(ad1);

                        return result;
                    }

                    @Override
                    public Map<String, Object> getConfigurationProperties() {
                        Map<String, Object> result = new HashMap<>();
                        result.put("foo", "bar");
                        return result;
                    }
                };
            }
        };
    }

    private void remember(final ServiceRegistration<?> registerService) {
        this.registrations.add(registerService);
    }

    private void dump(final XmlComponentConfigurations result) {
        System.out.println("XML result:");
        for (final ComponentConfiguration cc : result.getConfigurations()) {
            System.out.println(cc.getPid());
        }
    }

    private <T> T get(final Class<T> clazz) {
        final ServiceReference<T> ref = this.context.getServiceReference(clazz);
        if (ref == null) {
            throw new IllegalStateException(String.format("Unable to find service for: %s", clazz));
        }

        return this.context.getService(ref);

    }
}
