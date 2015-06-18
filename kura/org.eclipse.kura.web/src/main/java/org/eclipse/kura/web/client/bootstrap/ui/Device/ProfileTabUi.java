package org.eclipse.kura.web.client.bootstrap.ui.Device;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.shared.model.GwtBSGroupedNVPair;
import org.eclipse.kura.web.shared.service.GwtBSDeviceService;
import org.eclipse.kura.web.shared.service.GwtBSDeviceServiceAsync;
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

	private static ProfileTabUiUiBinder uiBinder = GWT
			.create(ProfileTabUiUiBinder.class);

	interface ProfileTabUiUiBinder extends UiBinder<Widget, ProfileTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private static final ValidationMessages msgs = GWT
			.create(ValidationMessages.class);

	private final GwtBSDeviceServiceAsync gwtDeviceService = GWT
			.create(GwtBSDeviceService.class);

	@UiField
	DataGrid<GwtBSGroupedNVPair> profileGrid = new DataGrid<GwtBSGroupedNVPair>();
	private ListDataProvider<GwtBSGroupedNVPair> profileDataProvider = new ListDataProvider<GwtBSGroupedNVPair>();

	public ProfileTabUi() {
		initWidget(uiBinder.createAndBindUi(this));

		profileGrid.setRowStyles(new RowStyles<GwtBSGroupedNVPair>() {
			@Override
			public String getStyleNames(GwtBSGroupedNVPair row, int rowIndex) {
				return row.getValue().contains("  ") ? "rowHeader" : " ";
			}
		});
		
		

		loadProfileTable(profileGrid, profileDataProvider);
		

	}

	private void loadProfileTable(DataGrid<GwtBSGroupedNVPair> grid,
			
			ListDataProvider<GwtBSGroupedNVPair> dataProvider) {
						
		TextColumn<GwtBSGroupedNVPair> col1 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return msgs.getString(object.getName());
			}
		};
		col1.setCellStyleNames("status-table-row");
		grid.addColumn(col1, MSGS.devicePropName());

		TextColumn<GwtBSGroupedNVPair> col2 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
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

		gwtDeviceService
				.findDeviceConfiguration(new AsyncCallback<ArrayList<GwtBSGroupedNVPair>>() {

					@Override
					public void onFailure(Throwable caught) {
						profileDataProvider.getList().clear();
						profileDataProvider.getList().add(
								new GwtBSGroupedNVPair(
										"Not Available, please click Refresh",
										"Not Available, please click Refresh",
										"Not Available, please click Refresh"));
						profileDataProvider.flush();

					}

					@Override
					public void onSuccess(ArrayList<GwtBSGroupedNVPair> result) {
						String oldGroup = "devInfo";
						profileDataProvider.getList().add(new GwtBSGroupedNVPair("devInfo","devInfo","  "));
						for (GwtBSGroupedNVPair resultPair : result) {
							if (!oldGroup.equals(resultPair.getGroup())) {
								profileDataProvider.getList()
										.add(new GwtBSGroupedNVPair(resultPair.getGroup(), resultPair.getGroup(),
												"  "));
								oldGroup = resultPair.getGroup();
							}
							profileDataProvider.getList().add(resultPair);
						}						
						profileDataProvider.flush();

					}
				});
	}

}
