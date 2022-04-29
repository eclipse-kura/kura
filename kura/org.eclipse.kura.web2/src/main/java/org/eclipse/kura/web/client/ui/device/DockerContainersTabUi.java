/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.device;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class DockerContainersTabUi extends Composite implements Tab {

    private static DockerContainersTabUiUiBinder uiBinder = GWT.create(DockerContainersTabUiUiBinder.class);

    interface DockerContainersTabUiUiBinder extends UiBinder<Widget, DockerContainersTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final ValidationMessages validationMessages = GWT.create(ValidationMessages.class);
    private static final String ROW_HEADER_STYLE = "rowHeader";
    private static final String STATUS_TABLE_ROW_STYLE = "status-table-row";

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    private boolean isRequestRunning = false;

    /**
     * Switcher Related
     */

    @UiField
    Panel containerListPanel;

    @UiField
    Panel imageListPanel;

    @UiField
    CellTable<GwtGroupedNVPair> assetGrid = new CellTable<>();

    private final ListDataProvider<GwtGroupedNVPair> assetSwitcherProvider = new ListDataProvider<>();
    final SingleSelectionModel<GwtGroupedNVPair> switcherSelectionModel = new SingleSelectionModel<>();

    /**
     * Container View Related
     */

    @UiField
    Button containersRefresh;
    @UiField
    Button containersStart;
    @UiField
    Button containersStop;

    @UiField
    CellTable<GwtGroupedNVPair> bundlesGrid = new CellTable<>();
    private final ListDataProvider<GwtGroupedNVPair> bundlesDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtGroupedNVPair> selectionModel = new SingleSelectionModel<>();

    /**
     * Images View Related
     */

    @UiField
    Button imagesDelete;
    @UiField
    Button imagesRefresh;

    @UiField
    CellTable<GwtGroupedNVPair> imagesGrid = new CellTable<>();
    private final ListDataProvider<GwtGroupedNVPair> imagesDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtGroupedNVPair> imagesSelectionModel = new SingleSelectionModel<>();

    private final GwtDeviceServiceAsync deviceService = GWT.create(GwtDeviceService.class);
    private final GwtSecurityTokenServiceAsync securityTokenService = GWT.create(GwtSecurityTokenService.class);

    public DockerContainersTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        loadContainersTable(this.bundlesGrid, this.bundlesDataProvider);
        loadImagesTable(this.imagesGrid, this.imagesDataProvider);
        initTableSwitcher();

        this.containersRefresh.setText(MSGS.refresh());
        this.containersStart.setText(MSGS.deviceTabContainerStart());
        this.containersStop.setText(MSGS.deviceTabContainerStop());

        this.selectionModel.clear();
        this.bundlesGrid.setSelectionModel(this.selectionModel);
        this.selectionModel.addSelectionChangeHandler(event -> updateButtons());
        this.containersRefresh.addClickHandler(event -> refresh());
        this.containersStart.addClickHandler(event -> startSelectedContainer());
        this.containersStop.addClickHandler(event -> stopSelectedContainer());

        this.switcherSelectionModel.addSelectionChangeHandler(event -> updateSelectedPanels());

        updateButtons();

        this.imagesRefresh.setText(MSGS.refresh());
        this.imagesDelete.setText(MSGS.deviceTabContainerImagesDelete());

        this.imagesSelectionModel.clear();
        this.imagesGrid.setSelectionModel(this.imagesSelectionModel);
        this.imagesSelectionModel.addSelectionChangeHandler(event -> updateImageButtons());
        this.imagesDelete.addClickHandler(event -> deleteSelectedImage());
        this.imagesRefresh.addClickHandler(event -> refresh());

        updateImageButtons();

        EventService.Handler onBundleUpdatedHandler = eventInfo -> {
            if (DockerContainersTabUi.this.isVisible() && DockerContainersTabUi.this.isAttached()) {
                refresh();
                updateImageButtons();
            }
        };

        EventService.subscribe(ForwardedEventTopic.DOCKER_RUNNING, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.DOCKER_STARTED, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.DOCKER_STOPPED, onBundleUpdatedHandler);

    }

    private void updateButtons() {
        GwtGroupedNVPair selected = this.selectionModel.getSelectedObject();

        this.containersStart.setEnabled(false);
        this.containersStop.setEnabled(false);

        String status;

        if (selected == null || (status = selected.getStatus()) == null) {
            return;
        }

        boolean isActive = "bndActive".equals(status);

        this.containersStart.setEnabled(!isActive);
        this.containersStop.setEnabled(isActive);

    }

    private void updateSelectedPanels() {
        GwtGroupedNVPair selected = this.switcherSelectionModel.getSelectedObject();

        if (selected == null || (selected.getName()) == null) {
            return;
        }

        if (selected.getName().equals(MSGS.deviceTabContainerSwitcherGridImagesText())) {
            imageListPanel.setVisible(true);
            containerListPanel.setVisible(false);
        } else {
            imageListPanel.setVisible(false);
            containerListPanel.setVisible(true);
        }
    }

    private void initTableSwitcher() {

        imageListPanel.setVisible(false);
        containerListPanel.setVisible(true);

        List<GwtGroupedNVPair> typeList = new ArrayList<>();
        GwtGroupedNVPair containers = new GwtGroupedNVPair();
        containers.setName(MSGS.deviceTabContainerSwitcherGridContainerText());
        GwtGroupedNVPair images = new GwtGroupedNVPair();
        images.setName(MSGS.deviceTabContainerSwitcherGridImagesText());
        typeList.add(containers);
        typeList.add(images);

        this.assetSwitcherProvider.getList().addAll(typeList);

        TextColumn<GwtGroupedNVPair> switcherCol1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        switcherCol1.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        switcherCol1.setSortable(true);
        this.assetGrid.addColumn(switcherCol1, MSGS.deviceTabContainerSwitcherGridHeading());

        this.assetSwitcherProvider.addDataDisplay(this.assetGrid);
        this.assetGrid.setSelectionModel(this.switcherSelectionModel);

        this.assetGrid.getColumnSortList().push(switcherCol1);
    }

    private void updateImageButtons() {
        GwtGroupedNVPair selected = this.imagesSelectionModel.getSelectedObject();

        this.imagesDelete.setEnabled(false);

        String status;

        if (selected == null || (status = selected.getStatus()) == null) {
            return;
        }

        boolean isInstalled = "bndInstalled".equals(status);

        this.imagesDelete.setEnabled(isInstalled);

    }

    private void deleteSelectedImage() {
        EntryClassUi.showWaitModal();

        this.securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onSuccess(GwtXSRFToken token) {
                DockerContainersTabUi.this.deviceService.deleteImage(token,
                        DockerContainersTabUi.this.imagesSelectionModel.getSelectedObject().getId(),
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.showErrorMessage(MSGS.deviceTabContainerImagesDeleteErrorMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                            }
                        });

            }

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }
        });
        refresh();
    }

    private void startSelectedContainer() {
        EntryClassUi.showWaitModal();

        this.securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onSuccess(GwtXSRFToken token) {
                DockerContainersTabUi.this.deviceService.startContainer(token,
                        DockerContainersTabUi.this.selectionModel.getSelectedObject().getId(),
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

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }
        });
        refresh();
    }

    private void stopSelectedContainer() {
        EntryClassUi.showWaitModal();
        this.securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                DockerContainersTabUi.this.deviceService.stopContainer(token,
                        DockerContainersTabUi.this.selectionModel.getSelectedObject().getId(),
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);

                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                                DockerContainersTabUi.this.containersStop.setEnabled(false);
                            }
                        });
            }
        });
        refresh();
    }

    private void loadContainersTable(CellTable<GwtGroupedNVPair> bundlesGrid2,
            ListDataProvider<GwtGroupedNVPair> dataProvider) {

        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getId();
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader id = new TextHeader(MSGS.deviceBndId());
        id.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col1, id);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return validationMessages.getString(object.getStatus());
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader state = new TextHeader(MSGS.deviceBndState());
        state.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col2, state);

        TextColumn<GwtGroupedNVPair> col3 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader name = new TextHeader(MSGS.deviceBndName());
        name.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col3, name);

        TextColumn<GwtGroupedNVPair> col4 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getVersion();
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader version = new TextHeader(MSGS.deviceBndVersion());
        version.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col4, version);

        TextColumn<GwtGroupedNVPair> col5 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                if ((boolean) object.get("isFrameworkManaged")) {
                    return MSGS.trueLabel();
                } else {
                    return MSGS.falseLabel();
                }
            }
        };
        col5.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader arch = new TextHeader(MSGS.deviceTabContainerIsFrameworkManagedHeading());
        arch.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col5, arch);

        dataProvider.addDataDisplay(bundlesGrid2);
    }

    private void loadImagesTable(CellTable<GwtGroupedNVPair> bundlesGrid2,
            ListDataProvider<GwtGroupedNVPair> dataProvider) {

        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getId();
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader id = new TextHeader(MSGS.deviceBndId());
        id.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col1, id);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return validationMessages.getString(object.getStatus());
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader state = new TextHeader(MSGS.deviceBndState());
        state.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col2, state);

        TextColumn<GwtGroupedNVPair> col3 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader name = new TextHeader(MSGS.deviceBndName());
        name.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col3, name);

        TextColumn<GwtGroupedNVPair> col4 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getVersion();
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader version = new TextHeader(MSGS.deviceBndVersion());
        version.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col4, version);

        TextColumn<GwtGroupedNVPair> col5 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.get("arch");
            }
        };
        col5.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader isFrameworkManaged = new TextHeader(MSGS.deviceTabContainerImagesArchitextureCollumHeading());
        isFrameworkManaged.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col5, isFrameworkManaged);

        dataProvider.addDataDisplay(bundlesGrid2);
    }

    @Override
    public void refresh() {
        if (this.isRequestRunning) {
            return;
        }

        this.isRequestRunning = true;
        EntryClassUi.showWaitModal();

        this.bundlesDataProvider.getList().clear();

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                DockerContainersTabUi.this.isRequestRunning = false;
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {

                /**
                 * List Containers
                 */
                DockerContainersTabUi.this.gwtDeviceService.findContainers(token,
                        new AsyncCallback<List<GwtGroupedNVPair>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                DockerContainersTabUi.this.isRequestRunning = false;
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                                DockerContainersTabUi.this.bundlesDataProvider.flush();
                            }

                            @Override
                            public void onSuccess(List<GwtGroupedNVPair> result) {
                                DockerContainersTabUi.this.isRequestRunning = false;
                                EntryClassUi.hideWaitModal();
                                for (GwtGroupedNVPair resultPair : result) {
                                    DockerContainersTabUi.this.bundlesDataProvider.getList().add(resultPair);
                                }
                                int size = DockerContainersTabUi.this.bundlesDataProvider.getList().size();
                                DockerContainersTabUi.this.bundlesGrid.setVisibleRange(0, size);
                                DockerContainersTabUi.this.bundlesDataProvider.flush();
                                DockerContainersTabUi.this.selectionModel.clear();
                                updateButtons();
                            }
                        });

            }

        });

        EntryClassUi.showWaitModal();

        this.imagesDataProvider.getList().clear();

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                DockerContainersTabUi.this.isRequestRunning = false;
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {

                /**
                 * List Images
                 */
                DockerContainersTabUi.this.gwtDeviceService.findImages(token,
                        new AsyncCallback<List<GwtGroupedNVPair>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                DockerContainersTabUi.this.isRequestRunning = false;
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                                DockerContainersTabUi.this.imagesDataProvider.flush();
                            }

                            @Override
                            public void onSuccess(List<GwtGroupedNVPair> result) {
                                DockerContainersTabUi.this.isRequestRunning = false;
                                EntryClassUi.hideWaitModal();
                                for (GwtGroupedNVPair resultPair : result) {
                                    DockerContainersTabUi.this.imagesDataProvider.getList().add(resultPair);
                                }
                                int size = DockerContainersTabUi.this.imagesDataProvider.getList().size();
                                DockerContainersTabUi.this.imagesGrid.setVisibleRange(0, size);
                                DockerContainersTabUi.this.imagesDataProvider.flush();
                                DockerContainersTabUi.this.imagesSelectionModel.clear();
                                updateImageButtons();
                            }
                        });
            }

        });

    }

    @Override
    public void clear() {
        // Not needed
    }

    @Override
    public void setDirty(boolean flag) {
        // Not needed
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

}
