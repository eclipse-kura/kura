package org.eclipse.soda.dk.comm.internal;

/*************************************************************************
 * Copyright (c) 2007, 2009 IBM.                                         *
 * All rights reserved. This program and the accompanying materials      *
 * are made available under the terms of the Eclipse Public License v1.0 *
 * which accompanies this distribution, and is available at              *
 * http://www.eclipse.org/legal/epl-v10.html                             *
 *                                                                       *
 * Contributors:                                                         *
 *     IBM - initial API and implementation                              *
 ************************************************************************/
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public class Library {
	/**
	 * Define the http (String) constant.
	 */
	public static final String HTTP = "http"; //$NON-NLS-1$

	/**
	 * Define the file (String) constant.
	 */
	public static final String FILE = "file"; //$NON-NLS-1$

	/**
	 * Define the pathtype (String) field.
	 */
	private static String pathtype = ""; //$NON-NLS-1$

	/**
	 * Define the bundlepath (String) field.
	 */
	private static String bundlepath = ""; //$NON-NLS-1$

	/**
	 * Load_dkcomm.
	 */
	public static void load_dkcomm() {
		if (load_from_java_lib_path() == false) {
			// To remain portable across OSGI implementations, Kura will only load from lib path
			//load_from_bundle();
		}
	}

	/**
	 * Load_from_bundle.
	 */
	private static void load_from_bundle() {
		String file_separator = System.getProperty("file.separator"); //$NON-NLS-1$
		String javalibpath = System.getProperty("java.library.path"); //$NON-NLS-1$
		String path_seperator = System.getProperty("path.separator"); //$NON-NLS-1$
		int i = javalibpath.indexOf(path_seperator);
		javalibpath = javalibpath.substring(0, i) + file_separator;
		String os = System.getProperty("osgi.ws"); //$NON-NLS-1$
		String processor = System.getProperty("org.osgi.framework.processor"); //$NON-NLS-1$
		String libname = ""; //$NON-NLS-1$
		if (os.equalsIgnoreCase("win32")) { //$NON-NLS-1$
			libname = "dkcomm.dll"; //$NON-NLS-1$
		} else if (os.equalsIgnoreCase("linux")) { //$NON-NLS-1$
			libname = "dkcomm.so"; //$NON-NLS-1$
		}
		String libpath = "lib/" + os + '/' + processor + '/'; //$NON-NLS-1$
		try {
			if (pathtype.equals(FILE)) {
				unzipLib_local(javalibpath, libpath, libname, bundlepath);
			} else if (pathtype.equals(HTTP)) {
				unzipLib_http(javalibpath, libpath, libname, bundlepath);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			System.load(javalibpath + libname);
		} catch (java.lang.UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load_from_java_lib_path and return the boolean result.
	 * @return Results of the load_from_java_lib_path (<code>boolean</code>) value.
	 */
	private static boolean load_from_java_lib_path() {
		try {
			System.loadLibrary("dkcomm"); //$NON-NLS-1$
		} catch (final UnsatisfiedLinkError e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Perform unzip with the specified input and output parameters.
	 * @param input
	 *		The input (<code>BufferedInputStream</code>) parameter.
	 * @param output
	 *		The output (<code>BufferedOutputStream</code>) parameter.
	 * @throws IOException IOException.
	 */
	private static void performUnzip(final BufferedInputStream input, final BufferedOutputStream output) throws IOException {
		int block_size = 4000;
		byte[] block = new byte[block_size];
		int len = input.read(block, 0, block_size);
		while (len != -1) {
			output.write(block, 0, len);
			len = input.read(block, 0, block_size);
		}
		output.flush();
		input.close();
		output.close();
	}

	/**
	 * Set bundlepath with the specified type and path parameters.
	 * @param type
	 *		The type (<code>String</code>) parameter.
	 * @param path
	 *		The path (<code>String</code>) parameter.
	 */
	public static void setBundlepath(final String type, final String path) {
		pathtype = type;
		bundlepath = path;
	}

	/**
	 * Unzip lib_http with the specified javalibpath, libpath, libname and url parameters.
	 * @param javalibpath
	 *		The javalibpath (<code>String</code>) parameter.
	 * @param libpath
	 *		The libpath (<code>String</code>) parameter.
	 * @param libname
	 *		The libname (<code>String</code>) parameter.
	 * @param url
	 *		The URL (<code>String</code>) parameter.
	 * @throws IOException IOException.
	 */
	private static void unzipLib_http(final String javalibpath, final String libpath, final String libname, final String url) throws IOException {
		ZipInputStream zipInput = new ZipInputStream(new URL(url).openStream());
		while (zipInput.available() != 0) {
			try {
				if (zipInput.getNextEntry().getName().equals(libpath + libname)) {
					BufferedInputStream input = new BufferedInputStream(zipInput);
					BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(javalibpath + libname));
					performUnzip(input, output);
					break;
				}
			} catch (final Exception e) {
				break;
			}
		}
	}

	/**
	 * Unzip lib_local with the specified javalibpath, libpath, libname and jarname parameters.
	 * @param javalibpath
	 *		The javalibpath (<code>String</code>) parameter.
	 * @param libpath
	 *		The libpath (<code>String</code>) parameter.
	 * @param libname
	 *		The libname (<code>String</code>) parameter.
	 * @param jarname
	 *		The jarname (<code>String</code>) parameter.
	 * @throws IOException IOException.
	 */
	private static void unzipLib_local(final String javalibpath, final String libpath, final String libname, final String jarname) throws IOException {
		ZipFile bundleJar = new ZipFile(jarname);
		BufferedInputStream input = new BufferedInputStream(bundleJar.getInputStream(bundleJar.getEntry(libpath + libname)));
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(javalibpath + libname));
		performUnzip(input, output);
	}
}
