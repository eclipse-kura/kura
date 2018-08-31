/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.kura.system.SystemService;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class SystemServiceTest {

    @Test
    public void testActivateRelativeConfigFilePathsUpdate() {
        // verify the part of the code that replaces config file property values

        SystemServiceImpl systemService = new SystemServiceImpl();

        ComponentContext ctxMock = mock(ComponentContext.class);

        System.setProperty(SystemService.KURA_CONFIG, "file:kura/kura.properties");
        System.setProperty("dpa.configuration", "kura/dpa.properties");
        System.setProperty("log4j.configuration", "file:kura/log4j.properties");

        System.setProperty(SystemService.KURA_CUSTOM_CONFIG, "file:/opt/eclipse/kura/framework/kura.properties");

        systemService.activate(ctxMock);

        assertEquals("file:/opt/eclipse/kura/framework/kura.properties", System.getProperty(SystemService.KURA_CONFIG));
        assertEquals("/opt/eclipse/kura/data/dpa.properties", System.getProperty("dpa.configuration"));
        assertEquals("file:/opt/eclipse/kura/user/log4j.properties", System.getProperty("log4j.configuration"));
    }

    @Test
    public void testActivateWithExplicitPropertyFiles() throws IOException {
        // check that values are read from 'default' and custom files

        SystemServiceImpl systemService = new SystemServiceImpl() {

            // make sure not to read e.g. kura.properties from the classpath
            @Override
            protected String readResource(String resource) throws IOException {
                return null;
            }
        };

        ComponentContext ctxMock = mock(ComponentContext.class);

        String props = "/tmp/ssact_kura.properties";
        String customProps = "/tmp/ssact_kura_custom.properties";

        System.setProperty(SystemService.KURA_CONFIG, "file:" + props);
        System.setProperty(SystemService.KURA_CUSTOM_CONFIG, "file:" + customProps);
        System.clearProperty(SystemService.KEY_KURA_HOME_DIR);

        String key_proper = "property.proper";
        String val_proper = "proper property value";
        File f1 = writeFile(props,
                String.format("%s = %s\n%s = test ver", key_proper, val_proper, SystemService.KEY_KURA_VERSION));
        String key_custom = "property.custom";
        String val_custom = "custom property value";
        String val_test = "test ver override";
        File f2 = writeFile(customProps,
                String.format("%s = %s\n%s = %s", key_custom, val_custom, SystemService.KEY_KURA_VERSION, val_test));

        systemService.activate(ctxMock);

        f1.delete();
        f2.delete();

        // check that the defults are properly set
        Properties properties = systemService.getProperties();
        assertFalse(properties.containsKey(key_proper));
        assertFalse(properties.containsKey(key_custom));
        assertEquals(val_proper, properties.getProperty(key_proper));
        assertEquals(val_custom, properties.getProperty(key_custom));
        assertEquals(val_test, properties.getProperty(SystemService.KEY_KURA_VERSION));
    }

    @Test
    public void testActivateWithHomePropertyValues() throws IOException {
        // verify that fallback to kura's home directory works

        SystemServiceImpl systemService = new SystemServiceImpl() {

            // make sure not to read e.g. kura.properties from the classpath
            @Override
            protected String readResource(String resource) throws IOException {
                return null;
            }
        };

        ComponentContext ctxMock = mock(ComponentContext.class);

        String props = "/tmp/framework/kura.properties";
        String customProps = "/tmp/user/kura_custom.properties";

        System.clearProperty(SystemService.KURA_CONFIG);
        System.clearProperty(SystemService.KURA_CUSTOM_CONFIG);
        System.setProperty(SystemService.KEY_KURA_HOME_DIR, "/tmp");
        System.setProperty(SystemService.KEY_KURA_FRAMEWORK_CONFIG_DIR, "/tmp/framework");
        System.setProperty(SystemService.KEY_KURA_USER_CONFIG_DIR, "/tmp/user");

        String key_proper = "property.proper";
        String val_proper = "proper property value";
        File f1 = writeFile(props,
                String.format("%s = %s\n%s = test ver", key_proper, val_proper, SystemService.KEY_KURA_VERSION));
        String key_custom = "property.custom";
        String val_custom = "custom property value";
        String val_test = "test ver override";
        File f2 = writeFile(customProps,
                String.format("%s = %s\n%s = %s", key_custom, val_custom, SystemService.KEY_KURA_VERSION, val_test));

        systemService.activate(ctxMock);

        f1.delete();
        f2.delete();

        // check that the defults are properly set
        Properties properties = systemService.getProperties();
        assertFalse(properties.containsKey(key_proper));
        assertFalse(properties.containsKey(key_custom));
        assertEquals(val_proper, properties.getProperty(key_proper));
        assertEquals(val_custom, properties.getProperty(key_custom));
        assertEquals(val_test, properties.getProperty(SystemService.KEY_KURA_VERSION));
    }

    @Test
    public void testActivateWithUpdatedDefaults() throws IOException {
        // verify that update of certain default values works

        File dir = new File("/opt");
        if (!dir.canWrite()) {
            // cannot test in this case
            return;
        }

        SystemServiceImpl systemService = new SystemServiceImpl() {

            // make sure not to read e.g. kura.properties from the classpath
            @Override
            protected String readResource(String resource) throws IOException {
                return null;
            }
        };

        ComponentContext ctxMock = mock(ComponentContext.class);

        String props = "/tmp/kura.properties";

        System.setProperty(SystemService.KURA_CONFIG, "file:" + props);
        System.clearProperty(SystemService.KURA_CUSTOM_CONFIG);
        System.clearProperty(SystemService.KEY_KURA_HOME_DIR);

        File f1 = writeFile(props,
                String.format("%s = kura\n%s = kura/plugins\n%s = kura/packages", SystemService.KEY_KURA_HOME_DIR,
                        SystemService.KEY_KURA_PLUGINS_DIR, SystemService.KEY_KURA_PACKAGES_DIR));

        systemService.activate(ctxMock);

        f1.delete();

        // check that the defaults are properly set
        Properties properties = systemService.getProperties();
        assertEquals("/opt/eclipse/kura", properties.getProperty(SystemService.KEY_KURA_HOME_DIR));
        assertEquals("/opt/eclipse/kura/plugins", properties.getProperty(SystemService.KEY_KURA_PLUGINS_DIR));
        assertEquals("/opt/eclipse/kura/data/packages", properties.getProperty(SystemService.KEY_KURA_PACKAGES_DIR));
    }

    @Test
    public void testActivateAllProperties() throws IOException {
        // verify that certain system properties are actually used as kura's property overrides

        System.setProperty(SystemService.KEY_KURA_HOME_DIR, "/tmp");
        System.setProperty(SystemService.KEY_KURA_FRAMEWORK_CONFIG_DIR, "/tmp/framework");
        System.setProperty(SystemService.KEY_KURA_USER_CONFIG_DIR, "/tmp/user");
        System.setProperty(SystemService.KEY_KURA_NAME, "KEY_KURA_NAME");
        System.setProperty(SystemService.KEY_DEVICE_NAME, "KEY_DEVICE_NAME");
        System.setProperty(SystemService.KEY_PLATFORM, "KEY_PLATFORM");
        System.setProperty(SystemService.KEY_MODEL_ID, "KEY_MODEL_ID");
        System.setProperty(SystemService.KEY_MODEL_NAME, "KEY_MODEL_NAME");
        System.setProperty(SystemService.KEY_PART_NUMBER, "KEY_PART_NUMBER");
        System.setProperty(SystemService.KEY_SERIAL_NUM, "KEY_SERIAL_NUM");
        System.setProperty(SystemService.KEY_BIOS_VERSION, "KEY_BIOS_VERSION");
        System.setProperty(SystemService.KEY_FIRMWARE_VERSION, "KEY_FIRMWARE_VERSION");
        System.setProperty(SystemService.KEY_PRIMARY_NET_IFACE, "KEY_PRIMARY_NET_IFACE");
        System.setProperty(SystemService.KEY_KURA_DATA_DIR, "KEY_KURA_DATA_DIR");
        System.setProperty(SystemService.KEY_KURA_SNAPSHOTS_COUNT, "KEY_KURA_SNAPSHOTS_COUNT");
        System.setProperty(SystemService.KEY_KURA_HAVE_NET_ADMIN, "KEY_KURA_HAVE_NET_ADMIN");
        System.setProperty(SystemService.KEY_KURA_HAVE_WEB_INTER, "KEY_KURA_HAVE_WEB_INTER");
        System.setProperty(SystemService.KEY_KURA_STYLE_DIR, "KEY_KURA_STYLE_DIR");
        System.setProperty(SystemService.KEY_KURA_WIFI_TOP_CHANNEL, "KEY_KURA_WIFI_TOP_CHANNEL");
        System.setProperty(SystemService.KEY_KURA_KEY_STORE_PWD, "KEY_KURA_KEY_STORE_PWD");
        System.setProperty(SystemService.KEY_KURA_TRUST_STORE_PWD, "KEY_KURA_TRUST_STORE_PWD");
        System.setProperty(SystemService.KEY_FILE_COMMAND_ZIP_MAX_SIZE, "KEY_FILE_COMMAND_ZIP_MAX_SIZE");
        System.setProperty(SystemService.KEY_FILE_COMMAND_ZIP_MAX_NUMBER, "KEY_FILE_COMMAND_ZIP_MAX_NUMBER");
        System.setProperty(SystemService.KEY_OS_DISTRO, "KEY_OS_DISTRO");
        System.setProperty(SystemService.KEY_OS_DISTRO_VER, "KEY_OS_DISTRO_VER");
        System.setProperty(SystemService.CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE,
                "CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE");
        System.setProperty(SystemService.DB_URL_PROPNAME, "DB_URL_PROPNAME");
        System.setProperty(SystemService.DB_CACHE_ROWS_PROPNAME, "DB_CACHE_ROWS_PROPNAME");
        System.setProperty(SystemService.DB_LOB_FILE_PROPNAME, "DB_LOB_FILE_PROPNAME");
        System.setProperty(SystemService.DB_DEFRAG_LIMIT_PROPNAME, "DB_DEFRAG_LIMIT_PROPNAME");
        System.setProperty(SystemService.DB_LOG_DATA_PROPNAME, "DB_LOG_DATA_PROPNAME");
        System.setProperty(SystemService.DB_LOG_SIZE_PROPNAME, "DB_LOG_SIZE_PROPNAME");
        System.setProperty(SystemService.DB_NIO_PROPNAME, "DB_NIO_PROPNAME");
        System.setProperty(SystemService.DB_WRITE_DELAY_MILLIES_PROPNAME, "DB_WRITE_DELAY_MILLIES_PROPNAME");

        SystemServiceImpl systemService = new SystemServiceImpl() {

            // make sure not to read e.g. kura.properties from the classpath
            @Override
            protected String readResource(String resource) throws IOException {
                return null;
            }
        };

        ComponentContext ctxMock = mock(ComponentContext.class);

        systemService.activate(ctxMock);

        Properties props = systemService.getProperties();
        assertEquals("KEY_KURA_NAME", props.getProperty(SystemService.KEY_KURA_NAME));
        assertEquals("KEY_DEVICE_NAME", props.getProperty(SystemService.KEY_DEVICE_NAME));
        assertEquals("KEY_PLATFORM", props.getProperty(SystemService.KEY_PLATFORM));
        assertEquals("KEY_MODEL_ID", props.getProperty(SystemService.KEY_MODEL_ID));
        assertEquals("KEY_MODEL_NAME", props.getProperty(SystemService.KEY_MODEL_NAME));
        assertEquals("KEY_PART_NUMBER", props.getProperty(SystemService.KEY_PART_NUMBER));
        assertEquals("KEY_SERIAL_NUM", props.getProperty(SystemService.KEY_SERIAL_NUM));
        assertEquals("KEY_BIOS_VERSION", props.getProperty(SystemService.KEY_BIOS_VERSION));
        assertEquals("KEY_FIRMWARE_VERSION", props.getProperty(SystemService.KEY_FIRMWARE_VERSION));
        assertEquals("KEY_PRIMARY_NET_IFACE", props.getProperty(SystemService.KEY_PRIMARY_NET_IFACE));
        assertEquals("KEY_KURA_DATA_DIR", props.getProperty(SystemService.KEY_KURA_DATA_DIR));
        assertEquals("KEY_KURA_SNAPSHOTS_COUNT", props.getProperty(SystemService.KEY_KURA_SNAPSHOTS_COUNT));
        assertEquals("KEY_KURA_HAVE_NET_ADMIN", props.getProperty(SystemService.KEY_KURA_HAVE_NET_ADMIN));
        assertEquals("KEY_KURA_HAVE_WEB_INTER", props.getProperty(SystemService.KEY_KURA_HAVE_WEB_INTER));
        assertEquals("KEY_KURA_STYLE_DIR", props.getProperty(SystemService.KEY_KURA_STYLE_DIR));
        assertEquals("KEY_KURA_WIFI_TOP_CHANNEL", props.getProperty(SystemService.KEY_KURA_WIFI_TOP_CHANNEL));
        assertEquals("KEY_KURA_KEY_STORE_PWD", props.getProperty(SystemService.KEY_KURA_KEY_STORE_PWD));
        assertEquals("KEY_KURA_TRUST_STORE_PWD", props.getProperty(SystemService.KEY_KURA_TRUST_STORE_PWD));
        assertEquals("KEY_FILE_COMMAND_ZIP_MAX_SIZE", props.getProperty(SystemService.KEY_FILE_COMMAND_ZIP_MAX_SIZE));
        assertEquals("KEY_FILE_COMMAND_ZIP_MAX_NUMBER",
                props.getProperty(SystemService.KEY_FILE_COMMAND_ZIP_MAX_NUMBER));
        assertEquals("KEY_OS_DISTRO", props.getProperty(SystemService.KEY_OS_DISTRO));
        assertEquals("KEY_OS_DISTRO_VER", props.getProperty(SystemService.KEY_OS_DISTRO_VER));
        assertEquals("CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE",
                props.getProperty(SystemService.CONFIG_CONSOLE_DEVICE_MANAGE_SERVICE_IGNORE));
        assertEquals("DB_URL_PROPNAME", props.getProperty(SystemService.DB_URL_PROPNAME));
        assertEquals("DB_CACHE_ROWS_PROPNAME", props.getProperty(SystemService.DB_CACHE_ROWS_PROPNAME));
        assertEquals("DB_LOB_FILE_PROPNAME", props.getProperty(SystemService.DB_LOB_FILE_PROPNAME));
        assertEquals("DB_DEFRAG_LIMIT_PROPNAME", props.getProperty(SystemService.DB_DEFRAG_LIMIT_PROPNAME));
        assertEquals("DB_LOG_DATA_PROPNAME", props.getProperty(SystemService.DB_LOG_DATA_PROPNAME));
        assertEquals("DB_LOG_SIZE_PROPNAME", props.getProperty(SystemService.DB_LOG_SIZE_PROPNAME));
        assertEquals("DB_NIO_PROPNAME", props.getProperty(SystemService.DB_NIO_PROPNAME));
        assertEquals("DB_WRITE_DELAY_MILLIES_PROPNAME",
                props.getProperty(SystemService.DB_WRITE_DELAY_MILLIES_PROPNAME));
    }

    private File writeFile(String path, String content) throws IOException {
        File file = new File(path);
        file.createNewFile();
        file.deleteOnExit();

        FileWriter fw = new FileWriter(file);
        fw.append(content);
        fw.close();

        return file;
    }

}
