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
package org.eclipse.kura.web.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceSnapshotsServlet extends HttpServlet 
{
    private static final long serialVersionUID = -2533869595709953567L;

    private static Logger s_logger = LoggerFactory.getLogger(DeviceSnapshotsServlet.class);
    
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        String snapshotId = request.getParameter("snapshotId");

        response.setCharacterEncoding("UTF-8");
		response.setContentType("application/xml");
		response.setHeader("Content-Disposition", "attachment; filename=snapshot_"+snapshotId+".xml");
		response.setHeader("Cache-Control", "no-transform, max-age=0");
		PrintWriter writer = response.getWriter();
		try {

            ServiceLocator  locator = ServiceLocator.getInstance();
			ConfigurationService cs = locator.getService(ConfigurationService.class);			 
			if (snapshotId != null) {

				long sid = Long.parseLong(snapshotId);
				List<ComponentConfiguration> xmlConfigs = cs.getSnapshot(sid);
				
				//List<ComponentConfigurationImpl> decryptedConfigs = new ArrayList<ComponentConfigurationImpl>();
				for (ComponentConfiguration config : xmlConfigs) {
					if (config != null) {
						ComponentConfigurationImpl configImpl= (ComponentConfigurationImpl) config;
						try {
							Map<String, Object> decryptedProperties = decryptPasswords(configImpl);
							configImpl.setProperties(decryptedProperties);
							//decryptedConfigs.add(configImpl);
						} catch (Throwable t) {
							s_logger.warn("Error during snapshot password decryption");
						}
					}
				}
				//xmlConfigs.setConfigurations(decryptedConfigs);

								
				// build a list of configuration which can be marshalled in XML
				List<ComponentConfigurationImpl> configImpls = new ArrayList<ComponentConfigurationImpl>();
				for (ComponentConfiguration config : xmlConfigs) {
					configImpls.add((ComponentConfigurationImpl) config);
				}
				XmlComponentConfigurations xmlCompConfigs = new XmlComponentConfigurations();
				xmlCompConfigs.setConfigurations(configImpls);
				
				//
				// marshall the response and write it
				XmlUtil.marshal(xmlCompConfigs, writer);
			}
        } 
        catch (Exception e) {
            s_logger.error("Error creating Excel export", e);
            throw new ServletException(e);
        } 
        finally {
            if (writer != null)
            	writer.close();
        }
    }
    
    private Map<String, Object> decryptPasswords(ComponentConfiguration config) throws GwtKuraException {
		Map<String, Object> configProperties = config.getConfigurationProperties();
		CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);	

		Iterator<String> keys = configProperties.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = configProperties.get(key);
			if (value instanceof Password) {
				try {
					Password decryptedPassword = new Password(cryptoService.decryptAes(value.toString().toCharArray()));
					configProperties.put(key, decryptedPassword);
				} catch (Exception e) {
				}
			}
		}
		return configProperties;
	}
}

