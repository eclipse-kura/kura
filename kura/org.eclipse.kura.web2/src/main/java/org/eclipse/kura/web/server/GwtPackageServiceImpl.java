/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.web.client.util.GwtSafeHtmlUtils;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtBundleInfo;
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtPackageService;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class GwtPackageServiceImpl extends OsgiRemoteServiceServlet implements GwtPackageService {
	private static final long serialVersionUID = -3422518194598042896L;

	@Override
	public List<GwtDeploymentPackage> findDeviceDeploymentPackages(GwtXSRFToken xsrfToken) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		DeploymentAdmin deploymentAdmin = ServiceLocator.getInstance().getService(DeploymentAdmin.class);
		
		List<GwtDeploymentPackage> gwtDeploymentPackages = new ArrayList<GwtDeploymentPackage>();
		DeploymentPackage[] deploymentPackages = deploymentAdmin.listDeploymentPackages();
		
		if (deploymentPackages != null) {
			for (DeploymentPackage deploymentPackage : deploymentPackages) {
				GwtDeploymentPackage gwtDeploymentPackage = new GwtDeploymentPackage();
				gwtDeploymentPackage.setName(GwtSafeHtmlUtils.htmlEscape(deploymentPackage.getName()));
				gwtDeploymentPackage.setVersion(GwtSafeHtmlUtils.htmlEscape(deploymentPackage.getVersion().toString()));
				
				List<GwtBundleInfo> gwtBundleInfos = new ArrayList<GwtBundleInfo>();
				BundleInfo[] bundleInfos = deploymentPackage.getBundleInfos();
				if (bundleInfos != null) {
					for (BundleInfo bundleInfo : bundleInfos) {
						GwtBundleInfo gwtBundleInfo = new GwtBundleInfo();
						gwtBundleInfo.setName(GwtSafeHtmlUtils.htmlEscape(bundleInfo.getSymbolicName()));
						gwtBundleInfo.setVersion(GwtSafeHtmlUtils.htmlEscape(bundleInfo.getVersion().toString()));
						
						gwtBundleInfos.add(gwtBundleInfo);
					}
				}
				
				gwtDeploymentPackage.setBundleInfos(gwtBundleInfos);
				
				gwtDeploymentPackages.add(gwtDeploymentPackage);
			}
		}
		
		return gwtDeploymentPackages;
	}
	
	@Override
	public void uninstallDeploymentPackage(GwtXSRFToken xsrfToken, String packageName) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		DeploymentAgentService deploymentAgentService = ServiceLocator.getInstance().getService(DeploymentAgentService.class);		
		try {
			deploymentAgentService.uninstallDeploymentPackageAsync(GwtSafeHtmlUtils.htmlEscape(packageName));
		} 
		catch (Exception e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	@Override
	public String getMarketplaceUri(GwtXSRFToken xsrfToken, String url) throws GwtKuraException {
		String uri = null;
		URL mpUrl = null;
		HttpURLConnection connection = null;
	    
	    try {
	    	mpUrl = new URL(url);
	    	connection = (HttpURLConnection) mpUrl.openConnection();
	    	
	    	connection.setRequestMethod("GET");
	    	connection.connect();
	        
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        Document doc = db.parse(connection.getInputStream());

	        uri = doc.getElementsByTagName("updateurl").item(0).getTextContent();
	    	
	    } catch (MalformedURLException e) {
	    	throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} catch (IOException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} catch (SAXException e) {
		    throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} catch (ParserConfigurationException e) {
		    throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} finally {
	    	if (connection != null) {
	    		connection.disconnect();
	    	}
	      }
		
		return uri;
	}
}
