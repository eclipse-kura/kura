/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import java.util.*;

import javax.usb.*;
import javax.usb.util.*;

import com.ibm.jusb.*;
import com.ibm.jusb.os.*;
import com.ibm.jusb.util.*;

/**
 * Interface to/from JNI.
 * @author Dan Streetman
 */
class JavaxUsb
{
	//*************************************************************************
	// Public methods

	/** Load native library */
	public static void loadLibrary() throws UsbException
	{
		try { System.loadLibrary( LIBRARY_NAME ); }
		catch ( Exception e ) { throw new UsbException( EXCEPTION_WHILE_LOADING_SHARED_LIBRARY + " " + System.mapLibraryName( LIBRARY_NAME ) + " : " + e.getMessage() ); }
		catch ( Error e ) { throw new UsbException( ERROR_WHILE_LOADING_SHARED_LIBRARY + " " + System.mapLibraryName( LIBRARY_NAME ) + " : " + e.getMessage() ); }
	}

	/**
	 * Convert the error code to a UsbException.
	 * @param error The error code.
	 * @return A UsbException.
	 */
	public static UsbException errorToUsbException(int error) { return errorToUsbException(error, ""); }

	/**
	 * Convert the error code to a UsbException using the specified text.
	 * <p>
	 * The string is prepended to the detail message with a colon separating
	 * the specified text from the error message.
	 * @param error The error code.
	 * @param string The string to use in the UsbException.
	 * @return A UsbException.
	 */
	public static UsbException errorToUsbException(int error, String string)
	{
		error = Math.abs(error);

		if (0 < string.length())
			string += " : ";

		switch (error) {
		case 32:
			return new UsbStallException(string + "UsbStallException");
		case 71:
			return new UsbBitStuffException(string + "UsbBitStuffException");
		case 75:
			return new UsbBabbleException(string + "UsbBabbleException");
		case 84:
			return new UsbCRCException(string + "UsbCRCException");
		case 121:
			return new UsbShortPacketException(string + "UsbShortPacketException");
		default:
			return new UsbPlatformException(string + nativeGetErrorMessage(error));
		}
	}

	//*************************************************************************
	// Native methods

		//*********************************
		// Tracing/Logging methods

	/**
	 * Enable (or disable) tracing.
	 * @param enable If tracing of data should be enabled.
	 */
	static native void nativeSetTracing(boolean enable);

	/**
	 * Enable (or disable) tracing of a certain type of data.
	 * @param enable If tracing of data should be enabled.
	 * @param type The type of data.
	 */
	static native void nativeSetTraceType(boolean enable, String type);

	/**
	 * Set the level of tracing.
	 * @param level The level of tracing.
	 */
	static native void nativeSetTraceLevel(int level);

	/**
	 * Set the trace output.
	 * @param output Where to output; 1=stdout, 2=stderr, 3=file.
	 * @param filename The filename (ignored if output != 3).
	 */
	static native void nativeSetTraceOutput(int output, String filename);

		//*********************************
		// JavaxUsbTopologyUpdater methods

	/**
	 * Call the native function that updates the topology.
	 * @param services The LinuxUsbServices instance.
	 * @param list The List to fill with newly connected devices.
	 * @param list The List of currently connected devices, which still connected devices will be removed from.
	 * @return The error number if one occurred.
	 */
	static native int nativeTopologyUpdater( LinuxUsbServices services, List connected, List disconnected );

	/**
	 * Get the current active configuration number.
	 * @param device The LinuxDeviceOsImp.
	 * @return The active configuration number.
	 */
	static native int nativeGetActiveConfigurationNumber( LinuxDeviceOsImp device );

	/**
	 * Get the current active interface setting for the specified interface.
	 * @param device The LinuxDeviceOsImp.
	 * @param interfaceNumber The interface number to check.
	 * @return The active interface number.
	 */
	static native int nativeGetActiveInterfaceSettingNumber( LinuxDeviceOsImp device, int interfaceNumber );

		//*********************************
		// JavaxUsbTopologyListener methods

	/**
	 * Call the native function that listens for topology changes
	 * @param services The LinuxUsbServices instance.
	 * @return The error that caused the listener to exit.
	 */
	static native int nativeTopologyListener( LinuxUsbServices services );

		//*********************************
		// JavaxUsbDeviceProxy methods

	/**
	 * Start a LinuxDeviceProxy
	 * @param io A LinuxInterfaceIO object
	 */
	static native void nativeDeviceProxy( LinuxDeviceProxy proxy );

		//*********************************
		// JavaxUsbError methods

	/**
	 * @param error the error number
	 * @return the message associated with the specified error number
	 */
	static private native String nativeGetErrorMessage( int error );

	//*************************************************************************
	// Creation methods

	/** @return A new UsbHubImp with max ports */
	private static UsbHubImp createUsbHubImp( String key, int maxPorts )
	{
		UsbHubImp hub = new UsbHubImp( maxPorts, null, null );

		LinuxDeviceOsImp linuxDeviceOsImp = new LinuxDeviceOsImp( hub, new LinuxDeviceProxy(key) );
		hub.setUsbDeviceOsImp( linuxDeviceOsImp );

		return hub;
	}

	/** @return A new UsbDeviceImp */
	private static UsbDeviceImp createUsbDeviceImp( String key )
	{
		UsbDeviceImp device = new UsbDeviceImp( null, null );

		LinuxDeviceOsImp linuxDeviceOsImp = new LinuxDeviceOsImp( device, new LinuxDeviceProxy(key) );
		device.setUsbDeviceOsImp( linuxDeviceOsImp );

		return device;
	}

	/** @return A new UsbConfigurationImp */
	private static UsbConfigurationImp createUsbConfigurationImp( UsbDeviceImp device,
		byte length, byte type, short totalLen,
		byte numInterfaces, byte configValue, byte configIndex, byte attributes,
		byte maxPowerNeeded )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		numInterfaces += 0;
		configValue += 0;
		configIndex += 0;
		attributes += 0;
		maxPowerNeeded += 0;

		UsbConfigurationDescriptorImp desc = new UsbConfigurationDescriptorImp( length, type, totalLen,
			numInterfaces, configValue, configIndex, attributes, maxPowerNeeded );

		UsbConfigurationImp config = new UsbConfigurationImp( device, desc );

		return config;
	}

	/** @return A new UsbInterfaceImp */
	private static UsbInterfaceImp createUsbInterfaceImp( UsbConfigurationImp config,
		byte length, byte type,
		byte interfaceNumber, byte alternateNumber, byte numEndpoints,
		byte interfaceClass, byte interfaceSubClass, byte interfaceProtocol, byte interfaceIndex )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		interfaceNumber += 0;
		alternateNumber += 0;
		numEndpoints += 0;
		interfaceClass += 0;
		interfaceSubClass += 0;
		interfaceProtocol += 0;
		interfaceIndex += 0;

		UsbInterfaceDescriptorImp desc = new UsbInterfaceDescriptorImp( length, type,
			interfaceNumber, alternateNumber, numEndpoints, interfaceClass, interfaceSubClass,
			interfaceProtocol, interfaceIndex );

		UsbInterfaceImp iface = new UsbInterfaceImp( config, desc );

		LinuxDeviceOsImp linuxDeviceOsImp = (LinuxDeviceOsImp)iface.getUsbConfigurationImp().getUsbDeviceImp().getUsbDeviceOsImp();
		LinuxInterfaceOsImp linuxInterfaceOsImp = new LinuxInterfaceOsImp( iface, linuxDeviceOsImp );
		iface.setUsbInterfaceOsImp( linuxInterfaceOsImp );

		return iface;
	}

	/** @return A new UsbEndpointImp */
	private static UsbEndpointImp createUsbEndpointImp( UsbInterfaceImp iface,
		byte length, byte type,
		byte endpointAddress, byte attributes, byte interval, short maxPacketSize )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		endpointAddress += 0;
		attributes += 0;
		interval += 0;
		maxPacketSize += 0;

		UsbEndpointDescriptorImp desc = new UsbEndpointDescriptorImp( length, type,
			endpointAddress, attributes, interval, maxPacketSize );

		UsbEndpointImp ep = new UsbEndpointImp( iface, desc );
		UsbPipeImp pipe = null;

		LinuxInterfaceOsImp linuxInterfaceOsImp = (LinuxInterfaceOsImp)iface.getUsbInterfaceOsImp();
		switch (ep.getType()) {
		case UsbConst.ENDPOINT_TYPE_CONTROL:
			pipe = new UsbControlPipeImp( ep, null );
			pipe.setUsbPipeOsImp( new LinuxControlPipeImp( (UsbControlPipeImp)pipe, linuxInterfaceOsImp ) );
			break;
		case UsbConst.ENDPOINT_TYPE_BULK:
			pipe = new UsbPipeImp( ep, null );
			pipe.setUsbPipeOsImp( new LinuxPipeOsImp( pipe, linuxInterfaceOsImp ) );
			break;
		case UsbConst.ENDPOINT_TYPE_INTERRUPT:
			pipe = new UsbPipeImp( ep, null );
			pipe.setUsbPipeOsImp( new LinuxPipeOsImp( pipe, linuxInterfaceOsImp ) );
			break;
		case UsbConst.ENDPOINT_TYPE_ISOCHRONOUS:
			pipe = new UsbPipeImp( ep, null );
			pipe.setUsbPipeOsImp( new LinuxIsochronousPipeImp( pipe, linuxInterfaceOsImp ) );
			break;
		default:
//FIXME - log?
			throw new RuntimeException("Invalid UsbEndpoint type " + ep.getType());
		}

		return ep;
	}

	//*************************************************************************
	// Setup methods

	private static void configureUsbDeviceImp( UsbDeviceImp targetDevice,
		byte length, byte type,
		byte deviceClass, byte deviceSubClass, byte deviceProtocol, byte maxDefaultEndpointSize,
		byte manufacturerIndex, byte productIndex, byte serialNumberIndex, byte numConfigs, short vendorId,
		short productId, short bcdDevice, short bcdUsb, int speed )
	{
		/* BUG - Java (IBM JVM at least) does not handle certain JNI byte -> Java byte (or shorts) */
		/* Email ddstreet@ieee.org for more info */
		length += 0;
		type += 0;
		deviceClass += 0;
		deviceSubClass += 0;
		deviceProtocol += 0;
		maxDefaultEndpointSize += 0;
		manufacturerIndex += 0;
		productIndex += 0;
		serialNumberIndex += 0;
		numConfigs += 0;
		vendorId += 0;
		productId += 0;
		bcdDevice += 0;
		bcdUsb += 0;

		UsbDeviceDescriptorImp desc = new UsbDeviceDescriptorImp( length, type,
			bcdUsb, deviceClass, deviceSubClass, deviceProtocol, maxDefaultEndpointSize, vendorId, productId,
			bcdDevice, manufacturerIndex, productIndex, serialNumberIndex, numConfigs );

		targetDevice.setUsbDeviceDescriptor(desc);

		switch (speed) {
		case SPEED_LOW:
			targetDevice.setSpeed(UsbConst.DEVICE_SPEED_LOW);
			break;
		case SPEED_FULL:
			targetDevice.setSpeed(UsbConst.DEVICE_SPEED_FULL);
			break;
		case SPEED_UNKNOWN:
			targetDevice.setSpeed(UsbConst.DEVICE_SPEED_UNKNOWN);
			break;
		default:
			/* log */
			targetDevice.setSpeed(UsbConst.DEVICE_SPEED_UNKNOWN);
			break;
		}
	}

	//*************************************************************************
	// Class variables

	private static Hashtable msgLevelTable = new Hashtable();

	//*************************************************************************
	// Class constants

	public static final String LIBRARY_NAME = "JavaxUsb";

    public static final String ERROR_WHILE_LOADING_SHARED_LIBRARY = "Error while loading shared library";
    public static final String EXCEPTION_WHILE_LOADING_SHARED_LIBRARY = "Exception while loading shared library";

	private static final int SPEED_UNKNOWN = 0;
	private static final int SPEED_LOW = 1;
	private static final int SPEED_FULL = 2;
}
