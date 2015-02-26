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
/*
* Copyright (c) 2013 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.linux.net.route;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.route.RouteConfigIP4;
import org.eclipse.kura.net.route.RouteConfigIP6;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//public class RouteServiceImpl implements RouteService, IConfigurableComponentService {
public class RouteServiceImpl implements RouteService {

	private static Logger s_logger = LoggerFactory.getLogger(RouteServiceImpl.class);
	
	private static RouteServiceImpl s_routeService = null;
	
//	public static final String CONFIGURATION_NAME = "org.eclipse.kura.net.linux.route";
	
	private String m_osRouteConfigDirectory = null;	
	
	private RouteServiceImpl() {
		m_osRouteConfigDirectory = "/etc/sysconfig/network-scripts/";
	}

	public static synchronized RouteService getInstance() {
		if (s_routeService == null) {
			s_routeService =  new RouteServiceImpl();
		}
		
		return s_routeService;
	}
	
	private void addPersistentStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface) throws Exception {
		addStaticRoute(destination, gateway, netmask, iface, 0);
		RouteFile routeFile = new RouteFile(iface);
		if(routeFile.addRoute(destination, gateway, netmask, iface)) {
			s_logger.info("Route persistence added");
		} else {
			s_logger.error("Error adding route persistence");
		}
	}

	public void addStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface, int metric) throws Exception {
		RouteConfig tmpRoute = null;		
		StringBuffer command = new StringBuffer();
		command.append("route add -net " + destination.getHostAddress() + " ");
		if(netmask != null) {
			command.append("netmask " + netmask.getHostAddress() + " ");
		}
		if (gateway != null) {
			if ((gateway.getHostAddress().compareTo("0.0.0.0") != 0)
					&& (gateway.getHostAddress().compareTo("127.0.0.1") != 0)) {
				command.append("gw " + gateway.getHostAddress() + " ");
			}
		}
		if(iface != null) {
			command.append("dev " + iface + " ");
		}
		if(metric != 0 && metric != -1) {
			command.append("metric " + metric);
		}
		
		Process proc = null;
		try {
			s_logger.debug("Executing command:  " + command.toString());
			proc = ProcessUtil.exec(command.toString());
			proc.waitFor();
			if(proc.exitValue() != 0) {
				s_logger.error("Error adding static Route: " + command.toString());
				throw new Exception("Error adding Static Route");
			}
		} catch (IOException e) {
			s_logger.error("Error executing command:  route -n");
			throw e;
		}
		finally {
			ProcessUtil.destroy(proc);
		}
		
		if(destination instanceof IP4Address) {
			tmpRoute = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, iface, -1);
		} else if(destination instanceof IP6Address) {
			tmpRoute = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, iface, -1);
		}
		s_logger.info("Static route added successfully");
		s_logger.debug(tmpRoute.getDescription());
	}

	public RouteConfig getDefaultRoute(String iface) {
		RouteConfig [] routes = getRoutes();
		RouteConfig defaultRoute;
		ArrayList<RouteConfig> defaultRoutes = new ArrayList<RouteConfig>();
		
		// Search through routes and construct a list of all default routes for the specified interface
		for(int i=0;i<routes.length;i++) {
			if(routes[i].getInterfaceName().compareTo(iface)==0 && routes[i].getDestination().getHostAddress().compareTo("0.0.0.0")==0) {
				defaultRoutes.add(routes[i]);
			}
		}
		
		// If no default routes exist, return null
		if(defaultRoutes.size() == 0) {
			s_logger.debug("No default routes exist for inteface: " + iface);
			return null;
		}
		
		// Set the default route to the first one in the list
		defaultRoute = (RouteConfig) defaultRoutes.get(0);
		
		// Search for the default route with the lowest metric value
		for(int i=1; i<defaultRoutes.size(); i++) {
			if(defaultRoute.getMetric() > ((RouteConfig)defaultRoutes.get(i)).getMetric()) {
				defaultRoute = (RouteConfig)defaultRoutes.get(i);
			}
		}
		
		s_logger.info("Default route found for interface: " + iface);
		s_logger.debug("Default route:\n" + defaultRoute.getDescription());
		return defaultRoute;
	}

	public RouteConfig[] getRoutes() {
		String routeEntry = null;
		ArrayList<RouteConfig> routeList = new ArrayList<RouteConfig>();
		RouteConfig [] routes = null;
		RouteConfig tmpRoute = null;
		Process proc = null;		
		BufferedReader br = null;
		try {
			proc = ProcessUtil.exec("route -n");
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			br.readLine();
			br.readLine();
			while ((routeEntry = br.readLine()) != null) {
				tmpRoute = entryToRoute(routeEntry);
				if(tmpRoute != null) {
					routeList.add(tmpRoute);
				}
			}
		} 
		catch (IOException e) {
			s_logger.error("Error executing command:  route -n", e);
			return null;
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}			
			ProcessUtil.destroy(proc);
		}
				
		routes = new RouteConfig[routeList.size()];
		for(int i=0; i<routes.length; i++) {
			routes[i] = (RouteConfig)routeList.get(i);
		}
		
		return routes;
	}

	private void removePersistentStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface) throws Exception {
		removeStaticRoute(destination, gateway, netmask, iface);
		RouteFile routeFile = new RouteFile(iface);
		if(routeFile.removeRoute(destination, gateway, netmask, iface)) {
			s_logger.info("Route persistence removed");
		} else {
			s_logger.error("Error removing route persistence");
		}
	}

	public void removeStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface) throws Exception {
		RouteConfig tmpRoute = null;		
		StringBuffer command = new StringBuffer();
		command.append("route del -net " + destination.getHostAddress() + " ");
		if(netmask != null) {
			command.append("netmask " + netmask.getHostAddress() + " ");
		}
		if(gateway != null) {
			if (gateway.getHostAddress().compareTo("127.0.0.1") != 0) {
				command.append("gw " + gateway.getHostAddress() + " ");
			}
		}
		if(iface != null) {
			command.append("dev " + iface + " ");
		}
		
		Process proc = null;
		try {
			s_logger.debug("Executing command:  " + command.toString());
			proc = ProcessUtil.exec(command.toString());
			proc.waitFor();
			if(proc.exitValue() != 0) {
				s_logger.error("Error removing static Route");
				throw new Exception("Error removing Static Route");
			}
		} catch (IOException e) {
			s_logger.error("Error executing command:  route -n");
			throw e;
		}
		finally {
			ProcessUtil.destroy(proc);
		}
		
		if(destination instanceof IP4Address) {
			tmpRoute = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, iface, -1);
		} else if(destination instanceof IP6Address) {
			tmpRoute = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, iface, -1);
		}
		s_logger.info("Static route removed successfully");
		s_logger.debug(tmpRoute.getDescription());
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
			s_logger.error("Error parsing route table entry:  " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		
		if(destination instanceof IP4Address) {
			route = new RouteConfigIP4((IP4Address) destination, (IP4Address) gateway, (IP4Address) netmask, iface, metric);
		} else if(destination instanceof IP6Address) {
			route = new RouteConfigIP6((IP6Address) destination, (IP6Address) gateway, (IP6Address) netmask, iface, metric);
		}
		s_logger.trace("Route successfully read from route table entry");
		return route;
	}

	public String getDefaultInterface(IPAddress destination) {
		ArrayList<RouteConfig> matches = new ArrayList<RouteConfig>();
		RouteConfig [] routes = getRoutes();
		for(int i=0; i<routes.length; i++) {
			if(matchesRoute(destination, routes[i])) {
				matches.add(routes[i]);
			}
		}
		if(matches.size()>0) {
			RouteConfig dRoute = (RouteConfig)matches.get(0);
			for(int i=0; i<routes.length; i++) {
				if(dRoute.getMetric()>routes[i].getMetric()) {
					dRoute = routes[i];
				}
			}
			s_logger.debug("Found defualt interface " + dRoute.getInterfaceName() + " for destination " + destination.getHostAddress());
			return dRoute.getInterfaceName();
		}
		s_logger.debug("No default interface exists for destination " + destination.getHostAddress());
		return null;
	}
	
	private boolean matchesRoute(IPAddress destination, RouteConfig route) {
		byte mask = (byte)0xFF;
		byte [] dest = destination.getAddress();
		byte [] routeMask = route.getNetmask().getAddress();
		byte [] routeDest = route.getDestination().getAddress();
		byte [] dest_masked = new byte[4];
		for(int i=0; i<4; i++) {
			if(routeMask[i] == mask) {
				dest_masked[i] = dest[i];
			} else {
				dest_masked[i] = 0;
			}
		}
		for(int i=0; i<4; i++) {
			if(dest_masked[i] != routeDest[i]) {
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Container class for a new route from a configuration
	 */
	private class ConfigRoute {
		public int index;
		public IPAddress destination = null;
		public IPAddress netmask = null;
		public IPAddress gateway = null;
		public String iface = null;
		public ConfigRoute(int index) {
			this.index = index;
		}
		public boolean isComplete() {
			return destination != null && netmask != null && gateway != null && iface != null;
		}
		public String getDescription() {
			return "dest: " + destination.getHostAddress() + " nm: " + netmask.getHostAddress() + " gw: " + gateway.getHostAddress() + " iface: " + iface; 
		}
	}
	
//	public Object receiveConfig(Object config) throws KuraConfigurationException {
//		Properties props = (Properties)config;
//		Enumeration keys = props.keys();
//		ArrayList newRoutes = new ArrayList();
//		String key = null;
//		String param = null;
//		int index = 0;
//		ConfigRoute newRoute = null;
//		s_logger.debug("New Configuration received with " + props.size() + " elements");
//		
//		// Validate the configuration parameters and create ConfigRoute objects to hold the new routes
//		while(keys.hasMoreElements()) {
//			key = (String)keys.nextElement();
//			try {
//				index = getKeyIndex(key);
//			} catch (NumberFormatException e) {
//				s_logger.error("Error parsing configuration parameter " + key);
//				throw new KuraConfigurationException("Error parsing configuration parameter: " + key + ", " + e.getMessage());
//			}
//			param = getKeyParam(key);
//			
//			for(int i=0; i<newRoutes.size(); i++) {
//				ConfigRoute tmpRoute = (ConfigRoute)newRoutes.get(i);
//				if(tmpRoute.index == index) {
//					s_logger.debug("New parameter found for route " + index);
//					newRoute = tmpRoute;
//				}
//			}
//			
//			if(newRoute == null) {
//				s_logger.debug("New parameter found for NEW route " + index);
//				newRoute = new ConfigRoute(index);
//				newRoutes.add(newRoute);
//			}
//				
//			try {
//				if(param.compareTo("destination")==0) {
//					s_logger.debug("New Configuration - Route - destination: " + (String)props.get(key));
//					newRoute.destination = InetAddress.getByName((String)props.get(key));
//				} else if(param.compareTo("netmask")==0) {
//					s_logger.debug("New Configuration - Route - netmask: " + (String)props.get(key));
//					newRoute.netmask = InetAddress.getByName((String)props.get(key));
//				} else if(param.compareTo("gateway")==0) {
//					s_logger.debug("New Configuration - Route - gateway: " + (String)props.get(key));
//					newRoute.gateway = InetAddress.getByName((String)props.get(key));
//				} else if(param.compareTo("iface")==0) {
//					s_logger.debug("New Configuration - Route - iface: " + (String)props.get(key));
//					newRoute.iface = (String)props.get(key);
//				} else {
//					s_logger.error("Error parsing configuration, unknown parameter: " + key);
//					throw new KuraConfigurationException("Unknown parameter: " + key);	
//				}
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//				s_logger.error("Error parsing configuration, unknown host: " + (String)props.get(key));
//				throw new KuraConfigurationException("Unknown host: " + (String)props.get(key));
//				
//			}
//			newRoute = null;
//		}
//		
//		// Make sure all new route configurations are complete
//		for(int i=0; i<newRoutes.size(); i++) {
//			ConfigRoute cr = (ConfigRoute)newRoutes.get(i);
//			if(!cr.isComplete()) {
//				s_logger.error("Error parsing configuration, missing parameters for new route " + cr.index);
//				throw new KuraConfigurationException("Missing parameters for new route " + cr.index);
//			} else {
//				s_logger.debug("New complete route configuration:  " + cr.getDescription());
//			}
//		}
//		
//		// Clear current OS persistent route files
//		clearPersistenceFiles();
//		
//		// Create new routes
//		RouteFile routeFile;
//		for(int i=0; i<newRoutes.size(); i++) {
//			ConfigRoute cr = (ConfigRoute)newRoutes.get(i);
//			s_logger.debug("editing route file for interface:  " + cr.iface);
//			routeFile = new RouteFile(cr.iface);
//			s_logger.debug("adding route:  " + cr.getDescription());
//			routeFile.addRoute(cr.destination, cr.gateway, cr.netmask, cr.iface);
//		}
//		
//		return null;
//	}

	private int getKeyIndex(String key) throws NumberFormatException {
		int index;
		try {
			String indexString = key.substring(key.indexOf("_")+1);
			index = Integer.parseInt(indexString);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw e;
		}
		return index;
	}
	
	private String getKeyParam(String key) {
		return key.substring(0,key.indexOf("_"));
	}
	
	private void clearPersistenceFiles() {
		File dir = new File(m_osRouteConfigDirectory);
		String [] files = dir.list();
		for(int i=0; i<files.length; i++) {
			if(files[i].startsWith("route-")) {
				File fileToDelete = new File(m_osRouteConfigDirectory + files[i]);
				fileToDelete.delete();
			}
		}
	}
}
