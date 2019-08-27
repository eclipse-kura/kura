package org.eclipse.soda.dk.comm.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public class Activator implements BundleActivator {

    private final Logger logger = LoggerFactory.getLogger(Activator.class);

    /**
     * Parse bundle list with the specified raw list and bundle file parameters and return the String result.
     *
     * @param rawList The raw list (<code>String</code>) parameter.
     * @param bundleFile The bundle file (<code>String</code>) parameter.
     * @return Results of the parse bundle list (<code>String</code>) value.
     */
    private String parseBundleList(final String rawList, final String bundleFile) {
        String result = null;
        int i = rawList.indexOf(bundleFile);
        if (i != -1) {
            int j = rawList.lastIndexOf(',', i);
            int k = rawList.lastIndexOf("file:", i); //$NON-NLS-1$
            if (k != -1) {
                result = rawList.substring(k + 5, i + bundleFile.length());
            } else {
                result = rawList.substring(j + 1, i + bundleFile.length());
            }
        }
        return result;
    }

    /**
     * Parse install with the specified raw install parameter and return the String result.
     *
     * @param rawInstall The raw install (<code>String</code>) parameter.
     * @return Results of the parse install (<code>String</code>) value.
     */
    private String parseInstall(final String rawInstall) {
        String result;
        int i = rawInstall.indexOf("file:"); //$NON-NLS-1$
        result = rawInstall.substring(i + 5);
        if (result.startsWith("/")) { //$NON-NLS-1$
            result = result.substring(1);
        }
        return result;
    }

    /**
     * Parse loc with the specified raw loc parameter and return the String result.
     *
     * @param rawLoc The raw loc (<code>String</code>) parameter.
     * @return Results of the parse loc (<code>String</code>) value.
     */
    private String parseLoc(final String rawLoc) {
        String result;
        int i = rawLoc.indexOf("file:"); //$NON-NLS-1$
        result = rawLoc.substring(i + 5);
        if (rawLoc.endsWith("/")) { //$NON-NLS-1$
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Start with the specified arg0 parameter.
     *
     * @param ctx The <code>BundleContext</code> parameter.
     * @throws Exception Exception.
     */
    @Override
    public void start(final BundleContext ctx) throws Exception {
        // String bundle_loc = ctx.getBundle().getLocation();
        // if (bundle_loc.startsWith(Library.HTTP)) {
        // Library.setBundlepath(Library.HTTP, bundle_loc);
        // } else {
        // String bundle_file_name = parseLoc(bundle_loc);
        // String bundle_install = parseInstall(System.getProperty("osgi.install.area")); //$NON-NLS-1$
        // String bundle_path = parseBundleList(System.getProperty("osgi.bundles"), bundle_file_name); //$NON-NLS-1$
        // if (bundle_path == null || bundle_path.length() == bundle_file_name.length()) {
        // bundle_path = bundle_install + bundle_file_name;
        // }
        //
        // logger.info("bundle=" + bundle_file_name);
        // logger.info("bundle_install=" + bundle_install);
        // logger.info("bundle_path=" + bundle_path);
        //
        // Library.setBundlepath(Library.FILE, bundle_path);
        // }
    }

    /**
     * Stop with the specified arg0 parameter.
     *
     * @param ctx The <code>BundleContext</code> parameter.
     * @throws Exception Exception.
     */
    @Override
    public void stop(final BundleContext ctx) throws Exception {
        /* do nothing */
    }
}
