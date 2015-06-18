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

public class SystemPropertiesTabUi extends Composite {

	private static SystemPropertiesTabUiUiBinder uiBinder = GWT
			.create(SystemPropertiesTabUiUiBinder.class);

	interface SystemPropertiesTabUiUiBinder extends
			UiBinder<Widget, SystemPropertiesTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private static final ValidationMessages msgs = GWT
			.create(ValidationMessages.class);

	private final GwtBSDeviceServiceAsync gwtDeviceService = GWT.create(GwtBSDeviceService.class);
	
	@UiField
	DataGrid<GwtBSGroupedNVPair> systemPropertiesGrid = new DataGrid<GwtBSGroupedNVPair>();
	private ListDataProvider<GwtBSGroupedNVPair> systemPropertiesDataProvider = new ListDataProvider<GwtBSGroupedNVPair>();


	public SystemPropertiesTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		
		
		systemPropertiesGrid.setRowStyles(new RowStyles<GwtBSGroupedNVPair>() {
			@Override
			public String getStyleNames(GwtBSGroupedNVPair row, int rowIndex) {
				return row.getValue().contains("  ") ? "rowHeader" : " ";
			}
		});
		
		

		loadSystemPropertiesTable(systemPropertiesGrid, systemPropertiesDataProvider);
		
	}


	private void loadSystemPropertiesTable(
			DataGrid<GwtBSGroupedNVPair> grid,
			ListDataProvider<GwtBSGroupedNVPair> dataProvider) {
		TextColumn<GwtBSGroupedNVPair> col1 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return String.valueOf(object.getName());
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
		
	}
	
	public void loadSystemPropertiesData(){
		systemPropertiesDataProvider.getList().clear();
		
		gwtDeviceService.findSystemProperties(new AsyncCallback<ArrayList<GwtBSGroupedNVPair>>() {

			@Override
			public void onFailure(Throwable caught) {
				systemPropertiesDataProvider.getList().clear();
				systemPropertiesDataProvider.getList().add(
						new GwtBSGroupedNVPair(
								"Not Available, please click Refresh",
								"Not Available, please click Refresh",
								"Not Available, please click Refresh"));
				systemPropertiesDataProvider.flush();

				
			}

			@Override
			public void onSuccess(ArrayList<GwtBSGroupedNVPair> result) {
				for (GwtBSGroupedNVPair resultPair : result) {
					systemPropertiesDataProvider.getList().add(resultPair);
				}						
				systemPropertiesDataProvider.flush();

			}
			
		});
	}

}
