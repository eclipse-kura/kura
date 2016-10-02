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
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteTargetTest implements ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory
			.getLogger(RemoteTargetTest.class);

	private SystemService m_systemService;
	
	private ComponentContext m_ctx;
	private Map<String, Object> m_properties;

	private final ScheduledExecutorService m_scheduler = Executors
			.newSingleThreadScheduledExecutor();

	public void setSystemService(SystemService systemService) {
		m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		m_systemService = null;
	}

	protected void activate(final ComponentContext componentContext, Map<String,Object> properties) {
		s_logger.info("Activating");
		m_ctx = componentContext;
		m_properties = properties;
		doUpdate(properties);
		s_logger.info("Activated");
	}
	
	protected void updated(final Map<String,Object> properties) {
		// Note that a spurious updates might be triggered due
		// to a test calling a configuration rollback (which is usually the case).
		// This would cause the test runner to run the tests again in an
		// endless loop.
		if (m_properties != null && propertiesAreEqual(m_properties, properties)) {
			s_logger.info("Old and new properties are equal. Ignoring update");
			return;
		}
		
		m_properties = properties;
		
		s_logger.info("Updating");
		doUpdate(properties);
		s_logger.info("Updated");
	}
	
	private void doUpdate(final Map<String,Object> properties) {
		m_scheduler.submit(new Runnable() {
			public void run() {
				runTests(properties);
			}
		});		
	}
	
	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating");
		m_scheduler.shutdownNow();
		s_logger.info("Deactivated");
	}
	
	private void runTests(Map<String,Object> properties) {
		long bundleWaitTimeout = (Long) properties.get("bundle.wait.timeout");
		boolean shutdown = (Boolean) properties.get("auto.shutdown");
		
		TestRun testRun = new TestRun(properties, m_systemService, m_ctx,
				bundleWaitTimeout);

		testRun.run();

		PrintWriter writer = null;
		final String filename = formFilename();
		try {
			writer = new PrintWriter(filename, "UTF-8");
			testRun.printReport(writer);
		} catch (FileNotFoundException e) {
			s_logger.error("Failed to write test report: '{}'", filename, e);
		} catch (UnsupportedEncodingException e) {
			s_logger.error("Failed to write test report: '{}'", filename, e);
		} finally {
			if (writer != null) {
				writer.close();
			}
			
			if (shutdown) {
				System.exit(0);
			}
		}
	}
		
	private boolean propertiesAreEqual(Map<String, Object> a, Map<String, Object> b)
	{
		if (a.keySet().size() != b.keySet().size()) {
			return false;
		}
		
		for (String key : a.keySet()) {
			Object oa = a.get(key);
			Object ob = b.get(key);
			if (oa.getClass() != ob.getClass()) {
				return false;
			}
			List<Object> la = Arrays.asList(oa);
			List<Object> lb = Arrays.asList(ob);
			
			if (!la.equals(lb)) {
				return false;
			}
		}
		return true;
	}
	
	private String formFilename() {
		final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmSS");
		final String date = format.format(new Date());
		final String platform = m_systemService.getPlatform().replace(" ", "_");
		final String kuraVersion = m_systemService.getKuraVersion().replace(" ", "_");
		StringBuilder sb = new StringBuilder();
		sb.append("/tmp/test")
		.append("-")
		.append(date)
		.append("-")
		.append(kuraVersion)
		.append("-")
		.append(platform);
		
		return sb.toString();
	}
}
