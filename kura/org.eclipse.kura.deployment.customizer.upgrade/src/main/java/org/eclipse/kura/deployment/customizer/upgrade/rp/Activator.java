/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.deployment.customizer.upgrade.rp;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;

public class Activator implements BundleActivator {

	private UpgradeScriptResourceProcessorImpl urp;
	
	@Override
	public void start(BundleContext context) throws Exception {
		urp = new UpgradeScriptResourceProcessorImpl();
		Dictionary<String, String> dict = new Hashtable<String, String>();
		dict.put("service.pid", "org.eclipse.kura.deployment.customizer.upgrade.rp.UpgradeScriptResourceProcessor");
		urp.activate(context);
		context.registerService(ResourceProcessor.class.getName(), urp, dict);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		urp.deactivate();
	}

}
