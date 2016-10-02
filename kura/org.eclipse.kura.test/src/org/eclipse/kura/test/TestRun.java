/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *      Eurotech
 *******************************************************************************/
package org.eclipse.kura.test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;

public class TestRun {
	private SystemService m_systemService;
	private ComponentContext m_ctx;
	private List<TestBundleRun> m_bundleRuns;
	private String m_message;
	private Throwable m_throwable;
	private long m_startTime;
	private long m_endTime;
	private long m_timeout;
	private boolean m_successful;
	private Map<String, Object> m_properties;

	public TestRun(Map<String, Object> properties, SystemService systemService, ComponentContext componentContext, long timeout) {
		m_systemService = systemService;
		m_ctx = componentContext;
		m_bundleRuns = new ArrayList<TestBundleRun>();
		m_successful = true;
		m_properties = properties;
		m_timeout = timeout;
		setup();
	}

	public void addBundleRun(TestBundleRun bundleRun) {
		m_bundleRuns.add(bundleRun);
		bundleRun.setComponentContext(m_ctx);
		bundleRun.setTimeout(m_timeout);
	}
	
	public boolean wasSuccessful() {
		return m_successful;
	}
	
	public String getFailureMessage() {
		return m_message;
	}
	
	public Throwable getException() {
		return m_throwable;
	}
	
	public boolean isDone() {
		return m_endTime != 0;
	}
	
	public void run() {
		m_startTime = System.currentTimeMillis();
		// iterate over the test bundles
		for (TestBundleRun bundleRun : m_bundleRuns) {
			bundleRun.run();
			if (!bundleRun.wasSuccessful()) {
				m_successful = false;
			}
		}
		m_endTime = System.currentTimeMillis();
	}
	
	public void printReport(PrintWriter writer) {
		writer.print("Kura version: ");
		writer.print(m_systemService.getKuraVersion());
		if (m_successful) {
			writer.println(" [PASS]");
		} else {
			writer.println(" [FAIL]");
		}
		
		writer.print("Model ID: ");
		writer.println(m_systemService.getModelId());
		
		writer.print("Model name: ");
		writer.println(m_systemService.getModelName());
		
		writer.print("Platform: ");
		writer.println(m_systemService.getPlatform());

		writer.print("Java VM info: ");
		writer.println(m_systemService.getJavaVmInfo());

		writer.print("Java VM name: ");
		writer.println(m_systemService.getJavaVmName());

		writer.print("Java VM version: ");
		writer.println(m_systemService.getJavaVmVersion());

		writer.print("Java vendor: ");
		writer.println(m_systemService.getJavaVendor());
		
		writer.print("Java version: ");
		writer.println(m_systemService.getJavaVersion());
		
		writer.print("OS distribution: ");
		writer.println(m_systemService.getOsDistro());
		
		writer.print("OS distribution version: ");
		writer.println(m_systemService.getOsDistroVersion());
		
		writer.print("Start time: ");
		writer.println(m_startTime);
		
		writer.print("End time: ");
		writer.println(m_endTime);
		
		if (m_message != null) {
			writer.print("Message: ");
			writer.println(m_message);
		}

		if (m_throwable != null) {
			m_throwable.printStackTrace(writer);
		}
		
		writer.println();
		
		for (TestBundleRun bundleRun : m_bundleRuns) {
			bundleRun.printReport(writer);
			writer.println();
		}
	}

	public List<TestBundleRun> getBundleRuns() {
		return m_bundleRuns;
	}
	
	private void setup() {
		final Set<String> keys = m_properties.keySet();
		final TreeSet<String> orderedKeys = new TreeSet<String>(keys);
		for (String key : orderedKeys) {
			if (key.endsWith("test.bundle")) {
				final String bundleName = (String) m_properties.get(key);
				if (bundleName == null || bundleName.isEmpty()) {
					continue;
				}
				TestBundleRun bundleRun = new TestBundleRun(bundleName);
				addBundleRun(bundleRun);
				
				final int end = key.indexOf('.');
				final String index = key.substring(0, end);
				final String[] testClassNames = (String[]) m_properties.get(index + ".test.classes");
				
				if (testClassNames == null) {
					continue;
				}
				for (String className : testClassNames) {
					if (className == null || className.isEmpty()) {
						continue;
					}
					final TestClassRun classRun = new TestClassRun(className);
					bundleRun.addTestClassRun(classRun);
				}
			}
		}
	}
}
