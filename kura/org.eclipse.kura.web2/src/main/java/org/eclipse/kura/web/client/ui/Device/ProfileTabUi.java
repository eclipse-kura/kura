package org.eclipse.kura.web.client.ui.Device;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class ProfileTabUi extends Composite {

	private static ProfileTabUiUiBinder uiBinder = GWT.create(ProfileTabUiUiBinder.class);

	interface ProfileTabUiUiBinder extends UiBinder<Widget, ProfileTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private static final ValidationMessages msgs = GWT.create(ValidationMessages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

	@UiField
	DataGrid<GwtGroupedNVPair> profileGrid = new DataGrid<GwtGroupedNVPair>();
	private ListDataProvider<GwtGroupedNVPair> profileDataProvider = new ListDataProvider<GwtGroupedNVPair>();

	public ProfileTabUi() {
		initWidget(uiBinder.createAndBindUi(this));

		profileGrid.setRowStyles(new RowStyles<GwtGroupedNVPair>() {
			@Override
			public String getStyleNames(GwtGroupedNVPair row, int rowIndex) {
				return row.getValue().contains("  ") ? "rowHeader" : " ";
			}
		});
		
		

		loadProfileTable(profileGrid, profileDataProvider);
		

	}

	private void loadProfileTable(DataGrid<GwtGroupedNVPair> grid,
			
			ListDataProvider<GwtGroupedNVPair> dataProvider) {
						
		TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return msgs.getString(object.getName());
			}
		};
		col1.setCellStyleNames("status-table-row");
		grid.addColumn(col1, MSGS.devicePropName());

		TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return String.valueOf(object.getValue());
			}
		};
		col2.setCellStyleNames("status-table-row");
		grid.addColumn(col2, MSGS.devicePropValue());

		dataProvider.addDataDisplay(grid);
		//loadProfileData();
	}

	public void loadProfileData() {
		profileDataProvider.getList().clear();

		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtDeviceService.findDeviceConfiguration(token, new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

					@Override
					public void onFailure(Throwable caught) {
						profileDataProvider.getList().clear();
						FailureHandler.handle(caught);
						profileDataProvider.flush();

					}

					@Override
					public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
						String oldGroup = "devInfo";
						profileDataProvider.getList().add(new GwtGroupedNVPair("devInfo","devInfo","  "));
						for (GwtGroupedNVPair resultPair : result) {
							if (!oldGroup.equals(resultPair.getGroup())) {
								profileDataProvider.getList()
										.add(new GwtGroupedNVPair(resultPair.getGroup(), resultPair.getGroup(),
												"  "));
								oldGroup = resultPair.getGroup();
							}
							profileDataProvider.getList().add(resultPair);
						}						
						profileDataProvider.flush();

					}
				});
			}
		
		});
	}

}
