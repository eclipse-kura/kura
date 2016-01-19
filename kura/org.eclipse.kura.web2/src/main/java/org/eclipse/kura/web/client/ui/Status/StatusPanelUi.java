package org.eclipse.kura.web.client.ui.Status;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class StatusPanelUi extends Composite {

	private static StatusPanelUiUiBinder uiBinder = GWT
			.create(StatusPanelUiUiBinder.class);

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
	AnchorListItem statusRefresh;
	@UiField
	DataGrid<GwtGroupedNVPair> statusGrid = new DataGrid<GwtGroupedNVPair>();
	private ListDataProvider<GwtGroupedNVPair> statusGridProvider = new ListDataProvider<GwtGroupedNVPair>();

	public StatusPanelUi() {
		initWidget(uiBinder.createAndBindUi(this));
		statusRefresh.setText(MSG.refresh());

		statusGrid.setRowStyles(new RowStyles<GwtGroupedNVPair>() {
			@Override
			public String getStyleNames(GwtGroupedNVPair row, int rowIndex) {
				return row.getName().contains("_") ? "rowHeader" : " ";
			}
		});

		loadStatusTable(statusGrid, statusGridProvider);		
		statusRefresh.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				
				loadStatusData();

			}
		});
		//loadStatusData();
		
		}

	// get current session from UI parent
	public void setSession(GwtSession gwtBSSession) {
		currentSession = gwtBSSession;
	}

	// create table layout
	public void loadStatusTable(DataGrid<GwtGroupedNVPair> grid,
			ListDataProvider<GwtGroupedNVPair> dataProvider) {
		TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

			@Override
			public String getValue(GwtGroupedNVPair object) {
				return String.valueOf(object.getName());
			}
		};
		col1.setCellStyleNames("status-table-row");
		grid.addColumn(col1, "Property");

		TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

			@Override
			public String getValue(GwtGroupedNVPair object) {
				return String.valueOf(object.getValue());
			}
		};
		col2.setCellStyleNames("status-table-row");
		grid.addColumn(col2, "Value");
		dataProvider.addDataDisplay(grid);
	}

	// fetch table data
	public void loadStatusData() {
		statusGridProvider.getList().clear();
		
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
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

							}

							@Override
							public void onSuccess(ArrayList<GwtGroupedNVPair> result) {

								String oldGroup = "cloudStatus";
								String title;
								statusGridProvider
										.getList()
										.add(new GwtGroupedNVPair(" ","_"+msgs.getString("cloudStatus")+"_"
												, " "));
								for (GwtGroupedNVPair resultPair : result) {
									if (!oldGroup.equals(resultPair.getGroup())) {
										title = getTitle(resultPair.getGroup());
										statusGridProvider.getList()
												.add(new GwtGroupedNVPair(" ", title,
														" "));
										oldGroup = resultPair.getGroup();
									}
									statusGridProvider.getList().add(resultPair);
								}
								statusGridProvider.flush();

							}

						});
			}
			
		});
	}

	public String getTitle(String group) {
		
		return "_"+msgs.getString(group)+"_";
	/*	if (group.equalsIgnoreCase("positionStatus")) {
			return "_Position Status_";
		} else if (group.contains("networkStatusEthernet")) {
			return "_Ethernet Settings_";
		} else if (group.contains("networkStatusModem")) {
			return "_Cellular Settings_";
		} else if (group.contains("networkStatusWifi")) {
			return "_Wireless Settings_";
		} else if (group.contains("cloudStatus")) {
			return "_Cloud and Data Services_";
		} else {
			return group.toUpperCase();
		}*/
	}

}