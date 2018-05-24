/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.system;

import java.util.List;
import java.util.Properties;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;

/**
 * Service to provide basic system information including Operating System
 * information, JVM information and filesystem information.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface SystemService {

    public static final String KURA_CONFIG = "kura.configuration";
    public static final String KURA_PROPS_FILE = "kura.properties";
    public static final String KURA_CUSTOM_CONFIG = "kura.custom.configuration";
    public static final String KURA_CUSTOM_PROPS_FILE = "kura_custom.properties";
    public static final String OS_CLOUDBEES = "Linux (Cloudbees)";
    public static final String OS_LINUX = "Linux";
    public static final String OS_MAC_OSX = "Mac OS X";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String UNSUPPORTED = "UNSUPPORTED";

    public static final String KEY_KURA_NAME = "kura.name";
    public static final String KEY_KURA_VERSION = "kura.version";
    /**
     * @since 1.3
     */
    public static final String KEY_KURA_MARKETPLACE_COMPATIBILITY_VERSION = "kura.marketplace.compatibility.version";
    public static final String KEY_DEVICE_NAME = "kura.device.name";
    public static final String KEY_PLATFORM = "kura.platform";
    public static final String KEY_MODEL_ID = "kura.model.id";
    public static final String KEY_MODEL_NAME = "kura.model.name";
    public static final String KEY_PART_NUMBER = "kura.partNumber";
    public static final String KEY_SERIAL_NUM = "kura.serialNumber";
    public static final String KEY_BIOS_VERSION = "kura.bios.version";
    public static final String KEY_FIRMWARE_VERSION = "kura.firmware.version";
    public static final String KEY_PRIMARY_NET_IFACE = "kura.primary.network.interface";
    public static final String KEY_KURA_HOME_DIR = "kura.home";
    public static final String KEY_KURA_PLUGINS_DIR = "kura.plugins";
    /**
     * @since 1.2
     */
    public static final String KEY_KURA_PACKAGES_DIR = "kura.packages";
    public static final String KEY_KURA_DATA_DIR = "kura.data";
    public static final String KEY_KURA_TMP_DIR = "kura.tmp";
    public static final String KEY_KURA_SNAPSHOTS_DIR = "kura.snapshots";
    public static final String KEY_KURA_SNAPSHOTS_COUNT = "kura.snapshots.count";
    public static final String KEY_KURA_HAVE_NET_ADMIN = "kura.have.net.admin";
    public static final String KEY_KURA_HAVE_WEB_INTER = "kura.have.web.inter";
    public static final String KEY_KURA_STYLE_DIR = "kura.style.dir";
    public static final String KEY_KURA_WIFI_TOP_CHANNEL = "kura.wifi.top.channel";
    public static final String KEY_KURA_KEY_STORE_PWD = "kura.ssl.keyStorePassword";
    public static final String KEY_KURA_TRUST_STORE_PWD = "kura.ssl.trustStorePassword";
    public static final String KEY_FILE_COMMAND_ZIP_MAX_SIZE = "file.command.zip.max.size";
    public static final String KEY_FILE_COMMAND_ZIP_MAX_NUMBER = "file.command.zip.max.number";
    public static final String KEY_OS_ARCH = "os.arch";
    public static final String KEY_OS_NAME = "os.name";
    public static final String KEY_OS_VER = "os.version";
    public static final String KEY_OS_DISTRO = "os.distribution";
    public static final String KEY_OS_DISTRO_VER = "os.distribution.version";
    public static final String KEY_JAVA_VERSION = "java.runtime.version";
    public static final String KEY_JAVA_VENDOR = "java.runtime.name";
    public static final String KEY_JAVA_VM_NAME = "java.vm.name";
    public static final String KEY_JAVA_VM_VERSION = "java.vm.version";
    public static final String KEY_JAVA_VM_INFO = "java.vm.info";
    public static final String KEY_OSGI_FW_NAME = "org.osgi.framework.vendor";
    public static final String KEY_OSGI_FW_VERSION = "org.osgi.framework.version";
    public static final String KEY_JAVA_HOME = "java.home";
    public static final String KEY_FILE_SEP = "file.separator";
    public static final String CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE = "console.device.management.service.ignore";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_URL_PROPNAME = "db.service.hsqldb.url";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_WRITE_DELAY_MILLIES_PROPNAME = "db.service.hsqldb.write_delay_millis";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_LOG_DATA_PROPNAME = "db.service.hsqldb.log_data";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_CACHE_ROWS_PROPNAME = "db.service.hsqldb.cache_rows";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_LOB_FILE_PROPNAME = "db.service.hsqldb.lob_file_scale";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_DEFRAG_LIMIT_PROPNAME = "db.service.hsqldb.defrag_limit";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_LOG_SIZE_PROPNAME = "db.service.hsqldb.log_size";

    /**
     * @deprecated
     */
    @Deprecated
    public static final String DB_NIO_PROPNAME = "db.service.hsqldb.nio_data_file";

    /**
     * Gets the primary MAC address of the system
     *
     * @return
     */
    public String getPrimaryMacAddress();

    /**
     * Gets the name of the 'primary' network interface.
     *
     * @return
     */
    public String getPrimaryNetworkInterfaceName();

    /**
     * Gets the platform name Kura is running on. This could be catalyst, duracor1200, helios, isis, proteus, etc.
     *
     * @return The platform name.
     */
    public String getPlatform();

    /**
     * Gets the model identification of the device.
     *
     * @return Model ID.
     */
    public String getModelId();

    /**
     * Gets the model name of the device.
     *
     * @return Model name.
     */
    public String getModelName();

    /**
     * Gets the part number.
     *
     * @return Part number.
     */
    public String getPartNumber();

    /**
     * Gets the serial number of the device.
     *
     * @return Serial number.
     */
    public String getSerialNumber();

    /**
     * Returns a friendly name assigned to this device.
     *
     * @return
     */
    public String getDeviceName();

    /**
     * Gets the BIOS version of the device.
     *
     * @return BIOS version.
     */
    public String getBiosVersion();

    /**
     * Gets the firmware version.
     *
     * @return Firmware version.
     */
    public String getFirmwareVersion();

    /**
     * Gets the Operating System architecture for the system.
     *
     * @return The Operating System architecture as defined by the Java System property os.arch.
     */
    public String getOsArch();

    /**
     * Gets the Operating System name for the system.
     *
     * @return The Operating System name as defined by the Java System property os.name.
     */
    public String getOsName();

    /**
     * Gets the Operating System version for the system.
     *
     * @return The Operating System version as defined by the Java System property os.version.
     */
    public String getOsVersion();

    /**
     * Gets the Operating System Distribution name if appropriate.
     *
     * @return A String representing the Operating System Distribution name if appropriate.
     */
    public String getOsDistro();

    /**
     * Gets the Operating System Distribution version if appropriate.
     *
     * @return A String representing the Operating System Distribution version if appropriate.
     */
    public String getOsDistroVersion();

    /**
     * Gets the vendor of the Java VM that is currently being used.
     *
     * @return The Java Runtime version as defined by the Java System property java.vendor.
     */
    public String getJavaVendor();

    /**
     * Gets the Java version that is currently being used.
     *
     * @return The Java version as defined by the Java System property java.version.
     */
    public String getJavaVersion();

    /**
     * Gets the Java Virtual Machine name that is currently being used.
     *
     * @return The Java Virtual Machine name as defined by the Java System property java.vm.name.
     */
    public String getJavaVmName();

    /**
     * Gets the Java Virtual Machine version that is currently being used.
     *
     * @return The Java Virtual Machine version as defined by the Java System property java.vm.version.
     */
    public String getJavaVmVersion();

    /**
     * Gets the Java Virtual Machine information that is currently being used.
     *
     * @return The Java Virtual Machine version as defined by the Java System property java.vm.version.
     */
    public String getJavaVmInfo();

    /**
     * Gets the name of the OSGI Framework that is currently being used.
     *
     * @return The OSGI Framework Name as defined by the System property osgi.framework.name.
     */
    public String getOsgiFwName();

    /**
     * Gets the version of the OSGI Framework that is currently being used.
     *
     * @return The OSGI Framework Version as defined by the System property osgi.framework.version.
     */
    public String getOsgiFwVersion();

    /**
     * Gets the system file separator used by the filesystem.
     *
     * @return The system file separator used by the system.
     */
    public String getFileSeparator();

    /**
     * Gets the location where the JVM is stored in the filesystem.
     *
     * @return The location of the root JVM directory.
     */
    public String getJavaHome();

    /**
     * Gets the product version for this unit.
     *
     * The product version is defined in the kura.version property of the kura.properties file
     * located in the ${BASE_DIR}/${KURA_SYMLINK}/kura directory.
     *
     * @return The Kura version string as denoted in kura.version property of the kura.properties file.
     */
    public String getKuraVersion();

    /**
     * Gets the Eclipse Marketplace compatibility product version for this unit.
     *
     * The marketplace compatibility product version is defined in the {@code kura.marketplace.compatibility.version}
     * property of the kura.properties file located in the ${BASE_DIR}/${KURA_SYMLINK}/kura directory.
     * If the variable {@code kura.marketplace.compatibility.version} cannot be located, it defaults to the value
     * specified by {@link #getKuraVersion()}.
     *
     * @since 1.3
     * @return The marketplace compatibility Kura version string.
     */
    public String getKuraMarketplaceCompatibilityVersion();

    /**
     * Gets the location where the Kura root directory is stored in the filesystem.
     *
     * @return The root Kura directory.
     */
    public String getKuraHome();

    /**
     * Gets the location where all volatile Kura specific configuration and status information should be stored.
     *
     * The convention for each bundle that has filesystem dependencies is for those filesystem components
     * to be stored in the configuration directory root followed by a file separator followed by the project
     * name that needs some configuration. This will keep separate configuration components tied to their
     * appropriate projects.
     *
     * @return The location of the volatile Kura configuration and status directory root.
     */
    public String getKuraTemporaryConfigDirectory();

    /**
     * Gets the location where all Configuration Snapshots will be stored.
     * It is recommended for this directory not to be volatile so that the configuration
     * information can survive reboots and service configuration can be restored.
     *
     * @return The location of the volatile Kura configuration and status directory root.
     */
    public String getKuraSnapshotsDirectory();

    /**
     * Returns the maximum number of snapshots to be retained in the file system.
     * When the maximum number is reached, a garbage collector will delete the older snapshots.
     *
     * @return maximum number of snapshots to be retained.
     */
    public int getKuraSnapshotsCount();

    /**
     * Gets the location where all custom style information is stored.
     *
     * @return The location of the custom style directory.
     */
    public String getKuraStyleDirectory();

    /**
     * Gets the last wifi channel allowed for this device. In the U.S. this should be
     * 11. In most of Europe this should be 13.
     *
     * @return The last wifi channel allowed for this device (usually 11 or 13)
     */
    public int getKuraWifiTopChannel();

    public String getKuraWebEnabled();

    /**
     * Gets the location where all Kura persistent data should be stored.
     *
     * @return The location of the persistent Kura data directory root.
     */
    public String getKuraDataDirectory();

    /**
     * Returns the size in MegaBytes of the maximum file upload size permitted by the local file servlet
     *
     * @return The maximum size (in mega bytes) of files that can be uploaded using the command file upload function
     */
    public int getFileCommandZipMaxUploadSize();

    /**
     * Returns the maximum number of files that can be uploaded by the local file servlet
     *
     * @return The maximum number of files that can be uploaded using the command file upload function
     */
    public int getFileCommandZipMaxUploadNumber();

    /**
     * Returns all KuraProperties for this system. The returned instances is
     * initialized by loading the kura.properties file. Properties defined at
     * the System level - for example using the java -D command line flag -
     * are used to overwrite the values loaded from the kura.properties file
     * in a hierarchical configuration fashion.
     */
    public Properties getProperties();

    /**
     * Returns the OSGi bundles currently installed
     *
     * @return
     */
    public Bundle[] getBundles();

    /**
     * Returns the number of processors visible to this Java platform.
     *
     * @return
     */
    public int getNumberOfProcessors();

    /**
     * Returns the total memory visible to this Java instance in kilobytes.
     *
     * @return
     */
    public long getTotalMemory();

    /**
     * Returns the free memory for Java instance in kilobytes.
     *
     * @return
     */
    public long getFreeMemory();

    /**
     * Returns the password to access the private key from the keystore file.
     *
     * @return
     */
    public char[] getJavaKeyStorePassword();

    /**
     * Returns the password to unlock the trust store keystore file.
     *
     * @return
     */
    public char[] getJavaTrustStorePassword();

    /**
     * Returns a list of services that should be ignored by the Everyware Cloud Console
     *
     * @return
     */
    public List<String> getDeviceManagementServiceIgnore();

    /**
     * Returns the device hostname
     * 
     * @return a String that represents the device hostname
     * @since 2.0
     */
    public String getHostname();

}
