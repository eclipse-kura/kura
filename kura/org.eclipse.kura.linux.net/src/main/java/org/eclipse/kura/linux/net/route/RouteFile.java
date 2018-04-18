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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.route.RouteConfigIP4;
import org.eclipse.kura.net.route.RouteConfigIP6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteFile {

    private static final Logger logger = LoggerFactory.getLogger(RouteFile.class);

    private static final String OS_ROUTE_CONFIG_DIR = "/etc/sysconfig/network-scripts/";
    private final String interfaceName;
    private final File file;
    private ArrayList<RouteConfig> routes;

    public RouteFile(String interfaceName) {
        this.interfaceName = interfaceName;
        this.routes = new ArrayList<>();
        this.file = new File(OS_ROUTE_CONFIG_DIR + "route-" + this.interfaceName);
        if (this.file.exists()) {
            readFile();
        } else {
            createFile();
        }
    }

    private void readFile() {
        int i = 0;
        RouteConfig newRoute = null;
        Properties routeProps = new Properties();
        try (FileInputStream in = new FileInputStream(this.file)) {
            routeProps.load(in);
        } catch (FileNotFoundException e) {
            logger.warn("File not found", e);
        } catch (IOException e) {
            logger.warn("Exception while reading file", e);
        }

        newRoute = findRoute(routeProps, i);
        while (newRoute != null) {
            this.routes.add(newRoute);
            i++;
            newRoute = findRoute(routeProps, i);
        }
    }

    private RouteConfig findRoute(Properties props, int index) {
        RouteConfig route = null;
        IPAddress dest = null;
        IPAddress gw = null;
        IPAddress mask = null;
        if (!props.containsKey("ADDRESS" + index)) {
            return null;
        }
        try {
            dest = IPAddress.parseHostAddress((String) props.get("ADDRESS" + index));
            gw = IPAddress.parseHostAddress((String) props.get("GATEWAY" + index));
            mask = IPAddress.parseHostAddress((String) props.get("NETMASK" + index));
        } catch (UnknownHostException e) {
            logger.error("findRoute() :: failed to parse host address ", e);
        }
        if (dest != null && gw != null && mask != null) {
            if (dest instanceof IP4Address) {
                route = new RouteConfigIP4((IP4Address) dest, (IP4Address) gw, (IP4Address) mask, this.interfaceName,
                        -1);
            } else if (dest instanceof IP6Address) {
                route = new RouteConfigIP6((IP6Address) dest, (IP6Address) gw, (IP6Address) mask, this.interfaceName,
                        -1);
            }
        }
        return route;
    }

    public boolean addRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String ifaceName) {
        RouteConfig route = null;
        if (destination instanceof IP4Address) {
            route = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, ifaceName,
                    -1);
        } else if (destination instanceof IP6Address) {
            route = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, ifaceName,
                    -1);
        }
        if (route == null || routeIndex(route) != -1) {
            return false;
        }
        this.routes.add(route);
        storeFile();
        return true;
    }

    public boolean removeRoute(IPAddress destination, IPAddress gateway, IPAddress netmask) {
        RouteConfig route = null;
        if (destination instanceof IP4Address) {
            route = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask,
                    this.interfaceName, -1);
        } else if (destination instanceof IP6Address) {
            route = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask,
                    this.interfaceName, -1);
        }

        if (route != null) {
            int index = routeIndex(route);
            if (index != -1) {
                this.routes.remove(index);
                storeFile();
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean createFile() {
        boolean ret = true;
        try {
            ret = this.file.createNewFile();
        } catch (IOException e) {
            logger.warn("Exception while creating file", e);
        }
        return ret;
    }

    public RouteConfig[] getRoutes() {
        if (this.routes.isEmpty()) {
            return new RouteConfig[0];
        }
        RouteConfig[] result = new RouteConfig[this.routes.size()];
        for (int i = 0; i < this.routes.size(); i++) {
            result[i] = this.routes.get(i);
        }
        return result;
    }

    private void storeFile() {
        Properties props = new Properties();
        RouteConfig route = null;
        for (int i = 0; i < this.routes.size(); i++) {
            route = this.routes.get(i);
            props.put("ADDRESS" + i, route.getDestination().getHostAddress());
            props.put("GATEWAY" + i, route.getGateway().getHostAddress());
            props.put("NETMASK" + i, route.getNetmask().getHostAddress());
        }
        try (FileOutputStream out = new FileOutputStream(this.file)) {
            props.store(out, "Persistent routes for interface:  " + this.interfaceName);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            logger.warn("File not found", e);
        } catch (IOException e) {
            logger.warn("Exception while reading file", e);
        }
    }

    private int routeIndex(RouteConfig route) {
        for (int i = 0; i < this.routes.size(); i++) {
            RouteConfig tmp = this.routes.get(i);
            if (route.equals(tmp)) {
                return i;
            }
        }
        return -1;
    }

    public void cleanFile() {
        this.routes = new ArrayList<>();
    }

}
