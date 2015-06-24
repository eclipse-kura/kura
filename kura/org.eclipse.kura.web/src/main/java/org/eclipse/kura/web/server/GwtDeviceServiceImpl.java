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
package org.eclipse.kura.web.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.PasswordCommandService;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.ListLoadResult;

public class GwtDeviceServiceImpl extends OsgiRemoteServiceServlet implements GwtDeviceService 
{
	private static final Logger s_logger = LoggerFactory.getLogger(GwtDeviceServiceImpl.class);
	
	private static final String UNKNOWN     = "UNKNOWN";
	
	private static final long serialVersionUID = -4176701819112753800L;

	public ListLoadResult<GwtGroupedNVPair> findDeviceConfiguration() 
		throws GwtKuraException 
	{
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();

		PositionService positionService = ServiceLocator.getInstance().getService(PositionService.class);
		NetworkService networkService = ServiceLocator.getInstance().getService(NetworkService.class);
		SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
		SystemAdminService systemAdminService = ServiceLocator.getInstance().getService(SystemAdminService.class);
				
		try {
			
			Properties systemProperties = systemService.getProperties();
			
			pairs.add( new GwtGroupedNVPair("devInfo", "devKuraVersion",      systemService.getKuraVersion()));
			pairs.add( new GwtGroupedNVPair("devInfo", "devClientId",        systemService.getPrimaryMacAddress() != null ? systemService.getPrimaryMacAddress() : UNKNOWN));
			pairs.add( new GwtGroupedNVPair("devInfo", "devDisplayName",     systemService.getDeviceName()));
			pairs.add( new GwtGroupedNVPair("devInfo", "devUptime",          formatUptime(Long.parseLong(systemAdminService.getUptime()))));
			pairs.add( new GwtGroupedNVPair("devInfo", "devLastWifiChannel", String.valueOf(systemService.getKuraWifiTopChannel())));
			
			pairs.add( new GwtGroupedNVPair("devHw", "devModelName",       systemService.getModelName()));
			pairs.add( new GwtGroupedNVPair("devHw", "devModelId",         systemService.getModelId()));
			pairs.add( new GwtGroupedNVPair("devHw", "devPartNumber",      systemService.getPartNumber()));
			pairs.add( new GwtGroupedNVPair("devHw", "devSerialNumber",    systemService.getSerialNumber()));
		
			pairs.add( new GwtGroupedNVPair("devSw", "devFirmwareVersion", systemService.getFirmwareVersion()));
			pairs.add( new GwtGroupedNVPair("devSw", "devBiosVersion",     systemService.getBiosVersion()));
			pairs.add( new GwtGroupedNVPair("devSw", "devOsVersion",       systemService.getOsVersion()));
			pairs.add( new GwtGroupedNVPair("devSw", "devOs",              systemService.getOsName()));
			pairs.add( new GwtGroupedNVPair("devSw", "devOsArch",          systemService.getOsArch()));

			pairs.add( new GwtGroupedNVPair("devJava", "devJvmName",    systemProperties.getProperty(SystemService.KEY_JAVA_VM_NAME)));
			pairs.add( new GwtGroupedNVPair("devJava", "devJvmVersion", systemProperties.getProperty(SystemService.KEY_JAVA_VM_VERSION)));
			
			pairs.add( new GwtGroupedNVPair("devJava", "devJvmProfile", 			systemService.getJavaVendor() + " " + systemService.getJavaVersion()));
			pairs.add( new GwtGroupedNVPair("devJava", "devOsgiFramework", 			systemProperties.getProperty(SystemService.KEY_OSGI_FW_NAME)));
			pairs.add( new GwtGroupedNVPair("devJava", "devOsgiFrameworkVersion", 	systemProperties.getProperty(SystemService.KEY_OSGI_FW_VERSION)));
			if (systemService.getNumberOfProcessors() != -1) {
				pairs.add( new GwtGroupedNVPair("devJava", "devNumProc", String.valueOf(systemService.getNumberOfProcessors())));
			}
			pairs.add( new GwtGroupedNVPair("devJava", "devRamTot",  String.valueOf(systemService.getTotalMemory())+" MB"));
			pairs.add( new GwtGroupedNVPair("devJava", "devRamFree", String.valueOf(systemService.getFreeMemory())+" MB"));

			// get the network information
			String connectionIp = UNKNOWN;
			String connectionInterface = UNKNOWN;
			if (networkService != null) {

				// we have a network service.
				// use it to get the connection interface and IP
				List<NetInterface<? extends NetInterfaceAddress>> nis = networkService.getActiveNetworkInterfaces();
				if (!nis.isEmpty()) {

					NetInterface<? extends NetInterfaceAddress> ni = nis.get(0);

					StringBuilder sb = new StringBuilder();
					sb.append(ni.getName())
					.append(" (")
							.append(NetUtil.hardwareAddressToString(ni.getHardwareAddress()))
							.append(")");					
					connectionInterface = sb.toString();

					List<? extends NetInterfaceAddress> nias = ni.getNetInterfaceAddresses();
					if (!nias.isEmpty()) {
						if (nias.get(0).getAddress() != null) {
							connectionIp = nias.get(0).getAddress().getHostAddress();
						}
					}				
				}
			}
			if (UNKNOWN.equals(connectionIp) || UNKNOWN.equals(connectionInterface)) {

				s_logger.error("Unresolved NetworkService reference or IP address. Defaulting to JVM Networking Information.");
				try {				
					InetAddress addr = NetUtil.getCurrentInetAddress();
					if (addr != null) {   
						connectionIp = addr.getHostAddress();
						NetworkInterface netInterface = NetworkInterface.getByInetAddress(addr);
						if (netInterface != null) {
							connectionInterface = NetUtil.hardwareAddressToString(netInterface.getHardwareAddress());
						}
					}
				}
				catch (Exception se) {
					s_logger.warn("Error while getting ConnetionIP and ConnectionInterface", se);
				}			
			}
			
			pairs.add( new GwtGroupedNVPair("netInfo", "netConnIf", connectionInterface));
			pairs.add( new GwtGroupedNVPair("netInfo", "netConnIp", connectionIp));
			
			Position position = positionService.getPosition();
			pairs.add( new GwtGroupedNVPair("gpsInfo", "gpsLat",  Double.toString(Math.toDegrees(position.getLatitude().getValue()))));
			pairs.add( new GwtGroupedNVPair("gpsInfo", "gpsLong", Double.toString(Math.toDegrees(position.getLongitude().getValue()))));
			pairs.add( new GwtGroupedNVPair("gpsInfo", "gpsAlt",  Double.toString(Math.toDegrees(position.getAltitude().getValue()))));
			
			// TODO: Add cloud status information in the Denali Device Profile
//			deviceConfig.deviceStatus
//			deviceConfig.setAcceptEncoding(null); 
//			deviceConfig.setApplicationIdentifiers(null); 
//			deviceConfig.setLastEventOn(new Date()); 
//			deviceConfig.setLastEventType("UNKNOWN");
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, t);
		}
		return new BaseListLoadResult<GwtGroupedNVPair>(pairs);
	}
		
	
	
	@SuppressWarnings("unchecked")
	public ListLoadResult<GwtGroupedNVPair> findThreads() 
		throws GwtKuraException 
	{
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();

		// get root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
		}
        // enumerate all other threads
        int numGroups = rootGroup.activeGroupCount();
        final ThreadGroup[] groups = new ThreadGroup[2 * numGroups];
        numGroups = rootGroup.enumerate(groups);
        Arrays.sort(groups, ThreadGroupComparator.getInstance());
        for (int i = 0; i < groups.length; i++) {
        	
        	ThreadGroup group = groups[i];
        	if (group != null) {
	        	StringBuilder sbGroup = new StringBuilder(); 
	        	sbGroup.append("ThreadGroup ")
	        		   .append(group.getName())
	        		   .append(" [")
	        		   .append("maxprio=")
	        		   .append(group.getMaxPriority());
	        	sbGroup.append(", parent=");
	            if (group.getParent() != null) {
	            	sbGroup.append(group.getParent().getName());
	            }
	            else {
	            	sbGroup.append('-');
	            }
	
	            sbGroup.append(", isDaemon=");
	            sbGroup.append(group.isDaemon());
	            sbGroup.append(", isDestroyed=");
	            sbGroup.append(group.isDestroyed());
	            sbGroup.append(']');
	
	            int numThreads = group.activeCount();
	            Thread[] threads = new Thread[numThreads * 2];
	            group.enumerate(threads, false);
	            Arrays.sort(threads, ThreadComparator.getInstance());
	            for (int j=0; j<threads.length; j++) {
	            	
	            	Thread thread = threads[j];
	            	if (thread != null) {
	            		
			        	StringBuilder sbThreadName = new StringBuilder(); 
			        	sbThreadName.append(thread.getId())
			            			.append('/')
			            			.append(thread.getName());
			
			        	StringBuilder sbThreadValue = new StringBuilder();             	
			        	sbThreadValue.append("priority=");
			        	sbThreadValue.append(thread.getPriority());
			        	sbThreadValue.append(", alive=");
			        	sbThreadValue.append(thread.isAlive());
			        	sbThreadValue.append(", daemon=");
			        	sbThreadValue.append(thread.isDaemon());
			        	sbThreadValue.append(", interrupted=");
			        	sbThreadValue.append(thread.isInterrupted());
			        	sbThreadValue.append(", loader=");
			        	sbThreadValue.append(thread.getContextClassLoader());
			        	sbThreadValue.append(']');
			
			            pairs.add( new GwtGroupedNVPair(sbGroup.toString(), sbThreadName.toString(), sbThreadValue.toString()));
	            	}
	            }
        	}
        }
		return new BaseListLoadResult<GwtGroupedNVPair>(pairs);
	}


	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ListLoadResult<GwtGroupedNVPair> findSystemProperties() 
		throws GwtKuraException 
	{
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();
		// kura properties
		SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
		Properties kuraProps = systemService.getProperties();
        SortedSet kuraKeys = new TreeSet( kuraProps.keySet() );
        for (Iterator ki = kuraKeys.iterator(); ki.hasNext(); )
        {
            Object key = ki.next();
            pairs.add( new GwtGroupedNVPair("propsKura", key.toString(), kuraProps.get(key).toString()));
        }		
		return new BaseListLoadResult<GwtGroupedNVPair>(pairs);
	}
	
	
	
	public ListLoadResult<GwtGroupedNVPair> findBundles() 
		throws GwtKuraException 
	{
		List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();

		SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
		Bundle[] bundles = systemService.getBundles();
		if (bundles != null) {
			
			for (Bundle bundle : bundles) {
				
				if (bundle != null) {
				
					GwtGroupedNVPair pair = new GwtGroupedNVPair();
					pair.setId(String.valueOf(bundle.getBundleId()));
					pair.setName(getName(bundle));
					pair.setStatus(toStateString(bundle));
					pair.setVersion(getHeaderValue(bundle, Constants.BUNDLE_VERSION));
					
					pairs.add(pair);
				}
			}
		}
		return new BaseListLoadResult<GwtGroupedNVPair>(pairs);
	}
	
	public String executeCommand(String cmd, String pwd) throws GwtKuraException {
		PasswordCommandService commandService = ServiceLocator.getInstance().getService(PasswordCommandService.class);
		try {
			return commandService.execute(cmd, pwd);
		} catch (KuraException e) {
			//s_logger.error(e.getLocalizedMessage());
			if(e.getCode() == KuraErrorCode.OPERATION_NOT_SUPPORTED){
				throw new GwtKuraException(GwtKuraErrorCode.SERVICE_NOT_ENABLED);
			}else if(e.getCode() == KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID){
				throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
			}
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		} 
	}
	
	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------
	
	
    /**
     * Return a display name for the given <code>bundle</code>:
     * <ol>
     * <li>If the bundle has a non-empty <code>Bundle-Name</code> manifest
     * header that value is returned.</li>
     * <li>Otherwise the symbolic name is returned if set</li>
     * <li>Otherwise the bundle's location is returned if defined</li>
     * <li>Finally, as a last resort, the bundles id is returned</li>
     * </ol>
     *
     * @param bundle the bundle which name to retrieve
     * @param locale the locale, in which the bundle name is requested
     * @return the bundle name - see the description of the method for more details.
     */
    public String getName(Bundle bundle)
    {
        String name = ( String ) bundle.getHeaders().get( Constants.BUNDLE_NAME );
        if ( name == null || name.length() == 0 )
        {
            name = bundle.getSymbolicName();
            if ( name == null )
            {
                name = bundle.getLocation();
                if ( name == null )
                {
                    name = String.valueOf( bundle.getBundleId() );
                }
            }
        }
        return name;
    }

    /**
     * Returns the value of the header or the empty string if the header
     * is not available.
     *
     * @param bundle the bundle which header to retrieve
     * @param headerName the name of the header to retrieve
     * @return the header or empty string if it is not set
     */
    public static String getHeaderValue( Bundle bundle, String headerName )
    {
       Object value = bundle.getHeaders().get(headerName);
       if ( value != null )
       {
           return value.toString();
       }
       return "";
    }

    
    private String toStateString( final Bundle bundle )
    {
        switch ( bundle.getState() )
        {
            case Bundle.INSTALLED:
                return "bndInstalled";
            case Bundle.RESOLVED:
                return "bndResolved";
            case Bundle.STARTING:
                return "bndStarting";
            case Bundle.ACTIVE:
                return "bndActive";
            case Bundle.STOPPING:
                return "bndStopping";
            case Bundle.UNINSTALLED:
                return "bndUninstalled";
            default:
                return "bndUnknown";
        }
    }
    
    private String formatUptime(long uptime)
    {
                int  days    = (int) TimeUnit.MILLISECONDS.toDays(uptime);
                long hours   = TimeUnit.MILLISECONDS.toHours(uptime) - (days*24);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) - (TimeUnit.MILLISECONDS.toHours(uptime)* 60);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) - (TimeUnit.MILLISECONDS.toMinutes(uptime) *60);
                        
                StringBuilder sb = new StringBuilder();
                sb.append(days)
                .append(" days ")
                .append(hours)
                .append(":")
                .append(minutes)
                .append(":")
                .append(seconds)
                .append(" hms");

                return sb.toString();
    }
}

@SuppressWarnings("rawtypes")
final class ThreadComparator implements Comparator
{

    private ThreadComparator()
    {
        // prevent instantiation
    }

    private static final Comparator instance = new ThreadComparator();

    public static final Comparator getInstance()
    {
        return instance;
    }

    public int compare(Object thread1, Object thread2)
    {
        if (thread1 == null || thread2 == null) {
        	return (thread1 == null) ? -1 : 1;
        }
        if (thread1 == null || thread2 == null)
        	return 0;
        
        String t1 = ((Thread) thread1).getName();
        String t2 = ((Thread) thread2).getName();
        if (null == t1)
        {
            t1 = ""; //$NON-NLS-1$
        }
        if (null == t2)
        {
            t2 = ""; //$NON-NLS-1$
        }

        return t1.toLowerCase().compareTo(t2.toLowerCase());
    }
}

@SuppressWarnings("rawtypes")
final class ThreadGroupComparator implements Comparator
{

    private ThreadGroupComparator()
    {
        // prevent instantiation
    }

    private static final Comparator instance = new ThreadGroupComparator();

    public static final Comparator getInstance()
    {
        return instance;
    }

    public int compare(Object thread1, Object thread2)
    {
    	if (thread1 == null || thread2 == null) {
        	return (thread1 == null) ? -1 : 1;
        }
        if (thread1 == null || thread2 == null)
        	return 0;
        
        String t1 = ((ThreadGroup) thread1).getName();
        String t2 = ((ThreadGroup) thread2).getName();
        if (null == t1)
        {
            t1 = ""; //$NON-NLS-1$
        }
        if (null == t2)
        {
            t2 = ""; //$NON-NLS-1$
        }
        return t1.toLowerCase().compareTo(t2.toLowerCase());
    }
}

