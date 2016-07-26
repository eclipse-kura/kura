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
import org.eclipse.kura.web.client.messages.ValidationMessages;
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

public class BundlesTabUi extends Composite {

	private static BundlesTabUiUiBinder uiBinder = GWT.create(BundlesTabUiUiBinder.class);

	interface BundlesTabUiUiBinder extends UiBinder<Widget, BundlesTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private static final ValidationMessages msgs = GWT.create(ValidationMessages.class);
	
	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
	
	@UiField
	CellTable<GwtGroupedNVPair> bundlesGrid = new CellTable<GwtGroupedNVPair>();
	private ListDataProvider<GwtGroupedNVPair> bundlesDataProvider = new ListDataProvider<GwtGroupedNVPair>();

	public BundlesTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		loadBundlesTable(bundlesGrid, bundlesDataProvider);
		//loadBundlesData();
	}

	private void loadBundlesTable(CellTable<GwtGroupedNVPair> bundlesGrid2,

	ListDataProvider<GwtGroupedNVPair> dataProvider) {

		TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return object.getId();
			}
		};
		col1.setCellStyleNames("status-table-row");
		bundlesGrid2.addColumn(col1, MSGS.deviceBndId());

		TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return object.getName();
			}
		};
		col2.setCellStyleNames("status-table-row");
		bundlesGrid2.addColumn(col2, MSGS.deviceBndName());

		TextColumn<GwtGroupedNVPair> col3 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return msgs.getString(object.getStatus());
			}
		};
		col3.setCellStyleNames("status-table-row");
		bundlesGrid2.addColumn(col3, MSGS.deviceBndState());

		TextColumn<GwtGroupedNVPair> col4 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return object.getVersion();
			}
		};
		col4.setCellStyleNames("status-table-row");
		bundlesGrid2.addColumn(col4, MSGS.deviceBndVersion());

		dataProvider.addDataDisplay(bundlesGrid2);
	}

	public void loadBundlesData() {

		bundlesDataProvider.getList().clear();
		
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtDeviceService.findBundles(token, new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

					@Override
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
						bundlesDataProvider.flush();

					}

					@Override
					public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
						for (GwtGroupedNVPair resultPair : result) {
							bundlesDataProvider.getList().add(resultPair);
						}
						int size= bundlesDataProvider.getList().size();
						bundlesGrid.setVisibleRange(0, size);
						bundlesDataProvider.flush();
						EntryClassUi.hideWaitModal();

					}
				});
			}
			
		});
	}

}
