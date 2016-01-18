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
	private static final ValidationMessages msgs = GWT.create(ValidationMessages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
	
	@UiField
	DataGrid<GwtGroupedNVPair> threadsGrid = new DataGrid<GwtGroupedNVPair>();
	private ListDataProvider<GwtGroupedNVPair> threadsDataProvider = new ListDataProvider<GwtGroupedNVPair>();


	public ThreadsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		loadProfileTable(threadsGrid, threadsDataProvider);
		//loadThreadsData();
	}
	
	
	private void loadProfileTable(DataGrid<GwtGroupedNVPair> grid,
			
			ListDataProvider<GwtGroupedNVPair> dataProvider) {
						
		TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return object.getName();
			}
		};
		col1.setCellStyleNames("status-table-row");
		grid.addColumn(col1, MSGS.deviceThreadName());

		TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {
			@Override
			public String getValue(GwtGroupedNVPair object) {
				return String.valueOf(object.getValue());
			}
		};
		col2.setCellStyleNames("status-table-row");
		grid.addColumn(col2, MSGS.deviceThreadInfo());

		dataProvider.addDataDisplay(grid);
	}

	public void loadThreadsData() {
		threadsDataProvider.getList().clear();
	
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
	
			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}
	
			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtDeviceService.findThreads(token, new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {
	
					@Override
					public void onFailure(Throwable caught) {
						threadsDataProvider.getList().clear();
						threadsDataProvider.getList().add(
								new GwtGroupedNVPair(
										"Not Available, please click Refresh",
										"Not Available, please click Refresh",
										"Not Available, please click Refresh"));
						threadsDataProvider.flush();
	
					}
	
					@Override
					public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
						for (GwtGroupedNVPair resultPair : result) {
							threadsDataProvider.getList().add(resultPair);
						}						
						threadsDataProvider.flush();
	
					}
	
				});
			}
			
		});
	}
}
