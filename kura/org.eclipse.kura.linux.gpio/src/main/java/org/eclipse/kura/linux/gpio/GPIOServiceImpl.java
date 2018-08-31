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
 *     Red Hat Inc - Clean up kura properties handling
 *******************************************************************************/
package org.eclipse.kura.linux.gpio;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPIOServiceImpl implements GPIOService {

    private static final Logger logger = LoggerFactory.getLogger(GPIOServiceImpl.class);

    private static final HashSet<JdkDioPin> pins = new HashSet<JdkDioPin>();

    private SystemService systemService;

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    /**
     * Test if a file is available for loading
     *
     * @param path
     *            the path to test
     * @return the path from input which can be used for loading, {@code null}
     *         if the file is not present or should not be used for loading
     */
    private static String whenAvailable(String path) {
        if (path == null) {
            return null;
        }

        if (!path.startsWith("file:")) {
            path = "file:" + path;
        }

        try {
            final File file = new File(new URL(path).toURI());
            if (!file.isFile()) {
                return null;
            }
            if (!file.canRead()) {
                return null;
            }

            return path;
        } catch (Exception e) {
            return null;
        }
    }

    protected void activate(ComponentContext componentContext) {
        logger.debug("activating jdk.dio GPIOService");

        FileReader fr = null;
        try {
            String configFile = System.getProperty("jdk.dio.registry");
            if (configFile != null && !configFile.startsWith("file:")) {
                configFile = "file:" + configFile;
            }
            logger.debug("System property location: {}", configFile);

            if (configFile == null) {
                // Testing for Kura home relative path
                configFile = whenAvailable(
                        this.systemService.getKuraFrameworkConfigDirectory() + File.separator + "jdk.dio.properties");
                logger.debug("Kura Home relative location: {}", configFile);
            }

            if (configFile == null) {
                // Emulator?
                final String kuraConfig = this.systemService.getProperties().getProperty(SystemService.KURA_CONFIG);
                if (kuraConfig != null) {
                    configFile = kuraConfig.replace("kura.properties", "jdk.dio.properties");
                }
            }

            logger.debug("Final location: {}", configFile);

            if (configFile == null) {
                throw new IllegalStateException("Unable to locate 'jdk.dio.properties'");
            }

            File dioPropsFile = new File(new URL(configFile).toURI());
            if (dioPropsFile.exists()) {
                final Properties dioDefaults = new Properties();
                fr = new FileReader(dioPropsFile);
                dioDefaults.load(fr);

                pins.clear();

                for (final Map.Entry<Object, Object> entry : dioDefaults.entrySet()) {
                    final Object k = entry.getKey();
                    final String line = (String) entry.getValue();

                    final JdkDioPin p = JdkDioPin.parseFromProperty(k, line);
                    if (p != null) {
                        pins.add(p);
                    }
                }
                logger.info("Loaded File jdk.dio.properties: {}", dioPropsFile);
            } else {
                logger.warn("File does not exist: {}", dioPropsFile);
            }
        } catch (IOException e) {
            logger.error("Exception while accessing resource!", e);
        } catch (URISyntaxException e) {
            logger.error("Exception while accessing resource!", e);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    logger.error("Exception while releasing resource!", e);
                }
            }
        }

        logger.debug("GPIOService activated.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("deactivating jdk.dio GPIOService");
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName) {
        for (JdkDioPin p : pins) {
            if (p.getName().equals(pinName)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        for (JdkDioPin p : pins) {
            if (p.getName().equals(pinName)) {
                if (p.getDirection() != direction || p.getMode() != mode || p.getTrigger() != trigger) {
                    if (p.isOpen()) {
                        try {
                            p.close();
                        } catch (IOException e) {
                            logger.warn("Cannot close GPIO Pin {}", pinName);
                            return p;
                        }
                    }
                    int index = p.getIndex();
                    pins.remove(p);
                    JdkDioPin newPin = new JdkDioPin(index, pinName, direction, mode, trigger);
                    pins.add(newPin);
                    return newPin;
                }
                return p;
            }
        }
        return null;
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal) {
        for (JdkDioPin p : pins) {
            if (p.getIndex() == terminal) {
                return p;
            }
        }
        JdkDioPin newPin = new JdkDioPin(terminal);
        pins.add(newPin);
        return newPin;
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        for (JdkDioPin p : pins) {
            if (p.getIndex() == terminal) {
                if (p.getDirection() != direction || p.getMode() != mode || p.getTrigger() != trigger) {
                    if (p.isOpen()) {
                        try {
                            p.close();
                        } catch (IOException e) {
                            logger.warn("Cannot close GPIO Pin {}", terminal);
                            return p;
                        }
                    }
                    String pinName = p.getName();
                    pins.remove(p);
                    JdkDioPin newPin = new JdkDioPin(terminal, pinName, direction, mode, trigger);
                    pins.add(newPin);
                    return newPin;
                }
                return p;
            }
        }
        JdkDioPin newPin = new JdkDioPin(terminal, null, direction, mode, trigger);
        pins.add(newPin);
        return newPin;
    }

    @Override
    public Map<Integer, String> getAvailablePins() {
        HashMap<Integer, String> result = new HashMap<Integer, String>();
        for (JdkDioPin p : pins) {
            result.put(p.getIndex(), p.getName());
        }

        return result;
    }

}
