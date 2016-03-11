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
package org.eclipse.kura.web.client.ui.Device;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class ThreadsTabUi extends Composite {

	private static ThreadsTabUiUiBinder uiBinder = GWT.create(ThreadsTabUiUiBinder.class);

	interface ThreadsTabUiUiBinder extends UiBinder<Widget, ThreadsTabUi> {
	}
	
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
	
	@UiField
	CellTable<GwtGroupedNVPair> threadsGrid = new CellTable<GwtGroupedNVPair>();
	private ListDataProvider<GwtGroupedNVPair> threadsDataProvider = new ListDataProvider<GwtGroupedNVPair>();


	public ThreadsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		loadProfileTable(threadsGrid, threadsDataProvider);
		//loadThreadsData();
	}
	
	
	private void loadProfileTable(CellTable<GwtGroupedNVPair> threadsGrid2,
			
			ListDataProvider<GwtGroupedNVPair> dataProvider) {
						
		TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return object.getName();
			}
		};
		col1.setCellStyleNames("status-table-row");
		threadsGrid2.addColumn(col1, MSGS.deviceThreadName());

		TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return String.valueOf(object.getValue());
			}
		};
		col2.setCellStyleNames("status-table-row");
		threadsGrid2.addColumn(col2, MSGS.deviceThreadInfo());

		dataProvider.addDataDisplay(threadsGrid2);
	}

	public void loadThreadsData() {
		threadsDataProvider.getList().clear();
	
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
	
			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}
	
			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtDeviceService.findThreads(token, new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {
	
					@Override
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						threadsDataProvider.getList().clear();
						FailureHandler.handle(caught);
						threadsDataProvider.flush();
	
					}
	
					@Override
					public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
						int size= result.size();
						threadsGrid.setVisibleRange(0, size);
						for (GwtGroupedNVPair resultPair : result) {
							threadsDataProvider.getList().add(resultPair);
						}						
						threadsDataProvider.flush();
						EntryClassUi.hideWaitModal();
					}
	
				});
			}
			
		});
	}
}
