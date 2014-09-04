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
package org.eclipse.kura.web.client.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class NetworkInterfacesTable extends LayoutContainer
{
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

	@SuppressWarnings("unused")
	private GwtSession                  m_currentSession;

	private NetInterfaceConfigTabs 		m_netConfigTabs;
	 
	private ToolBar                     m_toolBar;
	private Grid<GwtNetInterfaceConfig>	m_grid;
	private BaseListLoader<ListLoadResult<GwtNetInterfaceConfig>> m_loader;
	private ListStore<GwtNetInterfaceConfig>                      m_store;
	private GwtNetInterfaceConfig      m_selectedIfConfig;
	
    public NetworkInterfacesTable(GwtSession currentSession,
    							  NetInterfaceConfigTabs netConfigTabs) 
    {
    	m_currentSession = currentSession;    
    	m_netConfigTabs  = netConfigTabs;
    }


	protected void onRender(Element parent, int index) 
	{
		super.onRender(parent, index);
     
		setLayout(new FitLayout());
		setBorders(false);
		setId("network-interfaces-table");
		
		initToolBar();
		initGrid();

        ContentPanel panel = new ContentPanel();
        panel.setLayout(new FitLayout());
        panel.setBorders(false);
        panel.setBodyBorder(true);
        panel.setHeaderVisible(false);
        
        panel.add(m_grid);
        panel.setBottomComponent(m_toolBar);
        
        add(panel);
        
        m_loader.load();
	}
    
	
    private void initToolBar() 
    {
    	m_toolBar = new ToolBar();
    	m_toolBar.setBorders(true);
    }
    
    
    private void initGrid() 
    {
        //
        // Column Configuration
        List<ColumnConfig> configs = new ArrayList<ColumnConfig>();

        ColumnConfig column = new ColumnConfig("name", MSGS.netInterfaceName(), 125);
        column.setAlignment(HorizontalAlignment.LEFT);
        column.setSortable(false);
        configs.add(column);

        // loader and store
        RpcProxy<ListLoadResult<GwtNetInterfaceConfig>> proxy = new RpcProxy<ListLoadResult<GwtNetInterfaceConfig>>() {
            @Override
            public void load(Object loadConfig, AsyncCallback<ListLoadResult<GwtNetInterfaceConfig>> callback) {
                gwtNetworkService.findNetInterfaceConfigurations(callback);
            }
        };
        m_loader = new BaseListLoader<ListLoadResult<GwtNetInterfaceConfig>>(proxy);
        m_loader.setRemoteSort(false);
        m_loader.setSortDir(SortDir.ASC);
        m_loader.setSortField("name");
         
        m_store = new ListStore<GwtNetInterfaceConfig>(m_loader);
        m_store.setStoreSorter(new StoreSorter<GwtNetInterfaceConfig>(new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				if (o1 == null) {
					o1 = new Integer(-1);
				}
				else {
					o1 = getIntFromName((String)o1); 
				}
				if (o2 == null) {
					o2 = new Integer(-1);
				}
				else {
					o2 = getIntFromName((String)o2); 
				}
				return (Integer)o1 - (Integer)o2;
			}
			
			private Integer getIntFromName (String name) {
				if ("lo".equals(name)) return new Integer(1);
				else if ("eth0".equals(name)) return new Integer(2);
				else if ("eth1".equals(name)) return new Integer(3);
				else if (name.contains("eth")) return new Integer(4);
				else if ("wlan0".equals(name)) return new Integer(10);
				else if ("ppp0".equals(name)) return new Integer(20);
				else if (name.contains("ppp")) return new Integer(21);
				else return new Integer(100);
			}
		}));

        m_grid = new Grid<GwtNetInterfaceConfig>(m_store, new ColumnModel(configs));
        m_grid.setBorders(false);
        m_grid.setStateful(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        m_grid.setAutoExpandColumn("name");
        m_grid.getView().setAutoFill(true);
        m_grid.getView().setEmptyText(MSGS.netTableNoInterfaces());

        GridSelectionModel<GwtNetInterfaceConfig> selectionModel = new GridSelectionModel<GwtNetInterfaceConfig>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        m_grid.setSelectionModel(selectionModel);
        
        
        //
        // on selection, complete the switch
        m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtNetInterfaceConfig>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GwtNetInterfaceConfig> se) {            	
            	GwtNetInterfaceConfig gwtNetIfConfig = se.getSelectedItem();
            	if (gwtNetIfConfig != null) {
            	    m_selectedIfConfig = gwtNetIfConfig;
            		m_netConfigTabs.setNetInterface(gwtNetIfConfig);
            	}
            }
        });        
        
        m_loader.addLoadListener( new DataLoadListener(m_netConfigTabs, m_grid));
    }
    
    public GwtNetInterfaceConfig getSelectedNetInterfaceConfig() {
        return m_selectedIfConfig;
    }
    
    // --------------------------------------------------------------------------------------
    //
    //    Interface List Management
    //
    // --------------------------------------------------------------------------------------

    public void refresh() {
        
        // check if there is a selected item
        m_loader.load();
    }
    
    // --------------------------------------------------------------------------------------
    //
    //    Status Information
    //
    // --------------------------------------------------------------------------------------
    public boolean isDirty() {
    	if (m_netConfigTabs != null) {
    		return m_netConfigTabs.isDirty();
    	}
    	else {
    		return false;
    	}
    }
        
    // --------------------------------------------------------------------------------------
    //
    //    Data Load Listener
    //
    // --------------------------------------------------------------------------------------

    
    private static class BeforeSelectEventListener implements Listener<BaseEvent> 
    {    
    	private NetInterfaceConfigTabs m_netConfigTabs;
		private Grid<GwtNetInterfaceConfig>	m_grid;

    	public BeforeSelectEventListener(NetInterfaceConfigTabs netConfigTabs,
    									 Grid<GwtNetInterfaceConfig> grid)
    	{
    		m_netConfigTabs = netConfigTabs;
    		m_grid = grid;
    	}
    	
		public void handleEvent(BaseEvent be) 
		{
			final BaseEvent theEvent = be;
			if (m_netConfigTabs != null && m_netConfigTabs.isDirty()) {
		        
				// cancel the event first
	        	theEvent.setCancelled(true);
	
	        	// ask for confirmation before switching
	        	@SuppressWarnings("unchecked")
				SelectionEvent<ModelData> se = (SelectionEvent<ModelData>) be;
	        	
	        	final int selectionIndex = se.getIndex();
	        	final GwtNetInterfaceConfig netIfToSwitchTo = (GwtNetInterfaceConfig) se.getModel();
	        	MessageBox.confirm(MSGS.confirm(), 
		            	MSGS.deviceConfigDirty(),
		                new Listener<MessageBoxEvent>() {  
		                    public void handleEvent(MessageBoxEvent ce) {
	                            // if confirmed, delete
	                            Dialog  dialog = ce.getDialog(); 
	                            if (dialog.yesText.equals(ce.getButtonClicked().getText())) {
	                        		m_netConfigTabs.setNetInterface(netIfToSwitchTo);
	                            	m_grid.getSelectionModel().select(selectionIndex, false);                                    	
	                            }
		                    }
		        });
			}
		}	
    }

    
    private static class DataLoadListener extends LoadListener
    {
    	private NetInterfaceConfigTabs m_netConfigTabs;
		private Grid<GwtNetInterfaceConfig>	m_grid;
		private GwtNetInterfaceConfig       m_selectedNetIf;
		private BeforeSelectEventListener   m_beforeSelectListener;
		
        public DataLoadListener(NetInterfaceConfigTabs netConfigTabs,
				 			    Grid<GwtNetInterfaceConfig> grid) {

    		m_netConfigTabs = netConfigTabs;
        	m_grid = grid;

            //
            // Selection Listener for the component
            // make sure the form is not dirty before switching.
            m_beforeSelectListener = new BeforeSelectEventListener(m_netConfigTabs, m_grid); 
            m_grid.getSelectionModel().addListener(Events.BeforeSelect, m_beforeSelectListener);
        }
        
        public void loaderBeforeLoad(LoadEvent le) {
        	m_grid.mask(MSGS.loading());
        	m_netConfigTabs.mask(MSGS.loading());
        	m_selectedNetIf = m_grid.getSelectionModel().getSelectedItem();
        	m_grid.getSelectionModel().removeListener(Events.BeforeSelect, m_beforeSelectListener);
        }
        
        public void loaderLoad(LoadEvent le) {
        	if (le.exception != null) {
                FailureHandler.handle(le.exception);
            }
        	
        	if (m_grid.getStore().getModels().size() > 0) {
	        	if (m_selectedNetIf == null) {
	        		m_grid.getSelectionModel().select(0, false);
	        	}
	        	else {
	        		int i=0;
	        		for (GwtNetInterfaceConfig netIf : m_grid.getStore().getModels()) {
	        			String netIfSerial = netIf.getHwSerial();
	        			String selIfSerial = m_selectedNetIf.getHwSerial();
	        			
	        			// Select the proper interface based on interface name
	        			// In case of modem, the interface name may have changed, so try checking serial numbers
	        			if ((netIf.getName().equals(m_selectedNetIf.getName())) ||
	        					(netIfSerial != null && selIfSerial != null && netIfSerial.equals(selIfSerial))) {
	        				m_grid.getSelectionModel().select(i, false);
	        				break;
	        			}
	        			i++;
	        		}
	        	}
        	} else {
    			m_netConfigTabs.removeAllInterfaceTabs();
    		}

        	m_grid.unmask();
        	m_netConfigTabs.unmask();
        	m_grid.getSelectionModel().addListener(Events.BeforeSelect, m_beforeSelectListener);
        }
        
        public void loaderLoadException(LoadEvent le) 
        {
        	if (le.exception != null) {
    			for( StackTraceElement e : le.exception.getStackTrace()) {
    				Log.debug(e.toString());
    			}
                FailureHandler.handle(le.exception);
            }
        	m_grid.unmask();
        	m_netConfigTabs.unmask();
        }
    }
}
