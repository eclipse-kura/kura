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
package org.eclipse.kura.web.client.ui.Packages;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtPackageService;
import org.eclipse.kura.web.shared.service.GwtPackageServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
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

public class PackagesPanelUi extends Composite {

    private static PackagesPanelUiUiBinder uiBinder = GWT.create(PackagesPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(PackagesPanelUi.class.getSimpleName());

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtPackageServiceAsync gwtPackageService = GWT.create(GwtPackageService.class);

    private static final String SERVLET_URL = "/" + GWT.getModuleName() + "/file/deploy";
    private static final Messages MSGS = GWT.create(Messages.class);

    private boolean isRefreshPending;
    private EntryClassUi entryClassUi;

    private final ListDataProvider<GwtDeploymentPackage> packagesDataProvider = new ListDataProvider<GwtDeploymentPackage>();
    private final SingleSelectionModel<GwtDeploymentPackage> selectionModel = new SingleSelectionModel<GwtDeploymentPackage>();

    private GwtSession gwtSession;
    private GwtDeploymentPackage selected;

    interface PackagesPanelUiUiBinder extends UiBinder<Widget, PackagesPanelUi> {
    }

    @UiField
    Modal uploadModal;
    @UiField
    FormPanel packagesFormFile, packagesFormUrl;
    @UiField
    Button fileCancel, fileSubmit, urlCancel, urlSubmit;
    @UiField
    TabListItem fileLabel;

    @UiField
    Alert notification;
    @UiField
    Modal uploadErrorModal;
    @UiField
    Text uploadErrorText;
    @UiField
    Button packagesRefresh, packagesInstall, packagesUninstall;
    @UiField
    CellTable<GwtDeploymentPackage> packagesGrid = new CellTable<GwtDeploymentPackage>(10);
    @UiField
    FileUpload filePath;
    @UiField
    TextBox formUrl;
    @UiField
    Hidden xsrfTokenFieldFile, xsrfTokenFieldUrl;

    public PackagesPanelUi() {

        // TODO - ServiceTree
        initWidget(uiBinder.createAndBindUi(this));
        this.packagesGrid.setSelectionModel(this.selectionModel);
        initTable();

        initTabButtons();

        initModalHandlers();

        initModal();

        EventService.Handler onPackagesUpdatedHandler = new EventService.Handler() {

            @Override
            public void handleEvent(GwtEventInfo eventInfo) {
                if (!PackagesPanelUi.this.isVisible() || !PackagesPanelUi.this.isAttached()) {
                    return;
                }

                if (eventInfo.get("exception") != null) {
                    PackagesPanelUi.this.uploadErrorText.setText(
                            "Failed to " + (eventInfo.getTopic().indexOf("UNINSTALL") != -1 ? "uninstall" : "install")
                                    + " deployment package");
                    PackagesPanelUi.this.uploadModal.hide();
                    PackagesPanelUi.this.uploadErrorModal.show();
                    return;
                }

                PackagesPanelUi.this.refresh();
            }
        };

        EventService.subscribe(ForwardedEventTopic.DEPLOYMENT_PACKAGE_INSTALLED, onPackagesUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.DEPLOYMENT_PACKAGE_UNINSTALLED, onPackagesUpdatedHandler);
    }

    public void setSession(GwtSession currentSession) {
        this.gwtSession = currentSession;
    }

    public void setMainUi(EntryClassUi entryClassUi) {
        this.entryClassUi = entryClassUi;
    }

    public void refresh() {
        refresh(100);
    }

    private void initTabButtons() {
        // Refresh Button
        this.packagesRefresh.setText(MSGS.refreshButton());
        this.packagesRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {

                refresh();
            }
        });
        // Install Button
        this.packagesInstall.setText(MSGS.packageAddButton());
        this.packagesInstall.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                upload();
            }
        });

        // Uninstall Button
        this.packagesUninstall.setText(MSGS.packageDeleteButton());
        this.packagesUninstall.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                PackagesPanelUi.this.selected = PackagesPanelUi.this.selectionModel.getSelectedObject();
                if (PackagesPanelUi.this.selected != null && PackagesPanelUi.this.selected.getVersion() != null) {
                    final Modal modal = new Modal();
                    ModalBody modalBody = new ModalBody();
                    ModalFooter modalFooter = new ModalFooter();
                    modal.setClosable(true);
                    modal.setTitle(MSGS.confirm());
                    modalBody.add(new Span(MSGS.deviceUninstallPackage(PackagesPanelUi.this.selected.getName())));
                    modalFooter.add(new Button("Yes", new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            modal.hide();
                            uninstall(PackagesPanelUi.this.selected);
                        }
                    }));
                    modalFooter.add(new Button("No", new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            modal.hide();
                        }
                    }));

                    modal.add(modalBody);
                    modal.add(modalFooter);
                    modal.show();
                }   // end if null
            }// end on click
        });
    }

    private void initModalHandlers() {
        this.fileSubmit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                PackagesPanelUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        PackagesPanelUi.this.xsrfTokenFieldFile.setValue(token.getToken());
                        if (!"".equals(PackagesPanelUi.this.filePath.getFilename())) {
                            PackagesPanelUi.this.packagesFormFile.submit();
                        } else {
                            PackagesPanelUi.this.uploadModal.hide();
                            PackagesPanelUi.this.uploadErrorModal.show();
                        }
                    }
                });

            }
        });

        this.fileCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                PackagesPanelUi.this.uploadModal.hide();
            }
        });

        this.urlSubmit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                PackagesPanelUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        if (!"".equals(PackagesPanelUi.this.formUrl.getValue())) {
                            PackagesPanelUi.this.xsrfTokenFieldUrl.setValue(token.getToken());
                            PackagesPanelUi.this.packagesFormUrl.submit();
                        } else {
                            PackagesPanelUi.this.uploadModal.hide();
                            PackagesPanelUi.this.uploadErrorModal.show();
                        }
                    }
                });
            }
        });

        this.urlCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                PackagesPanelUi.this.uploadModal.hide();
            }
        });
    }

    private void refresh(int delay) {
        Timer timer = new Timer() {

            @Override
            public void run() {
                loadPackagesData();
            }
        };
        timer.schedule(delay);
    }

    private void upload() {
        this.uploadModal.show();

        // ******FILE TAB ****//
        this.fileLabel.setText(MSGS.fileLabel());

        this.filePath.setName("uploadedFile");

        this.xsrfTokenFieldFile.setID("xsrfToken");
        this.xsrfTokenFieldFile.setName("xsrfToken");
        this.xsrfTokenFieldFile.setValue("");

        this.packagesFormFile.setAction(SERVLET_URL + "/upload");
        this.packagesFormFile.setEncoding(FormPanel.ENCODING_MULTIPART);
        this.packagesFormFile.setMethod(FormPanel.METHOD_POST);
        this.packagesFormFile.addSubmitCompleteHandler(new SubmitCompleteHandler() {

            @Override
            public void onSubmitComplete(SubmitCompleteEvent event) {
                String result = event.getResults();
                if (result == null || result.isEmpty()) {
                    PackagesPanelUi.this.uploadModal.hide();
                } else {
                    logger.log(Level.SEVERE, "Error uploading package!");
                }
            }
        });

        // ******URL TAB ****//
        this.formUrl.setName("packageUrl");

        this.xsrfTokenFieldUrl.setID("xsrfToken");
        this.xsrfTokenFieldUrl.setName("xsrfToken");
        this.xsrfTokenFieldUrl.setValue("");

        this.packagesFormUrl.setAction(SERVLET_URL + "/url");
        this.packagesFormUrl.setMethod(FormPanel.METHOD_POST);
        this.packagesFormUrl.addSubmitCompleteHandler(new SubmitCompleteHandler() {

            @Override
            public void onSubmitComplete(SubmitCompleteEvent event) {
                String result = event.getResults();
                if (result == null || result.isEmpty()) {
                    PackagesPanelUi.this.uploadModal.hide();
                } else {
                    String errMsg = result;
                    int startIdx = result.indexOf("<pre>");
                    int endIndex = result.indexOf("</pre>");
                    if (startIdx != -1 && endIndex != -1) {
                        errMsg = result.substring(startIdx + 5, endIndex);
                    }
                    logger.log(Level.SEVERE, MSGS.error() + ": " + MSGS.fileDownloadFailure() + ": " + errMsg);
                }
            }
        });
    }

    private void uninstall(final GwtDeploymentPackage selected) {

        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                PackagesPanelUi.this.gwtPackageService.uninstallDeploymentPackage(token, selected.getName(),
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                            }
                        });
            }

        });
    }

    private void initTable() {

        TextColumn<GwtDeploymentPackage> col1 = new TextColumn<GwtDeploymentPackage>() {

            @Override
            public String getValue(GwtDeploymentPackage object) {
                return object.getName();
            }
        };
        col1.setCellStyleNames("status-table-row");
        this.packagesGrid.addColumn(col1, "Name");

        TextColumn<GwtDeploymentPackage> col2 = new TextColumn<GwtDeploymentPackage>() {

            @Override
            public String getValue(GwtDeploymentPackage object) {
                return object.getVersion();
            }
        };
        col2.setCellStyleNames("status-table-row");
        this.packagesGrid.addColumn(col2, "Version");

        this.packagesDataProvider.addDataDisplay(this.packagesGrid);
    }

    private void loadPackagesData() {
        if (isRefreshPending) {
            return;
        }
        isRefreshPending = true;
        EntryClassUi.showWaitModal();
        this.packagesDataProvider.getList().clear();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                isRefreshPending = false;
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                PackagesPanelUi.this.gwtPackageService.findDeviceDeploymentPackages(token,
                        new AsyncCallback<List<GwtDeploymentPackage>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                isRefreshPending = false;
                                EntryClassUi.hideWaitModal();
                                GwtDeploymentPackage pkg = new GwtDeploymentPackage();
                                pkg.setName("Unavailable! Please click refresh");
                                pkg.setVersion(caught.getLocalizedMessage());
                                PackagesPanelUi.this.packagesDataProvider.getList().add(pkg);

                            }

                            @Override
                            public void onSuccess(List<GwtDeploymentPackage> result) {
                                isRefreshPending = false;
                                EntryClassUi.hideWaitModal();
                                for (GwtDeploymentPackage pair : result) {
                                    PackagesPanelUi.this.packagesDataProvider.getList().add(pair);
                                }
                                int size = PackagesPanelUi.this.packagesDataProvider.getList().size();
                                PackagesPanelUi.this.packagesGrid.setVisibleRange(0, size);
                                PackagesPanelUi.this.packagesDataProvider.flush();

                                if (PackagesPanelUi.this.packagesDataProvider.getList().isEmpty()) {
                                    PackagesPanelUi.this.packagesGrid.setVisible(false);
                                    PackagesPanelUi.this.notification.setVisible(true);
                                    PackagesPanelUi.this.notification.setText(MSGS.devicePackagesNone());
                                } else {
                                    PackagesPanelUi.this.packagesGrid.setVisible(true);
                                    PackagesPanelUi.this.notification.setVisible(false);
                                }
                                if (PackagesPanelUi.this.entryClassUi != null) {
                                    PackagesPanelUi.this.entryClassUi.fetchAvailableServices();
                                }
                            }
                        });
            }
        });
    }

    private void initModal() {
        this.uploadErrorModal.setTitle(MSGS.warning());
        this.uploadErrorText.setText(MSGS.missingFileUpload());
    }
}
