/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnings, Fix issue #596
 *******************************************************************************/
package org.eclipse.kura.test;

import java.util.Dictionary;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.system.SystemService;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteTargetTest {

    private static final Logger s_logger = LoggerFactory.getLogger(RemoteTargetTest.class);

    private static final String TEST_HEADER = "Unit-Test";

    private SystemService m_systemService;
    private CloudService m_cloudService;
    private CloudClient m_cloudClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ConfigurationAdmin m_configAdmin;
    private BundleTracker<?> bundleTracker;
    private TestExtender testExtender;

    public void setSystemService(SystemService systemService) {
        this.m_systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.m_systemService = null;
    }

    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
        try {
            this.m_cloudClient = cloudService.newCloudClient("RemoteTargetTest");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.m_configAdmin = configAdmin;
    }

    public void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
        this.m_configAdmin = configAdmin;
    }

    protected void activate(final ComponentContext componentContext) {
        //
        // if we are running in the Eclipse JUnit Plugin Test,
        // then we are running in emulator mode and therefore we
        // will leverage the JUnit runner available in Eclipse PDE.
        if (!"emulator".equals(System.getProperty("org.eclipse.kura.mode"))) {
            this.scheduler.submit(new Runnable() {

                @Override
                public void run() {
                    runTests(componentContext);
                }
            });
        }
    }

    private void runTests(ComponentContext componentContext) {
        s_logger.debug("m_systemService.getPlatform(): " + this.m_systemService.getPlatform());
        this.testExtender = new TestExtender(this.m_systemService.getPlatform(), componentContext.getBundleContext());

        this.bundleTracker = new BundleTracker<Object>(componentContext.getBundleContext(),
                Bundle.RESOLVED | Bundle.ACTIVE | Bundle.INSTALLED, this.testExtender);
        this.bundleTracker.open();

        Bundle[] currentBundles = this.bundleTracker.getBundles();
        if (currentBundles != null) {
            Bundle netAdminBundle = null;

            for (Bundle bundle : this.bundleTracker.getBundles()) {
                if ("org.eclipse.kura.net.admin".equals(bundle.getSymbolicName())) {
                    netAdminBundle = bundle;

                    s_logger.debug("Disabling network admin bundle");
                    try {
                        netAdminBundle.stop();
                    } catch (BundleException e) {
                        s_logger.warn("Could not stop net admin bundle", e);
                    }
                }
            }

            s_logger.debug("Starting tests");
            startingTests();

            for (Bundle bundle : currentBundles) {
                if (isTestFragment(bundle)) {
                    this.testExtender.addBundle(bundle.getBundleId(), bundle);

                    if (isAutoTestEnabled(bundle)) {
                        this.testExtender.test(bundle.getBundleId());
                    }
                }
            }

            finishedTests();

            if (netAdminBundle != null) {
                s_logger.debug("Re-enabling network admin bundle");
                try {
                    netAdminBundle.start();
                } catch (BundleException e) {
                    s_logger.warn("Could not start net admin bundle", e);
                }
            }

            s_logger.warn("Tests finished - shutting down");
            System.exit(0);
        }

        componentContext.getBundleContext().registerService(CommandProvider.class.getName(),
                new KuraTestCommandProvider(), null);
    }

    protected void deactivate(ComponentContext componentContext) {

    }

    public static final boolean isTestFragment(Bundle bundle) {
        String header = bundle.getHeaders().get(TEST_HEADER) + "";
        String fragment = bundle.getHeaders().get(org.osgi.framework.Constants.FRAGMENT_HOST) + "";
        return !"null".equals(header) && !"null".equals(fragment);
    }

    public static final boolean isAutoTestEnabled(Bundle bundle) {
        return "true".equals(bundle.getHeaders().get(TEST_HEADER) + "");
    }

    public class KuraTestCommandProvider implements CommandProvider {

        public Object _test(CommandInterpreter intp) {
            String nextArgument = intp.nextArgument();
            RemoteTargetTest.this.testExtender.test(Long.parseLong(nextArgument));
            return null;
        }

        public Object _testall(CommandInterpreter intp) {
            RemoteTargetTest.this.testExtender.testAll();
            return null;
        }

        public Object _helpTest(CommandInterpreter intp) {
            String help = getHelp();
            System.out.println(help);
            return null;
        }

        @Override
        public String getHelp() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("---Testing commands---\n\t");
            buffer.append("test [bundle id] - test bundle fragment id\n\t");
            buffer.append("testall - test all fragments\n\t");
            buffer.append("help - Print this help\n");
            return buffer.toString();
        }
    }

    private void startingTests() {
        // hijack the settings
        try {
            Configuration mqttConfig = this.m_configAdmin
                    .getConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",  "?");
            Dictionary<String, Object> mqttProps = mqttConfig.getProperties();
            mqttProps.put("broker-url", "mqtt://broker-sandbox.everyware-cloud.com:1883/");
            mqttProps.put("topic.context.account-name", "EDC-KURA-CI");
            mqttProps.put("username", "EDC-KURA-CI");
            mqttProps.put("password", "PYtv3?s@");
            mqttConfig.update(mqttProps);

            Configuration dataConfig = this.m_configAdmin.getConfiguration("org.eclipse.kura.data.DataService", "?");
            Dictionary<String, Object> dataProps = dataConfig.getProperties();
            dataProps.put("connect.auto-on-startup", true);
            dataConfig.update(dataProps);
        } catch (Exception e) {
            e.printStackTrace();
            s_logger.error("Failed to reconfigure the broker settings - failing out");
            System.exit(-1);
        }

        // wait for connection?
        while (!this.m_cloudService.isConnected()) {
            s_logger.warn("waiting for the cloud client to connect");
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        KuraPayload payload = new KuraPayload();
        try {
            this.m_cloudClient.publish("test/start", payload, 1, false);
        } catch (KuraException e) {
            e.printStackTrace();
        }
    }

    private void finishedTests() {
        // wait for connection??
        while (!this.m_cloudService.isConnected()) {
            s_logger.warn("waiting for the cloud client to connect");
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        KuraPayload payload = new KuraPayload();
        try {
            this.m_cloudClient.publish("test/finished", payload, 1, false);
        } catch (KuraException e) {
            e.printStackTrace();
        }
    }
}
