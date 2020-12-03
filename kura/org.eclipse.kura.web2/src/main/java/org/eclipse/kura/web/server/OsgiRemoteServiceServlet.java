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
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

public class OsgiRemoteServiceServlet extends KuraRemoteServiceServlet {

    private static final long serialVersionUID = -8826193840033103296L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Cache the current thread
        Thread currentThread = Thread.currentThread();
        // We are going to swap the class loader
        ClassLoader oldContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(this.getClass().getClassLoader());
        try {
            super.service(req, resp);
        } finally {
            currentThread.setContextClassLoader(oldContextClassLoader);
        }
    }

    /**
     * Gets the {@link SerializationPolicy} for given module base URL and strong
     * name if there is one.
     *
     * Override this method to provide a {@link SerializationPolicy} using an
     * alternative approach.
     *
     * @param request
     *            the HTTP request being serviced
     * @param moduleBaseURL
     *            as specified in the incoming payload
     * @param strongName
     *            a strong name that uniquely identifies a serialization policy
     *            file
     * @return a {@link SerializationPolicy} for the given module base URL and
     *         strong name, or <code>null</code> if there is none
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL,
            String strongName) {
        // The request can tell you the path of the web app relative to the
        // container root.
        String contextPath = request.getContextPath();
        String modulePath = null;

        if (moduleBaseURL != null) {
            try {
                modulePath = new URL(moduleBaseURL).getPath();
            } catch (MalformedURLException ex) {
                // log the information, we will default
                log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        /*
         * Check that the module path must be in the same web app as the servlet
         * itself. If you need to implement a scheme different than this,
         * override this method.
         */
        if (modulePath == null || !modulePath.startsWith(contextPath)) {
            String message = "ERROR: The module path requested, " + modulePath
                    + ", is not in the same web application as this servlet, " + contextPath
                    + ".  Your module may not be properly configured or your client and server code maybe out of date.";
            log(message, null);
        } else {
            // Strip off the context path from the module base URL. It should be
            // a
            // strict prefix.
            String contextRelativePath = modulePath.substring(contextPath.length());

            // adding a comment
            // adding a comment2

            String serializationPolicyFilePath = SerializationPolicyLoader
                    .getSerializationPolicyFileName(contextRelativePath + strongName);

            // Open the RPC resource file read its contents.
            InputStream is = getServletContext().getResourceAsStream(serializationPolicyFilePath);
            if (is == null) {
                // try: /www/denali/202D6ADA06C975A44587AEAB102E2B68.gwt.rpc
                String file = "/www" + serializationPolicyFilePath.replace("/admin", "");
                log("Trying " + file);
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            }

            try {
                if (is != null) {
                    try {
                        serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
                    } catch (ParseException e) {
                        log("ERROR: Failed to parse the policy file '" + serializationPolicyFilePath + "'", e);
                    } catch (IOException e) {
                        log("ERROR: Could not read the policy file '" + serializationPolicyFilePath + "'", e);
                    }
                } else {
                    String message = "ERROR: The serialization policy file '" + serializationPolicyFilePath
                            + "' was not found; did you forget to include it in this deployment?";
                    log(message, null);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore this error
                    }
                }
            }
        }
        return serializationPolicy;
    }
}
