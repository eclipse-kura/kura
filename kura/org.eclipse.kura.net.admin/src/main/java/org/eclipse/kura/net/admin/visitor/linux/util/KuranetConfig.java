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
package org.eclipse.kura.net.admin.visitor.linux.util;

import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KuranetConfig {

    private static final Logger logger = LoggerFactory.getLogger(KuranetConfig.class);

    private static final String KURA_USER_CONFIG_DIR;
    private static final String KURANET_FILENAME;
    private static final String KURANET_TMP_FILENAME;

    static {
        final BundleContext ctx = FrameworkUtil.getBundle(KuranetConfig.class).getBundleContext();

        final ServiceReference<SystemService> systemServiceRef = ctx.getServiceReference(SystemService.class);
        if (systemServiceRef == null) {
            throw new IllegalStateException("Unable to find instance of: " + SystemService.class.getName());
        }

        final SystemService service = ctx.getService(systemServiceRef);
        if (service == null) {
            throw new IllegalStateException("Unable to get instance of: " + SystemService.class.getName());
        }

        try {
            KURA_USER_CONFIG_DIR = service.getKuraUserConfigDirectory();
            KURANET_FILENAME = KURA_USER_CONFIG_DIR + "/kuranet.conf";
            KURANET_TMP_FILENAME = KURA_USER_CONFIG_DIR + "/kuranet.conf.tmp";
        } finally {
            ctx.ungetService(systemServiceRef);
        }
    }

    private KuranetConfig() {

    }

    // public static Properties getProperties() {
    // Properties kuraExtendedProps = new Properties();
    //
    // logger.debug("Getting {}", KURANET_FILENAME);
    //
    // File kuranetFile = new File(KURANET_FILENAME);
    //
    // if (kuranetFile.exists()) {
    // // found our match so load the properties
    // FileInputStream fis = null;
    // try {
    // fis = new FileInputStream(kuranetFile);
    // kuraExtendedProps.load(fis);
    // } catch (Exception e) {
    // logger.error("Could not load {}", KURANET_FILENAME, e);
    // } finally {
    // if (null != fis) {
    // try {
    // fis.close();
    // } catch (IOException e) {
    // logger.error("Could not load {}", KURANET_FILENAME, e);
    // }
    // }
    // }
    // } else {
    // logger.debug("File does not exist: {}", KURANET_FILENAME);
    // }
    //
    // return kuraExtendedProps;
    // }
    //
    // public static String getProperty(String key) {
    // Properties props = KuranetConfig.getProperties();
    // String value = props.getProperty(key);
    // logger.debug("Got property {} :: {}", key, value);
    // return value;
    // }
    //
    // public static void storeProperties(Properties props) throws IOException, KuraException {
    // Properties oldProperties = KuranetConfig.getProperties();
    //
    // if (!oldProperties.equals(props)) {
    // FileOutputStream fos = null;
    // try {
    // fos = new FileOutputStream(KURANET_TMP_FILENAME);
    // props.store(fos, null);
    // fos.flush();
    // fos.getFD().sync();
    //
    // // move the file if we made it this far
    // File tmpFile = new File(KURANET_TMP_FILENAME);
    // File file = new File(KURANET_FILENAME);
    // if (!FileUtils.contentEquals(tmpFile, file)) {
    // if (tmpFile.renameTo(file)) {
    // logger.trace("Successfully wrote kuranet props file");
    // } else {
    // logger.error("Failed to write kuranet props file");
    // throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
    // "error while building up new configuration file for kuranet props");
    // }
    // } else {
    // logger.info("Not rewriting kuranet props file because it is the same");
    // }
    // } finally {
    // if (fos != null) {
    // fos.close();
    // }
    // }
    // }
    // }
    //
    // public static void setProperty(String key, String value) throws IOException, KuraException {
    // logger.debug("Setting property " + key + " :: " + value);
    // Properties properties = KuranetConfig.getProperties();
    //
    // properties.setProperty(key, value);
    // KuranetConfig.storeProperties(properties);
    // }
    //
    // public static void deleteProperty(String key) throws IOException, KuraException {
    // Properties properties = KuranetConfig.getProperties();
    // if (properties.containsKey(key)) {
    // logger.debug("Deleting property {}", key);
    // properties.remove(key);
    // KuranetConfig.storeProperties(properties);
    // } else {
    // logger.debug("Property does not exist {}", key);
    // }
    // }
}
