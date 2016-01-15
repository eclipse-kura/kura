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

public class ThreadsTabUi extends Composite {

	private static ThreadsTabUiUiBinder uiBinder = GWT
			.create(ThreadsTabUiUiBinder.class);

	interface ThreadsTabUiUiBinder extends UiBinder<Widget, ThreadsTabUi> {
	}
	
	private static final Messages MSGS = GWT.create(Messages.class);
	private static final ValidationMessages msgs = GWT
			.create(ValidationMessages.class);

	private final GwtBSDeviceServiceAsync gwtDeviceService = GWT.create(GwtBSDeviceService.class);			
	@UiField
	DataGrid<GwtBSGroupedNVPair> threadsGrid = new DataGrid<GwtBSGroupedNVPair>();
	private ListDataProvider<GwtBSGroupedNVPair> threadsDataProvider = new ListDataProvider<GwtBSGroupedNVPair>();


	public ThreadsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		loadProfileTable(threadsGrid, threadsDataProvider);
		//loadThreadsData();
	}
	
	
private void loadProfileTable(DataGrid<GwtBSGroupedNVPair> grid,
			
			ListDataProvider<GwtBSGroupedNVPair> dataProvider) {
						
		TextColumn<GwtBSGroupedNVPair> col1 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return object.getName();
			}
		};
		col1.setCellStyleNames("status-table-row");
		grid.addColumn(col1, MSGS.deviceThreadName());

		TextColumn<GwtBSGroupedNVPair> col2 = new TextColumn<GwtBSGroupedNVPair>() {
			@Override
			public String getValue(GwtBSGroupedNVPair object) {
				return String.valueOf(object.getValue());
			}
		};
		col2.setCellStyleNames("status-table-row");
		grid.addColumn(col2, MSGS.deviceThreadInfo());

		dataProvider.addDataDisplay(grid);
	}

public void loadThreadsData() {
	threadsDataProvider.getList().clear();

	gwtDeviceService
			.findThreads(new AsyncCallback<ArrayList<GwtBSGroupedNVPair>>() {

				@Override
				public void onFailure(Throwable caught) {
					threadsDataProvider.getList().clear();
					threadsDataProvider.getList().add(
							new GwtBSGroupedNVPair(
									"Not Available, please click Refresh",
									"Not Available, please click Refresh",
									"Not Available, please click Refresh"));
					threadsDataProvider.flush();

				}

				@Override
				public void onSuccess(ArrayList<GwtBSGroupedNVPair> result) {
					for (GwtBSGroupedNVPair resultPair : result) {
						threadsDataProvider.getList().add(resultPair);
					}						
					threadsDataProvider.flush();

				}

			});
}
}
