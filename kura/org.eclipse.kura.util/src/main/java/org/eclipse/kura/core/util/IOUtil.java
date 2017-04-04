/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.util;

import static java.lang.Thread.currentThread;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * A few basic IO helper methods
 */
public final class IOUtil {

    private IOUtil() {
    }

    /**
     * Reads a resource fully and returns it as a string.
     *
     * @param resourceName
     *            the name of the resource
     * @return the content as string, or {@code null if the resource could not be found}
     * @throws IOException
     *             in case there is a resource but it cannot be read
     */
    public static String readResource(final String resourceName) throws IOException {
        return readResource(currentThread().getContextClassLoader().getResource(resourceName));
    }

    /**
     * Reads a resource fully and returns it as a string.
     *
     * @param ctx
     *            the bundle context to use for locating the resource
     * @param resourceName
     *            the name of the resource
     * @return the content as string, or {@code null if the resource could not be found}
     * @throws IOException
     *             in case there is a resource but it cannot be read
     */
    public static String readResource(BundleContext ctx, String resourceName) throws IOException {
        return readResource(ctx.getBundle().getResource(resourceName));
    }

    /**
     * Reads a resource fully and returns it as a string.
     *
     * @param bundle
     *            the bundle to use for getting the bundle context
     * @param resourceName
     *            the name of the resource
     * @return the content as string, or {@code null if the resource could not be found}
     * @throws IOException
     *             in case there is a resource but it cannot be read
     */
    public static String readResource(Bundle bundle, String resourceName) throws IOException {
        return readResource(bundle.getResource(resourceName));
    }

    /**
     * Reads a resource fully and returns it as a string.
     *
     * @param resourceUrl
     *            the URL to read the resource from, may be {@code null}
     * @return the content as string, or {@code null if the resource could not be found}
     * @throws IOException
     *             in case there is a resource but it cannot be read
     */
    public static String readResource(URL resourceUrl) throws IOException {
        if (resourceUrl == null) {
            return null;
        }

        return IOUtils.toString(resourceUrl);
    }
}
