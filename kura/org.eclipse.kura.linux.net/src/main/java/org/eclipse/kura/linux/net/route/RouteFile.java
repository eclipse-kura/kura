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
	private static final Logger s_logger = LoggerFactory.getLogger(RouteFile.class);
	
	private String osRouteConfigDirectory = "/etc/sysconfig/network-scripts/";
	private String interfaceName;
	private File file;
	private ArrayList<RouteConfig> routes;
	
	
	public RouteFile(String interfaceName) {
		this.interfaceName = interfaceName;
		routes = new ArrayList<RouteConfig>();
		file = new File(osRouteConfigDirectory + "route-" + this.interfaceName);
		if(file.exists()) {
			readFile();
		} else {
			createFile();
		}
	}
	
	private void readFile() {
		int i = 0;
		RouteConfig newRoute = null;
		FileInputStream in = null;
		Properties routeProps = new Properties();
		try {
			in = new FileInputStream(file);
			routeProps.load(in);
		} catch (FileNotFoundException e) {
			s_logger.warn("File not found", e);
		} catch (IOException e) {
			s_logger.warn("Exception while reading file", e);
		} finally{
			if(in != null){
				try{
					in.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}	
		}

		newRoute = findRoute(routeProps, i);
		while(newRoute != null) {
			routes.add(newRoute);
			i++;
			newRoute = findRoute(routeProps, i);
		}
	}
	
	private RouteConfig findRoute(Properties props, int index) {
		RouteConfig route = null;
		IPAddress dest = null;
		IPAddress gw = null;
		IPAddress mask = null;
		if(!props.containsKey("ADDRESS" + index)) {
			return null;
		}
		try {
			dest = IPAddress.parseHostAddress((String)props.get("ADDRESS" + index));
			gw = IPAddress.parseHostAddress((String)props.get("GATEWAY" + index));
			mask = IPAddress.parseHostAddress((String)props.get("NETMASK" + index));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if(dest != null && gw != null && mask != null) {
			if(dest instanceof IP4Address) {
				route = new RouteConfigIP4((IP4Address) dest, (IP4Address) gw, (IP4Address) mask, interfaceName, -1);
			} else if(dest instanceof IP6Address) {
				route = new RouteConfigIP6((IP6Address) dest, (IP6Address) gw, (IP6Address) mask, interfaceName, -1);
			}
		}
		return route;
	}
	
	public boolean addRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String ifaceName) {
		RouteConfig route = null;
		if(destination instanceof IP4Address) {
			route = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, ifaceName, -1);
		} else if(destination instanceof IP6Address) {
			route = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, ifaceName, -1);
		}
		if(routeIndex(route) != -1) {
			return false;
		}
		routes.add(route);
		storeFile();
		return true;
	}
	
	public boolean removeRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface) {
		RouteConfig route = null;
		if(destination instanceof IP4Address) {
			route = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, interfaceName, -1);
		} else if(destination instanceof IP6Address) {
			route = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, interfaceName, -1);
		}
		
		int index = routeIndex(route);
		if(index != -1) {
			routes.remove(index);
			storeFile();
			return true;
		}
		return false;
	}
	
	private void createFile() {
		try {
			file.createNewFile();
		} catch (IOException e) {
			s_logger.warn("Exception while creating file", e);
		}
	}
	
	public RouteConfig [] getRoutes() {
		if(routes.size()<1) {
			return null;
		}
		RouteConfig [] result = new RouteConfig[routes.size()];
		for(int i=0; i<routes.size(); i++) {
			result[i] = (RouteConfig)routes.get(i);
		}
		return result;
	}
	
	private void storeFile() {
		FileOutputStream out = null;
		Properties props = new Properties();
		RouteConfig route = null;
		for(int i=0; i<routes.size(); i++) {
			route = (RouteConfig)routes.get(i);
			props.put("ADDRESS"+i, route.getDestination().getHostAddress());
			props.put("GATEWAY"+i, route.getGateway().getHostAddress());
			props.put("NETMASK"+i, route.getNetmask().getHostAddress());
		}
		try {
			out = new FileOutputStream(file);
			props.store(out, "Persistent routes for interface:  " + interfaceName);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			s_logger.warn("File not found", e);
		} catch (IOException e) {
			s_logger.warn("Exception while reading file", e);
		}  finally{
			if(out != null){
				try{
					out.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
		}
		
	}
	
	private int routeIndex(RouteConfig route) {
		for(int i=0; i<routes.size(); i++) {
			RouteConfig tmp = (RouteConfig)routes.get(i);
			if(route.equals(tmp)) {
				return i;
			}
		}
		return -1;
	}
	
	public void cleanFile() {
		routes = new ArrayList<RouteConfig>();
	}

}
