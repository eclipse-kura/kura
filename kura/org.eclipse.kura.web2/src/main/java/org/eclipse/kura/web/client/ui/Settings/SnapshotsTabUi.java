package org.eclipse.kura.web.client.ui.Settings;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSnapshotService;
import org.eclipse.kura.web.shared.service.GwtSnapshotServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class SnapshotsTabUi extends Composite {

	private static SnapshotsTabUiUiBinder uiBinder = GWT.create(SnapshotsTabUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(SnapshotsTabUi.class.getSimpleName());

	interface SnapshotsTabUiUiBinder extends UiBinder<Widget, SnapshotsTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtSnapshotServiceAsync gwtSnapshotService = GWT.create(GwtSnapshotService.class);

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
	DataGrid<GwtSnapshot> snapshotsGrid = new DataGrid<GwtSnapshot>();
	
	private ListDataProvider<GwtSnapshot> snapshotsDataProvider = new ListDataProvider<GwtSnapshot>();
	final SingleSelectionModel<GwtSnapshot> selectionModel = new SingleSelectionModel<GwtSnapshot>();
	
	private CustomWindow m_downloadWindow;
	GwtSnapshot selected;

	public SnapshotsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initTable();
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
					//please see http://stackoverflow.com/questions/13277752/gwt-open-window-after-rpc-is-prevented-by-popup-blocker
					m_downloadWindow= CustomWindow.open(null, "_blank", "location=no"); 
					gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
						@Override
						public void onFailure(Throwable ex) {
							FailureHandler.handle(ex);
						}

						@Override
						public void onSuccess(GwtXSRFToken token) {
							downloadSnapshot(token.getToken());
						}
					});
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

		TextColumn<GwtSnapshot> col1 = new TextColumn<GwtSnapshot>() {
			@Override
			public String getValue(GwtSnapshot object) {
				return String.valueOf(object.getSnapshotId());
			}
		};
		col1.setCellStyleNames("status-table-row");
		snapshotsGrid.addColumn(col1, MSGS.deviceSnapshotId());

		TextColumn<GwtSnapshot> col2 = new TextColumn<GwtSnapshot>() {
			@Override
			public String getValue(GwtSnapshot object) {
				return String.valueOf(object.getCreatedOnFormatted());
			}
		};
		col2.setCellStyleNames("status-table-row");
		snapshotsGrid.addColumn(col2, MSGS.deviceSnapshotCreatedOn());

		snapshotsDataProvider.addDataDisplay(snapshotsGrid);
	}

	public void refresh() {
		notification.setVisible(false);
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtSnapshotService.findDeviceSnapshots(token, new AsyncCallback<ArrayList<GwtSnapshot>>() {
					@Override
					public void onFailure(Throwable ex) {
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(ArrayList<GwtSnapshot> result) {
						for (GwtSnapshot pair : result) {
							snapshotsDataProvider.getList().add(pair);
						}
						snapshotsDataProvider.flush();
					}
				});
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
		final GwtSnapshot snapshot = selectionModel.getSelectedObject();
		if (snapshot != null) {
			final Modal rollbackModal = new Modal();
			ModalBody rollbackModalBody = new ModalBody();
			ModalFooter rollbackModalFooter = new ModalFooter();
			rollbackModal.setTitle(MSGS.confirm());
			rollbackModal.setClosable(true);
			rollbackModalBody.add(new Span(MSGS.deviceSnapshotRollbackConfirm()));

			rollbackModalFooter.add(new Button("Yes", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

						@Override
						public void onFailure(Throwable ex) {
							FailureHandler.handle(ex);
						}

						@Override
						public void onSuccess(GwtXSRFToken token) {
							gwtSnapshotService.rollbackDeviceSnapshot(token, snapshot,new AsyncCallback<Void>() {

								@Override
								public void onFailure(Throwable ex) {
									FailureHandler.handle(ex);
								}

								@Override
								public void onSuccess(Void result) {
									refresh();
								}
							});
						}
						
					});
					
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

	private void downloadSnapshot(String tokenId) {
		final StringBuilder sbUrl = new StringBuilder();

		Long snapshot = selected.getSnapshotId();
		sbUrl.append("/" + GWT.getModuleName() + "/device_snapshots?")
		.append("snapshotId=")
		.append(snapshot)
		.append("&")
		.append("xsrfToken=")
		.append(tokenId);

		m_downloadWindow.setUrl(sbUrl.toString());
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
					//Growl.growl(MSGS.information() + ": ",
					//		MSGS.fileUploadSuccess());
					uploadModal.hide();
				} else {
					//Growl.growl(MSGS.information() + ": ",
					//		MSGS.fileUploadFailure());
				}

			}
		});

	}

}
