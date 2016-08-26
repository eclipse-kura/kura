/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * Provides Gogo Shell commands to add channel and create a factory Wire Asset
 * instance
 */
public final class AssetCommandProvider implements CommandProvider {

	/** Channel Identifier */
	private static int i = 0;

	/** The Configuration Service. */
	private volatile ConfigurationService m_configService;

	/**
	 * The command {@code addChannel} creates a channel and associates it with
	 * the provided asset name
	 *
	 * @param ci
	 *            the console interpreter
	 * @throws KuraException
	 *             if configuration service encounters any exception condition
	 *             while updating
	 */
	public void _addChannel(final CommandInterpreter ci) throws KuraException {
		final String argument = ci.nextArgument();
		if (argument == null) {
			ci.println("Usage: addChannel <assetPid>#<channelName>#<type>#<valueType>#<unit.id> - Creates a Channel");
			return;
		}
		final List<String> prop = Arrays.asList(argument.split("#"));
		final Map<String, Object> map = CollectionUtil.newHashMap();
		map.put(++i + ".CH.name", prop.get(1));
		map.put(i + ".CH.type", prop.get(2));
		map.put(i + ".CH.value.type", prop.get(3));
		map.put(i + ".CH.DRIVER.unit.id", Integer.valueOf(prop.get(4)));
		this.m_configService.updateConfiguration(prop.get(0), map);
	}

	/**
	 * The command {@code addModbusChannel} creates a Modbus channel and
	 * associates it with the provided asset name
	 *
	 * @param ci
	 *            the console interpreter
	 * @throws KuraException
	 *             if configuration service encounters any exception condition
	 *             while updating
	 */
	public void _addModbusChannel(final CommandInterpreter ci) throws KuraException {
		final String argument = ci.nextArgument();
		if (argument == null) {
			ci.println(
					"Usage: addModbusChannel <assetPid>#<channelName>#<type>#<valueType>#<unit.id>#<memory.address>#<primary.table> - Creates a Modbus Channel");
			return;
		}
		final List<String> prop = Arrays.asList(argument.split("#"));
		final Map<String, Object> map = CollectionUtil.newHashMap();
		map.put(++i + ".CH.name", prop.get(1));
		map.put(i + ".CH.type", prop.get(2));
		map.put(i + ".CH.value.type", prop.get(3));
		map.put(i + ".CH.DRIVER.unit.id", Integer.valueOf(prop.get(4)));
		map.put(i + ".CH.DRIVER.memory.address", Integer.valueOf(prop.get(5)));
		map.put(i + ".CH.DRIVER.primary.table", Integer.valueOf(prop.get(6)));
		this.m_configService.updateConfiguration(prop.get(0), map);
	}

	/**
	 * The command {@code createWireAsset} creates a Wire Asset
	 *
	 * @param ci
	 *            the console interpreter
	 * @throws KuraException
	 *             if configuration service encounters any exceptional condition
	 *             while creating factory configuration
	 */
	public void _createWireAsset(final CommandInterpreter ci) throws KuraException {
		final String argument = ci.nextArgument();
		if (argument == null) {
			ci.println(
					"Usage: createWireAsset <assetFactoryPid>#<assetPid>#<assetDescription>#<driverPid> - Creates a Wire Asset");
			return;
		}
		final List<String> prop = Arrays.asList(argument.split("#"));
		final Map<String, Object> map = CollectionUtil.newHashMap();
		map.put("asset.desc", prop.get(2));
		map.put("driver.pid", prop.get(3));
		this.m_configService.createFactoryConfiguration(prop.get(0), prop.get(1), map, false);
	}

	/**
	 * Binds the Wire Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindConfigurationService(final ConfigurationService wireHelperService) {
		if (this.m_configService == null) {
			this.m_configService = wireHelperService;
		}
	}

	/** Shows the Help Option for this command */
	@Override
	public String getHelp() {
		return "---Wire Asset Service---\n"
				+ "\tcreateWireAsset <assetFactoryPid>#<assetPid>#<assetDescription>#<driverPid> - Creates a Wire Asset\n"
				+ "\taddChannel <assetPid>#<channelName>#<type>#<valueType> - Creates a Channel\n"
				+ "\taddModbusChannel <assetPid>#<channelName>#<type>#<valueType>#<unit.id>#<memory.address>#<primary.table> - Creates a Modbus Channel\n";
	}

	/**
	 * Unbinds the Wire Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindConfigurationService(final ConfigurationService wireHelperService) {
		if (this.m_configService == wireHelperService) {
			this.m_configService = null;
		}
	}
}
