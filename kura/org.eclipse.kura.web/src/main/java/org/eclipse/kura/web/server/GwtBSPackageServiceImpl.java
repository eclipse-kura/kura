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
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtBSBundleInfo;
import org.eclipse.kura.web.shared.model.GwtBSDeploymentPackage;
import org.eclipse.kura.web.shared.service.GwtBSPackageService;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;

public class GwtBSPackageServiceImpl extends OsgiRemoteServiceServlet implements GwtBSPackageService
{
	private static final long serialVersionUID = -3422518194598042896L;


	public List<GwtBSDeploymentPackage> findDeviceDeploymentPackages()
		throws GwtKuraException 
	{
		DeploymentAdmin deploymentAdmin = ServiceLocator.getInstance().getService(DeploymentAdmin.class);
		
		List<GwtBSDeploymentPackage> gwtDeploymentPackages = new ArrayList<GwtBSDeploymentPackage>();
		DeploymentPackage[] deploymentPackages = deploymentAdmin.listDeploymentPackages();
		
		if (deploymentPackages != null) {
			for (DeploymentPackage deploymentPackage : deploymentPackages) {
				GwtBSDeploymentPackage gwtDeploymentPackage = new GwtBSDeploymentPackage();
				gwtDeploymentPackage.setName(deploymentPackage.getName());
				gwtDeploymentPackage.setVersion(deploymentPackage.getVersion().toString());
				
				List<GwtBSBundleInfo> gwtBundleInfos = new ArrayList<GwtBSBundleInfo>();
				BundleInfo[] bundleInfos = deploymentPackage.getBundleInfos();
				if (bundleInfos != null) {
					for (BundleInfo bundleInfo : bundleInfos) {
						GwtBSBundleInfo gwtBundleInfo = new GwtBSBundleInfo();
						gwtBundleInfo.setName(bundleInfo.getSymbolicName());
						gwtBundleInfo.setVersion(bundleInfo.getVersion().toString());
						
						gwtBundleInfos.add(gwtBundleInfo);
					}
				}
				
				gwtDeploymentPackage.setBundleInfos(gwtBundleInfos);
				
				gwtDeploymentPackages.add(gwtDeploymentPackage);
			}
		}
		
		return gwtDeploymentPackages;
	}

	
	
	public void uninstallDeploymentPackage(String packageName)
		throws GwtKuraException 
	{
		DeploymentAgentService deploymentAgentService = ServiceLocator.getInstance().getService(DeploymentAgentService.class);		
		try {
			deploymentAgentService.uninstallDeploymentPackageAsync(packageName);
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}

}
