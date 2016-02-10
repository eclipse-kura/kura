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
package org.eclipse.kura.web.client.device;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.ButtonEvent;
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
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.GridView;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class BundlesTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

	@SuppressWarnings("unused")
	private GwtSession      m_currentSession;
	private boolean         m_initialized;

	private boolean         m_dirty;
	private ToolBar         m_toolBar;
	private Button			m_refreshButton;
	private Button          m_startButton;
	private Button          m_stopButton;
	private Grid<GwtGroupedNVPair> m_grid;
	private ListStore<GwtGroupedNVPair> m_store;	
	private BaseListLoader<ListLoadResult<GwtGroupedNVPair>> m_loader;	


	public BundlesTab(GwtSession currentSession) {
		m_currentSession = currentSession;
		m_dirty = true;
		m_initialized    = false;
	}


	protected void onRender(Element parent, int index) 
	{        
		super.onRender(parent, index);         
		setLayout(new FitLayout());
		setId("device-bundles");

		initToolbar();
		initBundles();

		ContentPanel bundlePanel = new ContentPanel();
		bundlePanel.setBorders(false);
		bundlePanel.setBodyBorder(false);
		bundlePanel.setHeaderVisible(false);
		bundlePanel.setLayout( new FitLayout());
		bundlePanel.setScrollMode(Scroll.AUTO);
		bundlePanel.setTopComponent(m_toolBar);
		bundlePanel.add(m_grid);

		add(bundlePanel);
		m_initialized = true;
	}

	private void initToolbar() {
		m_toolBar = new ToolBar();
		m_toolBar.setBorders(true);
		m_toolBar.setId("packages-toolbar");

		m_refreshButton = new Button(MSGS.refreshButton(), 
				AbstractImagePrototype.create(Resources.INSTANCE.refresh()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				m_toolBar.disable();
				m_dirty = true;
				refresh();
			}
		}
				);

		m_refreshButton.setEnabled(true);
		m_toolBar.add(m_refreshButton);
		m_toolBar.add(new SeparatorToolItem());

		final AsyncCallback<Void> callback = new AsyncCallback<Void>() {                	    
			public void onFailure(Throwable caught) {
				FailureHandler.handle(caught);  
				m_dirty = true;
			}            
			public void onSuccess(Void arg0) {
				m_dirty = true;
				refresh();
			}
		};

		m_startButton = new Button(MSGS.deviceTabBundleStart(), 
				AbstractImagePrototype.create(Resources.INSTANCE.packageAdd()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				m_toolBar.disable();
				m_grid.mask(MSGS.waiting());
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtDeviceService.startBundle(token, m_grid.getSelectionModel().getSelectedItem().getId(), callback);
					}});
			}
		}
				);

		m_startButton.setEnabled(true);
		m_toolBar.add(m_startButton);
		m_toolBar.add(new SeparatorToolItem());

		m_stopButton = new Button(MSGS.deviceTabBundleStop(),
				AbstractImagePrototype.create(Resources.INSTANCE.packageDelete()),
				new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				final GwtGroupedNVPair selectedBundle = m_grid.getSelectionModel().getSelectedItem();
				MessageBox.confirm(MSGS.confirm(),
						MSGS.deviceStopBundle(selectedBundle.getName()),
						new Listener<MessageBoxEvent>() {
					@Override
					public void handleEvent(MessageBoxEvent be) {
						Dialog  dialog = be.getDialog();
						if (dialog.yesText.equals(be.getButtonClicked().getText())) {
							m_toolBar.disable();
							m_grid.mask(MSGS.waiting());
							gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
								@Override
								public void onFailure(Throwable ex) {
									FailureHandler.handle(ex);
								}

								@Override
								public void onSuccess(GwtXSRFToken token) {	
									gwtDeviceService.stopBundle(token, m_grid.getSelectionModel().getSelectedItem().getId(), callback);
								}});
						}

					}
				});
			}
		});


		m_stopButton.setEnabled(true);
		m_toolBar.add(m_stopButton);
		m_toolBar.add(new SeparatorToolItem());

		m_toolBar.disable();
	}

	private void initBundles() {
		RpcProxy<ListLoadResult<GwtGroupedNVPair>> proxy = new RpcProxy<ListLoadResult<GwtGroupedNVPair>>() {  
			@Override  
			protected void load(Object loadConfig, final AsyncCallback<ListLoadResult<GwtGroupedNVPair>> callback) {
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {	
						gwtDeviceService.findBundles(token, new AsyncCallback<ListLoadResult<GwtGroupedNVPair>>() {    				
							public void onSuccess(ListLoadResult<GwtGroupedNVPair> pairs) {
								callback.onSuccess(pairs);
							}    				
							public void onFailure(Throwable caught) {
								FailureHandler.handle(caught);
							}
						});
					}});
			}
		};

		m_loader = new BaseListLoader<ListLoadResult<GwtGroupedNVPair>>(proxy);
		m_loader.addLoadListener( new DataLoadListener());

		m_store = new ListStore<GwtGroupedNVPair>(m_loader);  

		ColumnConfig id  = new ColumnConfig("id",  MSGS.deviceBndId(), 10);  
		ColumnConfig name  = new ColumnConfig("name",  MSGS.deviceBndName(), 50);  
		ColumnConfig status = new ColumnConfig("statusLoc", MSGS.deviceBndState(), 20);  
		ColumnConfig version = new ColumnConfig("version", MSGS.deviceBndVersion(), 20);  

		List<ColumnConfig> config = new ArrayList<ColumnConfig>();  
		config.add(id);  
		config.add(name);  
		config.add(status);  
		config.add(version);  

		ColumnModel cm = new ColumnModel(config);    

		GridView view = new GridView();
		view.setForceFit(true);

		m_grid = new Grid<GwtGroupedNVPair>(m_store, cm);  
		m_grid.setView(view);
		m_grid.setBorders(false);
		m_grid.setLoadMask(true);
		m_grid.setStripeRows(true);

		GridSelectionModel<GwtGroupedNVPair> selectionModel = new GridSelectionModel<GwtGroupedNVPair>();
		selectionModel.setSelectionMode(SelectionMode.SINGLE);
		m_grid.setSelectionModel(selectionModel);
		m_grid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GwtGroupedNVPair>() {
			@Override
			public void selectionChanged(SelectionChangedEvent<GwtGroupedNVPair> se) {
				GwtGroupedNVPair selectedEntry = se.getSelectedItem();
				if (selectedEntry != null) {
					if ("bndActive".equals(selectedEntry.getStatus())) {
						m_startButton.disable();
						m_stopButton.enable();
					}
					else {
						m_stopButton.disable();
						m_startButton.enable();
					}
				}
				else {
					m_startButton.setEnabled(false);
					m_stopButton.setEnabled(false);                 
				}
			}

		});
	}

	public void refresh() 
	{
		if (m_dirty && m_initialized) {
			m_dirty = false;
			m_loader.load();
			m_toolBar.enable();
			m_startButton.disable();
			m_stopButton.disable();
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

		public void loaderBeforeLoad(LoadEvent le) {
			m_grid.mask(MSGS.loading());
		}

		public void loaderLoad(LoadEvent le) {
			if (le.exception != null) {
				FailureHandler.handle(le.exception);
			}
			m_startButton.disable();
			m_stopButton.disable();
			m_grid.unmask();
		}

		public void loaderLoadException(LoadEvent le) {

			if (le.exception != null) {
				FailureHandler.handle(le.exception);
			}
			m_startButton.disable();
			m_stopButton.disable();
			m_grid.unmask();
		}
	}
}
