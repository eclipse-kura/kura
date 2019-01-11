/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.packages;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.DropSupport;
import org.eclipse.kura.web.client.util.DropSupport.DropEvent;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtMarketplacePackageDescriptor;
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
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class PackagesPanelUi extends Composite {

    private static final String XSRF_TOKEN = "xsrfToken";
    private static final String DROPZONE_ACTIVE_STYLE_NAME = "active";
    private static RegExp marketplaceUrlRegexp = RegExp
            .compile("http[s]?:\\/\\/marketplace.eclipse.org/marketplace-client-intro\\?mpc_install=.*");

    private static PackagesPanelUiUiBinder uiBinder = GWT.create(PackagesPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(PackagesPanelUi.class.getSimpleName());

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtPackageServiceAsync gwtPackageService = GWT.create(GwtPackageService.class);

    private static final String SERVLET_URL = "/" + GWT.getModuleName() + "/file/deploy";

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final ValidationMessages VMSGS = GWT.create(ValidationMessages.class);

    private boolean isRefreshPending;
    private EntryClassUi entryClassUi;

    private final ListDataProvider<GwtDeploymentPackage> packagesDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtDeploymentPackage> selectionModel = new SingleSelectionModel<>();

    private GwtSession gwtSession;
    private GwtDeploymentPackage selected;

    interface PackagesPanelUiUiBinder extends UiBinder<Widget, PackagesPanelUi> {
    }

    @UiField
    Modal uploadModal;

    @UiField
    FormPanel packagesFormFile;
    @UiField
    FormPanel packagesFormUrl;

    @UiField
    Button fileCancel;
    @UiField
    Button fileSubmit;
    @UiField
    Button urlCancel;
    @UiField
    Button urlSubmit;
    @UiField
    Button packagesRefresh;
    @UiField
    Button packagesInstall;
    @UiField
    Button packagesUninstall;

    @UiField
    TabListItem fileLabel;

    @UiField
    Alert notification;

    @UiField
    Modal uploadErrorModal;

    @UiField
    Text uploadErrorText;

    @UiField
    CellTable<GwtDeploymentPackage> packagesGrid = new CellTable<>(10);

    @UiField
    FileUpload filePath;

    @UiField
    TextBox formUrl;

    @UiField
    Hidden xsrfTokenFieldFile;
    @UiField
    Hidden xsrfTokenFieldUrl;

    @UiField
    Well marketplaceInstallWell;

    @UiField
    Modal versionCheckModal;

    @UiField
    Paragraph versionMismatchErrorText;
    @UiField
    Paragraph maxKuraVersionLabel;
    @UiField
    Paragraph minKuraVersionLabel;
    @UiField
    Paragraph currentKuraVersionLabel;

    @UiField
    Button btnCancelMarketplaceInstall;
    @UiField
    Button btnConfirmMarketplaceInstall;

    @UiField
    HTMLPanel packagesIntro;

    @UiField
    Panel packagesDropzone;

    @UiField
    AlertDialog confirmDialog;

    private GwtMarketplacePackageDescriptor marketplaceDescriptor;

    public PackagesPanelUi() {

        // TODO - ServiceTree
        initWidget(uiBinder.createAndBindUi(this));

        Paragraph description = new Paragraph();
        description.setText(MSGS.packagesIntro());
        this.packagesIntro.add(description);

        this.packagesGrid.setSelectionModel(this.selectionModel);
        initTable();

        initTabButtons();

        initModalHandlers();

        initUploadErrorModal();
        initMarketplaceVersionCheckModal();

        initDragDrop();

        EventService.Handler onPackagesUpdatedHandler = eventInfo -> {
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
        this.packagesRefresh.addClickHandler(event -> refresh());
        // Install Button
        this.packagesInstall.setText(MSGS.packageAddButton());
        this.packagesInstall.addClickHandler(event -> upload());

        // Uninstall Button
        this.packagesUninstall.setText(MSGS.packageDeleteButton());
        this.packagesUninstall.addClickHandler(event -> {
            PackagesPanelUi.this.selected = PackagesPanelUi.this.selectionModel.getSelectedObject();
            if (PackagesPanelUi.this.selected != null && PackagesPanelUi.this.selected.getVersion() != null) {
                final Modal modal = new Modal();
                ModalBody modalBody = new ModalBody();
                ModalFooter modalFooter = new ModalFooter();
                modal.setClosable(true);
                modal.setTitle(MSGS.confirm());
                modalBody.add(new Span(MSGS.deviceUninstallPackage(PackagesPanelUi.this.selected.getName())));
                modalFooter.add(new Button("No", event11 -> modal.hide()));
                modalFooter.add(new Button("Yes", event12 -> {
                    modal.hide();
                    uninstall(PackagesPanelUi.this.selected);
                }));

                modal.add(modalBody);
                modal.add(modalFooter);
                modal.show();
            }
        });
    }

    private void initModalHandlers() {
        this.fileSubmit.addClickHandler(event -> PackagesPanelUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

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
        }));

        this.fileCancel.addClickHandler(event -> PackagesPanelUi.this.uploadModal.hide());

        this.urlSubmit.addClickHandler(event -> PackagesPanelUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

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
        }));

        this.urlCancel.addClickHandler(event -> PackagesPanelUi.this.uploadModal.hide());

        this.btnConfirmMarketplaceInstall.addClickHandler(event -> {
            if (PackagesPanelUi.this.marketplaceDescriptor != null) {
                installMarketplaceDp(PackagesPanelUi.this.marketplaceDescriptor);
                PackagesPanelUi.this.marketplaceDescriptor = null;
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

        this.xsrfTokenFieldFile.setID(XSRF_TOKEN);
        this.xsrfTokenFieldFile.setName(XSRF_TOKEN);
        this.xsrfTokenFieldFile.setValue("");

        this.packagesFormFile.setAction(SERVLET_URL + "/upload");
        this.packagesFormFile.setEncoding(FormPanel.ENCODING_MULTIPART);
        this.packagesFormFile.setMethod(FormPanel.METHOD_POST);
        this.packagesFormFile.addSubmitCompleteHandler(event -> {
            String result = event.getResults();
            if (result == null || result.isEmpty()) {
                PackagesPanelUi.this.uploadModal.hide();
            } else {
                logger.log(Level.SEVERE, "Error uploading package!");
            }
        });

        // ******URL TAB ****//
        this.formUrl.setName("packageUrl");

        this.xsrfTokenFieldUrl.setID(XSRF_TOKEN);
        this.xsrfTokenFieldUrl.setName(XSRF_TOKEN);
        this.xsrfTokenFieldUrl.setValue("");

        this.packagesFormUrl.setAction(SERVLET_URL + "/url");
        this.packagesFormUrl.setMethod(FormPanel.METHOD_POST);
        this.packagesFormUrl.addSubmitCompleteHandler(event -> {
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
        if (this.isRefreshPending) {
            return;
        }
        this.isRefreshPending = true;
        EntryClassUi.showWaitModal();
        this.packagesDataProvider.getList().clear();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                PackagesPanelUi.this.isRefreshPending = false;
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                PackagesPanelUi.this.gwtPackageService.findDeviceDeploymentPackages(token,
                        new AsyncCallback<List<GwtDeploymentPackage>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                PackagesPanelUi.this.isRefreshPending = false;
                                EntryClassUi.hideWaitModal();
                                GwtDeploymentPackage pkg = new GwtDeploymentPackage();
                                pkg.setName("Unavailable! Please click refresh");
                                pkg.setVersion(caught.getLocalizedMessage());
                                PackagesPanelUi.this.packagesDataProvider.getList().add(pkg);

                            }

                            @Override
                            public void onSuccess(List<GwtDeploymentPackage> result) {
                                PackagesPanelUi.this.isRefreshPending = false;
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
                                    PackagesPanelUi.this.entryClassUi.fetchAvailableServices(null);
                                }
                            }
                        });
            }
        });
    }

    private void initUploadErrorModal() {
        this.uploadErrorModal.setTitle(MSGS.warning());
        this.uploadErrorText.setText(MSGS.missingFileUpload());
    }

    private void initMarketplaceVersionCheckModal() {
        this.versionCheckModal.setTitle(VMSGS.marketplaceInstallDpVersionMismatch());
        this.versionMismatchErrorText.setText(VMSGS.marketplaceInstallDpVersionDoesNotMatch());
        this.currentKuraVersionLabel.setText(VMSGS.marketplaceInstallCurrentKuraVersion());
        this.minKuraVersionLabel.setText(VMSGS.marketplaceInstallMinKuraVersion());
        this.maxKuraVersionLabel.setText(VMSGS.marketplaceInstallMaxKuraVersion());

        this.btnCancelMarketplaceInstall.setText(VMSGS.marketplaceInstallDpVersionMismatchCancel());
        this.btnConfirmMarketplaceInstall.setText(VMSGS.marketplaceInstallDpVersionMismatchInstall());
    }

    private void eclipseMarketplaceInstall(String url) {

        // Construct the REST URL for Eclipse Marketplace
        final String nodeId = url.split("=")[1];

        // Generate security token
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                // Retrieve the URL of the DP via the Eclipse Marketplace API
                PackagesPanelUi.this.gwtPackageService.getMarketplacePackageDescriptor(token, nodeId,
                        new AsyncCallback<GwtMarketplacePackageDescriptor>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(GwtMarketplacePackageDescriptor descriptor) {
                                if (!descriptor.isCompatible()) {
                                    EntryClassUi.hideWaitModal();
                                    showVersionMismatchDialog(descriptor);
                                } else {
                                    installMarketplaceDp(descriptor);
                                }
                            }
                        });

            }
        });
    }

    private void installMarketplaceDp(final GwtMarketplacePackageDescriptor descriptor) {

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                PackagesPanelUi.this.gwtPackageService.installPackageFromMarketplace(token, descriptor,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                            }

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }
                        });
            }
        });
    }

    private boolean isEclipseMarketplaceUrl(String url) {
        return url != null && !url.isEmpty() && marketplaceUrlRegexp.test(url);
    }

    private void initDragDrop() {
        DropSupport drop = DropSupport.addIfSupported(this);
        if (drop != null) {
            drop.setListener(new DropSupport.Listener() {

                @Override
                public boolean onDrop(DropEvent event) {
                    final String url = event.getAsText();
                    packagesDropzone.removeStyleName(DROPZONE_ACTIVE_STYLE_NAME);
                    if (isEclipseMarketplaceUrl(url)) {
                        confirmDialog.show(MSGS.packagesMarketplaceInstallConfirmMessage(), () -> eclipseMarketplaceInstall(url));
                    }
                    return true;
                }

                @Override
                public boolean onDragOver(DropEvent event) {
                    packagesDropzone.addStyleName(DROPZONE_ACTIVE_STYLE_NAME);
                    return true;
                }

                @Override
                public void onDragExit(DropEvent event) {
                    packagesDropzone.removeStyleName(DROPZONE_ACTIVE_STYLE_NAME);
                }
            });
        } else {
            this.marketplaceInstallWell.setVisible(false);
        }
    }

    private void showVersionMismatchDialog(GwtMarketplacePackageDescriptor descriptor) {

        this.marketplaceDescriptor = descriptor;

        String minKuraVersion = descriptor.getMinKuraVersion();
        String maxKuraVersion = descriptor.getMaxKuraVersion();

        if (minKuraVersion == null || minKuraVersion.isEmpty()) {
            minKuraVersion = "unspecified";
        }
        if (maxKuraVersion == null || maxKuraVersion.isEmpty()) {
            maxKuraVersion = "unspecified";
        }

        this.currentKuraVersionLabel
                .setText(VMSGS.marketplaceInstallCurrentKuraVersion() + " " + descriptor.getCurrentKuraVersion());
        this.minKuraVersionLabel.setText(VMSGS.marketplaceInstallMinKuraVersion() + " " + minKuraVersion);
        this.maxKuraVersionLabel.setText(VMSGS.marketplaceInstallMaxKuraVersion() + " " + maxKuraVersion);
        this.versionCheckModal.show();

    }

}
