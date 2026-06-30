package javax.comm;

/**************************************************************************************************************************************************************************************************************************************************************
 * Copyright (c) 1999, 2009 IBM Corporation and others. All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and implementation
 *************************************************************************************************************************************************************************************************************************************************************/
import java.io.FileDescriptor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.soda.dk.comm.internal.Library;

/**
 * @author IBM
 */
public class CommPortIdentifier {
	/**
	 * Define polling time value.
	 */
	public static final int pollingTime = 1;

	/**
	 * Define port serial value.
	 */
	public static final int PORT_SERIAL = 1;

	/**
	 * Define port parallel value.
	 */
	public static final int PORT_PARALLEL = 2;

	static CommDriver commDriver = null;

	static boolean initialized = false;

	static Hashtable identifiers = new Hashtable(4); // initial number of
	// ports
	// static block to ensure proper initialization
	static {
		// TODO: Create a factory that would return the proper Comdriver based
		// on the property file
		// where the comdrivers are registered. (javax.comm.properties)
		// lazy initialization to avoid overhead.
		try {
			// System.loadLibrary("dkcomm"); // replace the old ibmcomm //$NON-NLS-1$
			Library.load_dkcomm();
		} catch (final UnsatisfiedLinkError e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (!initialized) {
			commDriver = new org.eclipse.soda.dk.comm.NSCommDriver();
			commDriver.initialize();
			initialized = true;
		}
	}

	/**
	 * Adds <CODE>portName</CODE> to the list of ports.
	 * 
	 * @param portName
	 *            The name of the port being added
	 * @param portType
	 *            The type of the port being added
	 * @param commDriver
	 *            The driver representing the port being added
	 * @see javax.comm.CommDriver
	 * @since CommAPI 1.1
	 */
	// TODO: This method is not done yet. CommDriver needs to be registered.
	// Also the signature is non-standard. Type needs to be added.
	public static void addPortName(final String portName, final int portType, final CommDriver commDriver) {
		// what do I do with CommDriver??
		identifiers.put(portName, new CommPortIdentifier(portName, portType));
		// name=portName;
	}

	/**
	 * Obtains the <CODE>CommPortIdentifier</CODE> object corresponding to a port that has already been opened by the application.
	 * 
	 * @param port
	 *            a CommPort object obtained from a previous open
	 * @return a CommPortIdentifier object
	 * @exception NoSuchPortException
	 *                if the port object is invalid
	 */
	public static CommPortIdentifier getPortIdentifier(final CommPort port) throws NoSuchPortException {
		return CommPortIdentifier.getPortIdentifier(port.getName());
	}

	/**
	 * Obtains a <CODE>CommPortIdentifier</CODE> object by using a port name. The port name may have been stored in persistent storage by the application.
	 * 
	 * @param portName
	 *            name of the port to open
	 * @return a <CODE>CommPortIdentifier</CODE> object
	 * @exception NoSuchPortException
	 *                if the port does not exist
	 */
	public static CommPortIdentifier getPortIdentifier(final String portName) throws NoSuchPortException {
		final Object comPortId = identifiers.get(portName);
		if (comPortId == null) {
			throw new NoSuchPortException();
		}
		return (CommPortIdentifier) comPortId;
	}

	/**
	 * Obtains an enumeration object that contains a <CODE>CommPortIdentifier</CODE> object for each port in the system.
	 * 
	 * @return an <CODE> Enumeration </CODE> object that can be used to enumerate all the ports known to the system
	 * @see java.util.Enumeration
	 */
	public static Enumeration getPortIdentifiers() {
		return identifiers.elements();
	}

	String name = null;

	String currentOwner = null;

	boolean currentlyOwned = false;

	private List listeners;

	CommPort commPort = null;

	int type = 0;

	private CommPortIdentifier(final String id, final int type) {
		this.name = id;
		this.type = type;
		this.listeners = new Vector();
	}

	/**
	 * Registers an interested application so that it can receive notification of changes in port ownership. This includes notification of the following events:
	 * <UL>
	 * <LI> <CODE>PORT_OWNED</CODE>: Port became owned
	 * <LI> <CODE>PORT_UNOWNED</CODE>: Port became unowned
	 * <LI> <CODE>PORT_OWNERSHIP_REQUESTED</CODE>: If the application owns this port and is willing to give up ownership, then it should call <CODE>close</CODE> now.
	 * </UL>
	 * The <CODE>ownershipChange</CODE> method of the listener registered using <CODE>addPortOwnershipListener</CODE> will be called with one of the above events.
	 * 
	 * @param listener
	 *            a <CODE>CommPortOwnershipListener</CODE> callback object
	 */
	public void addPortOwnershipListener(final CommPortOwnershipListener listener) {
		if ((listener != null) && !this.listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}

	/**
	 * Non-Api method. Called when close() method is called on CommPort object. Required to set port owner to null and propagate CommPortOwnership event.
	 */
	public synchronized void closePort() {
		this.currentlyOwned = false;
		this.currentOwner = null;
		this.commPort = null;
		fireOwnershipEvent(CommPortOwnershipListener.PORT_UNOWNED);
	}

	/**
	 * This method needs to be called when ownership of the port changes.
	 */
	void fireOwnershipEvent(final int eventType) {
		for (final Iterator eventListeners = this.listeners.iterator(); eventListeners.hasNext();) {
			final CommPortOwnershipListener listener = (CommPortOwnershipListener) eventListeners.next();
			listener.ownershipChange(eventType);
		}
	}

	/**
	 * Returns the owner of the port.
	 * 
	 * @return current owner of the port.
	 */
	public String getCurrentOwner() {
		return this.currentOwner;
		// TODO: Native code needs to check the owner of the port
	}

	/**
	 * Returns the name of the port.
	 * 
	 * @return the name of the port
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the port type.
	 * 
	 * @return portType - PORT_SERIAL or PORT_PARALLEL
	 */
	public int getPortType() {
		return this.type; // CommPortIdentifier.PORT_SERIAL, PORT_PARALLEL is
		// not implemented;
	}

	/**
	 * Non-Api method. Called when close() method is called on CommPort object. Required to set port owner to null and propagate CommPortOwnership event.
	 */
	synchronized void internalClosePort() {
		closePort();
	}

	/**
	 * Checks whether the port is owned.
	 * 
	 * @return boolean <CODE>true</CODE> if the port is owned by some application, <CODE>false</CODE> if the port is not owned.
	 */
	public boolean isCurrentlyOwned() {
		return this.currentlyOwned;
		// TODO: Native code needs to check the owner of port
	}

	/**
	 * Opens the communications port using a <CODE>FileDescriptor</CODE> object on platforms that support this technique.
	 * 
	 * @param fileDescriptor
	 *            The <CODE>FileDescriptor</CODE> object used to build a <CODE>CommPort</CODE>.
	 * @return a <CODE>CommPort</CODE> object.
	 * @exception UnsupportedCommOperationException
	 *                is thrown on platforms which do not support this functionality.
	 */
	public CommPort open(final FileDescriptor fileDescriptor) throws UnsupportedCommOperationException {
		throw new UnsupportedCommOperationException("Opening port with FileDescriptor is not supported!");
	}

	/**
	 * @param appName
	 * @param timeout
	 * @return comport
	 * @throws PortInUseException
	 */
	public synchronized CommPort open(final String appName, final int timeout) throws PortInUseException {
		if (isCurrentlyOwned()) {
			final PortInUseException piux = new PortInUseException(appName);
			piux.currentOwner = getCurrentOwner();
			throw piux;
		}
		this.currentOwner = appName;
		this.currentlyOwned = true;
		fireOwnershipEvent(CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED);
		return commDriver.getCommPort(getName(), getPortType());
	}

	/**
	 * Deregisters a <CODE>CommPortOwnershipListener</CODE> registered using <CODE>addPortOwnershipListener</CODE>
	 * 
	 * @param listener
	 *            The CommPortOwnershipListener object that was previously registered using addPortOwnershipListener
	 */
	public void removePortOwnershipListener(final CommPortOwnershipListener listener) {
		if (listener != null) {
			this.listeners.remove(listener);
		}
	}
}
