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
package org.eclipse.kura.wire.provider.test;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This WireServiceTest is responsible to test all the API methods of
 * {@link WireService}
 */
public class WireServiceTest {

	/**
	 * A latch initialized to check if both the stub wire components are
	 * available
	 */
	private static CountDownLatch componentsLatch = new CountDownLatch(1);

	/** A latch to be initialized with the no of OSGi dependencies needed */
	private static CountDownLatch dependencyLatch = new CountDownLatch(2);

	/** The Wire Emitter Factory PID */
	private static final String emitterFactoryPid = "org.eclipse.kura.wire.test.emitter";

	/** The Wire Emitter PID */
	private static final String emitterPid = "STUB.EMITTER";

	private static ExecutorService executorService = Executors.newSingleThreadExecutor();

	/** A flag to be set if both the stub components are available */
	private static boolean isComponentsAvailable;

	/**
	 * A lock to synchronize between JUnit test method to wait for the
	 * components to become available
	 */
	private static Lock lock = new ReentrantLock();

	/** The Wire Receiver Factory PID */
	private static final String receiverFactoryPid = "org.eclipse.kura.wire.test.receiver";

	/** The Wire Receiver PID */
	private static final String receiverPid = "STUB.RECEIVER";

	/** Configuration Service Reference */
	private static ConfigurationService s_configService;

	/** Logger */
	private static final Logger s_logger = LoggerFactory.getLogger(WireServiceTest.class);

	/** Configuration Service Reference */
	private static WireService s_wireService;

	/**
	 * Binds the configuration service dependency
	 *
	 * @param configService
	 *            the configuration service dependency
	 */
	public void bindConfigService(final ConfigurationService configService) {
		if (s_configService == null) {
			s_configService = configService;
			dependencyLatch.countDown();
		}
	}

	/**
	 * Binds the wire service dependency
	 *
	 * @param wireService
	 *            the wire service dependency
	 */
	public void bindWireService(final WireService wireService) {
		if (s_wireService == null) {
			s_wireService = wireService;
			dependencyLatch.countDown();
		}
	}

	/**
	 * Tests {@link WireService} methods
	 */
	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testCreateDeleteGetWireConfiguration() throws Exception {
		WireConfiguration configuration = null;
		// lock.lock();
		try {
			if (isComponentsAvailable) {
				s_logger.info("The components are available =====>");
				configuration = s_wireService.createWireConfiguration(emitterPid, receiverPid);
				assertNotNull(configuration);
				assertNotNull(configuration.getWire());
				assertEquals(configuration.getEmitterPid(), emitterPid);
				assertEquals(configuration.getReceiverPid(), receiverPid);
				final List<WireConfiguration> list = s_wireService.getWireConfigurations();
				s_logger.info("List Before Delete ====>" + list);
				assertEquals(1, list.size());
				s_wireService.deleteWireConfiguration(configuration);
				s_logger.info("List After Delete ====>" + list);
				assertEquals(0, list.size());
			}
		} finally {
			// lock.unlock();
		}
	}

	/**
	 * Tests the availability of injected OSGi services
	 */
	// @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	// @Test
	// public void testServiceExists() {
	// assertNotNull(WireServiceTest.s_wireService);
	// assertNotNull(WireServiceTest.s_configService);
	// }

	/**
	 * Unbinds the configuration service dependency
	 *
	 * @param configService
	 *            the configuration service dependency
	 */
	public void unbindConfigService(final ConfigurationService configService) {
		if (s_configService == configService) {
			s_configService = null;
		}
	}

	/**
	 * Unbinds the wire service dependency
	 *
	 * @param wireService
	 *            the wire service dependency
	 */
	public void unbindWireService(final WireService wireService) {
		if (s_wireService == wireService) {
			s_wireService = null;
		}
	}

	/**
	 * Creates the necessary emitter and receiver stub components
	 */
	private static void createStubComponents() {
		try {
			// TimeUnit.SECONDS.sleep(10);
			s_configService.createFactoryConfiguration(emitterFactoryPid, emitterPid, null, false);
			s_configService.createFactoryConfiguration(receiverFactoryPid, receiverPid, null, false);
		} catch (final Exception exception) {
			s_logger.error("Factory configuration Creation Exception => " + exception);
		}
	}

	/**
	 * Gets the factory PID of the provided wire component
	 */
	public static String getFactoryPid(final String wireComponentPid) {
		final BundleContext context = FrameworkUtil.getBundle(WireServiceTest.class).getBundleContext();
		final ServiceReference<?>[] refs = getServiceReferences(context, WireComponent.class.getName(), null);
		for (final ServiceReference<?> ref : refs) {
			s_logger.info("Property kura.service.pid ====>" + ref.getProperty(KURA_SERVICE_PID));
			if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
				return ref.getProperty(SERVICE_PID).toString();
			}
		}
		return null;
	}

	/**
	 * Gets the service references available in registry which matches the
	 * provided class instance and provided filter
	 */
	private static ServiceReference<?>[] getServiceReferences(final BundleContext bundleContext, final String clazz,
			final String filter) {
		try {
			final ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz, filter);
			return refs == null ? new ServiceReference[0] : refs;
		} catch (final InvalidSyntaxException ise) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ThrowableUtil.stackTraceAsString(ise));
		}
	}

	/**
	 * JUnit Callback to be triggered before creating the instance of this suite
	 *
	 * @throws Exception
	 *             if the dependent services are null
	 */
	@BeforeClass
	public static void setUpClass() throws Exception {
		// Wait for OSGi dependencies
		s_logger.info("Setting Up The Testcase....");
		try {
			dependencyLatch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			fail("OSGi dependencies unfulfilled");
		}
		final Future<?> future = executorService.submit(new Runnable() {

			@Override
			public void run() {
				try {
					createStubComponents();
					// wait for 10 seconds to become the components available in
					// registry
					try {
						for (;;) {
							if ((getFactoryPid(emitterPid) != null) && (getFactoryPid(receiverPid) != null)) {
								isComponentsAvailable = true;
								break;
							} else {
								TimeUnit.MILLISECONDS.sleep(100);
							}
						}
					} catch (final Exception ex) {
						s_logger.info("Exception ====>");
					}
				} catch (final Exception e) {
					s_logger.info("Error =====>");
				}
			}
		});
		try {
			future.get(5, TimeUnit.SECONDS);
		} catch (final Exception exception) {
			s_logger.info("Timeout exception===>");
		}
	}
}
