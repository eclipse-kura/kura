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
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
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

public class NatConfigTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	
	private GwtSession            m_currentSession;

	private Grid<GwtFirewallNatEntry>   m_grid;
	
	private BaseListLoader<ListLoadResult<GwtFirewallNatEntry>> m_loader;
	private GwtFirewallNatEntry m_selectedEntry;
	private boolean m_dirty;
	
	private ToolBar m_natToolBar;
	private Button m_newButton;
	private Button m_editButton;
	private Button m_deleteButton;
	private Button m_applyButton;
		
	public NatConfigTab(GwtSession currentSession) {
		m_currentSession = currentSession; 
	}
	
	protected void onRender(final Element parent, int index) {
		
		super.onRender(parent, index);

		m_dirty = false;
		
		//
		// Border layout that expands to the whole screen
		setLayout(new FitLayout());
		setBorders(false);
		setId("firewall-nat");
		
        LayoutContainer mf = new LayoutContainer();
        mf.setLayout(new BorderLayout());
		
		//
		// Center Panel: NAT Table
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER, 1F);
		centerData.setMargins(new Margins(0, 0, 0, 0));
		centerData.setSplit(true);  
		centerData.setMinSize(0);
		
		ContentPanel natTablePanel = new ContentPanel();
		natTablePanel.setBorders(false);
		natTablePanel.setBodyBorder(false);
		natTablePanel.setHeaderVisible(false);
		natTablePanel.setScrollMode(Scroll.AUTO);
		natTablePanel.setLayout(new FitLayout());
		
		initToolBar();
        initGrid();
        
        natTablePanel.setTopComponent(m_natToolBar);
        natTablePanel.add(m_grid);
		mf.add(natTablePanel, centerData);
		
        add(mf);
        refresh();
	}
	
	public void refresh() {
		if (m_loader != null) {
			if (!m_dirty) {
				if (gwtNetworkService != null) {
					m_loader.load();
				}
			}
		}
	}
    
    public boolean isDirty() {
    	return m_dirty;
    }
    
    public List<GwtFirewallNatEntry> getCurrentConfigurations() {
    	if(m_grid != null) {
    		ListStore<GwtFirewallNatEntry> store = m_grid.getStore();
    		return (store != null)? store.getModels() : null;
    	} else {
    		return null;
    	}
    }
    
	private void initToolBar() {
		m_natToolBar = new ToolBar();
		m_natToolBar.setId("nat-toolbar");
		
		m_applyButton = new Button(MSGS.firewallApply(),
        		AbstractImagePrototype.create(Resources.INSTANCE.accept()),
                new SelectionListener<ButtonEvent>() {
            		@Override
            		public void componentSelected(ButtonEvent ce) {
            			Log.debug("about to updateDeviceFirewallNats()");
            			List<GwtFirewallNatEntry> updatedNatConf = getCurrentConfigurations();

            			if(updatedNatConf != null) {
            				Log.debug("got updatedNatsConf: " + updatedNatConf.size());
            				mask(MSGS.applying());
	            			gwtNetworkService.updateDeviceFirewallNATs(updatedNatConf, new AsyncCallback<Void>() {
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
		m_natToolBar.add(m_applyButton);
		m_natToolBar.add(new SeparatorToolItem());
		
		//
		// New Open Port Button
		m_newButton = new Button(MSGS.newButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.add()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				final NatForm natForm = new NatForm(m_currentSession);
				natForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
					public void handleEvent(ComponentEvent be) {
						// add the new entry to the grid and select it
						if (natForm.getNewFirewallNatEntry() != null) {
							if(!duplicateEntry(natForm.getNewFirewallNatEntry())) {
								m_grid.getStore().add(natForm.getNewFirewallNatEntry());
								if (!natForm.isCanceled()) {
									m_applyButton.enable();
									m_dirty = true;
									//fireEvent(Events.Change);
								}
							} else {
								MessageBox.alert(MSGS.firewallNatFormError(), MSGS.firewallNatFormDuplicate(), new Listener<MessageBoxEvent>() {  
									public void handleEvent(MessageBoxEvent ce) {
										//noop
									}
								});
							}
						}
					}
				});
				natForm.show();
			}

		});
		m_natToolBar.add(m_newButton);
		m_natToolBar.add(new SeparatorToolItem());

		//
		// Edit Open Port Button
		m_editButton = new Button(MSGS.editButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.edit()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				if (m_grid != null) {
					final GwtFirewallNatEntry natEntry = m_grid.getSelectionModel().getSelectedItem();
					if (natEntry != null) {
						final NatForm natForm = new NatForm(m_currentSession, m_grid.getSelectionModel().getSelectedItem());
						natForm.addListener(Events.Hide, new Listener<ComponentEvent>() {
							public void handleEvent(ComponentEvent be) {
								if(!duplicateEntry(natForm.getNewFirewallNatEntry())) {
									m_grid.getStore().remove(natEntry);
									m_grid.getStore().add(natForm.getExistingFirewallNatEntry());
									if (!natForm.isCanceled()) {
										m_applyButton.enable();
										m_dirty = true;
										//fireEvent(Events.Change);
									}
								} else {
									MessageBox.alert(MSGS.firewallNatFormError(), MSGS.firewallNatFormDuplicate(), new Listener<MessageBoxEvent>() {  
										public void handleEvent(MessageBoxEvent ce) {
											//noop
										}
									});
								}
							}
						});
						natForm.show();
					}
				}
			}

		});
		m_editButton.setEnabled(false);
		m_natToolBar.add(m_editButton);
		m_natToolBar.add(new SeparatorToolItem());

	    
		//
		// Delete Open Port Entry Button
		m_deleteButton = new Button(MSGS.deleteButton(), 
			    AbstractImagePrototype.create(Resources.INSTANCE.delete()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				
				if (m_grid != null) {
					
					final GwtFirewallNatEntry firewallNatEntry = m_grid.getSelectionModel().getSelectedItem();
					if (firewallNatEntry != null) {

						// ask for confirmation						
						MessageBox.confirm(MSGS.confirm(), MSGS.firewallNatDeleteConfirmation(firewallNatEntry.getInInterface()),
							new Listener<MessageBoxEvent>() {  
							    public void handleEvent(MessageBoxEvent ce) {
							    	
							    	Log.debug("Trying to delete: " + firewallNatEntry.getInInterface());
							    	Log.debug("Button " + ce.getButtonClicked().getText());
							    	
							    	if(ce.getButtonClicked().getText().equals("Yes")) {
							    		m_grid.getStore().remove(firewallNatEntry);
							    		m_applyButton.enable();
							    		m_dirty = true;
										//fireEvent(Events.Change);
							    	}
							    }
							}
						);
					}
				}
			}
		});
		m_deleteButton.setEnabled(false);
		m_natToolBar.add(m_deleteButton);
	}
	
	private void initGrid() {
		//
		// Column Configuration
		ColumnConfig column = null;
		List<ColumnConfig> configs = new ArrayList<ColumnConfig>();
		
		column = new ColumnConfig("inInterface", MSGS.firewallNatInInterface(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("outInterface", MSGS.firewallNatOutInterface(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("protocol", MSGS.firewallNatProtocol(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("sourceNetwork", MSGS.firewallNatSourceNetwork(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("destinationNetwork", MSGS.firewallNatDestinationNetwork(), 120);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		column = new ColumnConfig("masquerade", MSGS.firewallNatMasquerade(), 60);
		column.setAlignment(HorizontalAlignment.CENTER);
		configs.add(column);
		
		// rpc data proxy  
		RpcProxy<ListLoadResult<GwtFirewallNatEntry>> proxy = new RpcProxy<ListLoadResult<GwtFirewallNatEntry>>() {
			@Override
			protected void load(Object loadConfig, AsyncCallback<ListLoadResult<GwtFirewallNatEntry>> callback) {
				gwtNetworkService.findDeficeFirewallNATs(callback);
			}
		};
        
        m_loader = new BaseListLoader<ListLoadResult<GwtFirewallNatEntry>>(proxy);
        m_loader.setSortDir(SortDir.DESC);  
        m_loader.setSortField("inInterface"); 
        
        SwappableListStore<GwtFirewallNatEntry> m_store = new SwappableListStore<GwtFirewallNatEntry>(m_loader);
        m_store.setKeyProvider( new ModelKeyProvider<GwtFirewallNatEntry>() {            
            public String getKey(GwtFirewallNatEntry firewallNatEntry) {
                return firewallNatEntry.getInInterface();
            }
        });
        
        m_grid = new Grid<GwtFirewallNatEntry>(m_store, new ColumnModel(configs));
        m_grid.setBorders(false);
        m_grid.setStateful(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        m_grid.setAutoExpandColumn("inInterface");
        m_grid.getView().setAutoFill(true);
        
        m_loader.addLoadListener(new DataLoadListener(m_grid));

        GridSelectionModel<GwtFirewallNatEntry> selectionModel = new GridSelectionModel<GwtFirewallNatEntry>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        m_grid.setSelectionModel(selectionModel);
        m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtFirewallNatEntry>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GwtFirewallNatEntry> se) {
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
	
	private class DataLoadListener extends LoadListener {
		private Grid<GwtFirewallNatEntry> m_grid;
		private GwtFirewallNatEntry m_selectedEntry;

		public DataLoadListener(Grid<GwtFirewallNatEntry> grid) {
			m_grid = grid;
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
				ListStore<GwtFirewallNatEntry> store = m_grid.getStore();
				GwtFirewallNatEntry modelEntry = store.findModel(m_selectedEntry.getInInterface());
				if (modelEntry != null) {
					m_grid.getSelectionModel().select(modelEntry, false);
					m_grid.getView().focusRow(store.indexOf(modelEntry));
				}
			}
		}
	}
	 
	private boolean duplicateEntry(GwtFirewallNatEntry firewallNatEntry) {

		boolean isDuplicateEntry = false;
		List<GwtFirewallNatEntry> entries = m_grid.getStore().getModels();
		if (entries != null && firewallNatEntry != null) {
			for (GwtFirewallNatEntry entry : entries) {
				
				String sourceNetwork = (entry.getSourceNetwork() != null)? entry.getSourceNetwork() : "0.0.0.0/0"; 
				String destinationNetwork = (entry.getDestinationNetwork() != null)? entry.getDestinationNetwork() : "0.0.0.0/0";
				String newSourceNetwork = (firewallNatEntry.getSourceNetwork() != null)? firewallNatEntry.getSourceNetwork() : "0.0.0.0/0";
				String newDestinationNetwork = (firewallNatEntry.getDestinationNetwork() != null)? firewallNatEntry.getDestinationNetwork() : "0.0.0.0/0";
				
				if (entry.getInInterface().equals(firewallNatEntry.getInInterface())
						&& entry.getOutInterface().equals(firewallNatEntry.getOutInterface())
						&& entry.getProtocol().equals(firewallNatEntry.getProtocol())
						&& sourceNetwork.equals(newSourceNetwork)
						&& destinationNetwork.equals(newDestinationNetwork)) {
					isDuplicateEntry = true;
					break;
				}
			}
		}
		
		return isDuplicateEntry;
	}
}
