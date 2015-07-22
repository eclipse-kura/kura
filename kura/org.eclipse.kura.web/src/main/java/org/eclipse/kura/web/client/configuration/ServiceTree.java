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
package org.eclipse.kura.web.client.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.ScaledAbstractImagePrototype;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;

import com.extjs.gxt.ui.client.data.BaseTreeLoader;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.ModelIconProvider;
import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Util;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGrid;
import com.extjs.gxt.ui.client.widget.treegrid.WidgetTreeGridCellRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;

public class ServiceTree extends ContentPanel
{
	private static final Messages MSGS = GWT.create(Messages.class);
	
	private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

	private GwtSession           m_currentSession;

	private ContentPanel         m_centerPanel;
	private ServiceConfiguration m_serviceConfiguration; 
	
    @SuppressWarnings("rawtypes")
	private BaseTreeLoader       m_loader;

	private TreeStore<ModelData> m_servicesStore; 
	private TreeGrid<ModelData>  m_servicesTree;

	
    public ServiceTree(GwtSession currentSession,
    				   ContentPanel centerPanel) 
    {
		m_currentSession = currentSession;    
		m_centerPanel    = centerPanel;
    }

    
	protected void onRender(Element parent, int index) 
	{
		super.onRender(parent, index);

		setBorders(false);
		setBodyBorder(true);
		setAnimCollapse(true);
		setHeaderVisible(true);
		setHeading(MSGS.services());
		setLayout( new FitLayout());
		initServiceTree();
		add(m_servicesTree, new MarginData(0, 0, 0, 0));
	}
	
	
	public boolean isDirty()
	{
		return (m_serviceConfiguration != null && m_serviceConfiguration.isDirty());
	}
	
	
	@SuppressWarnings("unchecked")
	private void initServiceTree() 
	{
		//
		// Service Tree
        // loader and store
        RpcProxy<List<GwtConfigComponent>> proxy = new RpcProxy<List<GwtConfigComponent>>() {  
            @Override  
            protected void load(Object loadConfig, final AsyncCallback<List<GwtConfigComponent>> callback) {
            	gwtComponentService.findComponentConfigurations( new AsyncCallback<List<GwtConfigComponent>>() {
					public void onFailure(Throwable caught) {
						FailureHandler.handle(caught);
					}
					public void onSuccess(List<GwtConfigComponent> results) {
						callback.onSuccess(results);
					}
				});
            }
        };

        m_loader = new BaseTreeLoader<GwtConfigComponent>(proxy);        
        m_servicesStore = new TreeStore<ModelData>(m_loader);
        m_servicesStore.setKeyProvider( new ModelKeyProvider<ModelData>() {            
            public String getKey(ModelData component) {
            	if (component instanceof GwtConfigComponent) {
            		return ((GwtConfigComponent) component).getComponentId();
            	}
                return component.toString();
            }
        });
        
        ColumnConfig name1 = new ColumnConfig("componentName", "Name", 100);
        name1.setRenderer(new WidgetTreeGridCellRenderer<ModelData>(){
            @Override
            public Widget getWidget(ModelData model, String property, ColumnData config, int rowIndex, int colIndex,
                                    ListStore<ModelData> store, Grid<ModelData> grid) {         
                Label label = new Label((String)model.get(property));
                label.setStyleAttribute("padding-left", "5px");
                return label;
            }
        });
        ColumnModel cm1 = new ColumnModel(Arrays.asList(name1));

        m_servicesTree = new TreeGrid<ModelData>(m_servicesStore, cm1);
        m_servicesTree.setId("nav-services");
        m_servicesTree.setBorders(false);
        m_servicesTree.setHideHeaders(true);
        m_servicesTree.setAutoExpandColumn("componentName");
        m_servicesTree.getTreeView().setRowHeight(36);
        m_servicesTree.setIconProvider( new ModelIconProvider<ModelData>() {
			public AbstractImagePrototype getIcon(ModelData model) {
				if (model.get("componentIcon") != null) {										
					String icon = (String) model.get("componentIcon");
					if ("BluetoothService".equals(icon)){
						return AbstractImagePrototype.create(Resources.INSTANCE.bluetooth32());
					}
            		if ("CloudService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.cloud32());
            		}
            		if ("DiagnosticsService".equals(icon)) {
                        return AbstractImagePrototype.create(Resources.INSTANCE.diagnostics32());
					} 
            		else if ("ClockService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.clock32());
            		}
            		else if ("DataService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.databaseConnect32());
            		}
            		else if ("MqttDataTransport".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.mqtt32());
            		}
            		else if ("PositionService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.gps32());
            		}
            		else if ("WatchdogService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.dog32());
            		}
                    else if ("SslManagerService".equals(icon)) {
                        return AbstractImagePrototype.create(Resources.INSTANCE.lock32());
                    }
            		else if ("VpnService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.vpn32());
            		}
            		else if ("ProvisioningService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.provisioning32());
            		}
            		else if ("CommandPasswordService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.command32());
            		}
            		else if ("DenaliService".equals(icon)) {
            			return AbstractImagePrototype.create(Resources.INSTANCE.systemLock32());
            		}
            		else if (icon != null && 
            				(icon.toLowerCase().startsWith("http://") ||
            			     icon.toLowerCase().startsWith("https://")) &&
            				Util.isImagePath(icon)) {
            			return new ScaledAbstractImagePrototype(IconHelper.createPath(icon, 32, 32));
            		}
            		else {
            			return AbstractImagePrototype.create(Resources.INSTANCE.plugin32());
            		}
				} else {
					return AbstractImagePrototype.create(Resources.INSTANCE.plugin32());
				}
			}
        });
        
        //
        // Selection Listener for the component
        // make sure the form is not dirty before switching.
        final ServiceTree theServiceTree = this;
        m_servicesTree.getSelectionModel().addListener(Events.BeforeSelect, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {				
				final BaseEvent theEvent = be;				
            	SelectionEvent<ModelData> se = (SelectionEvent<ModelData>) be;                	
            	final GwtConfigComponent componentToSwitchTo = (GwtConfigComponent) se.getModel();
				if (m_serviceConfiguration != null && m_serviceConfiguration.isDirty()) {
			        
					// cancel the event first
                	theEvent.setCancelled(true);
                	
                	// need to reselect the current entry
                	// as the BeforeSelect event cleared it
                	// we need to do this without raising events
					m_servicesTree.getSelectionModel().setFiresEvents(false);
					m_servicesTree.getSelectionModel().select(false, m_serviceConfiguration.getGwtConfigComponent());
					m_servicesTree.getSelectionModel().setFiresEvents(true);

                	// ask for confirmation before switching
                	MessageBox.confirm(MSGS.confirm(), 
			            	MSGS.deviceConfigDirty(),
			                new Listener<MessageBoxEvent>() {  
			                    public void handleEvent(MessageBoxEvent ce) {
                                    // if confirmed, switch
                                    Dialog  dialog = ce.getDialog(); 
                                    if (dialog.yesText.equals(ce.getButtonClicked().getText())) {
                            			List<Component> comps = m_centerPanel.getItems();
                            			if (comps != null && comps.size() > 0) {
                            				m_centerPanel.removeAll();
                            			}
                                		m_centerPanel.setHeading(componentToSwitchTo.getComponentName());
                                		m_serviceConfiguration = new ServiceConfiguration(m_currentSession, componentToSwitchTo, theServiceTree);
                                		m_centerPanel.add(m_serviceConfiguration);
                                		m_centerPanel.layout();
                                    	m_servicesTree.getSelectionModel().select(false, componentToSwitchTo);
                                    	theServiceTree.fireEvent(Events.Select);
                                    }
			                    }
			        });
				}
				else {

					m_servicesTree.getSelectionModel().setFiresEvents(false);

					List<Component> comps = m_centerPanel.getItems();
					if (comps != null && comps.size() > 0) {
						m_centerPanel.removeAll();
					}
            		m_centerPanel.setHeading(componentToSwitchTo.getComponentName());
            		m_serviceConfiguration = new ServiceConfiguration(m_currentSession, componentToSwitchTo, theServiceTree);
            		m_centerPanel.add(m_serviceConfiguration);
            		m_centerPanel.layout();
                	m_servicesTree.getSelectionModel().select(false, componentToSwitchTo);
					
                	m_servicesTree.getSelectionModel().setFiresEvents(true);
                	theServiceTree.fireEvent(Events.Select);
				}
			}	
        });
        
        //
        // on selection, complete the switch
//        final ServiceTree theServiceTree = this;
//        m_servicesTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
//        m_servicesTree.getSelectionModel().addSelectionChangedListener( new SelectionChangedListener<ModelData>() {
//            @Override
//            public void selectionChanged(SelectionChangedEvent<ModelData> se) {
//
//            	GwtConfigComponent configComponent = (GwtConfigComponent) se.getSelectedItem();
//            	if (configComponent != null) {            		
//            		
//            		m_centerPanel.removeAll();
//            		
//            		m_centerPanel.setHeading(configComponent.getComponentName());
//            		m_serviceConfiguration = new ServiceConfiguration(m_currentSession, configComponent, theServiceTree);
//            		m_centerPanel.add(m_serviceConfiguration);
//            		m_centerPanel.layout();
//            		
//            		theServiceTree.fireEvent(Events.Select);
//            	}
//            }
//        });
        
        m_loader.addLoadListener( new DataLoadListener(m_servicesTree, m_centerPanel));
	}

	
	
	
	public void refreshServicePanel()
	{
		m_loader.load();
	}
	
	
	public void clearSelection()
	{
		m_servicesTree.getSelectionModel().deselectAll();
	}
	
	
	
    // --------------------------------------------------------------------------------------
    //
    //    Data Load Listener
    //
    // --------------------------------------------------------------------------------------

    private class DataLoadListener extends LoadListener
    {
    	private TreeGrid<ModelData> m_servicesTree; 
        private GwtConfigComponent  m_selectedComponent;
        private ContentPanel		m_centerPanel;

        
        public DataLoadListener(TreeGrid<ModelData> servicesTree, ContentPanel centerPanel) {
        	m_servicesTree      = servicesTree;
        	m_selectedComponent = null;
        	m_centerPanel		= centerPanel;
        }
        
        
        public void loaderBeforeLoad(LoadEvent le) {
        	m_servicesTree.mask(MSGS.loading());
        	m_centerPanel.mask(MSGS.loading());
        	m_selectedComponent = (GwtConfigComponent) m_servicesTree.getSelectionModel().getSelectedItem();
        }
        
        
        public void loaderLoad(LoadEvent le) 
        {
            m_servicesTree.unmask();
            m_centerPanel.unmask();
        	if (le.exception != null) {
                FailureHandler.handle(le.exception);
            }
        	
            if (m_selectedComponent != null) {
            	boolean bSelectedComponent = false;
            	ListStore<ModelData> store = m_servicesTree.getStore();
            	for (ModelData md : store.getModels()) {            		
            		GwtConfigComponent gwtConfigComp = (GwtConfigComponent) md;
            		if (gwtConfigComp.getComponentId().equals(m_selectedComponent.getComponentId())) {
                    	m_servicesTree.getSelectionModel().select(gwtConfigComp, false);
                    	m_servicesTree.getView().focusRow(store.indexOf(gwtConfigComp));
                    	bSelectedComponent = true;
                    	break;            			
            		}
            	}
            	if (!bSelectedComponent) {
            		// select the first entry if we cannot carry it 
            		// forward from the previous selection
                	m_servicesTree.getSelectionModel().select(0, false);
                	m_servicesTree.getView().focusRow(0);            		
            	}
            }
        }

        
        public void loaderLoadException(LoadEvent le) 
        {            
        	if (le.exception != null) {
                FailureHandler.handle(le.exception);
            }

        	List<ModelData> comps = new ArrayList<ModelData>();
    		GwtConfigComponent comp = new GwtConfigComponent();
    		comp.setComponentId(MSGS.deviceNoDeviceSelected());
    		comp.setComponentName(MSGS.deviceNoComponents());
    		comp.setComponentDescription(MSGS.deviceNoConfigSupported());
    		comps.add(comp);
    		m_servicesStore.removeAll();
    		m_servicesStore.add(comps, false);

    		m_servicesTree.unmask();
    		m_centerPanel.unmask();
        }
    }	
}
