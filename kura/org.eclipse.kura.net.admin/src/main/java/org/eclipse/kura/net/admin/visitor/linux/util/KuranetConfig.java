/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.net.admin.visitor.linux.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KuranetConfig {
    
    private static final Logger s_logger = LoggerFactory.getLogger(KuranetConfig.class);
            
    private static final String KURANET_FILENAME = "/opt/eclipse/kura/data/kuranet.conf";
    private static final String KURANET_TMP_FILENAME = "/opt/eclipse/kura/data/kuranet.conf.tmp";
    
    public static Properties getProperties() {
        Properties kuraExtendedProps = null;   
        
        s_logger.debug("Getting " + KURANET_FILENAME);

        File kuranetFile = new File(KURANET_FILENAME);

        if(kuranetFile.exists()) {
            //found our match so load the properties
            kuraExtendedProps = new Properties();
            try {
                kuraExtendedProps.load(new FileInputStream(kuranetFile));
            } catch (Exception e) {
                s_logger.error("Could not load " + KURANET_FILENAME);
            }
        } else {
            s_logger.debug("File does not exist: " + KURANET_FILENAME);
        }

        return kuraExtendedProps;
    }
    
    public static String getProperty(String key) {
        String value = null;
        
        Properties props = KuranetConfig.getProperties();
        if(props != null) {
            value = props.getProperty(key);
            s_logger.debug("Got property " + key + " :: " + value);
        }

        return value;
    }
    
    public static void storeProperties(Properties props) throws IOException, KuraException {
        Properties oldProperties = KuranetConfig.getProperties();
        
        if(oldProperties == null || !oldProperties.equals(props)) {
        	FileOutputStream fos = new FileOutputStream(KURANET_TMP_FILENAME);
            props.store(fos, null);
            fos.flush();
            fos.getFD().sync();
            fos.close();
            
          //move the file if we made it this far
            File tmpFile = new File(KURANET_TMP_FILENAME);
            File file = new File(KURANET_FILENAME);
            if(!FileUtils.contentEquals(tmpFile, file)) {
	            if(tmpFile.renameTo(file)){
	            	s_logger.trace("Successfully wrote kuranet props file");
	            }else{
	            	s_logger.error("Failed to write kuranet props file");
	            	throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for kuranet props");
	            }
            } else {
				s_logger.info("Not rewriting kuranet props file because it is the same");
			}
        }
    }
    
    public static void setProperty(String key, String value) throws IOException, KuraException {
        s_logger.debug("Setting property " + key + " :: " + value);
        Properties properties = KuranetConfig.getProperties();
        
        if(properties == null) {
            properties = new Properties();
        }
        
        properties.setProperty(key, value);
        KuranetConfig.storeProperties(properties);
    }
    
    public static void deleteProperty(String key) throws IOException, KuraException {
    	Properties properties = KuranetConfig.getProperties();
        
        if(properties != null) {
        	if(properties.containsKey(key)) {
            	s_logger.debug("Deleting property " + key);
        		properties.remove(key);
        		KuranetConfig.storeProperties(properties);
        	} else {
        		s_logger.debug("Property does not exist " + key);
        	}
        }
    }
}
