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
package org.eclipse.kura.internal.wire;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.Preconditions;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireService;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * Provides Gogo Shell commands to create, delete Wire Configurations and list
 * the available ones
 */
public final class WireServiceCommandProvider implements CommandProvider {

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The Wire Service. */
	private volatile WireService m_wireService;

	/**
	 * The command {@code createConfig} creates a Wire Configuration between the
	 * provided emitter PID and receiver PID
	 *
	 * @param ci
	 *            the console interpreter
	 */
	public void _createConfig(final CommandInterpreter ci) {
		final String argument = ci.nextArgument();
		if (argument == null) {
			ci.println("Usage: createConfig <emitterPid>----<receiverPid>");
		}
		final List<String> pids = this.getPids(argument);
		if (pids.isEmpty() || (pids.size() != 2)) {
			ci.println("The format is wrongly provided");
			ci.println("Usage: createConfig <emitterPid>----<receiverPid>");
		}
		final String emitterPid = pids.get(0);
		final String receiverPid = pids.get(1);
		try {
			this.m_wireService.createWireConfiguration(emitterPid, receiverPid);
		} catch (final KuraException e) {
			ci.println("Exception occurred ==> " + ThrowableUtil.stackTraceAsString(e));
		}
	}

	/**
	 * The command {@code deleteConfig} delete existing Wire Configuration
	 * between the provided emitter PID and receiver PID
	 *
	 * @param ci
	 *            the console interpreter
	 */
	public void _deleteConfig(final CommandInterpreter ci) {
		final String argument = ci.nextArgument();
		if (argument == null) {
			ci.println("Usage: deleteConfig <emitterPid>----<receiverPid>");
		}
		final List<String> pids = this.getPids(argument);
		if (pids.isEmpty() || (pids.size() != 2)) {
			ci.println("The format is wrongly provided");
			ci.println("Usage: deleteConfig <emitterPid>----<receiverPid>");
		}
		final String emitterPid = pids.get(0);
		final String receiverPid = pids.get(1);
		for (final WireConfiguration configuration : this.m_wireService.getWireConfigurations()) {
			if (configuration.getEmitterPid().equals(emitterPid)
					&& configuration.getReceiverPid().equals(receiverPid)) {
				this.m_wireService.deleteWireConfiguration(configuration);
				return;
			}
		}
	}

	/**
	 * The command {@code listConfigs} lists all the available Wire
	 * Configurations
	 *
	 * @param ci
	 *            the console interpreter
	 */
	public void _listConfigs(final CommandInterpreter ci) {
		ci.println("=================== Wire Configurations ===================");
		final List<WireConfiguration> configs = this.m_wireService.getWireConfigurations();
		int i = 0;
		for (final WireConfiguration config : configs) {
			ci.println(new StringBuilder().append(i++).append(".").append(" ").append("Emitter PID ===>").append(" ")
					.append(config.getEmitterPid()).append("  ").append("Receiver PID ===>").append(" ")
					.append(config.getReceiverPid()).toString());
		}
		ci.println("=================== =================== ===================");
	}

	/**
	 * Binds the Wire Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireService(final WireService wireHelperService) {
		if (this.m_wireService == null) {
			this.m_wireService = wireHelperService;
		}
	}

	/** Shows the Help Option for this command */
	@Override
	public String getHelp() {
		return "---Wire Service---\n"
				+ "\tcreateConfig <emitterPid>----<receiverPid> - Creates a Wire Configuration between the provided emitter and receiver\n"
				+ "\tlistConfigs - list all created Wire Configurations\n"
				+ "\tdeleteConfig <emitterPid>----<receiverPid> - Deletes the already created Wire Configuration between the provided emitter and receiver\n";
	}

	/**
	 * Retrieves the emitter and receiver PIDs from the provided param
	 *
	 * @param param
	 *            the param in the format of {@code emitterPid----receiverPid}
	 * @return the list containing the emitter and receiver PID
	 */
	private List<String> getPids(final String param) {
		Preconditions.checkNull(param, s_message.stringNonNull());
		final String delimiter = "----";
		return Arrays.asList(param.split(delimiter));
	}

	/**
	 * Unbinds the Wire Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireService wireHelperService) {
		if (this.m_wireService == wireHelperService) {
			this.m_wireService = null;
		}
	}
}
