/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Jens Reimann <jreimann@redhat.com>
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.PasswordCommandService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtDeviceServiceImpl extends OsgiRemoteServiceServlet implements GwtDeviceService {

    private static final String DEV_JAVA = "devJava";

    private static final String DEV_SW = "devSw";

    private static final String DEV_HW = "devHw";

    private static final String DEV_INFO = "devInfo";

    private static final Logger logger = LoggerFactory.getLogger(GwtDeviceServiceImpl.class);

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final String UNKNOWN = "UNKNOWN";

    private static final long serialVersionUID = -4176701819112753800L;

    @Override
    public ArrayList<GwtGroupedNVPair> findDeviceConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtGroupedNVPair> pairs = new ArrayList<>();

        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
        SystemAdminService systemAdminService = ServiceLocator.getInstance().getService(SystemAdminService.class);

        try {

            Properties systemProperties = systemService.getProperties();

            pairs.add(new GwtGroupedNVPair(DEV_INFO, "devKuraVersion", systemService.getKuraVersion()));
            pairs.add(new GwtGroupedNVPair(DEV_INFO, "devClientId",
                    systemService.getPrimaryMacAddress() != null ? systemService.getPrimaryMacAddress() : UNKNOWN));
            pairs.add(new GwtGroupedNVPair(DEV_INFO, "devDisplayName", systemService.getDeviceName()));
            pairs.add(new GwtGroupedNVPair(DEV_INFO, "devUptime",
                    formatUptime(Long.parseLong(systemAdminService.getUptime()))));
            pairs.add(new GwtGroupedNVPair(DEV_INFO, "devLastWifiChannel",
                    String.valueOf(systemService.getKuraWifiTopChannel())));

            pairs.add(new GwtGroupedNVPair(DEV_HW, "devModelName", systemService.getModelName()));
            pairs.add(new GwtGroupedNVPair(DEV_HW, "devModelId", systemService.getModelId()));
            pairs.add(new GwtGroupedNVPair(DEV_HW, "devPartNumber", systemService.getPartNumber()));
            pairs.add(new GwtGroupedNVPair(DEV_HW, "devSerialNumber", systemService.getSerialNumber()));

            pairs.add(new GwtGroupedNVPair(DEV_SW, "devFirmwareVersion", systemService.getFirmwareVersion()));
            pairs.add(new GwtGroupedNVPair(DEV_SW, "devBiosVersion", systemService.getBiosVersion()));
            pairs.add(new GwtGroupedNVPair(DEV_SW, "devOsVersion", systemService.getOsVersion()));
            pairs.add(new GwtGroupedNVPair(DEV_SW, "devOs", systemService.getOsName()));
            pairs.add(new GwtGroupedNVPair(DEV_SW, "devOsArch", systemService.getOsArch()));

            pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devJvmName",
                    systemProperties.getProperty(SystemService.KEY_JAVA_VM_NAME)));
            pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devJvmVersion",
                    systemProperties.getProperty(SystemService.KEY_JAVA_VM_VERSION)));

            pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devJvmProfile",
                    systemService.getJavaVendor() + " " + systemService.getJavaVersion()));
            pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devOsgiFramework",
                    systemProperties.getProperty(SystemService.KEY_OSGI_FW_NAME)));
            pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devOsgiFrameworkVersion",
                    systemProperties.getProperty(SystemService.KEY_OSGI_FW_VERSION)));
            if (systemService.getNumberOfProcessors() != -1) {
                pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devNumProc",
                        String.valueOf(systemService.getNumberOfProcessors())));
            }
            pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devRamTot",
                    String.valueOf(systemService.getTotalMemory()) + " kB"));
            pairs.add(new GwtGroupedNVPair(DEV_JAVA, "devRamFree",
                    String.valueOf(systemService.getFreeMemory()) + " kB"));
        } catch (Exception e) {
            final HttpServletRequest request = getThreadLocalRequest();
            final HttpSession session = request.getSession(false);
            auditLogger.warn("UI Device - Failure - Failed to list device info for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
        return new ArrayList<>(pairs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<GwtGroupedNVPair> findThreads(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtGroupedNVPair> pairs = new ArrayList<>();

        // get root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        // enumerate all other threads
        final ThreadGroup[] groups = new ThreadGroup[2 * rootGroup.activeGroupCount()];
        rootGroup.enumerate(groups);
        Arrays.sort(groups, ThreadGroupComparator.getInstance());
        for (ThreadGroup group : groups) {

            if (group != null) {
                StringBuilder sbGroup = new StringBuilder();
                sbGroup.append("ThreadGroup ").append(group.getName()).append(" [").append("maxprio=")
                        .append(group.getMaxPriority());
                sbGroup.append(", parent=");
                if (group.getParent() != null) {
                    sbGroup.append(group.getParent().getName());
                } else {
                    sbGroup.append('-');
                }

                sbGroup.append(", isDaemon=");
                sbGroup.append(group.isDaemon());
                sbGroup.append(", isDestroyed=");
                sbGroup.append(group.isDestroyed());
                sbGroup.append(']');

                int numThreads = group.activeCount();
                Thread[] threads = new Thread[numThreads * 2];
                group.enumerate(threads, false);
                Arrays.sort(threads, ThreadComparator.getInstance());
                for (Thread thread : threads) {

                    if (thread != null) {

                        StringBuilder sbThreadName = new StringBuilder();
                        sbThreadName.append(thread.getId()).append('/').append(thread.getName());

                        StringBuilder sbThreadValue = new StringBuilder();
                        sbThreadValue.append("priority=");
                        sbThreadValue.append(thread.getPriority());
                        sbThreadValue.append(", alive=");
                        sbThreadValue.append(thread.isAlive());
                        sbThreadValue.append(", daemon=");
                        sbThreadValue.append(thread.isDaemon());
                        sbThreadValue.append(", interrupted=");
                        sbThreadValue.append(thread.isInterrupted());
                        sbThreadValue.append(", loader=");
                        sbThreadValue.append(thread.getContextClassLoader());
                        sbThreadValue.append(']');

                        pairs.add(new GwtGroupedNVPair(sbGroup.toString(), sbThreadName.toString(),
                                sbThreadValue.toString()));
                    }
                }
            }
        }
        return new ArrayList<>(pairs);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ArrayList<GwtGroupedNVPair> findSystemProperties(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtGroupedNVPair> pairs = new ArrayList<>();
        // kura properties
        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
        Properties kuraProps = systemService.getProperties();
        SortedSet kuraKeys = new TreeSet(kuraProps.keySet());
        for (Iterator ki = kuraKeys.iterator(); ki.hasNext();) {
            Object key = ki.next();
            pairs.add(new GwtGroupedNVPair("propsKura", key.toString(), kuraProps.get(key).toString()));
        }
        return new ArrayList<>(pairs);
    }

    @Override
    public ArrayList<GwtGroupedNVPair> findBundles(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtGroupedNVPair> pairs = new ArrayList<>();

        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
        Bundle[] bundles = systemService.getBundles();
        if (bundles != null) {

            for (Bundle bundle : bundles) {

                if (bundle != null) {

                    GwtGroupedNVPair pair = new GwtGroupedNVPair();
                    pair.setId(String.valueOf(bundle.getBundleId()));
                    pair.setName(getName(bundle));
                    pair.setStatus(toStateString(bundle));
                    pair.setVersion(getHeaderValue(bundle, Constants.BUNDLE_VERSION));

                    pairs.add(pair);
                }
            }
        }
        return new ArrayList<>(pairs);
    }

    @Override
    public void startBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
        Bundle[] bundles = systemService.getBundles();

        logger.info("Starting bundle with ID: {}", bundleId);
        for (Bundle b : bundles) {
            if (b.getBundleId() == Long.parseLong(bundleId)) {
                try {
                    b.start();
                    auditLogger.info(
                            "UI Device - Success - Successfully started bundle for user: {}, session: {}, bundle: {}",
                            session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), bundleId);
                    return;
                } catch (BundleException e) {
                    logger.error("Failed to start bundle {}", b.getBundleId(), e);
                    auditLogger.warn(
                            "UI Device - Failure - Failed to start bundle for user: {}, session: {}, bundle: {}",
                            session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), bundleId);
                    throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
                }
            }
        }
        // Bundle was not found, throw error
        logger.error("Could not find bundle with ID: {}", bundleId);
        auditLogger.warn("UI Device - Failure - Failed to start bundle for user: {}, session: {}, bundle: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), bundleId);
        throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
    }

    @Override
    public void stopBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
        Bundle[] bundles = systemService.getBundles();

        logger.info("Stopping bundle with ID: {}", bundleId);
        for (Bundle b : bundles) {
            if (b.getBundleId() == Long.parseLong(bundleId)) {
                try {
                    b.stop();
                    auditLogger.info(
                            "UI Device - Success - Successfully stopped bundle for user: {}, session: {}, bundle: {}",
                            session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), bundleId);
                    return;
                } catch (BundleException e) {
                    logger.error("Failed to stop bundle {}", b.getBundleId(), e);
                    auditLogger.warn(
                            "UI Device - Failure - Failed to stop bundle for user: {}, session: {}, bundle: {}",
                            session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), bundleId);
                    throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
                }
            }
        }

        // Bundle was not found, throw error
        logger.error("Could not find bundle with ID: {}", bundleId);
        auditLogger.warn("UI Device - Failure - Failed to stop bundle for user: {}, session: {}, bundle: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), bundleId);
        throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);

    }

    @Override
    public String executeCommand(GwtXSRFToken xsrfToken, String cmd, String pwd) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        PasswordCommandService commandService = ServiceLocator.getInstance().getService(PasswordCommandService.class);
        try {
            String result = commandService.execute(cmd, pwd);
            auditLogger.info(
                    "UI Device - Success - Successfully executed command for user: {}, session: {}, command: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), cmd);
            return result;
        } catch (KuraException e) {
            GwtKuraException gwtKuraException = null;
            if (e.getCode() == KuraErrorCode.OPERATION_NOT_SUPPORTED) {
                gwtKuraException = new GwtKuraException(GwtKuraErrorCode.SERVICE_NOT_ENABLED);
            } else if (e.getCode() == KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID) {
                gwtKuraException = new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            } else {
                gwtKuraException = new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
            }

            auditLogger.warn("UI Device - Failure - Failed to execute command for user: {}, session: {}, command: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), cmd);

            throw gwtKuraException;
        }
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Return a display name for the given <code>bundle</code>:
     * <ol>
     * <li>If the bundle has a non-empty <code>Bundle-Name</code> manifest
     * header that value is returned.</li>
     * <li>Otherwise the symbolic name is returned if set</li>
     * <li>Otherwise the bundle's location is returned if defined</li>
     * <li>Finally, as a last resort, the bundles id is returned</li>
     * </ol>
     *
     * @param bundle
     *            the bundle which name to retrieve
     * @param locale
     *            the locale, in which the bundle name is requested
     * @return the bundle name - see the description of the method for more
     *         details.
     */
    private String getName(Bundle bundle) {
        String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
        if (name == null || name.length() == 0) {
            name = bundle.getSymbolicName();
            if (name == null) {
                name = bundle.getLocation();
                if (name == null) {
                    name = String.valueOf(bundle.getBundleId());
                }
            }
        }
        return name;
    }

    /**
     * Returns the value of the header or the empty string if the header is not
     * available.
     *
     * @param bundle
     *            the bundle which header to retrieve
     * @param headerName
     *            the name of the header to retrieve
     * @return the header or empty string if it is not set
     */
    private static String getHeaderValue(Bundle bundle, String headerName) {
        Object value = bundle.getHeaders().get(headerName);
        if (value != null) {
            return value.toString();
        }
        return "";
    }

    private String toStateString(final Bundle bundle) {
        switch (bundle.getState()) {
        case Bundle.INSTALLED:
            return "bndInstalled";
        case Bundle.RESOLVED:
            return "bndResolved";
        case Bundle.STARTING:
            return "bndStarting";
        case Bundle.ACTIVE:
            return "bndActive";
        case Bundle.STOPPING:
            return "bndStopping";
        case Bundle.UNINSTALLED:
            return "bndUninstalled";
        default:
            return "bndUnknown";
        }
    }

    private String formatUptime(long uptime) {
        int days = (int) TimeUnit.MILLISECONDS.toDays(uptime);
        long hours = TimeUnit.MILLISECONDS.toHours(uptime) - days * 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) - TimeUnit.MILLISECONDS.toHours(uptime) * 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MILLISECONDS.toMinutes(uptime) * 60;

        StringBuilder sb = new StringBuilder();
        sb.append(days).append(" days ").append(hours).append(":").append(minutes).append(":").append(seconds)
                .append(" hms");

        return sb.toString();
    }
}

@SuppressWarnings("rawtypes")
final class ThreadComparator implements Comparator {

    private ThreadComparator() {
        // prevent instantiation
    }

    private static final Comparator instance = new ThreadComparator();

    public static final Comparator getInstance() {
        return instance;
    }

    @Override
    public int compare(Object thread1, Object thread2) {
        if (thread1 == null || thread2 == null) {
            return thread1 == null ? -1 : 1;
        }

        String t1 = ((Thread) thread1).getName();
        String t2 = ((Thread) thread2).getName();
        if (null == t1) {
            t1 = ""; //$NON-NLS-1$
        }
        if (null == t2) {
            t2 = ""; //$NON-NLS-1$
        }

        return t1.toLowerCase().compareTo(t2.toLowerCase());
    }
}

@SuppressWarnings("rawtypes")
final class ThreadGroupComparator implements Comparator {

    private ThreadGroupComparator() {
        // prevent instantiation
    }

    private static final Comparator instance = new ThreadGroupComparator();

    public static final Comparator getInstance() {
        return instance;
    }

    @Override
    public int compare(Object thread1, Object thread2) {
        if (thread1 == null || thread2 == null) {
            return thread1 == null ? -1 : 1;
        }

        String t1 = ((ThreadGroup) thread1).getName();
        String t2 = ((ThreadGroup) thread2).getName();
        if (null == t1) {
            t1 = ""; //$NON-NLS-1$
        }
        if (null == t2) {
            t2 = ""; //$NON-NLS-1$
        }
        return t1.toLowerCase().compareTo(t2.toLowerCase());
    }
}
