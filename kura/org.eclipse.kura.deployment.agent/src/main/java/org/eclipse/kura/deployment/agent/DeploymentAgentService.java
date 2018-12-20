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
package org.eclipse.kura.deployment.agent;

public interface DeploymentAgentService {

    /**
     * {@linkplain org.osgi.service.event.Event Event} topic on which the result
     * of the deployment package installation is posted.
     */
    public static final String EVENT_INSTALLED_TOPIC = DeploymentAgentService.class.getPackage().getName().replace('.',
            '/') + "/INSTALLED";

    /**
     * {@linkplain org.osgi.service.event.Event Event} property
     * representing the URL of the deployment package.
     */
    public static final String EVENT_PACKAGE_URL = "deploymentpackage.url";

    /**
     * {@linkplain org.osgi.service.event.Event Event} property
     * representing the success (true or false)
     * of the deployment package installation/uninstallation.
     */
    public static final String EVENT_SUCCESSFUL = "successful";

    /**
     * {@linkplain org.osgi.service.event.Event Event} property
     * representing an Exception thrown installing or uninstalling
     * the deployment package.
     */
    public static final String EVENT_EXCEPTION = "exception";

    /**
     * {@linkplain org.osgi.service.event.Event Event} topic on which the result
     * of the deployment package uninstallation is posted.
     */
    public static final String EVENT_UNINSTALLED_TOPIC = DeploymentAgentService.class.getPackage().getName()
            .replace('.', '/') + "/UNINSTALLED";

    /**
     * {@linkplain org.osgi.service.event.Event Event} property
     * representing the symbolic name of the deployment package.
     */
    public static final String EVENT_PACKAGE_NAME = "deploymentpackage.name";

    public static final String EVENT_PACKAGE_VERSION = "deploymentpackage.version";

    /**
     * Installs a Deployment Package asynchronously fetching it at the given URL.
     * The method queues the package URL and returns immediately.
     * The result of the package installation is notified asynchronously
     * posting an {@linkplain org.osgi.service.event.Event Event}
     * on the topic {@linkplain #EVENT_INSTALLED_TOPIC}
     *
     * @see org.osgi.service.deploymentadmin.DeploymentAdmin#installDeploymentPackage installDeploymentPackage
     *
     *
     * @param url
     *            The URL of the deployment package
     * @throws Exception
     *             If the installation of a deployment package at the same URL is still pending
     */
    public void installDeploymentPackageAsync(String url) throws Exception;

    /**
     * Uninstalls a Deployment Package asynchronously.
     * The method queues the package symbolic name and returns immediately.
     * The result of the package uninstallation is notified asynchronously
     * posting an {@linkplain org.osgi.service.event.Event Event}
     * on the topic {@linkplain #EVENT_UNINSTALLED_TOPIC}
     *
     * @see org.osgi.service.deploymentadmin.DeploymentAdmin#uninstallDeploymentPackage uninstallDeploymentPackage
     *
     *
     * @param name
     *            The symbolic name of the deployment package
     * @throws Exception
     *             If the uninstallation of a deployment package at the same symbolic name is still pending
     */
    public void uninstallDeploymentPackageAsync(String name) throws Exception;

    /**
     * Asks if the installation of a deployment package at the given URL is pending.
     *
     * @param url
     *            The URL of the deployment package
     * @return true if the installation of a deployment package at URL is pending
     */
    public boolean isInstallingDeploymentPackage(String url);

    /**
     * Asks if the uninstallation of a deployment package with the given symbolic name is pending.
     *
     * @param name
     *            The symbolic name of the deployment package
     * @return true if the uninstallation of a deployment package at URL is pending
     */
    public boolean isUninstallingDeploymentPackage(String name);
}