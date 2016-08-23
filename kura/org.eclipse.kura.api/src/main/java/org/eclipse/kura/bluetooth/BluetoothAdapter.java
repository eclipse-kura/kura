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
package org.eclipse.kura.bluetooth;

import org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementScanListener;

/**
 * BluetoothAdapter represents the physical Bluetooth adapter on the host machine (ex: hci0).
 *
 */
public interface BluetoothAdapter {

	/**
	 * Get the MAC address of the Bluetooth adapter.
	 * 
	 * @return The MAC address of the adapter
	 */
	public String getAddress();
	
	/**
	 * Kill the process started by startLeScan or startBeaconScan.<br>
	 * SIGINT must be sent to the hcitool process. Otherwise the adapter must be toggled (down/up).
	 * 
	 */
	public void killLeScan();
	
	/**
	 * Return true if a lescan is running
	 * 
	 */
	public boolean isScanning();

	/**
	 * Return the status of the adapter
	 * 
	 * @return true if adapter is enabled, false otherwise
	 */
	public boolean isEnabled();
	
	/**
	 * Return true if the adapter supports Bluetooth LE.
	 * 
	 * @return	true if the adapter supports Bluetooth LE, false otherwise
	 */
	public boolean isLeReady();
	
	/**
	 * Enable the Bluetooth adapter
	 */
	public void enable();
	
	/**
	 * Disable the Bluetooth adapter
	 */
	public void disable();
	
	/**
	 * Starts an asynchronous scan for Bluetooth LE devices. Results are
	 * relayed through the {@link BluetoothLeScanListener} when the scan
	 * is complete.
	 * 
	 * @param listener	  Interface for collecting scan results
	 */
	public void startLeScan(BluetoothLeScanListener listener);
	
	/**
	* Starts an asynchronous scan for Bluetooth LE advertisements. Advertisement
	* data is relayed through the {@link org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementScanListener} as it arrives.
	*
	* @param companyName Hexadecimal string representing the company code
	* @param listener Interface for collecting beacon data.
	* 
	* @since {@link org.eclipse.kura.bluetooth} 1.3.0
	* 
	*/
	public void startAdvertisementScan(String companyName, BluetoothAdvertisementScanListener listener);

	/**
	* Starts an asynchronous scan for Bluetooth LE beacons. Beacon data is
	* relayed through the {@link BluetoothBeaconScanListener} as it arrives.
	*
	* @param companyName Hexadecimal string representing the company code
	* @param listener Interface for collecting beacon data.
	*/
	void startBeaconScan(String companyName, BluetoothBeaconScanListener listener);
	
	
	/**
	 * Get a remote Bluetooth device based on hardware adress
	 * 
	 * @param address	Hardware address of remote device
	 * @return BluetoothDevice
	 */
	public BluetoothDevice getRemoteDevice(String address);
	
	/**
	 * Start Beacon advertising for the given interface.
	 * 
	 */
	public void startBeaconAdvertising();
	
	/**
	 * Stop Beacon advertising for the given interface.
	 * 
	 */
	public void stopBeaconAdvertising();

	/**
	 * Set the Beacon advertising interval for the given interface.
	 * 
	 * @param min 	Minimum time interval between advertises
	 * @param max 	Maximum time interval between advertises

	 */
	public void setBeaconAdvertisingInterval(Integer min, Integer max);
	
	/**
	 * Set the data in to the Beacon advertising packet for the given interface.
	 * 
	 * @param uuid            Proximity UUID
	 * @param major           Groups beacons with the same proximity UUID
	 * @param minor           Differentiates beacons with the same proximity UUID and major value
	 * @param txPower         Transmitting power \@1m
	 * @param companyCode     Indicates the manufacturer
	 * @param LELimited       Indicates LE Limited Discoverable Mode (the device advertises for 30.72s and then stops)
	 * @param LEGeneral       Indicates LE General Discoverable Mode (the device advertises indefinitely)
	 * @param BR_EDRSupported Indicates whether BR/EDR is supported
	 * @param LE_BRController Indicates whether LE and BR/EDR Controller operates simultaneously
	 * @param LE_BRHost       Indicates whether LE and BR/EDR Host operates simultaneously
	 * 
	 */
	public void setBeaconAdvertisingData(String uuid, Integer major, Integer minor, String companyCode, Integer txPower, boolean LELimited, boolean LEGeneral,
			boolean BR_EDRSupported, boolean LE_BRController, boolean LE_BRHost);
	
	/**
	 * Execute a command to the given interface.
	 * 
	 * @param ogf       OpCode Group Field
	 * @param ocf       OpCode Command Field
	 * @param parameter Parameters passed to the command
	 * 
	 */
	public void ExecuteCmd(String ogf, String ocf, String parameter);
	
}
