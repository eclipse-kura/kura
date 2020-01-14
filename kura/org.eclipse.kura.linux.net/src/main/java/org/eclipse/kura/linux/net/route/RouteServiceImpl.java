/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
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
    private static final String ROUTE = "route";
    private static final String INADDR_ANY = "0.0.0.0";
    private static final String LOCALHOST = "127.0.0.1";

    private CommandExecutorService executorService;

    public RouteServiceImpl(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void addStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface, int metric)
            throws KuraException {
        RouteConfig tmpRoute = null;

        String[] commandLine = formRouteAddCommand(destination, gateway, netmask, iface, metric);
        Command command = new Command(commandLine);
        command.setTimeout(60);
        CommandStatus status = this.executorService.execute(command);
        int exitValue = status.getExitStatus();
        if (exitValue != 0) {
            if (logger.isErrorEnabled()) {
                logger.error("Error adding static Route: {}", String.join(" ", commandLine));
            }
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", commandLine), exitValue);
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

    private String[] formRouteAddCommand(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface,
            int metric) {
        List<String> command = new ArrayList<>();
        command.add(ROUTE);
        command.add("add");
        command.add("-net");
        command.add(destination.getHostAddress());
        if (netmask != null) {
            command.add("netmask");
            command.add(netmask.getHostAddress());
        }
        if ((gateway != null) && (gateway.getHostAddress().compareTo(INADDR_ANY) != 0)
                && (gateway.getHostAddress().compareTo(LOCALHOST) != 0)) {
            command.add("gw");
            command.add(gateway.getHostAddress());
        }
        if (iface != null) {
            command.add("dev");
            command.add(iface);
        }
        if (metric != 0 && metric != -1) {
            command.add("metric");
            command.add(Integer.toString(metric));
        }
        return command.toArray(new String[0]);
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
        RouteConfig[] routes = new RouteConfig[0];
        String[] commandLine = { ROUTE, "-n" };
        Command command = new Command(commandLine);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if (status.getExitStatus() != 0) {
            if (logger.isErrorEnabled()) {
                logger.warn(FAILED_TO_EXECUTE_MSG, String.join(" ", commandLine));
            }
        } else {
            routes = parseGetRoutes(
                    new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        }
        return routes;
    }

    private RouteConfig[] parseGetRoutes(String commandOutput) {
        RouteConfig tmpRoute = null;
        RouteConfig[] routes = null;
        ArrayList<RouteConfig> routeList = new ArrayList<>();
        String[] lines = commandOutput.split("\n");
        for (int i = 2; i < lines.length; i++) {
            tmpRoute = entryToRoute(lines[i]);
            if (tmpRoute != null) {
                routeList.add(tmpRoute);
            }
        }
        routes = new RouteConfig[routeList.size()];
        return routeList.toArray(routes);
    }

    @Override
    public void removeStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface)
            throws KuraException {
        RouteConfig tmpRoute = null;

        String[] commandLine = formRouteDeleteCommand(destination, gateway, netmask, iface);
        Command command = new Command(commandLine);
        command.setTimeout(60);
        CommandStatus status = this.executorService.execute(command);
        int exitValue = status.getExitStatus();
        if (exitValue != 0) {
            if (logger.isErrorEnabled()) {
                logger.error("Error removing static route: {}", String.join(" ", commandLine));
            }
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", commandLine), exitValue);
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

    private String[] formRouteDeleteCommand(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface) {
        List<String> command = new ArrayList<>();
        command.add(ROUTE);
        command.add("del");
        command.add("-net");
        command.add(destination.getHostAddress());
        if (netmask != null) {
            command.add("netmask");
            command.add(netmask.getHostAddress());
        }
        if ((gateway != null) && (gateway.getHostAddress().compareTo(LOCALHOST) != 0)) {
            command.add("gw");
            command.add(gateway.getHostAddress());
        }
        if (iface != null) {
            command.add("dev");
            command.add(iface);
        }
        return command.toArray(new String[0]);
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
