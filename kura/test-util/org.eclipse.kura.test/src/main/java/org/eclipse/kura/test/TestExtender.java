/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc 
 *******************************************************************************/
package org.eclipse.kura.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.kura.test.annotation.TestTarget;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExtender implements BundleTrackerCustomizer<Object> {

    private static final Logger logger = LoggerFactory.getLogger(TestExtender.class);

    private static final String KURA_TEST_REPORT_FILENAME = "/tmp/kura_test_report.txt";

    private final Map<Long, Bundle> bundles;
    private final BundleContext bundleContext;
    private BufferedWriter reportWriter;
    private final String platform;

    public TestExtender(String platform, BundleContext bundleContext) {
        this.bundles = new HashMap<Long, Bundle>();
        this.bundleContext = bundleContext;
        this.platform = platform;

        new File(KURA_TEST_REPORT_FILENAME).delete();
    }

    void addBundle(long bundleId, Bundle bundle) {
        this.bundles.put(bundleId, bundle);
    }

    public void testAll() {
        Set<Map.Entry<Long, Bundle>> entrySet = this.bundles.entrySet();
        logger.debug("Testing all bundles");
        for (Entry<Long, Bundle> entry : entrySet) {
            test(entry.getKey());
        }
    }

    public void test(long bundleId) {
        logger.debug("Testing bundle: " + bundleId);

        List<Class<?>> testClazzs = getTestClass(this.bundles.get(bundleId));
        for (Class<?> clazz : testClazzs) {
            try {
                if (!clazz.isInterface()) {
                    logger.debug("Testing CLASS in bundle with ID: " + bundleId + "  : [" + clazz.getName() + "]");
                    Test inspectClass = inspectClass(clazz);
                    testClass(clazz.getName(), inspectClass, clazz.newInstance());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public Class<?> loadClass(String clazz, Bundle bundleHost) {
        try {
            Class<?> loadClass = bundleHost.loadClass(clazz);
            logger.debug("Loaded class: " + loadClass);
            return loadClass;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Bundle getHostBundle(Bundle bundle) {
        String fragment = bundle.getHeaders().get(org.osgi.framework.Constants.FRAGMENT_HOST) + "";
        Bundle[] contextBundles = this.bundleContext.getBundles();
        for (Bundle ibundle : contextBundles) {
            if (ibundle.getSymbolicName().equals(fragment)) {
                logger.debug("Host bundle is: " + ibundle.getSymbolicName());
                return ibundle;
            }
        }
        throw new RuntimeException();
    }

    public List<Class<?>> getTestClass(Bundle bundle) {
        try {
            List<Class<?>> clazzs = new ArrayList<Class<?>>();
            Enumeration<?> entrs = bundle.findEntries("/", "*Test.class", true);
            if (entrs == null || !entrs.hasMoreElements()) {
                return Collections.emptyList();
            }
            Bundle hostBundle = getHostBundle(bundle);
            while (entrs.hasMoreElements()) {
                URL e = (URL) entrs.nextElement();
                String file = e.getFile();

                String className = file.replaceAll("/", ".").replaceAll(".class", "").replaceFirst(".", "");
                if (className.startsWith("bin.src.main.java.")) {
                    className = className.substring(18);
                } else if (className.startsWith("targetes.")) {
                    className = className.substring(9);
                }
                logger.debug("Trying to load class: " + className);
                Class<?> clazz = loadClass(className, hostBundle);
                logger.debug("Adding test class: " + clazz);
                clazzs.add(clazz);
            }
            return clazzs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Test inspectClass(Class<?> clazz) {
        Test test = new Test();
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {

            if (method.isAnnotationPresent(org.junit.BeforeClass.class)) {
                logger.debug("Adding before class: " + method);
                test.setBeforeClass(method);
            }
            if (method.isAnnotationPresent(org.junit.AfterClass.class)) {
                logger.debug("Adding after class: " + method);
                test.setAfterClass(method);
            }
            if (method.isAnnotationPresent(org.junit.Before.class)) {
                logger.debug("Adding before: " + method);
                test.setBefore(method);
            }
            if (method.isAnnotationPresent(org.junit.After.class)) {
                logger.debug("Adding after: " + method);
                test.setAfter(method);
            }
            if (method.isAnnotationPresent(org.junit.Test.class)) {

                if (method.isAnnotationPresent(TestTarget.class)) {
                    TestTarget testTargetAnnotation = method.getAnnotation(TestTarget.class);
                    String[] potentialPlatforms = testTargetAnnotation.targetPlatforms();
                    for (String potentialPlatform : potentialPlatforms) {
                        if (potentialPlatform.equals(TestTarget.PLATFORM_ALL)
                                || potentialPlatform.equals(this.platform)) {
                            logger.debug("TestTarget found " + potentialPlatform + " - Adding test: " + method);
                            test.addTest(method);
                            break;
                        }
                    }
                } else {
                    logger.debug("No TestTarget Annotation present - Adding test: " + method);
                    test.addTest(method);
                }
            }
        }
        return test;
    }

    public void testClass(String className, Test testClass, Object object) {
        try {
            try {
                if (testClass.getBeforeClass() != null) {
                    testClass.getBeforeClass().invoke(object, new Object[0]);
                }

                this.reportWriter = new BufferedWriter(new FileWriter(new File(KURA_TEST_REPORT_FILENAME), true));

                List<Method> tests = testClass.getTests();
                for (Method method : tests) {
                    try {
                        if (testClass.getBefore() != null) {
                            testClass.getBefore().invoke(object, new Object[0]);
                        }

                        try {
                            method.invoke(object, new Object[0]);
                            logger.info("Method : [ " + className + "." + method.getName() + " ] PASS");

                            this.reportWriter.write("Method : [ " + className + "." + method.getName() + " ] PASS\n");
                            this.reportWriter.flush();
                        } catch (Exception ex) {
                            logger.error("Method : [ " + className + "." + method.getName() + " ] FAIL", ex);
                            this.reportWriter = new BufferedWriter(
                                    new FileWriter(new File(KURA_TEST_REPORT_FILENAME), true));
                            this.reportWriter.write("Method : [ " + className + "." + method.getName() + " ] FAIL\n");
                            this.reportWriter.flush();
                        }
                    } finally {
                        if (testClass.getAfter() != null) {
                            testClass.getAfter().invoke(object, new Object[0]);
                        }
                    }
                }
            } finally {
                if (testClass.getAfterClass() != null) {
                    testClass.getAfterClass().invoke(object, new Object[0]);
                }

                this.reportWriter.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        logger.debug("Tracker - Adding Bundle: " + bundle.getSymbolicName());
        return bundle;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        logger.debug("Tracker - Modified Bundle: " + bundle.getSymbolicName());
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        logger.debug("Tracker - Removed Bundle: " + bundle.getSymbolicName());
    }
}
