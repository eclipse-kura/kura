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
package org.eclipse.kura.web.client.firewall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.SwappableListStore;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class PortForwardingConfigTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private GwtSession            m_currentSession;

	private Grid<GwtFirewallPortForwardEntry>   m_grid;
	private BaseListLoader<ListLoadResult<GwtFirewallPortForwardEntry>> m_loader;
	private GwtFirewallPortForwardEntry m_selectedEntry;
	private boolean m_dirty;
	
	private ToolBar            m_portForwardToolBar;
	private Button             m_newButton;
	private Button             m_editButton;
	private Button             m_deleteButton;
	private Button				m_applyButton;
	
    public PortForwardingConfigTab(GwtSession currentSession) {
    	m_currentSession = currentSession;
    }

	protected void onRender(final Element parent, int index) {
		
		super.onRender(parent, index);

		m_dirty = false;
		
		//
		// Borderlayout that expands to the whole screen
		setLayout(new FitLayout());
		setBorders(false);
		setId("firewall-port-forwarding");
		
        LayoutContainer mf = new LayoutContainer();
        mf.setLayout(new BorderLayout());
		
		//
		// Center Panel: Open Ports Table
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER, 1F);
		centerData.setMargins(new Margins(0, 0, 0, 0));
		centerData.setSplit(true);  
		centerData.setMinSize(0);
		
		ContentPanel portForwardTablePanel = new ContentPanel();
		portForwardTablePanel.setBorders(false);
		portForwardTablePanel.setBodyBorder(false);
		portForwardTablePanel.setHeaderVisible(false);
		portForwardTablePanel.setScrollMode(Scroll.AUTO);
		portForwardTablePanel.setLayout(new FitLayout());
		
		initToolBar();
        initGrid();
        
        portForwardTablePanel.setTopComponent(m_portForwardToolBar);
        portForwardTablePanel.add(m_grid);
		mf.add(portForwardTablePanel, centerData);
		
        add(mf);
        
        refresh();
	}
	
    public void refresh() {
        m_loader.load();                    
    }
    
    public List<GwtFirewallPortForwardEntry> getCurrentConfigurations() {
    	if(m_grid != null) {
    		ListStore<GwtFirewallPortForwardEntry> store = m_grid.getStore();
    		return store.getModels();
    	} else {
    		return null;
    	}
    }
    
    public boolean isDirty() {
    	return m_dirty;
    }
	
	private void initToolBar() {
		
		m_portForwardToolBar = new ToolBar();
		m_portForwardToolBar.setId("firewall-port-forwarding-toolbar");
		
		m_applyButton = new Button(MSGS.firewallApply(),
        		AbstractImagePrototype.create(Resources.INSTANCE.accept()),
                new SelectionListener<ButtonEvent>() {
            		@Override
            		public void componentSelected(ButtonEvent ce) {
            			Log.debug("about to updateDeviceFirewallPortForwards()");
            			List<GwtFirewallPortForwardEntry> updatedPortForwardConf = getCurrentConfigurations();

            			if(updatedPortForwardConf != null) {
            				Log.debug("got updatedPortForwardConf: " + updatedPortForwardConf.size());
            				mask(MSGS.applying());
	            			gwtNetworkService.updateDeviceFirewallPortForwards(updatedPortForwardConf, new AsyncCallback<Void>() {
	            				public void onSuccess(Void result) {
									Log.debug("updated!");
									m_dirty = false;
									m_applyButton.disable();
									unmask();
								}
	            				
								public void onFailure(Throwable caught) {
									Log.debug("caught: " + caught.toString());
									unmask();
									FailureHandler.handle(caught);
								}
	            			});
            			}
            		}
    	});
		
		m_applyButton.disable();
		m_portForwardToolBar.add(m_applyButton);
		m_portForwardToolBar.add(new SeparatorToolItem());

		//
		// New Open Port Button
		m_newButton = new Button(MSGS.newButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.add()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				final PortForwardForm portForwardForm = new PortForwardForm(m_currentSession);
				portForwardForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
					public void handleEvent(ComponentEvent be) {
						// add the new entry to the grid and select it
						if (portForwardForm.getNewPortForwardEntry() != null) {
							if(!duplicateEntry(portForwardForm.getNewPortForwardEntry())) {
								m_grid.getStore().add(portForwardForm.getNewPortForwardEntry());
								if (!portForwardForm.isCanceled()) {
									m_applyButton.enable();
									m_dirty = true;
								}
							} else {
								MessageBox.alert(MSGS.firewallPortForwardFormError(), MSGS.firewallPortForwardFormDuplicate(), new Listener<MessageBoxEvent>() {  
									public void handleEvent(MessageBoxEvent ce) {
										//noop
									}
								});
							}
						}
					}
				});
				portForwardForm.show();
			}

		});
		m_portForwardToolBar.add(m_newButton);
		m_portForwardToolBar.add(new SeparatorToolItem());

		//
		// Edit Open Port Button
		m_editButton = new Button(MSGS.editButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.edit()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				if (m_grid != null) {
					final GwtFirewallPortForwardEntry portForwardEntry = m_grid.getSelectionModel().getSelectedItem();
					if (portForwardEntry != null) {
						final PortForwardForm portForwardForm = new PortForwardForm(m_currentSession, m_grid.getSelectionModel().getSelectedItem());
						portForwardForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
							public void handleEvent(ComponentEvent be) {
								if(!duplicateEntry(portForwardForm.getNewPortForwardEntry())) {
									m_grid.getStore().remove(portForwardEntry);
									m_grid.getStore().add(portForwardForm.getExistingPortForwardEntry());
									if (!portForwardForm.isCanceled()) {
										m_dirty = true;
										m_applyButton.enable();
									}
								} else {
									MessageBox.alert(MSGS.firewallPortForwardFormError(), MSGS.firewallPortForwardFormDuplicate(), new Listener<MessageBoxEvent>() {  
										public void handleEvent(MessageBoxEvent ce) {
											//noop
										}
									});
								}
							}
						});
						portForwardForm.show();
					}
				}
			}

		});
		m_editButton.setEnabled(false);
		m_portForwardToolBar.add(m_editButton);
		m_portForwardToolBar.add(new SeparatorToolItem());

	    
		//
		// Delete Open Port Entry Button
		m_deleteButton = new Button(MSGS.deleteButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.delete()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				
				if (m_grid != null) {
					
					final GwtFirewallPortForwardEntry portForwardEntry = m_grid.getSelectionModel().getSelectedItem();
					if (portForwardEntry != null) {

						// ask for confirmation						
						MessageBox.confirm(MSGS.confirm(), MSGS.firewallPortForwardDeleteConfirmation(portForwardEntry.getInPort().toString()),
							new Listener<MessageBoxEvent>() {  
							    public void handleEvent(MessageBoxEvent ce) {
							    	
							    	Log.debug("Trying to delete: " + portForwardEntry.getInPort().toString());
							    	Log.debug("Button " + ce.getButtonClicked().getText());
							    	
							    	if(ce.getButtonClicked().getText().equals("Yes")) {
							    		m_grid.getStore().remove(portForwardEntry);
							    		m_applyButton.enable();
							    		m_dirty = true;
							    	}
							    }
							}
						);
					}
				}
			}
		});
		m_deleteButton.setEnabled(false);
		m_portForwardToolBar.add(m_deleteButton);
	}
	
	private void initGrid() {

		//
		// Column Configuration
		ColumnConfig column = null;
		List<ColumnConfig> configs = new ArrayList<ColumnConfig>();
		
		column = new ColumnConfig("inboundInterface", MSGS.firewallPortForwardInboundInterface(), 80);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("outboundInterface", MSGS.firewallPortForwardOutboundInterface(), 80);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("address", MSGS.firewallPortForwardAddress(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("protocol", MSGS.firewallPortForwardProtocol(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("inPort", MSGS.firewallPortForwardInPort(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("outPort", MSGS.firewallPortForwardOutPort(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("masquerade", MSGS.firewallPortForwardMasquerade(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("permittedNetwork", MSGS.firewallPortForwardPermittedNetwork(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("permittedMAC", MSGS.firewallPortForwardPermittedMac(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("sourcePortRange", MSGS.firewallPortForwardSourcePortRange(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);

        // rpc data proxy  
        RpcProxy<ListLoadResult<GwtFirewallPortForwardEntry>> proxy = new RpcProxy<ListLoadResult<GwtFirewallPortForwardEntry>>() {  
          @Override  
          protected void load(Object loadConfig, AsyncCallback<ListLoadResult<GwtFirewallPortForwardEntry>> callback) {  
        	  gwtNetworkService.findDeviceFirewallPortForwards(callback);
          }  
        };  
        
        m_loader = new BaseListLoader<ListLoadResult<GwtFirewallPortForwardEntry>>(proxy);
        m_loader.setSortDir(SortDir.DESC);  
        m_loader.setSortField("inPort"); 
        m_loader.setRemoteSort(true);  
        
        SwappableListStore<GwtFirewallPortForwardEntry> m_store = new SwappableListStore<GwtFirewallPortForwardEntry>(m_loader);
        m_store.setKeyProvider( new ModelKeyProvider<GwtFirewallPortForwardEntry>() {            
            public String getKey(GwtFirewallPortForwardEntry portForwardEntry) {
                return portForwardEntry.getInPort().toString();
            }
        });
        
        m_grid = new Grid<GwtFirewallPortForwardEntry>(m_store, new ColumnModel(configs));
        m_grid.setBorders(false);
        m_grid.setStateful(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        m_grid.setAutoExpandColumn("inPort");
        m_grid.getView().setAutoFill(true);
        //m_grid.getView().setEmptyText(MSGS.deviceTableNoDevices());

        m_loader.addLoadListener(new DataLoadListener(m_grid));

        GridSelectionModel<GwtFirewallPortForwardEntry> selectionModel = new GridSelectionModel<GwtFirewallPortForwardEntry>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        m_grid.setSelectionModel(selectionModel);
        m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtFirewallPortForwardEntry>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GwtFirewallPortForwardEntry> se) {
                m_selectedEntry = se.getSelectedItem();
                if (m_selectedEntry != null) {
                	m_editButton.setEnabled(true);
                	m_deleteButton.setEnabled(true);
                } else {
                	m_editButton.setEnabled(false);
                	m_deleteButton.setEnabled(false);                 
                }
            }
        });
	}
	
    private class DataLoadListener extends LoadListener
    {
        private Grid<GwtFirewallPortForwardEntry> m_grid;
        private GwtFirewallPortForwardEntry       m_selectedEntry;

        public DataLoadListener(Grid<GwtFirewallPortForwardEntry> grid) {
            m_grid 			 = grid;
            m_selectedEntry = null;
        }
        
        public void loaderBeforeLoad(LoadEvent le) {
        	m_selectedEntry = m_grid.getSelectionModel().getSelectedItem();
        }
        
        public void loaderLoad(LoadEvent le) {
        	if (le.exception != null) {
                FailureHandler.handle(le.exception);
            }
        	
            if (m_selectedEntry != null) {
                ListStore<GwtFirewallPortForwardEntry> store = m_grid.getStore();
                GwtFirewallPortForwardEntry modelEntry = store.findModel(m_selectedEntry.getInPort().toString());
                if (modelEntry != null) {
                    m_grid.getSelectionModel().select(modelEntry, false);
                    m_grid.getView().focusRow(store.indexOf(modelEntry));
                }
            }
        }
    }
    
    private boolean duplicateEntry(GwtFirewallPortForwardEntry portForwardEntry) {
		
		boolean isDuplicateEntry = false; 
		List<GwtFirewallPortForwardEntry> entries = m_grid.getStore().getModels();
		if (entries != null && portForwardEntry != null) {
			for (GwtFirewallPortForwardEntry entry : entries) {
				if (entry.getInboundInterface().equals(portForwardEntry.getInboundInterface())
						&& entry.getOutboundInterface().equals(portForwardEntry.getOutboundInterface())
						&& entry.getAddress().equals(portForwardEntry.getAddress())
						&& entry.getProtocol().equals(portForwardEntry.getProtocol())
						&& entry.getOutPort().equals(portForwardEntry.getOutPort())
						&& entry.getInPort().equals(portForwardEntry.getInPort())) {
					
					String permittedNetwork = (entry.getPermittedNetwork() != null)? entry.getPermittedNetwork() : "0.0.0.0/0";
					String newPermittedNetwork = (portForwardEntry.getPermittedNetwork() != null)?  portForwardEntry.getPermittedNetwork() : "0.0.0.0/0";
					String permittedMAC = (entry.getPermittedMAC() != null)? entry.getPermittedMAC().toUpperCase() : "";
					String newPermittedMAC = (portForwardEntry.getPermittedMAC() != null)? portForwardEntry.getPermittedMAC().toUpperCase() : "";
					String sourcePortRange = (entry.getSourcePortRange() != null)? entry.getSourcePortRange() : "";
					String newSourcePortRange = (portForwardEntry.getSourcePortRange() != null)? portForwardEntry.getSourcePortRange() : "";
					
					if (permittedNetwork.equals(newPermittedNetwork)
							&& permittedMAC.equals(newPermittedMAC)
							&& sourcePortRange.equals(newSourcePortRange)) {
						isDuplicateEntry = true;
						break;
					}
				}
			}
		}
		return isDuplicateEntry;
	}
}
