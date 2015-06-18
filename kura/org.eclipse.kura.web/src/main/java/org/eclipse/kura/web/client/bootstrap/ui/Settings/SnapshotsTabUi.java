package org.eclipse.kura.web.client.bootstrap.ui.Settings;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtBSSnapshot;
import org.eclipse.kura.web.shared.service.GwtBSNetworkService;
import org.eclipse.kura.web.shared.service.GwtBSNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtBSSnapshotService;
import org.eclipse.kura.web.shared.service.GwtBSSnapshotServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class SnapshotsTabUi extends Composite {

	private static SnapshotsTabUiUiBinder uiBinder = GWT
			.create(SnapshotsTabUiUiBinder.class);

	interface SnapshotsTabUiUiBinder extends UiBinder<Widget, SnapshotsTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtBSSnapshotServiceAsync gwtSnapshotService = GWT
			.create(GwtBSSnapshotService.class);
	private final GwtBSNetworkServiceAsync gwtNetworkService = GWT
			.create(GwtBSNetworkService.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName()
			+ "/file/configuration/snapshot";

	@UiField
	Modal uploadModal;
	@UiField
	FormPanel snapshotsForm;
	@UiField
	Button uploadCancel, uploadUpload;

	@UiField
	AnchorListItem refresh, download, rollback, upload;
	@UiField
	Alert notification;
	@UiField
	DataGrid<GwtBSSnapshot> snapshotsGrid = new DataGrid<GwtBSSnapshot>();
	private ListDataProvider<GwtBSSnapshot> snapshotsDataProvider = new ListDataProvider<GwtBSSnapshot>();
	final SingleSelectionModel<GwtBSSnapshot> selectionModel = new SingleSelectionModel<GwtBSSnapshot>();
	GwtBSSnapshot selected;

	public SnapshotsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initTable();
		refresh();
		snapshotsGrid.setSelectionModel(selectionModel);

		refresh.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				refresh();
			}
		});

		download.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				selected = selectionModel.getSelectedObject();
				if (selected != null) {
					StringBuilder sbUrl = new StringBuilder();
					sbUrl.append(
							"/" + GWT.getModuleName() + "/device_snapshots?")
							.append("snapshotId=")
							.append(selected.getSnapshotId());
					Window.open(sbUrl.toString(), "_blank", "location=no");
				}
			}
		});

		rollback.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				rollback();
			}
		});

		upload.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				uploadAndApply();
			}
		});
	}

	private void initTable() {

		TextColumn<GwtBSSnapshot> col1 = new TextColumn<GwtBSSnapshot>() {
			@Override
			public String getValue(GwtBSSnapshot object) {
				return String.valueOf(object.getSnapshotId());
			}
		};
		col1.setCellStyleNames("status-table-row");
		snapshotsGrid.addColumn(col1, MSGS.deviceSnapshotId());

		TextColumn<GwtBSSnapshot> col2 = new TextColumn<GwtBSSnapshot>() {
			@Override
			public String getValue(GwtBSSnapshot object) {
				return String.valueOf(object.getCreatedOnFormatted());
			}
		};
		col2.setCellStyleNames("status-table-row");
		snapshotsGrid.addColumn(col2, MSGS.deviceSnapshotCreatedOn());

		snapshotsDataProvider.addDataDisplay(snapshotsGrid);
	}

	private void refresh() {
		notification.setVisible(false);

		gwtSnapshotService
				.findDeviceSnapshots(new AsyncCallback<ArrayList<GwtBSSnapshot>>() {
					@Override
					public void onFailure(Throwable caught) {
						Growl.growl("Failed: ", caught.getLocalizedMessage());
					}

					@Override
					public void onSuccess(ArrayList<GwtBSSnapshot> result) {
						for (GwtBSSnapshot pair : result) {
							snapshotsDataProvider.getList().add(pair);
						}
						snapshotsDataProvider.flush();
					}
				});

		if (snapshotsDataProvider.getList().size() == 0) {
			snapshotsGrid.setVisible(false);
			notification.setVisible(true);
			notification.setText("No Snapshots Available");
			download.setEnabled(false);
			rollback.setEnabled(false);
		} else {
			snapshotsGrid.setVisible(true);
			notification.setVisible(false);
			download.setEnabled(true);
			rollback.setEnabled(true);
		}
	}

	private void rollback() {
		final GwtBSSnapshot snapshot = selectionModel.getSelectedObject();
		if (snapshot != null) {
			final Modal rollbackModal = new Modal();
			ModalBody rollbackModalBody = new ModalBody();
			ModalFooter rollbackModalFooter = new ModalFooter();
			rollbackModal.setTitle(MSGS.confirm());
			rollbackModal.setClosable(true);
			rollbackModalBody
					.add(new Span(MSGS.deviceSnapshotRollbackConfirm()));

			rollbackModalFooter.add(new Button("Yes", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					gwtSnapshotService.rollbackDeviceSnapshot(snapshot,new AsyncCallback<Void>() {

						@Override
						public void onFailure(Throwable caught) {
							Growl.growl("Error: "+ caught.getLocalizedMessage());
						}

						@Override
						public void onSuccess(Void result) {
							refresh();
						}
					});// end callback
					
					if (snapshot.getSnapshotId() == 0L) {
						if (gwtNetworkService != null) {
							gwtNetworkService.rollbackDefaultConfiguration(new AsyncCallback<Void>() {
								public void onFailure(Throwable caught) {
									Growl.growl("Error: "+ caught.getLocalizedMessage());
								}

								public void onSuccess(Void arg0) {
									refresh();
								}
							});// end callback
						}
					}//end if snapshotId==0
					rollbackModal.hide();
				}
			}));

			rollbackModalFooter.add(new Button("No", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					rollbackModal.hide();
				}
			}));

			rollbackModal.add(rollbackModalBody);
			rollbackModal.add(rollbackModalFooter);
			rollbackModal.show();

		}
	}

	private void uploadAndApply() {
		uploadModal.show();
		uploadModal.setTitle(MSGS.upload());
		snapshotsForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		snapshotsForm.setMethod(FormPanel.METHOD_POST);
		snapshotsForm.setAction(SERVLET_URL);

		uploadCancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				uploadModal.hide();
			}
		});

		uploadUpload.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				snapshotsForm.submit();
			}
		});

		snapshotsForm.addSubmitCompleteHandler(new SubmitCompleteHandler() {
			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				String htmlResponse = event.getResults();
				if (htmlResponse == null || htmlResponse.isEmpty()) {
					Growl.growl(MSGS.information() + ": ",
							MSGS.fileUploadSuccess());
					uploadModal.hide();
				} else {
					Growl.growl(MSGS.information() + ": ",
							MSGS.fileUploadFailure());
				}

			}
		});

	}

}
