/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.route;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.route.RouteConfigIP4;
import org.eclipse.kura.net.route.RouteConfigIP6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteServiceImpl implements RouteService {

	private static final Logger logger = LoggerFactory.getLogger(RouteServiceImpl.class);

	private static final String FAILED_TO_EXECUTE_MSG = "Failed to execute {} ";

	private static final String INADDR_ANY = "0.0.0.0";
	private static final String LOCALHOST = "127.0.0.1";

	private static RouteServiceImpl routeService = null;

	public static synchronized RouteService getInstance() {
		if (routeService == null) {
			routeService = new RouteServiceImpl();
		}
		return routeService;
	}

	@Override
	public void addStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface, int metric)
			throws KuraException {
		RouteConfig tmpRoute = null;

		String command = formRouteAddCommand(destination, gateway, netmask, iface, metric);
		SafeProcess proc = null;
		try {
			logger.debug("Executing command:  {}", command);
			proc = ProcessUtil.exec(command);
			proc.waitFor();
			if (proc.exitValue() != 0) {
				logger.error("Error adding static Route: {}", command);
				throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, command, proc.exitValue());
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, "Error executing 'route add' command");
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}

		if (destination instanceof IP4Address) {
			tmpRoute = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, iface,
					-1);
		} else if (destination instanceof IP6Address) {
			tmpRoute = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, iface,
					-1);
		}
		if (tmpRoute != null) {
			logger.info("Static route added successfully");
			logger.debug(tmpRoute.getDescription());
		}
	}

	private String formRouteAddCommand(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface,
			int metric) {
		StringBuilder command = new StringBuilder();
		command.append("route add -net ").append(destination.getHostAddress());
		if (netmask != null) {
			command.append(" netmask ").append(netmask.getHostAddress());
		}
		if ((gateway != null) && (gateway.getHostAddress().compareTo(INADDR_ANY) != 0)
				&& (gateway.getHostAddress().compareTo(LOCALHOST) != 0)) {
			command.append(" gw ").append(gateway.getHostAddress());
		}
		if (iface != null) {
			command.append(" dev ").append(iface);
		}
		if (metric != 0 && metric != -1) {
			command.append(" metric ").append(metric);
		}
		return command.toString();
	}

	@Override
	public RouteConfig getDefaultRoute(String iface) {
		RouteConfig[] routes = getRoutes();
		RouteConfig defaultRoute;
		ArrayList<RouteConfig> defaultRoutes = new ArrayList<>();

		// Search through routes and construct a list of all default routes for
		// the specified interface
		for (RouteConfig route : routes) {
			if (route.getInterfaceName().compareTo(iface) == 0
					&& route.getDestination().getHostAddress().compareTo(INADDR_ANY) == 0) {
				defaultRoutes.add(route);
			}
		}

		// If no default routes exist, return null
		if (defaultRoutes.isEmpty()) {
			logger.debug("No default routes exist for inteface: {}", iface);
			return null;
		}

		// Set the default route to the first one in the list
		defaultRoute = defaultRoutes.get(0);

		// Search for the default route with the lowest metric value
		for (int i = 1; i < defaultRoutes.size(); i++) {
			if (defaultRoute.getMetric() > defaultRoutes.get(i).getMetric()) {
				defaultRoute = defaultRoutes.get(i);
			}
		}

		logger.info("Default route found for interface: {}", iface);
		logger.debug("Default route:\n{}", defaultRoute.getDescription());
		return defaultRoute;
	}

	@Override
	public RouteConfig[] getRoutes() {
		RouteConfig[] routes = null;
		SafeProcess proc = null;
		String cmd = "route -n";
		try {
			proc = ProcessUtil.exec(cmd);
			if (proc.waitFor() != 0) {
				logger.warn(FAILED_TO_EXECUTE_MSG, cmd);
				ProcessUtil.destroy(proc);
				return new RouteConfig[0];
			}
			routes = parseGetRoutes(proc);
		} catch (Exception e) {
			logger.warn(FAILED_TO_EXECUTE_MSG, cmd, e);
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
			return new RouteConfig[0];
		}
		return routes;
	}

	private RouteConfig[] parseGetRoutes(SafeProcess proc) throws KuraException {
		String routeEntry = null;
		RouteConfig tmpRoute = null;
		RouteConfig[] routes = null;
		ArrayList<RouteConfig> routeList = new ArrayList<>();
		try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
				BufferedReader br = new BufferedReader(isr)) {
			routeEntry = br.readLine();
			routeEntry = br.readLine();
			while ((routeEntry = br.readLine()) != null) {
				tmpRoute = entryToRoute(routeEntry);
				if (tmpRoute != null) {
					routeList.add(tmpRoute);
				}
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
		}
		routes = new RouteConfig[routeList.size()];
		for (int i = 0; i < routes.length; i++) {
			routes[i] = routeList.get(i);
		}
		return routes;
	}

	@Override
	public void removeStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface)
			throws KuraException {
		RouteConfig tmpRoute = null;

		SafeProcess proc = null;
		String command = formRouteDeleteCommand(destination, gateway, netmask, iface);
		try {
			logger.debug("Executing command: {}", command);
			proc = ProcessUtil.exec(command);
			proc.waitFor();
			if (proc.exitValue() != 0) {
				logger.error("Error removing static route: {}", command);
				throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, command, proc.exitValue());
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, "Error executing 'route del' command");
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}

		if (destination instanceof IP4Address) {
			tmpRoute = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, iface,
					-1);
		} else if (destination instanceof IP6Address) {
			tmpRoute = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, iface,
					-1);
		}
		if (tmpRoute != null) {
			logger.info("Static route removed successfully");
			logger.debug(tmpRoute.getDescription());
		}
	}

	private String formRouteDeleteCommand(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface) {
		StringBuilder command = new StringBuilder();
		command.append("route del -net ").append(destination.getHostAddress());
		if (netmask != null) {
			command.append(" netmask ").append(netmask.getHostAddress());
		}
		if ((gateway != null) && (gateway.getHostAddress().compareTo(LOCALHOST) != 0)) {
			command.append(" gw ").append(gateway.getHostAddress());
		}
		if (iface != null) {
			command.append(" dev ").append(iface);
		}
		return command.toString();
	}

	private RouteConfig entryToRoute(String entry) {
		RouteConfig route;
		IPAddress destination;
		IPAddress gateway;
		IPAddress netmask;
		int metric;
		String iface;
		String tmp;

		try {
			route = null;
			StringTokenizer tok = new StringTokenizer(entry, " ");
			tmp = tok.nextToken();
			destination = IPAddress.parseHostAddress(tmp);
			gateway = IPAddress.parseHostAddress(tok.nextToken());
			netmask = IPAddress.parseHostAddress(tok.nextToken());
			tok.nextToken();
			metric = Integer.parseInt(tok.nextToken());
			tok.nextToken();
			tok.nextToken();
			iface = tok.nextToken();
		} catch (Exception e) {
			logger.error("Error parsing route table entry: ", e);
			return null;
		}

		if (destination instanceof IP4Address) {
			route = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, iface,
					metric);
		} else if (destination instanceof IP6Address) {
			route = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, iface,
					metric);
		}
		logger.trace("Route successfully read from route table entry");
		return route;
	}

	@Override
	public String getDefaultInterface(IPAddress destination) {
		ArrayList<RouteConfig> matches = new ArrayList<>();
		RouteConfig[] routes = getRoutes();
		for (RouteConfig route : routes) {
			if (matchesRoute(destination, route)) {
				matches.add(route);
			}
		}
		if (!matches.isEmpty()) {
			RouteConfig dRoute = matches.get(0);
			for (RouteConfig route : routes) {
				if (dRoute.getMetric() > route.getMetric()) {
					dRoute = route;
				}
			}
			logger.debug("Found defualt interface {} for destination {}", dRoute.getInterfaceName(),
					destination.getHostAddress());
			return dRoute.getInterfaceName();
		}
		logger.debug("No default interface exists for destination {}", destination.getHostAddress());
		return null;
	}

	private boolean matchesRoute(IPAddress destination, RouteConfig route) {
		byte mask = (byte) 0xFF;
		byte[] dest = destination.getAddress();
		byte[] routeMask = route.getNetmask().getAddress();
		byte[] routeDest = route.getDestination().getAddress();
		byte[] destMasked = new byte[4];
		for (int i = 0; i < 4; i++) {
			if (routeMask[i] == mask) {
				destMasked[i] = dest[i];
			} else {
				destMasked[i] = 0;
			}
		}
		for (int i = 0; i < 4; i++) {
			if (destMasked[i] != routeDest[i]) {
				return false;
			}
		}
		return true;
	}
}
