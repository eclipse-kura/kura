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
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class BundlesTabUi extends Composite {

	private static BundlesTabUiUiBinder uiBinder = GWT
			.create(BundlesTabUiUiBinder.class);

	interface BundlesTabUiUiBinder extends UiBinder<Widget, BundlesTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private static final ValidationMessages msgs = GWT
			.create(ValidationMessages.class);
	private final GwtBSDeviceServiceAsync gwtDeviceService = GWT.create(GwtBSDeviceService.class);
	
	@UiField
	DataGrid<GwtBSGroupedNVPair> bundlesGrid = new DataGrid<GwtBSGroupedNVPair>();
	private ListDataProvider<GwtBSGroupedNVPair> bundlesDataProvider = new ListDataProvider<GwtBSGroupedNVPair>();

	public BundlesTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		loadBundlesTable(bundlesGrid, bundlesDataProvider);
		//loadBundlesData();
	}

	private void loadBundlesTable(DataGrid<GwtBSGroupedNVPair> grid,

	ListDataProvider<GwtBSGroupedNVPair> dataProvider) {

		TextColumn<GwtBSGroupedNVPair> col1 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return object.getId();
			}
		};
		col1.setCellStyleNames("status-table-row");
		grid.addColumn(col1, MSGS.deviceBndId());

		TextColumn<GwtBSGroupedNVPair> col2 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return object.getName();
			}
		};
		col2.setCellStyleNames("status-table-row");
		grid.addColumn(col2, MSGS.deviceBndName());

		TextColumn<GwtBSGroupedNVPair> col3 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return msgs.getString(object.getStatus());
			}
		};
		col3.setCellStyleNames("status-table-row");
		grid.addColumn(col3, MSGS.deviceBndState());

		TextColumn<GwtBSGroupedNVPair> col4 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return object.getVersion();
			}
		};
		col4.setCellStyleNames("status-table-row");
		grid.addColumn(col4, MSGS.deviceBndVersion());

		dataProvider.addDataDisplay(grid);
	}

	public void loadBundlesData() {

		bundlesDataProvider.getList().clear();
		gwtDeviceService
				.findBundles(new AsyncCallback<ArrayList<GwtBSGroupedNVPair>>() {

					@Override
					public void onFailure(Throwable caught) {
						bundlesDataProvider.getList().add(
								new GwtBSGroupedNVPair(
										"Not Available, please click Refresh",
										"Not Available, please click Refresh",
										"Not Available, please click Refresh"));
						bundlesDataProvider.flush();

					}

					@Override
					public void onSuccess(ArrayList<GwtBSGroupedNVPair> result) {
						for (GwtBSGroupedNVPair resultPair : result) {
							bundlesDataProvider.getList().add(resultPair);
						}
						bundlesDataProvider.flush();

					}
				});
	}

}
