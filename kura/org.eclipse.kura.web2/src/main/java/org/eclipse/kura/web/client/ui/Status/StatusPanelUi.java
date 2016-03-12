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
package org.eclipse.kura.web.client.ui.Status;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class StatusPanelUi extends Composite {

	private static final Logger logger = Logger.getLogger(StatusPanelUi.class.getSimpleName());
	private static StatusPanelUiUiBinder uiBinder = GWT.create(StatusPanelUiUiBinder.class);

	interface StatusPanelUiUiBinder extends UiBinder<Widget, StatusPanelUi> {
	}

	
	private static final ValidationMessages msgs = GWT.create(ValidationMessages.class);
	private static final Messages MSG = GWT.create(Messages.class);
	
	private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);
	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	
	GwtSession currentSession;

	@UiField
	Well statusWell;
	@UiField
	Button statusRefresh, statusConnect, statusDisconnect;
	@UiField
	CellTable<GwtGroupedNVPair> statusGrid = new CellTable<GwtGroupedNVPair>();
	private ListDataProvider<GwtGroupedNVPair> statusGridProvider = new ListDataProvider<GwtGroupedNVPair>();

	public StatusPanelUi() {
		logger.log(Level.FINER, "Initializing StatusPanelUi...");
		initWidget(uiBinder.createAndBindUi(this));
		statusRefresh.setText(MSG.refresh());
		statusConnect.setText(MSG.connectButton());
		statusDisconnect.setText(MSG.disconnectButton());

		statusGrid.setRowStyles(new RowStyles<GwtGroupedNVPair>() {
			@Override
			public String getStyleNames(GwtGroupedNVPair row, int rowIndex) {
				if ("Cloud and Data Service".equals(row.getName()) ||
						"Ethernet Settings".equals(row.getName()) ||
						"Wireless Settings".equals(row.getName()) ||
						"Cellular Settings".equals(row.getName()) ||
						"Position Status".equals(row.getName())) {
					return "rowHeader";
				}
				else return " ";
			}
		});

		loadStatusTable(statusGrid, statusGridProvider);		
		
		statusRefresh.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				loadStatusData();
			}
		});
		
		statusConnect.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				connectDataService();
			}
		});
		
		statusDisconnect.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				disconnectDataService();
			}
		});
		
	}

	// get current session from UI parent
	public void setSession(GwtSession gwtBSSession) {
		currentSession = gwtBSSession;
	}

	// create table layout
	public void loadStatusTable(CellTable<GwtGroupedNVPair> grid, ListDataProvider<GwtGroupedNVPair> dataProvider) {
		TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

			@Override
			public String getValue(GwtGroupedNVPair object) {
				return String.valueOf(object.getName());
			}
		};
		col1.setCellStyleNames("status-table-row");
		grid.addColumn(col1);

		Column<GwtGroupedNVPair, SafeHtml> col2 = new Column<GwtGroupedNVPair, SafeHtml>(new SafeHtmlCell()) {

			public SafeHtml getValue(GwtGroupedNVPair object) {
				return SafeHtmlUtils.fromTrustedString(String.valueOf(object.getValue()));
			}
		};
		
		col2.setCellStyleNames("status-table-row");
		grid.addColumn(col2);
		dataProvider.addDataDisplay(grid);
	}

	// fetch table data
	public void loadStatusData() {
		statusGridProvider.getList().clear();
		
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}
			
			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtStatusService.getDeviceConfig(token, currentSession.isNetAdminAvailable(),
						new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {
							@Override
							public void onFailure(Throwable caught) {
								FailureHandler.handle(caught);
								statusGridProvider.flush();
								EntryClassUi.hideWaitModal();
							}

							@Override
							public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
								String title = "cloudStatus";
								statusGridProvider.getList().add(new GwtGroupedNVPair(" ",msgs.getString(title), " "));
								for (GwtGroupedNVPair resultPair : result) {
									if ("Connection Status".equals(resultPair.getName())) {
										if ("CONNECTED".equals(resultPair.getValue())) {
											statusConnect.setEnabled(false);
											statusDisconnect.setEnabled(true);
										}
										else {
											statusConnect.setEnabled(true);
											statusDisconnect.setEnabled(false);
										}
									}
									if (!title.equals(resultPair.getGroup())) {
										title = resultPair.getGroup();
										statusGridProvider.getList().add(new GwtGroupedNVPair(" ", msgs.getString(title), " "));
									}
									statusGridProvider.getList().add(resultPair);
								}
								int size= statusGridProvider.getList().size();
								statusGrid.setVisibleRange(0, size);
								statusGridProvider.flush();
								EntryClassUi.hideWaitModal();
							}

						});
			}
			
		});
	}
	
	private void connectDataService() {
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}
			
			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtStatusService.connectDataService(token, new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						EntryClassUi.hideWaitModal();
						loadStatusData();
					}
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}
				});
			}
		});
	}
	
	private void disconnectDataService() {
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}
			
			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtStatusService.disconnectDataService(token, new AsyncCallback<Void>() {
					public void onSuccess(Void result) {
						EntryClassUi.hideWaitModal();
						loadStatusData();
					}
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}
				});
			}
		});
	}

}