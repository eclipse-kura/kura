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
import java.util.HashMap;
import java.util.Map;
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
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.constants.ColumnSize;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
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
import com.google.gwt.user.client.ui.HTML;
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
	
	private GwtSession currentSession;
	private ListDataProvider<GwtGroupedNVPair> statusGridProvider = new ListDataProvider<GwtGroupedNVPair>();
	private EntryClassUi parent;
	
	private Map<String, Button> connectButtons    = new HashMap<String, Button>();
	private Map<String, Button> disconnectButtons = new HashMap<String, Button>();

	@UiField
	Well statusWell;
	@UiField
	Button statusRefresh, statusConnect, statusDisconnect, cancel;
	@UiField
	CellTable<GwtGroupedNVPair> statusGrid = new CellTable<GwtGroupedNVPair>();
	@UiField
	PanelBody connectPanel;
	@UiField
	Modal connectModal;

	public StatusPanelUi() {
		logger.log(Level.FINER, "Initializing StatusPanelUi...");
		initWidget(uiBinder.createAndBindUi(this));
		// Set text for buttons
		statusRefresh.setText(MSG.refresh());
		statusConnect.setText(MSG.connectButton());
		statusDisconnect.setText(MSG.disconnectButton());
		cancel.setText(MSG.cancelButton());

		statusGrid.setRowStyles(new RowStyles<GwtGroupedNVPair>() {
			@Override
			public String getStyleNames(GwtGroupedNVPair row, int rowIndex) {
				if ("Cloud and Data Service".equals(row.getName()) ||
				        "Connection Name".equals(row.getName()) ||
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
				showConnectModal();
			}
		});
		
		statusDisconnect.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				showConnectModal();
			}
		});
		
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hideConnectModal();
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
		connectPanel.clear();
		connectButtons.clear();
		disconnectButtons.clear();
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
								String connectionName = null;
								statusGridProvider.getList().add(new GwtGroupedNVPair(" ",msgs.getString(title), " "));
								
                                parent.updateConnectionStatusImage(false);
                                
								int connectionNameIndex= 0;
								
								for (GwtGroupedNVPair resultPair : result) {
									if ("Connection Name".equals(resultPair.getName()) && "CloudService".equals(resultPair.getValue())) {
										GwtGroupedNVPair connectionStatus= result.get(connectionNameIndex + 1); // done based on the idea that in the pairs data connection name is before connection status
										
										if ("Connection Status".equals(connectionStatus.getName()) && "CONNECTED".equals(connectionStatus.getValue())) {
											parent.updateConnectionStatusImage(true);
										} else {
											parent.updateConnectionStatusImage(false);
										}
									}
									connectionNameIndex++;
									
									// Setup connection button grid
									if ("Connection Name".equals(resultPair.getName())) {
										connectionName = resultPair.getValue();
										addConnectionRow(connectionName);
									}
									if ("Connection Status".equals(resultPair.getName())) {
										if ("CONNECTED".equals(resultPair.getValue())) {
											connectButtons.get(connectionName).setEnabled(false);
											disconnectButtons.get(connectionName).setEnabled(true);
										} else {
											connectButtons.get(connectionName).setEnabled(true);
											disconnectButtons.get(connectionName).setEnabled(false);
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
	
	public void setParent(EntryClassUi parent) {
		this.parent= parent;
	}
	
	private void showConnectModal() {
		connectModal.show();
	}
	
	private void hideConnectModal() {
		connectModal.hide();
	}
	
	private void addConnectionRow(String connectId) {
		// Create new row in Modal
		Row row = new Row();
		row.addStyleName("connection-table-row");
		
		// Add Column with connection string
		org.gwtbootstrap3.client.ui.Column columnLabel = new org.gwtbootstrap3.client.ui.Column(ColumnSize.MD_6);
		columnLabel.add(new HTML(connectId));
		row.add(columnLabel);
		
		// Add connect/disconnect buttons
		org.gwtbootstrap3.client.ui.Column columnButtons = new org.gwtbootstrap3.client.ui.Column(ColumnSize.MD_6);
		ButtonGroup bg = new ButtonGroup();
		
		Button connectButton = new Button();
		connectButton.setText(MSG.connectButton());
		connectButton.addStyleName("fa");
		connectButton.addStyleName("fa-toggle-on");
		connectButton.setId(connectId);
		connectButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				String targetId = Element.as(event.getNativeEvent().getEventTarget()).getId();
				connectDataService(targetId);
			}
		});
		connectButtons.put(connectId, connectButton);
		
		Button disconnectButton = new Button();
		disconnectButton.setText(MSG.disconnectButton());
		disconnectButton.addStyleName("fa");
		disconnectButton.addStyleName("fa-toggle-on");
		disconnectButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				String targetId = Element.as(event.getNativeEvent().getEventTarget()).getId();
				disconnectDataService(targetId);
			}
		});
		disconnectButtons.put(connectId, disconnectButton);
		
		bg.add(connectButton);
		bg.add(disconnectButton);
		columnButtons.add(bg);
		row.add(columnButtons);
		
		// Add row
		connectPanel.add(row);
	}
	
	private void connectDataService(final String connectionId) {
		hideConnectModal();
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}
			
			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtStatusService.connectDataService(token, connectionId, new AsyncCallback<Void>() {
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
	
	private void disconnectDataService(final String connectionId) {
		hideConnectModal();
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}
			
			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtStatusService.disconnectDataService(token, connectionId, new AsyncCallback<Void>() {
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