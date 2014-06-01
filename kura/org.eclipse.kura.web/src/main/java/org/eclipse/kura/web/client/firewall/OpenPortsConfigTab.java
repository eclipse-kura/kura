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
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
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

public class OpenPortsConfigTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private GwtSession            m_currentSession;

	private Grid<GwtFirewallOpenPortEntry>   m_grid;
	private BaseListLoader<ListLoadResult<GwtFirewallOpenPortEntry>> m_openPortsLoader;
	private GwtFirewallOpenPortEntry m_selectedEntry;
	private boolean m_dirty;
	
	private ToolBar            m_openPortsToolBar;
	private Button             m_newButton;
	private Button             m_editButton;
	private Button             m_deleteButton;
	private Button				m_applyButton;
	
    public OpenPortsConfigTab(GwtSession currentSession) {
    	m_currentSession = currentSession;
    }

	protected void onRender(final Element parent, int index) {
		
		super.onRender(parent, index);

		m_dirty = false;
		
		//
		// Borderlayout that expands to the whole screen
		setLayout(new FitLayout());
		setBorders(false);
		setId("firewall-open-ports");
		
        LayoutContainer mf = new LayoutContainer();
        mf.setLayout(new BorderLayout());
		
		//
		// Center Panel: Open Ports Table
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER, 1F);
		centerData.setMargins(new Margins(0, 0, 0, 0));
		centerData.setSplit(true);  
		centerData.setMinSize(0);
		
		ContentPanel openPortsTablePanel = new ContentPanel();
		openPortsTablePanel.setBorders(false);
		openPortsTablePanel.setBodyBorder(false);
		openPortsTablePanel.setHeaderVisible(false);
		openPortsTablePanel.setScrollMode(Scroll.AUTO);
		openPortsTablePanel.setLayout(new FitLayout());
		
        initOpenPortsToolBar();
        initOpenPortsGrid();
        
		openPortsTablePanel.setTopComponent(m_openPortsToolBar);
		openPortsTablePanel.add(m_grid);
		mf.add(openPortsTablePanel, centerData);
		
        add(mf);
        refresh();
	}
	
    public void refresh() {
        m_openPortsLoader.load();                    
    }
    
    public List<GwtFirewallOpenPortEntry> getCurrentConfigurations() {
    	if(m_grid != null) {
    		ListStore<GwtFirewallOpenPortEntry> store = m_grid.getStore();
    		return store.getModels();
    	} else {
    		return null;
    	}
    }
    
    public boolean isDirty() {
    	return m_dirty;
    }
	
	private void initOpenPortsToolBar() {
		
		m_openPortsToolBar = new ToolBar();
		m_openPortsToolBar.setId("firewall-open-ports-toolbar");
		
		m_applyButton = new Button(MSGS.firewallApply(),
        		AbstractImagePrototype.create(Resources.INSTANCE.accept()),
                new SelectionListener<ButtonEvent>() {
            		@Override
            		public void componentSelected(ButtonEvent ce) {
            			Log.debug("about to updateDeviceFirewallOpenPorts() and updateDeviceFirewallPortForwards()");
            			List<GwtFirewallOpenPortEntry> updatedOpenPortConf = getCurrentConfigurations();

            			if(updatedOpenPortConf != null) {
            				Log.debug("got updatedOpenPortConf: " + updatedOpenPortConf.size());
            				mask(MSGS.applying());
	            			gwtNetworkService.updateDeviceFirewallOpenPorts(updatedOpenPortConf, new AsyncCallback<Void>() {
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
		m_openPortsToolBar.add(m_applyButton);
		m_openPortsToolBar.add(new SeparatorToolItem());
		
		//
		// New Open Port Button
		m_newButton = new Button(MSGS.newButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.add()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				final OpenPortForm openPortForm = new OpenPortForm(m_currentSession);
				openPortForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
					public void handleEvent(ComponentEvent be) {
						// add the new open port entry to the grid and select it
						if (openPortForm.getNewOpenPortEntry() != null) {
							if(!duplicateEntry(openPortForm.getNewOpenPortEntry())) {
								m_grid.getStore().add(openPortForm.getNewOpenPortEntry());
								if (!openPortForm.isCanceled()) {
									m_applyButton.enable();
									m_dirty = true;
								}
							} else {
								MessageBox.alert(MSGS.firewallOpenPortFormError(), MSGS.firewallOpenPortFormDuplicate(), new Listener<MessageBoxEvent>() {  
									public void handleEvent(MessageBoxEvent ce) {
										//noop
									}
								});
							}
						}
					}
				});
				openPortForm.show();
			}

		});
		m_openPortsToolBar.add(m_newButton);
		m_openPortsToolBar.add(new SeparatorToolItem());

		//
		// Edit Open Port Button
		m_editButton = new Button(MSGS.editButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.edit()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				if (m_grid != null) {
					final GwtFirewallOpenPortEntry openPortEntry = m_grid.getSelectionModel().getSelectedItem();
					if (openPortEntry != null) {
						if(openPortEntry.getPort().equals(22)) {
							MessageBox.alert(MSGS.firewallOpenPortsCaution(), MSGS.firewallOpenPorts22(), new Listener<MessageBoxEvent>() {  
								public void handleEvent(MessageBoxEvent ce) {
									showEditOpenPort(openPortEntry);
								}
							});
						} else if(openPortEntry.getPort().equals(80)) {
							MessageBox.alert(MSGS.firewallOpenPortsCaution(), MSGS.firewallOpenPorts80(), new Listener<MessageBoxEvent>() {  
								public void handleEvent(MessageBoxEvent ce) {
									showEditOpenPort(openPortEntry);
								}
							});
						} else {
							showEditOpenPort(openPortEntry);
						}
					}
				}
			}

		});
		m_editButton.setEnabled(false);
		m_openPortsToolBar.add(m_editButton);
		m_openPortsToolBar.add(new SeparatorToolItem());

	    
		//
		// Delete Open Port Entry Button
		m_deleteButton = new Button(MSGS.deleteButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.delete()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				
				if (m_grid != null) {
					
					final GwtFirewallOpenPortEntry openPortEntry = m_grid.getSelectionModel().getSelectedItem();
					if (openPortEntry != null) {

						if(openPortEntry.getPort().equals(22)) {
							// ask for confirmation						
							MessageBox.confirm(MSGS.confirm(), MSGS.firewallOpenPortDeleteConfirmation22(openPortEntry.getPort().toString()),
								new Listener<MessageBoxEvent>() {  
								    public void handleEvent(MessageBoxEvent ce) {
								    	
								    	Log.debug("Trying to delete: " + openPortEntry.getPort().toString());
								    	Log.debug("Button " + ce.getButtonClicked().getText());
								    	
								    	if(ce.getButtonClicked().getText().equals("Yes")) {
								    		m_grid.getStore().remove(openPortEntry);
								    		m_applyButton.enable();
								    		m_dirty = true;
								    	}
								    }
								}
							);
						} else if(openPortEntry.getPort().equals(80)) {
							// ask for confirmation						
							MessageBox.confirm(MSGS.confirm(), MSGS.firewallOpenPortDeleteConfirmation80(openPortEntry.getPort().toString()),
								new Listener<MessageBoxEvent>() {  
								    public void handleEvent(MessageBoxEvent ce) {
								    	
								    	Log.debug("Trying to delete: " + openPortEntry.getPort().toString());
								    	Log.debug("Button " + ce.getButtonClicked().getText());
								    	
								    	if(ce.getButtonClicked().getText().equals("Yes")) {
								    		m_grid.getStore().remove(openPortEntry);
								    		m_applyButton.enable();
								    		m_dirty = true;
								    	}
								    }
								}
							);
						} else {
							// ask for confirmation						
							MessageBox.confirm(MSGS.confirm(), MSGS.firewallOpenPortDeleteConfirmation(openPortEntry.getPort().toString()),
								new Listener<MessageBoxEvent>() {  
								    public void handleEvent(MessageBoxEvent ce) {
								    	
								    	Log.debug("Trying to delete: " + openPortEntry.getPort().toString());
								    	Log.debug("Button " + ce.getButtonClicked().getText());
								    	
								    	if(ce.getButtonClicked().getText().equals("Yes")) {
								    		m_grid.getStore().remove(openPortEntry);
								    		m_applyButton.enable();
								    		m_dirty = true;
								    	}
								    }
								}
							);
						}
					}
				}
			}
		});
		m_deleteButton.setEnabled(false);
		m_openPortsToolBar.add(m_deleteButton);
	}
	
	private void initOpenPortsGrid() {

		//
		// Column Configuration
		ColumnConfig column = null;
		List<ColumnConfig> configs = new ArrayList<ColumnConfig>();
		
		column = new ColumnConfig("port", MSGS.firewallOpenPort(), 100);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("protocol", MSGS.firewallOpenPortProtocol(), 100);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);

		column = new ColumnConfig("permittedNetwork", MSGS.firewallOpenPortPermittedNetwork(), 130);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("permittedInterfaceName", MSGS.firewallOpenPortPermittedInterfaceName(), 130);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("unpermittedInterfaceName", MSGS.firewallOpenPortUnpermittedInterfaceName(), 130);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("permittedMAC", MSGS.firewallOpenPortPermittedMac(), 130);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("sourcePortRange", MSGS.firewallOpenPortSourcePortRange(), 130);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);

        // rpc data proxy  
        RpcProxy<ListLoadResult<GwtFirewallOpenPortEntry>> proxy = new RpcProxy<ListLoadResult<GwtFirewallOpenPortEntry>>() {  
          @Override  
          protected void load(Object loadConfig, AsyncCallback<ListLoadResult<GwtFirewallOpenPortEntry>> callback) {  
        	  gwtNetworkService.findDeviceFirewallOpenPorts(callback);
          }  
        };  
        
        m_openPortsLoader = new BaseListLoader<ListLoadResult<GwtFirewallOpenPortEntry>>(proxy);
        m_openPortsLoader.setSortDir(SortDir.DESC);  
        m_openPortsLoader.setSortField("port"); 
        m_openPortsLoader.setRemoteSort(true);  
        
        SwappableListStore<GwtFirewallOpenPortEntry> m_store = new SwappableListStore<GwtFirewallOpenPortEntry>(m_openPortsLoader);
        m_store.setKeyProvider( new ModelKeyProvider<GwtFirewallOpenPortEntry>() {            
            public String getKey(GwtFirewallOpenPortEntry openPortEntry) {
                return openPortEntry.getPort().toString();
            }
        });
        
        m_grid = new Grid<GwtFirewallOpenPortEntry>(m_store, new ColumnModel(configs));
        m_grid.setBorders(false);
        m_grid.setStateful(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        m_grid.setAutoExpandColumn("port");
        m_grid.getView().setAutoFill(true);
        //m_grid.getView().setEmptyText(MSGS.deviceTableNoDevices());

        m_openPortsLoader.addLoadListener(new DataLoadListener(m_grid));

        GridSelectionModel<GwtFirewallOpenPortEntry> selectionModel = new GridSelectionModel<GwtFirewallOpenPortEntry>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        m_grid.setSelectionModel(selectionModel);
        m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtFirewallOpenPortEntry>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GwtFirewallOpenPortEntry> se) {
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
        private Grid<GwtFirewallOpenPortEntry> m_grid;
        private GwtFirewallOpenPortEntry       m_selectedEntry;

        public DataLoadListener(Grid<GwtFirewallOpenPortEntry> grid) {
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
                ListStore<GwtFirewallOpenPortEntry> store = m_grid.getStore();
                GwtFirewallOpenPortEntry modelEntry = store.findModel(m_selectedEntry.getPort().toString());
                if (modelEntry != null) {
                    m_grid.getSelectionModel().select(modelEntry, false);
                    m_grid.getView().focusRow(store.indexOf(modelEntry));
                }
            }
        }
    }
    
    private boolean duplicateEntry(GwtFirewallOpenPortEntry openPortEntry) {
    	List<GwtFirewallOpenPortEntry> entries = m_grid.getStore().getModels();
    	if (entries != null && openPortEntry != null) {
	    	for(GwtFirewallOpenPortEntry entry : entries) {
	    		if (entry.getPort().equals(openPortEntry.getPort()) &&
	    				entry.getPermittedNetwork().equals(openPortEntry.getPermittedNetwork())) {
	    			return true;
	    		}
	    	}
    	}
    	
    	return false;
    }
    
    private void showEditOpenPort(final GwtFirewallOpenPortEntry openPortEntry) {
    	final OpenPortForm openPortForm = new OpenPortForm(m_currentSession, m_grid.getSelectionModel().getSelectedItem());
		openPortForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
			public void handleEvent(ComponentEvent be) {
				if(!duplicateEntry(openPortForm.getNewOpenPortEntry())) {
					m_grid.getStore().remove(openPortEntry);
					m_grid.getStore().add(openPortForm.getExistingOpenPortEntry());
					if (!openPortForm.isCanceled()) {
						m_applyButton.enable();
						m_dirty = true;
					}
				} else {
					MessageBox.alert(MSGS.firewallOpenPortFormError(), MSGS.firewallOpenPortFormDuplicate(), new Listener<MessageBoxEvent>() {  
						public void handleEvent(MessageBoxEvent ce) {
							//noop
						}
					});
				}
			}
		});
		openPortForm.show();
    }
}
