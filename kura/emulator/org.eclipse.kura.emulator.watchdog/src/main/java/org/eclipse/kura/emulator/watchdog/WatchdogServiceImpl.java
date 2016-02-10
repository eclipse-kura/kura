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
package org.eclipse.kura.emulator.watchdog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.eclipse.kura.watchdog.WatchdogService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchdogServiceImpl implements WatchdogService, ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(WatchdogServiceImpl.class);
	
	private Map<String,Object>				m_properties;
	private ScheduledExecutorService		m_executor;
	private ScheduledFuture<?>				m_future;
	private int 							pingInterval = 10000;	//milliseconds
	private static ArrayList<CriticalServiceImpl>	s_criticalServiceList;
	private boolean 						m_configEnabled = false;	// initialized in properties, if false -> no watchdog
	private boolean 						m_enabled; 

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
		m_properties=properties;
		if(properties == null) {
			s_logger.debug("activating WatchdogService with null props");
		} else {
			s_logger.debug("activating WatchdogService with " + properties.toString());
		}
		s_criticalServiceList = new ArrayList<CriticalServiceImpl>();
		m_enabled=false;
		
		//clean up if this is not our first run
		if(m_executor != null) {
			m_executor.shutdown();
			while(!m_executor.isTerminated()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			m_executor = null;
		}
		
		m_executor = Executors.newSingleThreadScheduledExecutor();

		m_future=m_executor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				if(m_configEnabled)
					doWatchdogLoop();
			}
		}, 0, pingInterval, TimeUnit.MILLISECONDS);		
	}
	
	protected void deactivate(ComponentContext componentContext) {
		m_executor.shutdown();
		while(!m_executor.isTerminated()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		m_executor = null;
		s_criticalServiceList = null;
	}
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.debug("updated...");		
		m_properties = properties;
		if(m_properties!=null){
			if(m_properties.get("enabled") != null){
				if(m_properties.get("enabled") != null){
					m_configEnabled = (Boolean) m_properties.get("enabled");
				}
			}
			if(!m_configEnabled)
				return;
			if(m_properties.get("pingInterval") != null){
				pingInterval = (Integer) m_properties.get("pingInterval");
				if(m_future!=null){
					m_future.cancel(false);
					while(!m_future.isDone()){
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				m_future=m_executor.scheduleAtFixedRate(new Runnable() {
					public void run() {
						Thread.currentThread().setName(getClass().getSimpleName());
						if(m_configEnabled)
							doWatchdogLoop();
					}
				}, 0, pingInterval, TimeUnit.MILLISECONDS);		
			}
		}
	}
	
	@Override
	public void startWatchdog() {
		// TODO Auto-generated method stub
		m_enabled=true;
	}

	@Override
	public void stopWatchdog() {
		// TODO Auto-generated method stub
		m_enabled=false;
	}

	@Override
	public int getHardwareTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

    @Override
    public void registerCriticalComponent(CriticalComponent criticalComponent) {
        final CriticalServiceImpl service = new CriticalServiceImpl(criticalComponent.getCriticalComponentName(), criticalComponent.getCriticalComponentTimeout());
        synchronized(s_criticalServiceList) {
            // avoid to add same component twice (eg in case of a package updating) 
            boolean existing=false;
            for(CriticalServiceImpl csi:s_criticalServiceList){
                if(criticalComponent.getCriticalComponentName().compareTo(csi.getName())==0) {
                    existing=true;
                }
            }
            if(!existing)
                s_criticalServiceList.add(service);
        }
        
        s_logger.debug("Added " + criticalComponent.getCriticalComponentName() + ", with timeout = " + criticalComponent.getCriticalComponentTimeout() +
        ", list contains " + s_criticalServiceList.size() + " critical services");
    }

    @Override
    @Deprecated
	public void registerCriticalService(CriticalComponent criticalComponent) {
        registerCriticalComponent(criticalComponent);
	}

    @Override
    public void unregisterCriticalComponent(CriticalComponent criticalComponent) {
        synchronized(s_criticalServiceList) {
            for(int i=0; i<s_criticalServiceList.size(); i++) {
                if(criticalComponent.getCriticalComponentName().compareTo(((CriticalServiceImpl)s_criticalServiceList.get(i)).getName())==0) {
                    s_criticalServiceList.remove(i);
                    s_logger.debug("Critical service " + criticalComponent.getCriticalComponentName() + " removed, " + System.currentTimeMillis());
                }
            }
        }
    }

    @Override
	public void unregisterCriticalService(CriticalComponent criticalComponent) {
        unregisterCriticalComponent(criticalComponent);
	}

	@Override
	public List<CriticalComponent> getCriticalComponents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkin(CriticalComponent criticalService) {
		synchronized(s_criticalServiceList) {
			for(CriticalServiceImpl csi:s_criticalServiceList){
				if(criticalService.getCriticalComponentName().compareTo(csi.getName())==0) {
					csi.update();
				}
			}
		}
	}

	private void doWatchdogLoop() {
		if(!m_enabled)
			return;
		
		boolean failure=false;
		// Critical Services
		synchronized(s_criticalServiceList) {
			if(s_criticalServiceList.size() > 0) {
				for(CriticalServiceImpl csi:s_criticalServiceList){
					if(csi.isTimedOut()) {
						failure=true;
						s_logger.warn("Critical service " + csi.getName() + " failed -> SYSTEM REBOOT");
					}
				}
			}
		}
		if(!failure)
			refresh_watchdog();
	}

	private void refresh_watchdog() {
		File f = new File("/dev/watchdog");
		if(f.exists()){
			try {
				FileOutputStream fos = new FileOutputStream(f);
				PrintWriter pw = new PrintWriter(fos);
				pw.write("w");
				pw.flush();
				fos.getFD().sync();
				pw.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isConfigEnabled() {
		return m_configEnabled;
	}

	public void setConfigEnabled(boolean configEnabled) {
		this.m_configEnabled = configEnabled;
	}
}
