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

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassRun {
	private static final Logger s_logger = LoggerFactory
			.getLogger(TestClassRun.class);

	private String m_className;
	private String m_message;
	private Throwable m_throwable;
	private Result m_result;
	private boolean m_successful;
	private Bundle m_loaderBundle;

	public TestClassRun(String className) {
		m_className = className;
		m_successful = true;
	}

	public void setLoaderBundle(Bundle bundle) {
		m_loaderBundle = bundle;
	}

	public String getTestClass() {
		return m_className;
	}

	public Throwable getException() {
		return m_throwable;
	}

	public String getFailureMessage() {
		return m_message;
	}

	public Result getResult() {
		return m_result;
	}

	public boolean wasSuccessful() {
		return m_successful;
	}

	public void run() {
		Request request = null;
		try {
			// this will load all the test classes from the loader bundle
			// and create a JUnit Request instance.
			request = newJUnitRequest();

			s_logger.info("Running test class: '{}'", m_className);

			JUnitCore core = new JUnitCore();
			m_result = core.run(request);
			if (!m_result.wasSuccessful()) {
				m_successful = false;
			}
		} catch (ClassNotFoundException e) {
			m_message = MessageFormat.format(
					"Cannot create JUnit request for test class: '{0}'",
					m_className);
			m_throwable = e;
			m_successful = false;
			s_logger.error("Cannot create JUnit request for test class: '{}'",
					m_className, e);
		}
	}

	public void printReport(PrintWriter writer) {
		writer.print("Test class: ");
		writer.print(m_className);
		if (m_successful) {
			writer.println(" [PASS]");
		} else {
			writer.println(" [FAIL]");
		}

		if (m_message != null) {
			writer.print("Message: ");
			writer.println(m_message);
		}

		if (m_throwable != null) {
			m_throwable.printStackTrace(writer);
		}

		writer.print("Run count: ");
		writer.println(m_result.getRunCount());

		writer.print("Failure count: ");
		writer.println(m_result.getFailureCount());

		writer.print("Ignore count: ");
		writer.println(m_result.getIgnoreCount());

		writer.print("Run time: ");
		writer.println(m_result.getRunTime());

		if (!m_result.wasSuccessful()) {
			List<Failure> failures = m_result.getFailures();
			if (failures != null) {
				for (Failure failure : failures) {
					writer.print("Test: ");
					writer.println(failure.getTestHeader());

					writer.print("Failure message: ");
					writer.println(failure.getMessage());

					writer.print("Failure trace: ");
					writer.println(failure.getTrace());
				}
			}
		}
	}

	private Request newJUnitRequest() throws ClassNotFoundException {
		final Class<?> clazz = m_loaderBundle.loadClass(m_className);
		final Request request = Request.aClass(clazz);
		final Description suite = request.getRunner().getDescription();

		final ArrayList<Description> children = suite.getChildren();
		if (children != null) {
			for (Description child : children) {
				s_logger.info("Loading test class: '{}' from bundle: '{}'",
						child.getClassName(), m_loaderBundle);
				m_loaderBundle.loadClass(child.getClassName());
			}
		}
		return request;
	}
}
