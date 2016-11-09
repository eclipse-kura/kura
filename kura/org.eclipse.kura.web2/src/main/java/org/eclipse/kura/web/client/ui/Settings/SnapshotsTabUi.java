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
package org.eclipse.kura.web.client.ui.Settings;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSnapshot;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSnapshotService;
import org.eclipse.kura.web.shared.service.GwtSnapshotServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class SnapshotsTabUi extends Composite implements Tab {

    private static SnapshotsTabUiUiBinder uiBinder = GWT.create(SnapshotsTabUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(SnapshotsTabUi.class.getSimpleName());

    interface SnapshotsTabUiUiBinder extends UiBinder<Widget, SnapshotsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtSnapshotServiceAsync gwtSnapshotService = GWT.create(GwtSnapshotService.class);

    private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/configuration/snapshot";

    @UiField
    Modal uploadModal;
    @UiField
    FormPanel snapshotsForm;
    @UiField
    Button uploadCancel, uploadUpload;

    @UiField
    Button refresh, download, rollback, upload;
    @UiField
    Alert notification;
    @UiField
    FileUpload filePath;
    @UiField
    Hidden xsrfTokenField;
    @UiField
    CellTable<GwtSnapshot> snapshotsGrid = new CellTable<GwtSnapshot>();
    
    private Element downloadIframe;

    private final ListDataProvider<GwtSnapshot> snapshotsDataProvider = new ListDataProvider<GwtSnapshot>();
    final SingleSelectionModel<GwtSnapshot> selectionModel = new SingleSelectionModel<GwtSnapshot>();

    GwtSnapshot selected;

    public SnapshotsTabUi() {
        logger.log(Level.FINER, "Initiating SnapshotsTabUI...");
        initWidget(uiBinder.createAndBindUi(this));
        initTable();
        this.snapshotsGrid.setSelectionModel(this.selectionModel);

        initDownloadIframe();
        
        initInterfaceButtons();

        initUploadModalHandlers();

        this.snapshotsForm.addSubmitCompleteHandler(new SubmitCompleteHandler() {

            @Override
            public void onSubmitComplete(SubmitCompleteEvent event) {
                String htmlResponse = event.getResults();
                EntryClassUi.hideWaitModal();
                if (htmlResponse == null || htmlResponse.isEmpty()) {
                    logger.log(Level.FINER, MSGS.information() + ": " + MSGS.fileUploadSuccess());
                    refresh();
                } else {
                    logger.log(Level.SEVERE, MSGS.information() + ": " + MSGS.fileUploadFailure());
                    FailureHandler.handle(new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR));
                }
            }
        });
    }
    
    @Override
    public void setDirty(boolean flag) {
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void refresh() {
        this.notification.setVisible(false);
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                SnapshotsTabUi.this.gwtSnapshotService.findDeviceSnapshots(token,
                        new AsyncCallback<ArrayList<GwtSnapshot>>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(ArrayList<GwtSnapshot> result) {
                        SnapshotsTabUi.this.snapshotsDataProvider.getList().clear();
                        for (GwtSnapshot pair : result) {
                            SnapshotsTabUi.this.snapshotsDataProvider.getList().add(pair);
                        }
                        int snapshotsDataSize = SnapshotsTabUi.this.snapshotsDataProvider.getList().size();
                        if (snapshotsDataSize == 0) {
                            SnapshotsTabUi.this.snapshotsGrid.setVisible(false);
                            SnapshotsTabUi.this.notification.setVisible(true);
                            SnapshotsTabUi.this.notification.setText("No Snapshots Available");
                            SnapshotsTabUi.this.download.setEnabled(false);
                            SnapshotsTabUi.this.rollback.setEnabled(false);
                        } else {
                            SnapshotsTabUi.this.snapshotsGrid.setVisibleRange(0, snapshotsDataSize);
                            SnapshotsTabUi.this.snapshotsGrid.setVisible(true);
                            SnapshotsTabUi.this.notification.setVisible(false);
                            SnapshotsTabUi.this.download.setEnabled(true);
                            SnapshotsTabUi.this.rollback.setEnabled(true);
                        }
                        SnapshotsTabUi.this.snapshotsDataProvider.flush();
                        EntryClassUi.hideWaitModal();
                    }
                });
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
        this.snapshotsGrid.addColumn(col1, MSGS.deviceSnapshotId());

        TextColumn<GwtSnapshot> col2 = new TextColumn<GwtSnapshot>() {

            @Override
            public String getValue(GwtSnapshot object) {
                return String.valueOf(object.get("createdOnFormatted"));
            }
        };
        col2.setCellStyleNames("status-table-row");
        this.snapshotsGrid.addColumn(col2, MSGS.deviceSnapshotCreatedOn());

        this.snapshotsDataProvider.addDataDisplay(this.snapshotsGrid);
    }

    private void initUploadModalHandlers() {
        this.uploadCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                SnapshotsTabUi.this.uploadModal.hide();
            }
        });

        this.uploadUpload.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                SnapshotsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        SnapshotsTabUi.this.xsrfTokenField.setValue(token.getToken());
                        SnapshotsTabUi.this.snapshotsForm.submit();
                        SnapshotsTabUi.this.uploadModal.hide();
                        EntryClassUi.showWaitModal();
                    }
                });
            }
        });
    }

    private void initInterfaceButtons() {
        this.refresh.setText(MSGS.refresh());
        this.refresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                refresh();
            }
        });

        this.download.setText(MSGS.download());
        this.download.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                SnapshotsTabUi.this.selected = SnapshotsTabUi.this.selectionModel.getSelectedObject();
                if (SnapshotsTabUi.this.selected != null) {
                    // please see
                    // http://stackoverflow.com/questions/13277752/gwt-open-window-after-rpc-is-prevented-by-popup-blocker
                    SnapshotsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

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

        this.rollback.setText(MSGS.rollback());
        this.rollback.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                rollback();
            }
        });

        this.upload.setText(MSGS.upload());
        this.upload.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                uploadAndApply();
            }
        });
    }

    private native void initDownloadIframe() /*-{
		var iframe = document.createElement('iframe');
		iframe.style.display = 'none'
		document.getElementsByTagName('body')[0].appendChild(iframe);
		this.@org.eclipse.kura.web.client.ui.Settings.SnapshotsTabUi::downloadIframe = iframe;
	}-*/;
    
    private void rollback() {
        final GwtSnapshot snapshot = this.selectionModel.getSelectedObject();
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
                    EntryClassUi.showWaitModal();
                    SnapshotsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            SnapshotsTabUi.this.gwtSnapshotService.rollbackDeviceSnapshot(token, snapshot,
                                    new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(ex);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    EntryClassUi.hideWaitModal();
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

        Long snapshot = this.selected.getSnapshotId();
        sbUrl.append("/" + GWT.getModuleName() + "/device_snapshots?").append("snapshotId=").append(snapshot)
                .append("&").append("xsrfToken=").append(URL.encodeQueryString(tokenId));

        downloadFile(sbUrl.toString());
    }
    
    private native void downloadFile(String url) /*-{
    		var downloadIframe = this.@org.eclipse.kura.web.client.ui.Settings.SnapshotsTabUi::downloadIframe;
    		downloadIframe.setAttribute('src', url);
    }-*/;

    private void uploadAndApply() {
        this.uploadModal.show();
        this.uploadModal.setTitle(MSGS.upload());
        this.snapshotsForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        this.snapshotsForm.setMethod(FormPanel.METHOD_POST);
        this.snapshotsForm.setAction(SERVLET_URL);

        this.filePath.setName("uploadedFile");

        this.xsrfTokenField.setID("xsrfToken");
        this.xsrfTokenField.setName("xsrfToken");
        this.xsrfTokenField.setValue("");

    }
}
