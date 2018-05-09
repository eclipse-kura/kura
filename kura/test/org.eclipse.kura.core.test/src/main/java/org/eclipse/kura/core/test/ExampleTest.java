/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
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
package org.eclipse.kura.core.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

@SuppressWarnings("deprecation")
public class ExampleTest implements DataServiceListener {

    // If you wonder why we need to use static members is our test cases, keep reading.
    // Two instances of the ExampleTest gets created during the test run:
    // * by the OSGi framework (this is a declarative service component)
    // * by the JUnit framework. This instance needs to access the OSGi service we want to test (the dependency).
    // The service being tested is provided by the OSGi framework through the setXxxService bind method.
    // Thus the service need to be shared across the two ExampleTest instances
    // through a static class member.
    //
    // The CountDownLatch instance need to be shared too because it's checked by the
    // ExampleTest instance created by JUnit but decremented by the instance
    // created by OSGi.
    //
    // In the example below the service under test (the dependency) is the DataService.
    // The DataService notifies some events to interested client components, the ExampleTest
    // class in this example, through the DataServiceListener interface.
    // The ExampleTest does not register itself as a listener explicitly calling a register method of the
    // DataService. Instead, it registers the DataServiceListener as a provided service in its component definition.
    // The DataService will dynamically discover from the OSGi service registry the registered DataServiceListeners.
    //
    // The important thing to note here is that, while the ExampleTest instance created by OSGi is registered
    // to the OSGi framework as a DataServiceListener, the one created by JUnit is not.
    // When JUnit runs a test, it uses the DataService through its instance of ExampleTest.
    // Since this instance is not registered in the OSGi framework, the DataService will not find it and
    // will not call it.
    // This is a problem if a JUnit test needs to synchronize with a callback because the synchronization
    // cannot happen through instance members (e.g. a Lock).
    // The easiest think is to share everything is needed through static members.
    //
    // See also:
    // http://stackoverflow.com/questions/7161338/using-osgi-declarative-services-in-the-context-of-a-junit-test
    private static DataService s_dataService;
    private static CountDownLatch s_dependencyLatch = new CountDownLatch(1);	// initialize with number of
    // dependencies

    private static Lock s_lock = new ReentrantLock();
    private static Condition s_condition = s_lock.newCondition();
    private static Set<Integer> s_messageIds = new HashSet<Integer>();

    public ExampleTest() {
        super();
        System.err.println("New instance created");
    }

    //
    // OSGi activation methods. These methods are called only once.
    // There is only a single instance of this class created by the OSGi framework.
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this) + ": activated");
    }

    public void updated(Map<String, Object> properties) {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this) + ": updated");
    }

    protected void deactivate(ComponentContext componentContext) {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this) + ": deactivated");
    }

    public void setDataService(DataService dataService) {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this) + ": setXxxService");
        s_dataService = dataService;
        s_dependencyLatch.countDown();
    }

    public void unsetDataService(DataService dataService) {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this) + ": unsetDataService");
        s_dataService = null;
    }

    //
    // JUnit 4 stuff
    @BeforeClass
    public static void setUpOnce() {
        System.err.println("Setup test preconditions. This static method is called once");
        // Wait for OSGi dependencies
        try {
            s_dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }
    }

    @Before
    public void setUp() throws KuraConnectException {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this)
                + ": setup test preconditions. This is method is called before every @Test method like the JUnit 3 setUp method");
        if (!s_dataService.isConnected()) {
            s_dataService.connect();
        }
    }

    @Test
    public void Test() throws InterruptedException, KuraStoreException {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this) + ": test");

        s_lock.lock();
        try {
            Integer messageId = s_dataService.publish("#account-name/#client-id/a/b/c", "Hello!".getBytes(), 1, false,
                    5);
            s_messageIds.add(messageId);
            boolean confirmed = s_condition.await(5, TimeUnit.SECONDS);
            assertTrue(confirmed);
        } catch (KuraStoreException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            s_lock.unlock();
        }
    }

    @After
    public void tearDown() {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this)
                + ": release resources that might have been allocated by the @Before method. This is method is called after every @Test method like the JUnit 3 tearDown method");
    }

    @AfterClass
    public static void tearDownOnce() {
        System.err.println(
                "Release resources that might have been allocated by the @BeforeClass method. This static method is called once");
    }

    //
    // DataServiceListener
    @Override
    public void onConnectionEstablished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnecting() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnected() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionLost(Throwable cause) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        System.err.println("ExampleTest instance :" + System.identityHashCode(this) + ": onMessageConfirmed");
        s_lock.lock();
        s_messageIds.remove(messageId);
        if (s_messageIds.isEmpty()) {
            s_condition.signal();
        }
        s_lock.unlock();
    }
}
