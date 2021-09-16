/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.system;

import static java.lang.Thread.currentThread;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.system.ExtendedProperties;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemServiceImpl extends SuperSystemService implements SystemService {

    private static final String PROPERTY_PROVIDER_SUFFIX = ".provider";

    private static final String DMIDECODE_COMMAND = "dmidecode -t system";

    private static final String SPACES_REGEX = ":\\s+";

    private static final String BIN_SH = "/bin/sh";

    private static final String LINUX_2_6_34_12_WR4_3_0_0_STANDARD = "2.6.34.12-WR4.3.0.0_standard";

    private static final String LINUX_2_6_34_9_WR4_2_0_0_STANDARD = "2.6.34.9-WR4.2.0.0_standard";

    private static final Logger logger = LoggerFactory.getLogger(SystemServiceImpl.class);

    private static final String CLOUDBEES_SECURITY_SETTINGS_PATH = "/private/eurotech/settings-security.xml";
    private static final String LOG4J_CONFIGURATION = "log4j.configuration";
    private static final String DPA_CONFIGURATION = "dpa.configuration";
    private static final String KURA_PATH = "/opt/eclipse/kura";
    private static final String OS_WINDOWS = "windows";

    private static boolean onCloudbees = false;

    private Properties kuraProperties;
    private ComponentContext componentContext;

    private NetworkService networkService;
    private CommandExecutorService executorService;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void unsetNetworkService(NetworkService networkService) {
        this.networkService = null;
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    @SuppressWarnings({ "rawtypes", "unchecked", "checkstyle:methodLength" })
    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;

        AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                // privileged code goes here, for example:
                onCloudbees = new File(CLOUDBEES_SECURITY_SETTINGS_PATH).exists();
                return null; // nothing to return
            } catch (Exception e) {
                logger.warn("Unable to execute privileged in SystemService");
                return null;
            }
        });

        // load the defaults from the kura.properties files
        Properties kuraDefaults = new Properties();
        boolean updateTriggered = false;
        try {
            // special cases for older versions to fix relative path bug in v2.0.4 and earlier
            if (System.getProperty(KURA_CONFIG) != null
                    && System.getProperty(KURA_CONFIG).trim().equals("file:kura/kura.properties")) {
                System.setProperty(KURA_CONFIG, "file:/opt/eclipse/kura/framework/kura.properties");
                updateTriggered = true;
                logger.warn("Overridding invalid kura.properties location");
            }
            if (System.getProperty(DPA_CONFIGURATION) != null
                    && System.getProperty(DPA_CONFIGURATION).trim().equals("kura/dpa.properties")) {
                System.setProperty(DPA_CONFIGURATION, "/opt/eclipse/kura/packages/dpa.properties");
                updateTriggered = true;
                logger.warn("Overridding invalid dpa.properties location");
            }
            if (System.getProperty(LOG4J_CONFIGURATION) != null
                    && System.getProperty(LOG4J_CONFIGURATION).trim().equals("file:kura/log4j.properties")) {
                System.setProperty(LOG4J_CONFIGURATION, "file:/opt/eclipse/kura/user/log4j.properties");
                updateTriggered = true;
                logger.warn("Overridding invalid log4j.properties location");
            }

            // load the default kura.properties
            // look for kura.properties as resource in the classpath
            // if not found, look for such file in the kura.home directory
            String kuraHome = System.getProperty(KEY_KURA_HOME_DIR);
            String kuraFrameworkConfig = System.getProperty(KEY_KURA_FRAMEWORK_CONFIG_DIR);
            String kuraUserConfig = System.getProperty(KEY_KURA_USER_CONFIG_DIR);
            String kuraConfig = System.getProperty(KURA_CONFIG);
            String kuraProps = readResource(KURA_PROPS_FILE);

            if (kuraProps != null) {
                kuraDefaults.load(new StringReader(kuraProps));
                logger.info("Loaded Jar Resource kura.properties.");
            } else if (kuraConfig != null) {
                loadKuraDefaults(kuraDefaults, kuraConfig);
            } else if (kuraFrameworkConfig != null) {
                File kuraPropsFile = new File(kuraFrameworkConfig + File.separator + KURA_PROPS_FILE);
                if (kuraPropsFile.exists()) {
                    try (final FileReader fr = new FileReader(kuraPropsFile)) {
                        kuraDefaults.load(fr);
                    }
                    logger.info("Loaded File kura.properties: {}", kuraPropsFile);
                } else {
                    logger.warn("File does not exist: {}", kuraPropsFile);
                }

            } else {
                logger.error("Could not locate kura.properties file");
            }

            // Try to reload kuraHome with the value set in kura.properties file.
            if (kuraHome == null) {
                kuraHome = kuraDefaults.getProperty(KEY_KURA_HOME_DIR);
            }
            // Try to reload kuraFrameworkConfig with the value set in kura.properties file.
            if (kuraFrameworkConfig == null) {
                kuraFrameworkConfig = kuraDefaults.getProperty(KEY_KURA_FRAMEWORK_CONFIG_DIR);
            }
            // Try to reload kurauserConfig with the value set in kura.properties file.
            if (kuraUserConfig == null) {
                kuraUserConfig = kuraDefaults.getProperty(KEY_KURA_USER_CONFIG_DIR);
            }

            // load custom kura properties
            // look for kura_custom.properties as resource in the classpath
            // if not found, look for such file in the kura.user.config directory
            Properties kuraCustomProps = new Properties();
            String kuraCustomConfig = System.getProperty(KURA_CUSTOM_CONFIG);
            String kuraCustomProperties = readResource(KURA_CUSTOM_PROPS_FILE);

            if (kuraCustomProperties != null) {
                kuraCustomProps.load(new StringReader(kuraCustomProperties));
                logger.info("Loaded Jar Resource: {}", KURA_CUSTOM_PROPS_FILE);
            } else if (kuraCustomConfig != null) {
                loadKuraCustom(kuraCustomProps, kuraCustomConfig);
            } else if (kuraUserConfig != null) {
                File kuraCustomPropsFile = new File(kuraUserConfig + File.separator + KURA_CUSTOM_PROPS_FILE);
                if (kuraCustomPropsFile.exists()) {
                    Reader reader = new FileReader(kuraCustomPropsFile);
                    try {
                        kuraCustomProps.load(reader);
                    } finally {
                        reader.close();
                    }
                    logger.info("Loaded File {}: {}", KURA_CUSTOM_PROPS_FILE, kuraCustomPropsFile);
                } else {
                    logger.warn("File does not exist: {}", kuraCustomPropsFile);
                }
            } else {
                logger.info("Did not locate a kura_custom.properties file in {}", kuraUserConfig);
            }

            // Override defaults with values from kura_custom.properties
            kuraDefaults.putAll(kuraCustomProps);

            // more path overrides based on earlier Kura problem with relative paths
            if (kuraDefaults.getProperty(KEY_KURA_HOME_DIR) != null
                    && kuraDefaults.getProperty(KEY_KURA_HOME_DIR).trim().equals("kura")) {
                kuraDefaults.setProperty(KEY_KURA_HOME_DIR, KURA_PATH);
                updateTriggered = true;
                logger.warn("Overridding invalid kura.home location");
            }
            if (kuraDefaults.getProperty(KEY_KURA_PLUGINS_DIR) != null
                    && kuraDefaults.getProperty(KEY_KURA_PLUGINS_DIR).trim().equals("kura/plugins")) {
                kuraDefaults.setProperty(KEY_KURA_PLUGINS_DIR, "/opt/eclipse/kura/plugins");
                updateTriggered = true;
                logger.warn("Overridding invalid kura.plugins location");
            }
            if (kuraDefaults.getProperty(KEY_KURA_PACKAGES_DIR) != null
                    && kuraDefaults.getProperty(KEY_KURA_PACKAGES_DIR).trim().equals("kura/packages")) {
                kuraDefaults.setProperty(KEY_KURA_PACKAGES_DIR, "/opt/eclipse/kura/data/packages");
                updateTriggered = true;
                logger.warn("Overridding invalid kura.packages location");
            }

            if (updateTriggered) {
                File directory;       // Desired current working directory

                directory = new File(KURA_PATH).getAbsoluteFile();
                if (directory.exists() || directory.mkdirs()) {
                    String oldDir = System.getProperty("user.dir");
                    if (System.setProperty("user.dir", directory.getAbsolutePath()) != null) {
                        logger.warn("Changed working directory to /opt/eclipse/kura from {}", oldDir);
                    }
                }
            }

            // build the m_kuraProperties instance with the defaults
            this.kuraProperties = new Properties(kuraDefaults);

            // take care of the CloudBees environment
            // that is run in the continuous integration.
            if (onCloudbees) {
                this.kuraProperties.put(KEY_OS_NAME, OS_CLOUDBEES);
            }

            // Put the Net Admin and Web interface availability property so that is available through a get.
            Boolean hasNetAdmin = Boolean.valueOf(this.kuraProperties.getProperty(KEY_KURA_HAVE_NET_ADMIN, "true"));
            this.kuraProperties.put(KEY_KURA_HAVE_NET_ADMIN, hasNetAdmin);
            logger.info("Kura has net admin? {}", hasNetAdmin);
            String webInterfaceEnabled = this.kuraProperties.getProperty(KEY_KURA_HAVE_WEB_INTER, "true");
            this.kuraProperties.put(KEY_KURA_HAVE_WEB_INTER, webInterfaceEnabled);
            logger.info("Is Kura web interface enabled? {}", webInterfaceEnabled);
            String kuraVersion = this.kuraProperties.getProperty(KEY_KURA_VERSION, "version-unknown");
            this.kuraProperties.put(KEY_KURA_VERSION, kuraVersion);
            logger.info("Kura version? {}", kuraVersion);

            if (System.getProperty(KEY_KURA_NAME) != null) {
                this.kuraProperties.put(KEY_KURA_NAME, System.getProperty(KEY_KURA_NAME));
            }
            if (System.getProperty(KEY_KURA_VERSION) != null) {
                this.kuraProperties.put(KEY_KURA_VERSION, System.getProperty(KEY_KURA_VERSION));
            }
            if (System.getProperty(KEY_DEVICE_NAME) != null) {
                this.kuraProperties.put(KEY_DEVICE_NAME, System.getProperty(KEY_DEVICE_NAME));
            }
            if (System.getProperty(KEY_PLATFORM) != null) {
                this.kuraProperties.put(KEY_PLATFORM, System.getProperty(KEY_PLATFORM));
            }
            if (System.getProperty(KEY_MODEL_ID) != null) {
                this.kuraProperties.put(KEY_MODEL_ID, System.getProperty(KEY_MODEL_ID));
            }
            if (System.getProperty(KEY_MODEL_NAME) != null) {
                this.kuraProperties.put(KEY_MODEL_NAME, System.getProperty(KEY_MODEL_NAME));
            }
            if (System.getProperty(KEY_PART_NUMBER) != null) {
                this.kuraProperties.put(KEY_PART_NUMBER, System.getProperty(KEY_PART_NUMBER));
            }
            if (System.getProperty(KEY_SERIAL_NUM) != null) {
                this.kuraProperties.put(KEY_SERIAL_NUM, System.getProperty(KEY_SERIAL_NUM));
            }
            if (System.getProperty(KEY_BIOS_VERSION) != null) {
                this.kuraProperties.put(KEY_BIOS_VERSION, System.getProperty(KEY_BIOS_VERSION));
            }
            if (System.getProperty(KEY_FIRMWARE_VERSION) != null) {
                this.kuraProperties.put(KEY_FIRMWARE_VERSION, System.getProperty(KEY_FIRMWARE_VERSION));
            }
            if (System.getProperty(KEY_PRIMARY_NET_IFACE) != null) {
                this.kuraProperties.put(KEY_PRIMARY_NET_IFACE, System.getProperty(KEY_PRIMARY_NET_IFACE));
            }
            if (System.getProperty(KEY_KURA_HOME_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_HOME_DIR, System.getProperty(KEY_KURA_HOME_DIR));
            }
            if (System.getProperty(KEY_KURA_FRAMEWORK_CONFIG_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_FRAMEWORK_CONFIG_DIR,
                        System.getProperty(KEY_KURA_FRAMEWORK_CONFIG_DIR));
            }
            if (System.getProperty(KEY_KURA_USER_CONFIG_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_USER_CONFIG_DIR, System.getProperty(KEY_KURA_USER_CONFIG_DIR));
            }
            if (System.getProperty(KEY_KURA_PLUGINS_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_PLUGINS_DIR, System.getProperty(KEY_KURA_PLUGINS_DIR));
            }
            if (System.getProperty(KEY_KURA_PACKAGES_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_PACKAGES_DIR, System.getProperty(KEY_KURA_PACKAGES_DIR));
            }
            if (System.getProperty(KEY_KURA_DATA_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_DATA_DIR, System.getProperty(KEY_KURA_DATA_DIR));
            }
            if (System.getProperty(KEY_KURA_TMP_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_TMP_DIR, System.getProperty(KEY_KURA_TMP_DIR));
            }
            if (System.getProperty(KEY_KURA_SNAPSHOTS_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_SNAPSHOTS_DIR, System.getProperty(KEY_KURA_SNAPSHOTS_DIR));
            }
            if (System.getProperty(KEY_KURA_SNAPSHOTS_COUNT) != null) {
                this.kuraProperties.put(KEY_KURA_SNAPSHOTS_COUNT, System.getProperty(KEY_KURA_SNAPSHOTS_COUNT));
            }
            if (System.getProperty(KEY_KURA_HAVE_NET_ADMIN) != null) {
                this.kuraProperties.put(KEY_KURA_HAVE_NET_ADMIN, System.getProperty(KEY_KURA_HAVE_NET_ADMIN));
            }
            if (System.getProperty(KEY_KURA_HAVE_WEB_INTER) != null) {
                this.kuraProperties.put(KEY_KURA_HAVE_WEB_INTER, System.getProperty(KEY_KURA_HAVE_WEB_INTER));
            }
            if (System.getProperty(KEY_KURA_STYLE_DIR) != null) {
                this.kuraProperties.put(KEY_KURA_STYLE_DIR, System.getProperty(KEY_KURA_STYLE_DIR));
            }
            if (System.getProperty(KEY_KURA_WIFI_TOP_CHANNEL) != null) {
                this.kuraProperties.put(KEY_KURA_WIFI_TOP_CHANNEL, System.getProperty(KEY_KURA_WIFI_TOP_CHANNEL));
            }
            if (System.getProperty(KEY_KURA_KEY_STORE_PWD) != null) {
                this.kuraProperties.put(KEY_KURA_KEY_STORE_PWD, System.getProperty(KEY_KURA_KEY_STORE_PWD));
            }
            if (System.getProperty(KEY_KURA_TRUST_STORE_PWD) != null) {
                this.kuraProperties.put(KEY_KURA_TRUST_STORE_PWD, System.getProperty(KEY_KURA_TRUST_STORE_PWD));
            }
            if (System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_SIZE) != null) {
                this.kuraProperties.put(KEY_FILE_COMMAND_ZIP_MAX_SIZE,
                        System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_SIZE));
            }
            if (System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_NUMBER) != null) {
                this.kuraProperties.put(KEY_FILE_COMMAND_ZIP_MAX_NUMBER,
                        System.getProperty(KEY_FILE_COMMAND_ZIP_MAX_NUMBER));
            }
            if (System.getProperty(KEY_OS_ARCH) != null) {
                this.kuraProperties.put(KEY_OS_ARCH, System.getProperty(KEY_OS_ARCH));
            }
            if (System.getProperty(KEY_OS_NAME) != null) {
                this.kuraProperties.put(KEY_OS_NAME, System.getProperty(KEY_OS_NAME));
            }
            if (System.getProperty(KEY_OS_VER) != null) {
                this.kuraProperties.put(KEY_OS_VER, getOsVersion()); // System.getProperty(KEY_OS_VER)
            }
            if (System.getProperty(KEY_OS_DISTRO) != null) {
                this.kuraProperties.put(KEY_OS_DISTRO, System.getProperty(KEY_OS_DISTRO));
            }
            if (System.getProperty(KEY_OS_DISTRO_VER) != null) {
                this.kuraProperties.put(KEY_OS_DISTRO_VER, System.getProperty(KEY_OS_DISTRO_VER));
            }
            if (System.getProperty(KEY_JAVA_VERSION) != null) {
                this.kuraProperties.put(KEY_JAVA_VERSION, System.getProperty(KEY_JAVA_VERSION));
            }
            if (System.getProperty(KEY_JAVA_VENDOR) != null) {
                this.kuraProperties.put(KEY_JAVA_VENDOR, System.getProperty(KEY_JAVA_VENDOR));
            }
            if (System.getProperty(KEY_JAVA_VM_NAME) != null) {
                this.kuraProperties.put(KEY_JAVA_VM_NAME, System.getProperty(KEY_JAVA_VM_NAME));
            }
            if (System.getProperty(KEY_JAVA_VM_VERSION) != null) {
                this.kuraProperties.put(KEY_JAVA_VM_VERSION, System.getProperty(KEY_JAVA_VM_VERSION));
            }
            if (System.getProperty(KEY_JAVA_VM_INFO) != null) {
                this.kuraProperties.put(KEY_JAVA_VM_INFO, System.getProperty(KEY_JAVA_VM_INFO));
            }
            if (System.getProperty(KEY_OSGI_FW_NAME) != null) {
                this.kuraProperties.put(KEY_OSGI_FW_NAME, System.getProperty(KEY_OSGI_FW_NAME));
            }
            if (System.getProperty(KEY_OSGI_FW_VERSION) != null) {
                this.kuraProperties.put(KEY_OSGI_FW_VERSION, System.getProperty(KEY_OSGI_FW_VERSION));
            }
            if (System.getProperty(KEY_JAVA_HOME) != null) {
                this.kuraProperties.put(KEY_JAVA_HOME, System.getProperty(KEY_JAVA_HOME));
            }
            if (System.getProperty(KEY_FILE_SEP) != null) {
                this.kuraProperties.put(KEY_FILE_SEP, System.getProperty(KEY_FILE_SEP));
            }
            if (System.getProperty(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE) != null) {
                this.kuraProperties.put(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE,
                        System.getProperty(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE));
            }
            if (System.getProperty(DB_URL_PROPNAME) != null) {
                this.kuraProperties.put(DB_URL_PROPNAME, System.getProperty(DB_URL_PROPNAME));
            }
            if (System.getProperty(DB_CACHE_ROWS_PROPNAME) != null) {
                this.kuraProperties.put(DB_CACHE_ROWS_PROPNAME, System.getProperty(DB_CACHE_ROWS_PROPNAME));
            }
            if (System.getProperty(DB_LOB_FILE_PROPNAME) != null) {
                this.kuraProperties.put(DB_LOB_FILE_PROPNAME, System.getProperty(DB_LOB_FILE_PROPNAME));
            }
            if (System.getProperty(DB_DEFRAG_LIMIT_PROPNAME) != null) {
                this.kuraProperties.put(DB_DEFRAG_LIMIT_PROPNAME, System.getProperty(DB_DEFRAG_LIMIT_PROPNAME));
            }
            if (System.getProperty(DB_LOG_DATA_PROPNAME) != null) {
                this.kuraProperties.put(DB_LOG_DATA_PROPNAME, System.getProperty(DB_LOG_DATA_PROPNAME));
            }
            if (System.getProperty(DB_LOG_SIZE_PROPNAME) != null) {
                this.kuraProperties.put(DB_LOG_SIZE_PROPNAME, System.getProperty(DB_LOG_SIZE_PROPNAME));
            }
            if (System.getProperty(DB_NIO_PROPNAME) != null) {
                this.kuraProperties.put(DB_NIO_PROPNAME, System.getProperty(DB_NIO_PROPNAME));
            }
            if (System.getProperty(DB_WRITE_DELAY_MILLIES_PROPNAME) != null) {
                this.kuraProperties.put(DB_WRITE_DELAY_MILLIES_PROPNAME,
                        System.getProperty(DB_WRITE_DELAY_MILLIES_PROPNAME));
            }
            if (System.getProperty(KEY_CPU_VERSION) != null) {
                this.kuraProperties.put(KEY_CPU_VERSION, System.getProperty(KEY_CPU_VERSION));
            }
            if (System.getProperty(KEY_COMMAND_USER) != null) {
                this.kuraProperties.put(KEY_COMMAND_USER, System.getProperty(KEY_COMMAND_USER));
            }

            if (getKuraHome() == null) {
                logger.error("Did not initialize kura.home");
            } else {
                logger.info("Kura home directory is {}", getKuraHome());
                createDirIfNotExists(getKuraHome());
            }
            if (getKuraFrameworkConfigDirectory() == null) {
                logger.error("Did not initialize kura.framework.config");
            } else {
                logger.info("Kura framework configuration directory is {}", getKuraFrameworkConfigDirectory());
                createDirIfNotExists(getKuraFrameworkConfigDirectory());
            }
            if (getKuraUserConfigDirectory() == null) {
                logger.error("Did not initialize kura.user.config");
            } else {
                logger.info("Kura user configuration directory is {}", getKuraUserConfigDirectory());
                createDirIfNotExists(getKuraUserConfigDirectory());
            }
            if (getKuraSnapshotsDirectory() == null) {
                logger.error("Did not initialize kura.snapshots");
            } else {
                logger.info("Kura snapshots directory is {}", getKuraSnapshotsDirectory());
                createDirIfNotExists(getKuraSnapshotsDirectory());
            }
            if (getKuraTemporaryConfigDirectory() == null) {
                logger.error("Did not initialize kura.tmp");
            } else {
                logger.info("Kura tmp directory is {}", getKuraTemporaryConfigDirectory());
                createDirIfNotExists(getKuraTemporaryConfigDirectory());
            }

            logger.info("Kura version {} is starting", getKuraVersion());
        } catch (IOException e) {
            throw new ComponentException("Error loading default properties", e);
        }
    }

    private void loadKuraCustom(Properties kuraCustomProps, String kuraCustomConfig) {
        try {
            final URL kuraConfigUrl = new URL(kuraCustomConfig);
            try (InputStream in = kuraConfigUrl.openStream()) {
                kuraCustomProps.load(in);
            }
            logger.info("Loaded URL kura_custom.properties: {}", kuraCustomConfig);
        } catch (Exception e) {
            logger.warn("Could not open kuraCustomConfig URL: ", e);
        }
    }

    private void loadKuraDefaults(Properties kuraDefaults, String kuraConfig) {
        try {
            final URL kuraConfigUrl = new URL(kuraConfig);
            try (InputStream in = kuraConfigUrl.openStream()) {
                kuraDefaults.load(in);
            }
            logger.info("Loaded URL kura.properties: {}", kuraConfig);
        } catch (Exception e) {
            logger.warn("Could not open kuraConfig URL", e);
        }
    }

    protected String readResource(String resource) throws IOException {
        if (resource == null) {
            return null;
        }

        final URL resourceUrl = currentThread().getContextClassLoader().getResource(resource);

        if (resourceUrl == null) {
            return null;
        }

        return IOUtils.toString(resourceUrl);
    }

    protected void deactivate(ComponentContext componentContext) {
        this.componentContext = null;
        this.kuraProperties = null;
    }

    public void updated(Map<String, Object> properties) {
        // nothing to do
        // all properties of the System service are read-only
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    /**
     * Returns all KuraProperties for this system. The returned instances is
     * initialized by loading the kura.properties file. Properties defined at
     * the System level - for example using the java -D command line flag -
     * are used to overwrite the values loaded from the kura.properties file
     * in a hierarchical configuration fashion.
     */
    @Override
    public Properties getProperties() {
        return this.kuraProperties;
    }

    @Override
    public String getPrimaryMacAddress() {
        String primaryNetworkInterfaceName = getPrimaryNetworkInterfaceName();

        if (OS_MAC_OSX.equals(getOsName())) {
            return getPrimaryMacAddressOSX(primaryNetworkInterfaceName);
        } else if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            return getPrimaryMacAddressWindows(primaryNetworkInterfaceName);
        } else {
            return getPrimaryMacAddressLinux(primaryNetworkInterfaceName);
        }
    }

    private String getPrimaryMacAddressOSX(String primaryNetworkInterfaceName) {
        String macAddress = null;
        try {
            logger.info("executing: ifconfig and looking for {}", primaryNetworkInterfaceName);
            List<String> out = Arrays
                    .asList(runSystemCommand("ifconfig", false, this.executorService).split("\\r?\\n"));
            ListIterator<String> iterator = out.listIterator();
            String line;
            while (iterator.hasNext()) {
                line = iterator.next();
                if (line.startsWith(primaryNetworkInterfaceName)) {
                    // get the next line and save the MAC
                    line = iterator.next();
                    if (line == null) {
                        throw new IOException("Null input!");
                    }
                    if (!line.trim().startsWith("ether")) {
                        line = iterator.next();
                    }
                    String[] splitLine = line.split(" ");
                    if (splitLine.length > 0) {
                        macAddress = splitLine[1].toUpperCase();
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to get network interfaces", e);
        }
        return macAddress;
    }

    private String getPrimaryMacAddressWindows(String primaryNetworkInterfaceName) {
        String macAddress = null;
        try {
            logger.info("executing: InetAddress.getLocalHost {}", primaryNetworkInterfaceName);
            InetAddress ip = InetAddress.getLocalHost();
            // Windows options are either ethX or wlanX, and eth0 may really not be the correct one
            InetAddress ip2 = getPrimaryIPWindows("eth");
            if (ip2 == null) {
                ip2 = getPrimaryIPWindows("wlan");
            }
            if (ip2 != null) {
                ip = ip2;
            }
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            macAddress = hardwareAddressToString(mac);
            logger.info("macAddress {}", macAddress);
        } catch (UnknownHostException | SocketException e) {
            logger.error(e.getLocalizedMessage());
        }
        return macAddress;
    }

    private String getPrimaryMacAddressLinux(String primaryNetworkInterfaceName) {
        String macAddress = null;
        try {
            List<NetInterface<? extends NetInterfaceAddress>> interfaces = this.networkService.getNetworkInterfaces();
            if (interfaces != null) {
                for (NetInterface<? extends NetInterfaceAddress> iface : interfaces) {
                    if (iface.getName() != null && primaryNetworkInterfaceName.equals(iface.getName())) {
                        macAddress = hardwareAddressToString(iface.getHardwareAddress());
                        break;
                    }
                }
            }
        } catch (KuraException e) {
            logger.error("Failed to get network interfaces", e);
        }
        return macAddress;
    }

    /**
     * Returns ip of the first interface name of which begins with <code>prefix</code>.
     *
     * @param prefix
     *            network interface name prefix e.g. eth, wlan
     * @return ip of the first interface name of which begins with prefix; null if none found with ip
     * @throws SocketException
     */
    private InetAddress getPrimaryIPWindows(String prefix) throws SocketException {
        InetAddress ip = null;

        Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
        while (networks.hasMoreElements()) {
            NetworkInterface network = networks.nextElement();
            if (network.getName().startsWith(prefix)) {
                Enumeration<InetAddress> ips = network.getInetAddresses();
                if (ips.hasMoreElements()) {
                    ip = ips.nextElement();
                    break;
                }
            }
        }

        return ip;
    }

    @Override
    public String getPrimaryNetworkInterfaceName() {
        final Optional<String> propertyValue = getProperty(KEY_PRIMARY_NET_IFACE);

        if (propertyValue.isPresent()) {
            return propertyValue.get();
        } else {
            if (OS_MAC_OSX.equals(getOsName())) {
                return "en0";
            } else if (OS_LINUX.equals(getOsName())) {
                return "eth0";
            } else if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
                return OS_WINDOWS;
            } else {
                logger.error("Unsupported platform");
                return null;
            }
        }
    }

    @Override
    public String getPlatform() {
        return getProperty(KEY_PLATFORM).orElse(null);
    }

    @Override
    public String getOsArch() {
        final Optional<String> override = getProperty(KEY_OS_ARCH);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_OS_ARCH);
    }

    @Override
    public String getOsName() {
        final Optional<String> override = getProperty(KEY_OS_NAME);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_OS_NAME);
    }

    @Override
    public String getOsVersion() {
        final Optional<String> override = getProperty(KEY_OS_VER);
        if (override.isPresent()) {
            return override.get();
        }

        StringBuilder sbOsVersion = new StringBuilder();
        sbOsVersion.append(System.getProperty(KEY_OS_VER));
        if (OS_LINUX.equals(getOsName())) {
            File linuxKernelVersion = null;

            linuxKernelVersion = new File("/proc/sys/kernel/version");
            if (linuxKernelVersion.exists()) {
                StringBuilder kernelVersionData = new StringBuilder();
                try (FileReader fr = new FileReader(linuxKernelVersion); BufferedReader in = new BufferedReader(fr)) {
                    String tempLine = null;
                    while ((tempLine = in.readLine()) != null) {
                        kernelVersionData.append(" ");
                        kernelVersionData.append(tempLine);
                    }
                    sbOsVersion.append(kernelVersionData.toString());
                } catch (IOException e) {
                    logger.error("Failed to get OS version", e);
                }
            }

        }

        return sbOsVersion.toString();
    }

    @Override
    public String getOsDistro() {
        return getProperty(KEY_OS_DISTRO).orElse(null);
    }

    @Override
    public String getOsDistroVersion() {
        return getProperty(KEY_OS_DISTRO_VER).orElse(null);
    }

    @Override
    public String getJavaVendor() {
        final Optional<String> override = getProperty(KEY_JAVA_VENDOR);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_JAVA_VENDOR);
    }

    @Override
    public String getJavaVersion() {
        final Optional<String> override = getProperty(KEY_JAVA_VERSION);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_JAVA_VERSION);
    }

    @Override
    public String getJavaVmName() {
        final Optional<String> override = getProperty(KEY_JAVA_VM_NAME);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_JAVA_VM_NAME);
    }

    @Override
    public String getJavaVmVersion() {
        final Optional<String> override = getProperty(KEY_JAVA_VM_VERSION);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_JAVA_VM_VERSION);
    }

    @Override
    public String getJavaVmInfo() {
        final Optional<String> override = getProperty(KEY_JAVA_VM_INFO);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_JAVA_VM_INFO);
    }

    @Override
    public String getOsgiFwName() {
        final Optional<String> override = getProperty(KEY_OSGI_FW_NAME);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_OSGI_FW_NAME);
    }

    @Override
    public String getOsgiFwVersion() {
        final Optional<String> override = getProperty(KEY_OSGI_FW_VERSION);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_OSGI_FW_VERSION);
    }

    @Override
    public int getNumberOfProcessors() {
        try {
            return Runtime.getRuntime().availableProcessors();
        } catch (Throwable t) {
            // NoSuchMethodError on pre-1.4 runtimes
        }
        return -1;
    }

    @Override
    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory() / 1024;
    }

    @Override
    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory() / 1024;
    }

    @Override
    public String getFileSeparator() {
        final Optional<String> override = getProperty(KEY_FILE_SEP);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_FILE_SEP);
    }

    @Override
    public String getJavaHome() {
        final Optional<String> override = getProperty(KEY_JAVA_HOME);
        if (override.isPresent()) {
            return override.get();
        }

        return System.getProperty(KEY_JAVA_HOME);
    }

    public String getKuraName() {
        return getProperty(KEY_KURA_NAME).orElse(null);
    }

    @Override
    public String getKuraVersion() {
        return getProperty(KEY_KURA_VERSION).orElse(null);
    }

    @Override
    public String getKuraMarketplaceCompatibilityVersion() {
        final Optional<String> override = getProperty(KEY_KURA_MARKETPLACE_COMPATIBILITY_VERSION);
        final String marketplaceCompatibilityVersion;

        if (override.isPresent()) {
            marketplaceCompatibilityVersion = override.get();
        } else {
            marketplaceCompatibilityVersion = getKuraVersion();
        }

        return marketplaceCompatibilityVersion.replaceAll("KURA[-_ ]", "").replaceAll("[-_]", ".");
    }

    @Override
    public String getKuraFrameworkConfigDirectory() {
        return getProperty(KEY_KURA_FRAMEWORK_CONFIG_DIR).orElse(null);
    }

    @Override
    public String getKuraUserConfigDirectory() {
        return getProperty(KEY_KURA_USER_CONFIG_DIR).orElse(null);
    }

    @Override
    public String getKuraHome() {
        return getProperty(KEY_KURA_HOME_DIR).orElse(null);
    }

    public String getKuraPluginsDirectory() {
        return getProperty(KEY_KURA_PLUGINS_DIR).orElse(null);
    }

    @Override
    public String getKuraDataDirectory() {
        return getProperty(KEY_KURA_DATA_DIR).orElse(null);
    }

    @Override
    public String getKuraTemporaryConfigDirectory() {
        return getProperty(KEY_KURA_TMP_DIR).orElse(null);
    }

    @Override
    public String getKuraSnapshotsDirectory() {
        return getProperty(KEY_KURA_SNAPSHOTS_DIR).orElse(null);
    }

    @Override
    public int getKuraSnapshotsCount() {
        int iMaxCount = 10;
        final Optional<String> maxCount = getProperty(KEY_KURA_SNAPSHOTS_COUNT);
        if (maxCount.isPresent() && maxCount.get().trim().length() > 0) {
            try {
                iMaxCount = Integer.parseInt(maxCount.get());
            } catch (NumberFormatException nfe) {
                logger.error("Error - Invalid kura.snapshots.count setting. Using default.", nfe);
            }
        }
        return iMaxCount;
    }

    @Override
    public int getKuraWifiTopChannel() {
        final Optional<String> topWifiChannel = getProperty(KEY_KURA_WIFI_TOP_CHANNEL);
        if (topWifiChannel.isPresent() && topWifiChannel.get().trim().length() > 0) {
            return Integer.parseInt(topWifiChannel.get());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("The last wifi channel is not defined for this system - setting fake value.");
        }

        return Integer.MAX_VALUE;
    }

    @Override
    public String getKuraStyleDirectory() {
        return getProperty(KEY_KURA_STYLE_DIR).orElse(null);
    }

    @Override
    public String getKuraWebEnabled() {
        return getProperty(KEY_KURA_HAVE_WEB_INTER).orElse(null);
    }

    @Override
    public int getFileCommandZipMaxUploadSize() {
        final Optional<String> commandMaxUpload = getProperty(KEY_FILE_COMMAND_ZIP_MAX_SIZE);
        if (commandMaxUpload.isPresent() && commandMaxUpload.get().trim().length() > 0) {
            return Integer.parseInt(commandMaxUpload.get());
        }
        logger.warn("Maximum command line upload size not available. Set default to 100 MB");
        return 100;
    }

    @Override
    public int getFileCommandZipMaxUploadNumber() {
        final Optional<String> commandMaxFilesUpload = getProperty(KEY_FILE_COMMAND_ZIP_MAX_NUMBER);
        if (commandMaxFilesUpload.isPresent() && commandMaxFilesUpload.get().trim().length() > 0) {
            return Integer.parseInt(commandMaxFilesUpload.get());
        }
        logger.warn(
                "Missing the parameter that specifies the maximum number of files uploadable using the command servlet. Set default to 1024 files");
        return 1024;
    }

    @Override
    public String getBiosVersion() {
        final Optional<String> override = getProperty(KEY_BIOS_VERSION);
        if (override.isPresent()) {
            return override.get();
        }

        String biosVersion = UNSUPPORTED;

        if (OS_LINUX.equals(getOsName())) {
            if (LINUX_2_6_34_9_WR4_2_0_0_STANDARD.equals(getOsVersion())
                    || LINUX_2_6_34_12_WR4_3_0_0_STANDARD.equals(getOsVersion())) {
                biosVersion = runSystemCommand("eth_vers_bios", false, this.executorService);
            } else {
                String biosTmp = runSystemCommand("dmidecode -s bios-version", false, this.executorService);
                if (biosTmp.length() > 0 && !biosTmp.contains("Permission denied")) {
                    biosVersion = biosTmp;
                }
            }
        } else if (OS_MAC_OSX.equals(getOsName())) {
            String[] cmds = { BIN_SH, "-c", "system_profiler SPHardwareDataType | grep 'Boot ROM'" };
            String biosTmp = runSystemCommand(cmds, true, this.executorService);
            if (biosTmp.contains(": ")) {
                biosVersion = biosTmp.split(SPACES_REGEX)[1];
            }
        } else if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            String[] cmds = { "wmic", "bios", "get", "smbiosbiosversion" };
            String biosTmp = runSystemCommand(cmds, false, this.executorService);
            if (biosTmp.contains("SMBIOSBIOSVersion")) {
                biosVersion = biosTmp.split("SMBIOSBIOSVersion\\s+")[1];
                biosVersion = biosVersion.trim();
            }
        }

        return biosVersion;
    }

    @Override
    public String getDeviceName() {
        final Optional<String> override = getProperty(KEY_DEVICE_NAME);
        if (override.isPresent()) {
            return override.get();
        }

        String deviceName = UNKNOWN;
        if (OS_MAC_OSX.equals(getOsName())) {
            String displayTmp = runSystemCommand("scutil --get ComputerName", false, this.executorService);
            if (displayTmp.length() > 0) {
                deviceName = displayTmp;
            }
        } else if (OS_LINUX.equals(getOsName()) || OS_CLOUDBEES.equals(getOsName())
                || getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            String displayTmp = runSystemCommand("hostname", false, this.executorService);
            if (displayTmp.length() > 0) {
                deviceName = displayTmp;
            }
        }
        return deviceName;
    }

    @Override
    public String getFirmwareVersion() {
        final Optional<String> override = getProperty(KEY_FIRMWARE_VERSION);
        if (override.isPresent()) {
            return override.get();
        }

        String fwVersion = UNSUPPORTED;

        if (OS_LINUX.equals(getOsName()) && getOsVersion() != null) {
            if (getOsVersion().startsWith(LINUX_2_6_34_9_WR4_2_0_0_STANDARD)
                    || getOsVersion().startsWith(LINUX_2_6_34_12_WR4_3_0_0_STANDARD)) {
                fwVersion = runSystemCommand("eth_vers_cpld", false, this.executorService) + " "
                        + runSystemCommand("eth_vers_uctl", false, this.executorService);
            } else if (getOsVersion().startsWith("3.0.35-12.09.01+yocto")) {
                fwVersion = runSystemCommand("eth_vers_avr", false, this.executorService);
            }
        }
        return fwVersion;
    }

    @Override
    public String getModelId() {
        final Optional<String> override = getProperty(KEY_MODEL_ID);
        if (override.isPresent()) {
            return override.get();
        }

        String modelId = UNKNOWN;

        if (OS_MAC_OSX.equals(getOsName())) {
            String modelTmp = runSystemCommand("sysctl -b hw.model", false, this.executorService);
            if (modelTmp.length() > 0) {
                modelId = modelTmp;
            }
        } else if (OS_LINUX.equals(getOsName())) {
            String modelTmp = runSystemCommand(DMIDECODE_COMMAND, false, this.executorService);
            if (modelTmp.contains("Version: ")) {
                modelId = modelTmp.split("Version:\\s+")[1].split("\n")[0];
            }
        } else if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            String[] cmds = { "wmic", "baseboard", "get", "Version" };
            String biosTmp = runSystemCommand(cmds, false, this.executorService);
            if (biosTmp.contains("Version")) {
                modelId = biosTmp.split("Version\\s+")[1];
                modelId = modelId.trim();
            }
        }

        return modelId;
    }

    @Override
    public String getModelName() {
        final Optional<String> override = getProperty(KEY_MODEL_NAME);
        if (override.isPresent()) {
            return override.get();
        }

        String modelName = UNKNOWN;

        if (OS_MAC_OSX.equals(getOsName())) {
            String[] cmds = { BIN_SH, "-c", "system_profiler SPHardwareDataType | grep 'Model Name'" };
            String modelTmp = runSystemCommand(cmds, true, this.executorService);
            if (modelTmp.contains(": ")) {
                modelName = modelTmp.split(SPACES_REGEX)[1];
            }
        } else if (OS_LINUX.equals(getOsName())) {
            String modelTmp = runSystemCommand(DMIDECODE_COMMAND, false, this.executorService);
            if (modelTmp.contains("Product Name: ")) {
                modelName = modelTmp.split("Product Name:\\s+")[1].split("\n")[0];
            }
        } else if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            String[] cmds = { "wmic", "baseboard", "get", "Product" };
            String biosTmp = runSystemCommand(cmds, false, this.executorService);
            if (biosTmp.contains("Product")) {
                modelName = biosTmp.split("Product\\s+")[1];
                modelName = modelName.trim();
            }
        }

        return modelName;
    }

    @Override
    public String getPartNumber() {
        final Optional<String> override = getProperty(KEY_PART_NUMBER);
        if (override.isPresent()) {
            return override.get();
        }

        String partNumber = UNSUPPORTED;

        if (OS_LINUX.equals(getOsName()) && (LINUX_2_6_34_9_WR4_2_0_0_STANDARD.equals(getOsVersion())
                || LINUX_2_6_34_12_WR4_3_0_0_STANDARD.equals(getOsVersion()))) {
            partNumber = runSystemCommand("eth_partno_bsp", false, this.executorService) + " "
                    + runSystemCommand("eth_partno_epr", false, this.executorService);
        }

        return partNumber;
    }

    @Override
    public String getSerialNumber() {
        final Optional<String> override = getProperty(KEY_SERIAL_NUM);
        if (override.isPresent()) {
            return override.get();
        }

        String serialNum = UNKNOWN;

        if (OS_MAC_OSX.equals(getOsName())) {
            String[] cmds = { BIN_SH, "-c", "system_profiler SPHardwareDataType | grep 'Serial Number'" };
            String serialTmp = runSystemCommand(cmds, true, this.executorService);
            if (serialTmp.contains(": ")) {
                serialNum = serialTmp.split(SPACES_REGEX)[1];
            }
        } else if (OS_LINUX.equals(getOsName())) {
            String serialTmp = runSystemCommand(DMIDECODE_COMMAND, false, this.executorService);
            if (serialTmp.contains("Serial Number: ")) {
                serialNum = serialTmp.split("Serial Number:\\s+")[1].split("\n")[0];
            }
        } else if (getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            String[] cmds = { "wmic", "bios", "get", "SerialNumber" };
            String biosTmp = runSystemCommand(cmds, false, this.executorService);
            if (biosTmp.contains("SerialNumber")) {
                serialNum = biosTmp.split("SerialNumber\\s+")[1];
                serialNum = serialNum.trim();
            }
        }

        return serialNum;
    }

    @Override
    public char[] getJavaKeyStorePassword() {
        final Optional<String> keyStorePwd = getProperty(KEY_KURA_KEY_STORE_PWD);
        if (keyStorePwd.isPresent()) {
            return keyStorePwd.get().toCharArray();
        }
        return new char[0];
    }

    @Override
    public char[] getJavaTrustStorePassword() {
        final Optional<String> trustStorePwd = getProperty(KEY_KURA_TRUST_STORE_PWD);
        if (trustStorePwd.isPresent()) {
            return trustStorePwd.get().toCharArray();
        }
        return new char[0];
    }

    @Override
    public Bundle[] getBundles() {
        if (this.componentContext == null) {
            return null;
        }
        return this.componentContext.getBundleContext().getBundles();
    }

    @Override
    public List<SystemResourceInfo> getSystemPackages() throws KuraProcessExecutionErrorException {

        List<SystemResourceInfo> packagesInfo = new ArrayList<>();
        CommandStatus debStatus = execute(new String[] { "dpkg-query", "-W" });
        if (debStatus.getExitStatus().isSuccessful()
                && ((ByteArrayOutputStream) debStatus.getOutputStream()).size() > 0) {
            parseSystemPackages(packagesInfo, debStatus, SystemResourceType.DEB);
        }

        CommandStatus rpmStatus = execute(
                new String[] { "rpm", "-qa", "--queryformat", "'%{NAME} %{VERSION}-%{RELEASE}\n'" });
        if (rpmStatus.getExitStatus().isSuccessful()
                && ((ByteArrayOutputStream) rpmStatus.getOutputStream()).size() > 0) {
            parseSystemPackages(packagesInfo, rpmStatus, SystemResourceType.RPM);
        }

        CommandStatus apkStatus = execute(new String[] { "apk", "list", "-I", "|", "awk", "'{ print $1 }'" });
        if (apkStatus.getExitStatus().isSuccessful()
                && ((ByteArrayOutputStream) apkStatus.getOutputStream()).size() > 0) {
            parseSystemPackages(packagesInfo, apkStatus, SystemResourceType.APK);
        }

        if (!debStatus.getExitStatus().isSuccessful() && !rpmStatus.getExitStatus().isSuccessful()
                && !apkStatus.getExitStatus().isSuccessful()) {
            throw new KuraProcessExecutionErrorException("Failed to retrieve system packages.");
        }
        return packagesInfo;
    }

    private void parseSystemPackages(List<SystemResourceInfo> packagesInfo, CommandStatus status,
            SystemResourceType type) {
        String[] packages = new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8)
                .split("\n");
        Arrays.asList(packages).stream().forEach(p -> {
            String[] fields = p.split("\\s+"); // this works for dpkg and rpm where separator for version and name is a
                                               // sequence of spaces
            if (fields.length >= 2) {
                packagesInfo.add(new SystemResourceInfo(fields[0], fields[1], type));
            } else {
                // apk case: need more complex parsing
                String[] nameAndVersion = getApkNameAndVersion(fields[0]);
                packagesInfo.add(new SystemResourceInfo(nameAndVersion[0], nameAndVersion[1], type));
            }
        });
    }

    /**
     * An APK package name consists of the name and the version separated by "-".
     * The name and the version itself can contain "-".
     * Assumptions are that the fullName starts with the package name and ends with the version.
     * 
     * @param fullName
     *            of the APK software package, e.g. "busybox-extras-1.31.1-r10"
     * @return String array with name in position 0 and version in position 1
     */
    private String[] getApkNameAndVersion(String fullName) {
        String[] split = fullName.split("-");
        String name = "";
        String version = "";
        int matchIndex = 1000;

        for (int i = 0; i < split.length; i++) {
            String s = split[i];

            if (i > 0 && i < matchIndex) {
                // version is never at the beginning
                if (s.matches("\\d+.(\\w+(.)?)+")) {
                    version += s;
                    matchIndex = i;
                } else {
                    name += "-" + s;
                }
            }
            if (i > matchIndex) {
                // everything else after match is version
                version += "-" + s;
            }
        }
        // assuming the first part belongs to the name
        name = split[0] + name;

        return new String[] { name, version };
    }

    private CommandStatus execute(String[] commandLine) {
        Command command = new Command(commandLine);
        command.setExecuteInAShell(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        command.setErrorStream(err);
        command.setOutputStream(out);
        CommandStatus status = this.executorService.execute(command);
        if (logger.isDebugEnabled()) {
            logger.debug("execute command {} :: exited with code - {}", command, status.getExitStatus().getExitCode());
            logger.debug("execute stderr {}", new String(err.toByteArray(), Charsets.UTF_8));
            logger.debug("execute stdout {}", new String(out.toByteArray(), Charsets.UTF_8));
        }
        return status;
    }

    @Override
    public List<String> getDeviceManagementServiceIgnore() {
        final Optional<String> servicesToIgnore = getProperty(CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE);
        List<String> services = new ArrayList<>();
        if (servicesToIgnore.isPresent() && !servicesToIgnore.get().trim().isEmpty()) {
            String[] servicesArray = servicesToIgnore.get().split(",");
            if (servicesArray != null && servicesArray.length > 0) {
                services = Arrays.asList(servicesArray);
            }
        }

        return services;
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    private static void createDirIfNotExists(String fileName) {
        // Make sure the configuration directory exists - create it if not
        File file = new File(fileName);
        if (!file.exists() && !file.mkdirs()) {
            logger.error("Failed to create the temporary configuration directory: {}", fileName);
            if (Boolean.getBoolean("org.eclipse.kura.core.dontExitOnFailure")) {
                throw new RuntimeException(
                        String.format("Failed to create the temporary configuration directory: %s", fileName));
            }
            System.exit(-1);
        }
    }

    @Override
    public String getHostname() {
        String hostname = UNKNOWN;

        if (OS_MAC_OSX.equals(getOsName())) {
            hostname = runSystemCommand("scutil --get ComputerName", false, this.executorService);
        } else if (OS_LINUX.equals(getOsName()) || OS_CLOUDBEES.equals(getOsName())
                || getOsName().toLowerCase().startsWith(OS_WINDOWS)) {
            hostname = runSystemCommand("hostname", false, this.executorService);
        }

        return hostname;
    }

    @Override
    public String getNetVirtualDevicesConfig() {
        String status = NetInterfaceStatus.netIPv4StatusDisabled.name();
        String virtualDefaultConfig = this.kuraProperties.getProperty(KEY_KURA_NET_VIRTUAL_DEVICES_CONFIG);
        if (virtualDefaultConfig != null && virtualDefaultConfig.equalsIgnoreCase("unmanaged")) {
            status = NetInterfaceStatus.netIPv4StatusUnmanaged.name();
        }
        return status;
    }

    private static String hardwareAddressToString(byte[] macAddress) {
        if (macAddress == null) {
            return "N/A";
        }

        if (macAddress.length != 6) {
            throw new IllegalArgumentException("macAddress is invalid");
        }

        StringJoiner sj = new StringJoiner(":");
        for (byte item : macAddress) {
            sj.add(String.format("%02X", item));
        }

        return sj.toString();
    }

    @Override
    public String getCpuVersion() {
        final Optional<String> override = getProperty(KEY_CPU_VERSION);
        if (override.isPresent()) {
            return override.get();
        }

        if (OS_LINUX.equals(getOsName())) {
            try {
                return probeCpuVersionLinux();
            } catch (final Exception e) {
                // do nothing
            }
        }

        return "unknown";
    }

    private static String probeCpuVersionLinux() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;

            while ((line = reader.readLine()) != null) {
                final int separatorIndex = line.indexOf(':');

                if (separatorIndex == -1) {
                    continue;
                }

                final String key = line.substring(0, separatorIndex).trim();

                if (key.equals("model name")) {
                    return line.substring(separatorIndex + 1).trim();
                }
            }
        }

        throw new IOException("Could not retrieve cpu version");
    }

    protected Optional<String> getProperty(final String key) {
        final String prop = this.kuraProperties.getProperty(key);

        if (prop != null) {
            return Optional.of(prop);
        }

        final String externalProvider = this.kuraProperties.getProperty(key + PROPERTY_PROVIDER_SUFFIX);

        if (externalProvider != null) {
            final String result = processCommandOutput(runSystemCommand(externalProvider, true, this.executorService));

            if (result != null && !result.isEmpty()) {
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }

    private String processCommandOutput(final String result) {
        if (result == null) {
            return null;
        }

        final String trimmed = result.trim();

        if (trimmed.isEmpty()) {
            return trimmed;
        }

        int i;

        for (i = trimmed.length() - 1; i > 0; i--) {
            if (trimmed.charAt(i) != '\n') {
                break;
            }
        }

        return trimmed.substring(0, i + 1);
    }

    @Override
    public Optional<ExtendedProperties> getExtendedProperties() {
        return Optional.empty();
    }

    @Override
    public String getCommandUser() {
        final Optional<String> override = getProperty(KEY_COMMAND_USER);
        if (override.isPresent()) {
            return override.get();
        }

        return "unknown";
    }

    @Override
    public boolean isLegacyBluetoothBeaconScan() {
        final Optional<String> override = getProperty(KEY_LEGACY_BT_BEACON_SCAN);
        if (override.isPresent()) {
            return Boolean.parseBoolean(override.get());
        }

        return false;
    }
}
