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
package org.eclipse.kura.web.client;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.web.client.configuration.ServiceTree;
import org.eclipse.kura.web.client.device.DevicePanel;
import org.eclipse.kura.web.client.firewall.FirewallPanel;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.network.NetworkPanel;
import org.eclipse.kura.web.client.packages.PackagesPanel;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.settings.SettingsPanel;
import org.eclipse.kura.web.client.status.StatusPanel;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BaseTreeModel;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.ModelIconProvider;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGrid;
import com.extjs.gxt.ui.client.widget.treegrid.WidgetTreeGridCellRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;

public class WestNavigationView extends LayoutContainer
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private GwtSession           m_currentSession;

	private ContentPanel         m_centerPanel;

	private ContentPanel 		 m_systemPanel;
	private TreeStore<ModelData> m_systemStore;
	private TreeGrid<ModelData>  m_systemTree;

	private ServiceTree   		 m_servicesTree;
	
	private GwtSession           m_gwtSession;


	public WestNavigationView(GwtSession currentSession, 
							  ContentPanel center) 
	{
		m_gwtSession = currentSession;
		m_centerPanel = center;
		m_currentSession = currentSession;
		setId("west-panel-wrapper");
	}

	
	protected void onRender(Element parent, int index) 
	{
		super.onRender(parent, index);

        final BorderLayout borderLayout = new BorderLayout();
		setLayout(borderLayout);
		setBorders(false);

		// init the service tree first as it is referenced by many.
		m_servicesTree = new ServiceTree(m_currentSession, m_centerPanel);
		m_servicesTree.addListener(Events.Select, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				if (m_systemTree != null) {
					m_systemTree.getSelectionModel().deselectAll();
					m_systemTree.clearState();
				}
			}
		});

		//
		// north - system panel
		initSystemPanel();

		//
		// West Navigation
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 257);  
        northData.setCollapsible(false);  
        northData.setFloatable(false);  
        northData.setHideCollapseTool(false);  
        northData.setSplit(false);
        northData.setMargins(new Margins(0, 0, 0, 0));
        m_systemPanel.setId("nav-system-wrapper");
        add(m_systemPanel, northData);
		
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER, 200);  
        centerData.setCollapsible(false);  
        centerData.setFloatable(false);  
        centerData.setHideCollapseTool(false);  
        centerData.setSplit(false);
        centerData.setMargins(new Margins(0, 0, 0, 0));
        m_servicesTree.setId("nav-services-wrapper");
        add(m_servicesTree, centerData);

		//
		// center - settings panel
	}


	private void initSystemPanel() 
	{
		//
		// System Panel 
		m_systemPanel = new ContentPanel();
		m_systemPanel.setBorders(false);
		m_systemPanel.setBodyBorder(true);
		m_systemPanel.setAnimCollapse(true);
		m_systemPanel.setHeading(MSGS.system());		
		m_systemPanel.setLayout( new FitLayout());

		m_systemStore = new TreeStore<ModelData>();
		m_systemStore.add(newItem("status", "Status", Resources.INSTANCE.information32()), false);
	    m_systemStore.add(newItem("device",   MSGS.device(),   Resources.INSTANCE.router32()),   false);
	    if (m_gwtSession.isNetAdminAvailable()) {
	    	m_systemStore.add(newItem("network",  MSGS.network(),  Resources.INSTANCE.network32()),  false);
	    	m_systemStore.add(newItem("firewall", MSGS.firewall(), Resources.INSTANCE.firewall32()), false);
	    }
		m_systemStore.add(newItem("packages", MSGS.packages(), Resources.INSTANCE.packages32()), false);
		m_systemStore.add(newItem("settings", MSGS.settings(), Resources.INSTANCE.settings32()), false);

		ColumnConfig name = new ColumnConfig("name", "Name", 100);
		name.setRenderer(new WidgetTreeGridCellRenderer<ModelData>(){
		    @Override
		    public Widget getWidget(ModelData model, String property, ColumnData config, int rowIndex, int colIndex,
		                            ListStore<ModelData> store, Grid<ModelData> grid) {	        
		        Label label = new Label((String)model.get(property));
		        label.setStyleAttribute("padding-left", "5px");
		        return label;
		    }
	    });
	    ColumnModel cm = new ColumnModel(Arrays.asList(name));

	    m_systemTree = new TreeGrid<ModelData>(m_systemStore, cm);
	    m_systemTree.setId("nav-system");
	    m_systemTree.setBorders(false);
	    m_systemTree.setHideHeaders(true);
	    m_systemTree.setAutoExpandColumn("name");
	    m_systemTree.getTreeView().setRowHeight(36);
	    m_systemTree.setIconProvider( new ModelIconProvider<ModelData>() {
			public AbstractImagePrototype getIcon(ModelData model) {
				if (model.get("icon") != null) {
					ImageResource ir = (ImageResource) model.get("icon");
					return AbstractImagePrototype.create(ir);						
				} else {
					return null;
				}
			}
		});
		m_systemTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		m_systemTree.getSelectionModel().addListener(Events.BeforeSelect, new Listener<BaseEvent>() {
			@SuppressWarnings("unchecked")
			public void handleEvent(BaseEvent be) {
				
				final BaseEvent theEvent = be;				
				SelectionEvent<ModelData> se = (SelectionEvent<ModelData>) be;
            	final ModelData selectedModel = se.getModel();
            	if (selectedModel == null) return;
            	//if (m_servicesTree != null && m_servicesTree.isDirty()) {
            	if (isDirty(m_centerPanel)) {
					// cancel the event first
                	theEvent.setCancelled(true);
                	
                	// need to reselect the current entry
                	// as the BeforeSelect event cleared it
                	// we need to do this without raising events
                	if (m_systemTree.getSelectionModel().getSelectedItem() != null) {
	                	m_systemTree.getSelectionModel().setFiresEvents(false);
	                	m_systemTree.getSelectionModel().select(false, m_systemTree.getSelectionModel().getSelectedItem());
	                	m_systemTree.getSelectionModel().setFiresEvents(true);
                	}
            		
        	    	MessageBox.confirm(MSGS.confirm(), 
        	            	MSGS.deviceConfigDirty(),
        	                new Listener<MessageBoxEvent>() {  
        	                    public void handleEvent(MessageBoxEvent ce) {
        	                        // if confirmed, delete
        	                        Dialog  dialog = ce.getDialog(); 
        	                        if (dialog.yesText.equals(ce.getButtonClicked().getText())) {
        	                        	selectModelId(selectedModel);        	                        
        	                        }
        	                    }
        	        });            		
            	}
            	else {
            		selectModelId(selectedModel);
            	}
			}
		});
		m_systemPanel.add(m_systemTree);
	}
	
	
	private void selectModelId(ModelData selectedModel) 
	{
    	String selectedId = (String) selectedModel.get("id");
    	if ("status".equals(selectedId)) {
    		m_centerPanel.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.information()));
            m_centerPanel.setHeading("Status");
            m_centerPanel.removeAll();                  
            m_centerPanel.add(new StatusPanel(m_currentSession));
            m_centerPanel.layout();
    	}
    	else if ("device".equals(selectedId)) {
            m_centerPanel.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.router()));
            m_centerPanel.setHeading(MSGS.device());
            m_centerPanel.removeAll();                  
            m_centerPanel.add(new DevicePanel(m_currentSession));
            m_centerPanel.layout();
		}
		else if ("network".equals(selectedId)) {
			if (m_centerPanel != null) {
				m_centerPanel.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.network()));
				m_centerPanel.setHeading(MSGS.network());
				List<Component> comps = m_centerPanel.getItems();
				if (comps != null && comps.size() > 0) {
					m_centerPanel.removeAll();
				}
				m_centerPanel.add(new NetworkPanel(m_currentSession));
				m_centerPanel.layout();
			}
		} 
		else if ("firewall".equals(selectedId)) {
			m_centerPanel.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.firewall()));
			m_centerPanel.setHeading(MSGS.firewall());
			List<Component> comps = m_centerPanel.getItems();
			if (comps != null && comps.size() > 0) {
				m_centerPanel.removeAll();
			}
			m_centerPanel.add(new FirewallPanel(m_currentSession));
			m_centerPanel.layout();
		}
		else if ("packages".equals(selectedId)) {
			m_centerPanel.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.packages()));
			m_centerPanel.setHeading(MSGS.packages());
			List<Component> comps = m_centerPanel.getItems();
			if (comps != null && comps.size() > 0) {
				m_centerPanel.removeAll();
			}
			m_centerPanel.add(new PackagesPanel(m_currentSession, m_servicesTree));	
			m_centerPanel.layout();
		}
		else if ("settings".equals(selectedId)) {
			m_centerPanel.setIcon(AbstractImagePrototype.create(Resources.INSTANCE.settings()));
			m_centerPanel.setHeading(MSGS.settings());
			List<Component> comps = m_centerPanel.getItems();
			if (comps != null && comps.size() > 0) {
				m_centerPanel.removeAll();
			}
			m_centerPanel.add(new SettingsPanel(m_currentSession, m_servicesTree));	
			m_centerPanel.layout();
		}
		
		// set the right selection in the system tree 
		// and clear the selection on the service tree
		m_systemTree.clearState();
		m_systemTree.getSelectionModel().setFiresEvents(false);
		m_systemTree.getSelectionModel().deselectAll();
		m_systemTree.getSelectionModel().select(false, selectedModel);
		m_systemTree.getSelectionModel().setFiresEvents(true);
		
		m_servicesTree.clearState();
		m_servicesTree.clearSelection();
	}	

	
	private ModelData newItem(String id, String text, Object iconStyle) {
		ModelData m = new BaseModelData();
		m.set("id",   id);
		m.set("name", text);
		m.set("icon", iconStyle);
		return m;
	}


		
	@SuppressWarnings("unused")
    private BaseTreeModel newTreeItem(String id, String text, Object icon) {
		BaseTreeModel m = new BaseTreeModel();
		m.set("id",   id);
		m.set("name", text);
		m.set("icon", icon);
		return m;
	}
	
	private boolean isDirty (ContentPanel centerPanel) {
		List<Component>comps = centerPanel.getItems();
		if (comps != null && comps.size() > 0) {
			Component comp = comps.get(0);
			if (comp instanceof NetworkPanel) {
				return ((NetworkPanel)comp).isDirty();
			}
			if (comp instanceof FirewallPanel) {
				return ((FirewallPanel)comp).isDirty();
			}
		}
		return false;
	}
	
}