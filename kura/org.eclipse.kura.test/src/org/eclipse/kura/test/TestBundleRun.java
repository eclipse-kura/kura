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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBundleRun implements BundleTrackerCustomizer<Object> {
	private static final Logger s_logger = LoggerFactory
			.getLogger(TestBundleRun.class);

	private Object m_lock;
	private ComponentContext m_ctx;
	private BundleTracker<Object> m_bundleTracker;
	private String m_bundleName;
	private String m_message;
	private Throwable m_throwable;
	private List<TestClassRun> m_classRuns;
	private long m_startTime;
	private long m_endTime;
	private boolean m_successful;
	private long m_timeout;

	public TestBundleRun(String bundleName) {
		m_bundleName = bundleName;
		m_lock = new Object();
		m_classRuns = new ArrayList<TestClassRun>();
		m_successful = true;
	}

	public void setComponentContext(ComponentContext componentContext) {
		m_ctx = componentContext;
	}

	public void setTimeout(long timeout) {
		m_timeout = timeout;
	}

	public void addTestClassRun(TestClassRun classRun) {
		m_classRuns.add(classRun);
	}

	public String getTestBundle() {
		return m_bundleName;
	}

	public List<TestClassRun> getTestClassRuns() {
		return m_classRuns;
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

	public void run() {
		m_startTime = System.currentTimeMillis();
		try {
			m_bundleTracker = new BundleTracker<Object>(m_ctx.getBundleContext(),
					Bundle.RESOLVED | Bundle.ACTIVE | Bundle.INSTALLED, this);
			m_bundleTracker.open();

			s_logger.info("Waiting for test bundle '{}'", m_bundleName);

			// Wait for the bundle from which to load test classes.
			// If this bundle is a fragment it will wait for its host.
			Bundle bundle = waitForLoaderBundle(m_bundleName);
			if (bundle == null) {
				m_successful = false;
				m_message = MessageFormat.format(
						"Timeout waiting for test bundle: '{0}'", m_bundleName);
				s_logger.warn("Timeout waiting for test bundle: '{}'",
						m_bundleName);
				return;
			}

			// run test classes
			for (TestClassRun classRun : m_classRuns) {
				classRun.setLoaderBundle(bundle);
				classRun.run();
				if (!classRun.wasSuccessful()) {
					m_successful = false;
				}
			}
		} catch (Throwable t) {
			m_successful = false;
			m_throwable = t;
		} finally {
			m_endTime = System.currentTimeMillis();
			if (m_bundleTracker != null) {
				m_bundleTracker.close();
			}
		}
	}

	@Override
	public Object addingBundle(Bundle bundle, BundleEvent event) {
		synchronized (m_lock) {
			m_lock.notifyAll();
		}
		return bundle;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
		s_logger.debug("Tracker - Modified Bundle: " + bundle.getSymbolicName());
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		s_logger.debug("Tracker - Removed Bundle: " + bundle.getSymbolicName());
	}

	public void printReport(PrintWriter writer) {
		writer.print("Test bundle: ");
		writer.print(m_bundleName);
		if (m_successful) {
			writer.println(" [PASS]");
		} else {
			writer.println(" [FAIL]");
		}


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

		for (TestClassRun classRun : m_classRuns) {
			classRun.printReport(writer);
		}
	}

	private Bundle waitForLoaderBundle(final String name)
			throws InterruptedException {
		// find the bundle from which to load test classes
		Bundle bundle = waitForBundle(name);
		if (bundle != null) {
			// check if this is a fragment
			String host = bundle.getHeaders().get(
					org.osgi.framework.Constants.FRAGMENT_HOST);
			if (host != null) {
				s_logger.info(
						"Bundle is a fragment. Waiting for host bundle '{}'",
						host);
				bundle = waitForBundle(host);
			}
		}
		return bundle;
	}

	private Bundle waitForBundle(String bundleName) throws InterruptedException {
		synchronized (m_lock) {
			if (findBundle(bundleName) == null) {
				m_lock.wait(m_timeout);
			}
		}
		return findBundle(bundleName);
	}

	private Bundle findBundle(String name) {
		Bundle b = null;
		Bundle[] bundles = m_bundleTracker.getBundles();
		if (bundles != null) {
			for (Bundle bundle : bundles) {
				if (name.equals(bundle.getSymbolicName())) {
					b = bundle;
					break;
				}
			}
		}
		return b;
	}
}
