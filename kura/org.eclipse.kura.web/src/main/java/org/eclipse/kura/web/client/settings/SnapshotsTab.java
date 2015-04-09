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
package org.eclipse.kura.web.client.settings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.configuration.ServiceTree;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.widget.FileUploadDialog;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSnapshotService;
import org.eclipse.kura.web.shared.service.GwtSnapshotServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class SnapshotsTab extends LayoutContainer {

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSnapshotServiceAsync gwtSnapshotService = GWT.create(GwtSnapshotService.class);
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	
	private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/configuration/snapshot";
	
	@SuppressWarnings("unused")
	private GwtSession             m_currentSession;
    private ServiceTree            m_servicesTree;

    private boolean    		       m_dirty;
    private boolean                m_initialized;
    
    private ToolBar                m_toolBar;
    private Button                 m_refreshButton;
    private Button                 m_downloadButton;
    private Button                 m_rollbackButton;
    private Button                 m_uploadButton;
    private ListStore<GwtSnapshot> m_store;
    private Grid<GwtSnapshot>      m_grid;
    private BaseListLoader<ListLoadResult<GwtSnapshot>> m_loader;
    private FileUploadDialog       m_fileUpload;

    
    public SnapshotsTab(GwtSession currentSession,
    					ServiceTree serviceTree) 
    {
        m_currentSession = currentSession;
    	m_servicesTree   = serviceTree;
        m_dirty          = false;
    	m_initialized    = false;
    }
    
    
    protected void onRender(Element parent, int index) {
        
        super.onRender(parent, index);        
        setLayout(new FitLayout());
        setBorders(false);
        setId("settings-snapshots");
        
        // init components
        initToolBar();
        initGrid();

		ContentPanel devicesHistoryPanel = new ContentPanel();
		devicesHistoryPanel.setBorders(false);
		devicesHistoryPanel.setBodyBorder(false);
		devicesHistoryPanel.setHeaderVisible(false);
		devicesHistoryPanel.setLayout( new FitLayout());
		devicesHistoryPanel.setScrollMode(Scroll.AUTO);
        devicesHistoryPanel.setTopComponent(m_toolBar);
		devicesHistoryPanel.add(m_grid);

        add(devicesHistoryPanel);
        m_initialized = true;
        reload();
    }
    
    
    private void initToolBar() {
        
        m_toolBar = new ToolBar();
        m_toolBar.setEnabled(true);
        m_toolBar.setId("settings-snapshots-toolbar");
        
        //
        // Refresh Button
        m_refreshButton = new Button(MSGS.refreshButton(), 
                AbstractImagePrototype.create(Resources.INSTANCE.refresh()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                reload();
            }
        });
        m_refreshButton.setEnabled(true);
        m_toolBar.add(m_refreshButton);
        m_toolBar.add(new SeparatorToolItem());

        m_downloadButton = new Button(MSGS.download(),
                AbstractImagePrototype.create(Resources.INSTANCE.snapshotDownload()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
            	downloadSnapshot();
            }
        });
        m_downloadButton.setEnabled(false);
        
        m_rollbackButton = new Button(MSGS.rollback(),
                AbstractImagePrototype.create(Resources.INSTANCE.snapshotRollback()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
            	rollbackSnapshot();
            }
        });
        m_rollbackButton.setEnabled(false);

        m_uploadButton = new Button(MSGS.upload(),
                AbstractImagePrototype.create(Resources.INSTANCE.snapshotUpload()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
            	uploadSnapshot();
            }
        });
        m_uploadButton.setEnabled(true);

        m_toolBar.add(m_downloadButton);
        m_toolBar.add(new SeparatorToolItem());
        m_toolBar.add(m_rollbackButton);
        m_toolBar.add(new SeparatorToolItem());
        m_toolBar.add(m_uploadButton);
    }
    
    private void initGrid() {
        
        List<ColumnConfig> columns = new ArrayList<ColumnConfig>();  
        
        ColumnConfig column = new ColumnConfig("snapshotId", MSGS.deviceSnapshotId(), 25);
        column.setSortable(false);
        column.setAlignment(HorizontalAlignment.CENTER);
        columns.add(column);

        column = new ColumnConfig("createdOnFormatted", MSGS.deviceSnapshotCreatedOn(), 75);
        column.setSortable(false);
        column.setAlignment(HorizontalAlignment.LEFT);
        columns.add(column);
                          
        // loader and store
        RpcProxy<ListLoadResult<GwtSnapshot>> proxy = new RpcProxy<ListLoadResult<GwtSnapshot>>() {
            @Override
            public void load(Object loadConfig, AsyncCallback<ListLoadResult<GwtSnapshot>> callback) {
                gwtSnapshotService.findDeviceSnapshots(callback);
            }
        };
        m_loader = new BaseListLoader<ListLoadResult<GwtSnapshot>>(proxy);
        m_loader.setSortDir(SortDir.DESC);  
        m_loader.setSortField("createdOnFormatted"); 
        m_loader.addLoadListener( new DataLoadListener());
        
        m_store = new ListStore<GwtSnapshot>(m_loader);        
        m_grid = new Grid<GwtSnapshot>(m_store, new ColumnModel(columns));
        m_grid.setBorders(false);   
        m_grid.setStateful(false);
        m_grid.setLoadMask(true);
        m_grid.setStripeRows(true);
        m_grid.setTrackMouseOver(false);
        m_grid.getView().setAutoFill(true);
		m_grid.getView().setEmptyText(MSGS.deviceSnapshotsNone());

        GridSelectionModel<GwtSnapshot> selectionModel = new GridSelectionModel<GwtSnapshot>();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        m_grid.setSelectionModel(selectionModel);
        m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtSnapshot>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<GwtSnapshot> se) {
                if (se.getSelectedItem() != null) {                	
                	m_downloadButton.setEnabled(true);
                	m_rollbackButton.setEnabled(true);
                }
                else {
                	m_downloadButton.setEnabled(false);
                	m_rollbackButton.setEnabled(false);
                }
            }
        });
    }

    
    
    // --------------------------------------------------------------------------------------
    //
    //    Device Event List Management
    //
    // --------------------------------------------------------------------------------------

    public void refreshWithDelay() {
    	Timer timer = new Timer() { 
    	    public void run() { 
    	        refresh();    	        
    	    } 
    	};
    	m_grid.mask(MSGS.waiting());
    	timer.schedule(5000);
    }

    public void refresh() {
		if (m_dirty && m_initialized) {
			m_dirty = false;
			m_toolBar.enable();
			m_refreshButton.enable();
		    reload();
		    
	    	// refresh the list
	    	// and reselect the item
	    	m_servicesTree.refreshServicePanel();
		}
	}
    
    public void reload() {
    	m_loader.load();
    }
    
    private void downloadSnapshot() {
    	GwtSnapshot snapshot = m_grid.getSelectionModel().getSelectedItem();
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append("/" + GWT.getModuleName() + "/device_snapshots?")
             .append("snapshotId=")
             .append(snapshot.getSnapshotId());
        Window.open(sbUrl.toString(), "_blank", "location=no");
	}
    
    private void uploadSnapshot() {
    	List<HiddenField<?>> hiddenFields = new ArrayList<HiddenField<?>>();
    	m_fileUpload = new FileUploadDialog(SERVLET_URL, hiddenFields);        	
    	m_fileUpload.addListener(Events.Hide, new Listener<BaseEvent>() {
    		public void handleEvent(BaseEvent be) {
    			m_dirty = true;
    			m_grid.mask(MSGS.applying());
            	m_toolBar.disable();

				refresh();
    		}
    	});

    	m_fileUpload.setHeading(MSGS.upload());
    	m_fileUpload.show();
    }
    
    private void rollbackSnapshot() {    	
    	
    	final GwtSnapshot snapshot = m_grid.getSelectionModel().getSelectedItem();
    	if (snapshot != null) {
    		
        	MessageBox.confirm(MSGS.confirm(), 
	            			   MSGS.deviceSnapshotRollbackConfirm(),
                new Listener<MessageBoxEvent>() {  
                    public void handleEvent(MessageBoxEvent ce) {
                        // if confirmed, delete
                        Dialog  dialog = ce.getDialog(); 
                        if (dialog.yesText.equals(ce.getButtonClicked().getText())) {
                        	m_dirty = true;
                        	m_grid.mask(MSGS.rollingBack());
                        	m_toolBar.disable();
                        	// do the rollback
                        	gwtSnapshotService.rollbackDeviceSnapshot(
                        			snapshot,  
                        			new AsyncCallback<Void>() {                        										 	    
						                public void onFailure(Throwable caught) {
						                    FailureHandler.handle(caught);
						                    m_dirty = true;
						                }                        								    
						                public void onSuccess(Void arg0) {
						                	refresh();
						                }
                        			});
                        	
                        	if (snapshot.getSnapshotId() == 0L) {
		                		if (gwtNetworkService != null) {
			                		gwtNetworkService.rollbackDefaultConfiguration(new AsyncCallback<Void>() {                        										 	    
						                public void onFailure(Throwable caught) {
						                    FailureHandler.handle(caught);
						                    m_dirty = true;
						                }                        								    
						                public void onSuccess(Void arg0) {
						                    refresh();
						                }
		                			});
		                		}
		                	}
                        }
                    }
        	});
    	}
    }
    
    
    // --------------------------------------------------------------------------------------
    //
    //    Data Load Listener
    //
    // --------------------------------------------------------------------------------------

    private class DataLoadListener extends LoadListener
    {
        public DataLoadListener() {
        }
        
        public void loaderLoad(LoadEvent le) {
        	if (le.exception != null) {
                FailureHandler.handle(le.exception);
            }
        }

        public void loaderLoadException(LoadEvent le) {
            
        	if (le.exception != null) {
                FailureHandler.handle(le.exception);
            }
        	m_store.removeAll();
        	m_grid.unmask();
        	m_toolBar.enable();
        }
    }
}
