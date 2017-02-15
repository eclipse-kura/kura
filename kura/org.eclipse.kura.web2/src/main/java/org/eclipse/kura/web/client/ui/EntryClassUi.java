/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.CloudServices.CloudServicesUi;
import org.eclipse.kura.web.client.ui.Device.DevicePanelUi;
import org.eclipse.kura.web.client.ui.Firewall.FirewallPanelUi;
import org.eclipse.kura.web.client.ui.Network.NetworkPanelUi;
import org.eclipse.kura.web.client.ui.Packages.PackagesPanelUi;
import org.eclipse.kura.web.client.ui.Settings.SettingsPanelUi;
import org.eclipse.kura.web.client.ui.Status.StatusPanelUi;
import org.eclipse.kura.web.client.ui.wires.WiresPanelUi;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtPackageService;
import org.eclipse.kura.web.shared.service.GwtPackageServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.shared.event.ModalHideEvent;
import org.gwtbootstrap3.client.shared.event.ModalHideHandler;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.logging.client.HasWidgetsLogHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class EntryClassUi extends Composite {

    interface EntryClassUIUiBinder extends UiBinder<Widget, EntryClassUi> {
    }

    // UI elements definition
    @UiField
    static Modal errorModal;
    @UiField
    Label footerLeft;
    @UiField
    Label footerCenter;
    @UiField
    Label footerRight;
    @UiField
    Panel contentPanel;
    @UiField
    Strong errorAlertText;
    @UiField
    PanelHeader contentPanelHeader;
    @UiField
    PanelBody contentPanelBody;
    @UiField
    TabListItem status;
    @UiField
    AnchorListItem device;
    @UiField
    AnchorListItem network;
    @UiField
    AnchorListItem firewall;
    @UiField
    AnchorListItem packages;
    @UiField
    AnchorListItem settings;
    @UiField
    AnchorListItem wires;
    @UiField
    AnchorListItem cloudServices;
    @UiField
    ScrollPanel servicesPanel;
    @UiField
    TextBox textSearch;
    @UiField
    NavPills servicesMenu;
    @UiField
    VerticalPanel errorLogArea;
    @UiField
    Modal errorPopup;
    @UiField
    Modal newFactoryComponentModal;
    @UiField
    FormLabel newFactoryComponentFormLabel;
    @UiField
    FormLabel componentInstanceNameLabel;
    @UiField
    Button buttonNewComponent;
    @UiField
    Button buttonNewComponentCancel;
    @UiField
    Button errorModalDismiss;
    @UiField
    ListBox factoriesList;
    @UiField
    Button factoriesButton;
    @UiField
    TextBox componentName;
    @UiField
    Button sidenavButton;
    @UiField
    Column sidenav;
    @UiField
    Panel sidenavOverlay;

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final Logger logger = Logger.getLogger(EntryClassUi.class.getSimpleName());
    private static final Logger errorLogger = Logger.getLogger("ErrorLogger");
    private static final EntryClassUIUiBinder uiBinder = GWT.create(EntryClassUIUiBinder.class);

    private static final String SELECT_COMPONENT = MSGS.servicesComponentFactorySelectorIdle();
    private static final String SIDENAV_HIDDEN_STYLE_NAME = "sidenav-hidden";
    private static final String SELECTED_ANCHOR_LIST_ITEM_STYLE_NAME = "selected-item";
    private static final String NOT_SCROLLABLE_STYLE_NAME = "not-scrollable";

    private static PopupPanel waitModal;

    private final StatusPanelUi statusBinder = GWT.create(StatusPanelUi.class);
    private final DevicePanelUi deviceBinder = GWT.create(DevicePanelUi.class);
    private final PackagesPanelUi packagesBinder = GWT.create(PackagesPanelUi.class);
    private final SettingsPanelUi settingsBinder = GWT.create(SettingsPanelUi.class);
    private final FirewallPanelUi firewallBinder = GWT.create(FirewallPanelUi.class);
    private final NetworkPanelUi networkBinder = GWT.create(NetworkPanelUi.class);
    private final CloudServicesUi cloudServicesBinder = GWT.create(CloudServicesUi.class);
    private final WiresPanelUi wiresBinder = GWT.create(WiresPanelUi.class);

    private final GwtPackageServiceAsync gwtPackageService = GWT.create(GwtPackageService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private final KeyUpHandler searchBoxChangeHandler = new KeyUpHandler() {

        @Override
        public void onKeyUp(KeyUpEvent event) {
            TextBox searchBox = (TextBox) event.getSource();
            String searchedValue = searchBox.getValue();
            if (searchedValue != null) {
                filterAvailableServices(searchedValue);
            }
        }
    };

    private final EntryClassUi ui;

    private GwtSession currentSession;
    private AnchorListItem service;
    private GwtConfigComponent addedItem;
    private GwtConfigComponent selected = null;

    private Modal modal;
    private ServicesUi servicesUi;
    private AnchorListItem selectedAnchorListItem;

    public EntryClassUi() {
        logger.log(Level.FINER, "Initiating UiBinder");
        this.ui = this;
        initWidget(uiBinder.createAndBindUi(this));
        initWaitModal();
        initNewComponentErrorModal();

        Date now = new Date();
        @SuppressWarnings("deprecation")
        int year = now.getYear() + 1900;
        this.footerLeft.setText(MSGS.copyright(String.valueOf(year)));
        this.footerLeft.setStyleName("copyright");
        this.contentPanel.setVisible(false);

        // Add handler for sidenav show/hide button
        this.sidenavButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (EntryClassUi.this.sidenav.getStyleName().contains(SIDENAV_HIDDEN_STYLE_NAME)) {
                    showSidenav();
                } else {
                    hideSidenav();
                }
            }
        });

        // Set client side logging
        errorLogger.addHandler(new HasWidgetsLogHandler(this.errorLogArea));
        this.errorPopup.addHideHandler(new ModalHideHandler() {

            @Override
            public void onHide(ModalHideEvent evt) {
                EntryClassUi.this.errorLogArea.clear();
            }
        });

        EventService.subscribe(ForwardedEventTopic.CLOUD_CONNECTION_STATUS_ESTABLISHED, new EventService.Handler() {

            @Override
            public void handleEvent(GwtEventInfo eventInfo) {
                updateConnectionStatusImage(true);
            }
        });
        EventService.subscribe(ForwardedEventTopic.CLOUD_CONNECTION_STATUS_LOST, new EventService.Handler() {

            @Override
            public void handleEvent(GwtEventInfo eventInfo) {
                updateConnectionStatusImage(false);
            }
        });

        FailureHandler.setPopup(this.errorPopup);

        showSidenav();

        initServicesTree();
    }

    public void setSelectedAnchorListItem(AnchorListItem selected) {
        if (this.selectedAnchorListItem != null) {
            this.selectedAnchorListItem.removeStyleName(SELECTED_ANCHOR_LIST_ITEM_STYLE_NAME);
        }
        this.selectedAnchorListItem = selected;
        this.selectedAnchorListItem.addStyleName(SELECTED_ANCHOR_LIST_ITEM_STYLE_NAME);
        hideSidenav();
    }

    private void showSidenav() {
        this.sidenav.removeStyleName(SIDENAV_HIDDEN_STYLE_NAME);
        this.sidenavOverlay.removeStyleName(SIDENAV_HIDDEN_STYLE_NAME);
        // prevent body scrolling if sidenav is shown
        RootPanel.get().addStyleName(NOT_SCROLLABLE_STYLE_NAME);
    }

    private void hideSidenav() {
        this.sidenav.addStyleName(SIDENAV_HIDDEN_STYLE_NAME);
        this.sidenavOverlay.addStyleName(SIDENAV_HIDDEN_STYLE_NAME);
        RootPanel.get().removeStyleName(NOT_SCROLLABLE_STYLE_NAME);
    }

    public void setSession(GwtSession GwtSession) {
        this.currentSession = GwtSession;
    }

    public void setFooter(GwtSession GwtSession) {

        this.footerRight.setText(GwtSession.getKuraVersion());

        if (GwtSession.isDevelopMode()) {
            this.footerCenter.setText(MSGS.developmentMode());
        }
    }

    public void discardWiresPanelChanges() {
        if (WiresPanelUi.isDirty()) {
            WiresPanelUi.clearUnsavedPanelChanges();
            WiresPanelUi.loadGraph();
        }
    }

    public void initSystemPanel(GwtSession GwtSession, boolean connectionStatus) {
        final EntryClassUi instanceReference = this;
        if (!GwtSession.isNetAdminAvailable()) {
            this.network.setVisible(false);
            this.firewall.setVisible(false);
        }

        // Status Panel
        updateConnectionStatusImage(connectionStatus);
        this.status.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        if (EntryClassUi.this.modal != null) {
                            EntryClassUi.this.modal.hide();
                        }
                        EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.status);
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText("Status");
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.statusBinder);
                        EntryClassUi.this.statusBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.statusBinder.setParent(instanceReference);
                        EntryClassUi.this.statusBinder.loadStatusData();
                        EntryClassUi.this.discardWiresPanelChanges();
                    }
                });

                renderDirtyConfigModal(b);
            }
        });

        // Device Panel
        this.device.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        forceTabsCleaning();
                        if (EntryClassUi.this.modal != null) {
                            EntryClassUi.this.modal.hide();
                        }
                        EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.device);
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.device());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.deviceBinder);
                        EntryClassUi.this.deviceBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.deviceBinder.initDevicePanel();
                        EntryClassUi.this.discardWiresPanelChanges();
                    }
                });
                renderDirtyConfigModal(b);
            }
        });

        // Network Panel
        if (this.network.isVisible()) {
            this.network.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            forceTabsCleaning();
                            if (EntryClassUi.this.modal != null) {
                                EntryClassUi.this.modal.hide();
                            }
                            EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.network);
                            EntryClassUi.this.contentPanel.setVisible(true);
                            EntryClassUi.this.contentPanelHeader.setText(MSGS.network());
                            EntryClassUi.this.contentPanelBody.clear();
                            EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.networkBinder);
                            EntryClassUi.this.networkBinder.setSession(EntryClassUi.this.currentSession);
                            EntryClassUi.this.networkBinder.initNetworkPanel();
                            EntryClassUi.this.discardWiresPanelChanges();
                        }
                    });
                    renderDirtyConfigModal(b);
                }
            });
        }

        // Firewall Panel
        if (this.firewall.isVisible()) {
            this.firewall.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            forceTabsCleaning();
                            if (EntryClassUi.this.modal != null) {
                                EntryClassUi.this.modal.hide();
                            }
                            EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.firewall);
                            EntryClassUi.this.contentPanel.setVisible(true);
                            EntryClassUi.this.contentPanelHeader.setText(MSGS.firewall());
                            EntryClassUi.this.contentPanelBody.clear();
                            EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.firewallBinder);
                            EntryClassUi.this.firewallBinder.initFirewallPanel();
                            EntryClassUi.this.discardWiresPanelChanges();
                        }
                    });
                    renderDirtyConfigModal(b);
                }
            });
        }

        // Packages Panel
        this.packages.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        forceTabsCleaning();
                        if (EntryClassUi.this.modal != null) {
                            EntryClassUi.this.modal.hide();
                        }
                        EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.packages);
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.packages());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.packagesBinder);
                        EntryClassUi.this.packagesBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.packagesBinder.setMainUi(EntryClassUi.this.ui);
                        EntryClassUi.this.packagesBinder.refresh();
                        EntryClassUi.this.discardWiresPanelChanges();
                    }
                });
                renderDirtyConfigModal(b);
            }
        });

        // Settings Panel
        this.settings.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        forceTabsCleaning();
                        if (EntryClassUi.this.modal != null) {
                            EntryClassUi.this.modal.hide();
                        }
                        EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.settings);
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.settings());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.settingsBinder);
                        EntryClassUi.this.settingsBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.settingsBinder.load();
                        EntryClassUi.this.discardWiresPanelChanges();
                    }
                });
                renderDirtyConfigModal(b);
            }
        });

        // Cloud services Panel
        this.cloudServices.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        forceTabsCleaning();
                        if (EntryClassUi.this.modal != null) {
                            EntryClassUi.this.modal.hide();
                        }
                        EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.cloudServices);
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.cloudServices());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.cloudServicesBinder);
                        EntryClassUi.this.cloudServicesBinder.refresh();
                        EntryClassUi.this.discardWiresPanelChanges();
                    }
                });
                renderDirtyConfigModal(b);
            }
        });

        // Wires Panel
        this.wires.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Button b = new Button(MSGS.yesButton(), new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        forceTabsCleaning();
                        if (EntryClassUi.this.modal != null) {
                            EntryClassUi.this.modal.hide();
                        }
                        EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.wires);
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.wires());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.wiresBinder);
                        WiresPanelUi.load();
                        EntryClassUi.this.discardWiresPanelChanges();
                    }
                });
                renderDirtyConfigModal(b);
            }
        });

    }

    private void filterAvailableServices(String serviceName) {
        if (serviceName == null) {
            showAllServices();
            return;
        }

        String tmpServiceName = serviceName.toLowerCase();
        if (tmpServiceName.isEmpty() || "".equals(tmpServiceName)) {
            showAllServices();
        } else {
            filterByServiceName(tmpServiceName);
        }
    }

    private void showAllServices() {
        for (int i = 0; i < this.servicesMenu.getWidgetCount(); i++) {
            ServicesAnchorListItem sl = (ServicesAnchorListItem) this.servicesMenu.getWidget(i);
            sl.setVisible(true);
        }
    }

    private void filterByServiceName(String tmpServiceName) {
        for (int i = 0; i < this.servicesMenu.getWidgetCount(); i++) {
            ServicesAnchorListItem sl = (ServicesAnchorListItem) this.servicesMenu.getWidget(i);
            if (tmpServiceName == null || sl.getServiceName().toLowerCase().indexOf(tmpServiceName) != -1) {
                sl.setVisible(true);
            } else {
                sl.setVisible(false);
            }
        }
    }

    public void fetchAvailableServices() {
        // (Re)Fetch Available Services
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                EntryClassUi.this.gwtComponentService.findServicesConfigurations(token,
                        new AsyncCallback<List<GwtConfigComponent>>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(List<GwtConfigComponent> result) {
                                EntryClassUi.this.servicesMenu.clear();
                                for (GwtConfigComponent pair : result) {
                                    if (!pair.isWireComponent()) {
                                        EntryClassUi.this.servicesMenu
                                                .add(new ServicesAnchorListItem(pair, EntryClassUi.this.ui));
                                    }
                                }
                                filterAvailableServices(EntryClassUi.this.textSearch.getValue());
                            }
                        });
            }
        });
    }

    private void initServicesTree() {
        // Keypress handler
        this.textSearch.addKeyUpHandler(this.searchBoxChangeHandler);

        initNewFactoryComponentModal();

        this.factoriesButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                // always empty the PID input field
                EntryClassUi.this.componentName.setValue("");
                EntryClassUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        EntryClassUi.this.gwtComponentService.findFactoryComponents(token,
                                new AsyncCallback<List<String>>() {

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                                    }

                                    @Override
                                    public void onSuccess(final List<String> result) {
                                        EntryClassUi.this.factoriesList.clear();
                                        EntryClassUi.this.factoriesList.addItem(SELECT_COMPONENT);
                                        for (final String servicePid : result) {
                                            EntryClassUi.this.factoriesList.addItem(servicePid);
                                        }
                                        EntryClassUi.this.newFactoryComponentModal.show();
                                    }
                                });
                    }
                });
            }
        });
    }

    private void initNewFactoryComponentModal() {
        this.newFactoryComponentModal.setTitle(MSGS.servicesComponentFactoryNew());
        this.newFactoryComponentFormLabel.setText(MSGS.servicesComponentFactoryFactory());
        this.componentInstanceNameLabel.setText(MSGS.servicesComponentFactoryName());
        this.componentName.setPlaceholder(MSGS.servicesComponentFactoryNamePlaceholder());
        this.buttonNewComponent.setText(MSGS.apply());
        this.buttonNewComponentCancel.setText(MSGS.cancelButton());

        // New factory configuration handler
        this.buttonNewComponent.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {

                EntryClassUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        String factoryPid = EntryClassUi.this.factoriesList.getSelectedValue();
                        String pid = EntryClassUi.this.componentName.getValue();
                        if (SELECT_COMPONENT.equalsIgnoreCase(factoryPid) || "".equals(pid)) {
                            EntryClassUi.this.errorAlertText.setText(MSGS.servicesComponentFactoryAlertNotSelected());
                            errorModal.show();
                            return;
                        }
                        EntryClassUi.this.gwtComponentService.createFactoryComponent(token, factoryPid, pid,
                                new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        fetchAvailableServices();
                                    }
                                });
                    }
                });
            }
        });
    }

    public void render(GwtConfigComponent item) {
        // Do everything Content Panel related in ServicesUi
        this.contentPanelBody.clear();
        this.servicesUi = new ServicesUi(item, this);
        this.contentPanel.setVisible(true);
        if (item != null) {
            this.contentPanelHeader.setText(item.getComponentName());
        }
        this.contentPanelBody.add(this.servicesUi);
    }

    public void updateConnectionStatusImage(boolean isConnected) {
        String imgColor;
        String statusMessage;

        if (isConnected) {
            imgColor = "background-color: #007f00";
            statusMessage = MSGS.connectionStatusConnected();
        } else {
            imgColor = "background-color: #eb3d00";
            statusMessage = MSGS.connectionStatusDisconnected();
        }

        StringBuilder imageSB = new StringBuilder();
        imageSB.append("<i class=\"fa fa-plug fa-fw\" ");
        imageSB.append(
                "style=\"float: right; width: 23px; height: 23px; line-height: 23px; color: white; border-radius: 23px; ");
        imageSB.append(imgColor + "\"");
        imageSB.append("\" title=\"");
        imageSB.append(statusMessage);
        imageSB.append("\"/>");

        String html = this.status.getHTML();
        String baseStatusHTML = html.substring(0, html.indexOf("Status") + "Status".length());
        StringBuilder statusHTML = new StringBuilder(baseStatusHTML);
        statusHTML.append(imageSB.toString());
        this.status.setHTML(statusHTML.toString());
    }

    // create the prompt for dirty configuration before switching to another tab
    private void renderDirtyConfigModal(Button b) {

        boolean isUiDirty = isServicesUiDirty() || isNetworkDirty();
        isUiDirty = isUiDirty || isFirewallDirty() || isSettingsDirty();
        isUiDirty = isUiDirty || isCloudServicesDirty() || isWiresDirty();

        if (isUiDirty) {
            this.modal = new Modal();

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.warning());
            this.modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigDirty()));
            this.modal.add(body);

            ModalFooter footer = new ModalFooter();
            Button no = new Button();
            no.setText(MSGS.noButton());
            no.addStyleName("fa fa-times");
            no.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    EntryClassUi.this.modal.hide();
                }
            });
            footer.add(no);

            b.addStyleName("fa fa-check");
            footer.add(b);
            this.modal.add(footer);
            this.modal.show();

        } else {
            b.click();
        }

    }

    public boolean isServicesUiDirty() {
        if (this.servicesUi != null) {
            return this.servicesUi.isDirty();
        } else {
            return false;
        }
    }

    public boolean isNetworkDirty() {
        if (this.network.isVisible()) {
            return this.networkBinder.isDirty();
        } else {
            return false;
        }
    }

    public boolean isFirewallDirty() {
        if (this.firewall.isVisible()) {
            return this.firewallBinder.isDirty();
        } else {
            return false;
        }
    }

    public boolean isSettingsDirty() {
        if (this.settings.isVisible()) {
            return this.settingsBinder.isDirty();
        } else {
            return false;
        }
    }

    public boolean isCloudServicesDirty() {
        if (this.cloudServices.isVisible()) {
            return this.cloudServicesBinder.isDirty();
        } else {
            return false;
        }
    }

    public boolean isWiresDirty() {
        if (this.wires.isVisible()) {
            return WiresPanelUi.isDirty();
        } else {
            return false;
        }
    }

    public void setDirty(boolean b) {
        if (this.servicesUi != null) {
            this.servicesUi.setDirty(false);
        }
        if (this.network.isVisible()) {
            this.networkBinder.setDirty(false);
        }
        if (this.firewall.isVisible()) {
            this.firewallBinder.setDirty(false);
        }
        if (this.settings.isVisible()) {
            this.settingsBinder.setDirty(false);
        }
    }

    private void initWaitModal() {
        waitModal = new PopupPanel(false, true);
        Icon icon = new Icon();
        icon.setType(IconType.COG);
        icon.setSize(IconSize.TIMES4);
        icon.setSpin(true);
        waitModal.setWidget(icon);
        waitModal.setGlassEnabled(true);
        waitModal.center();
        waitModal.hide();
    }

    private void initNewComponentErrorModal() {
        this.errorModalDismiss.setText(MSGS.closeButton());
    }

    public static void showWaitModal() {
        waitModal.show();
    }

    public static void hideWaitModal() {
        waitModal.hide();
    }

    private void forceTabsCleaning() {
        if (this.servicesUi != null) {
            this.servicesUi.setDirty(false);
        }
        if (this.network.isVisible()) {
            this.networkBinder.setDirty(false);
        }
        if (this.firewall.isVisible()) {
            this.firewallBinder.setDirty(false);
        }
        if (this.settings.isVisible()) {
            this.settingsBinder.setDirty(false);
        }
        if (this.cloudServices.isVisible()) {
            this.cloudServicesBinder.setDirty(false);
        }
    }

    GwtConfigComponent getSelected() {
        return this.selected;
    }

    void setSelected(GwtConfigComponent selected) {
        this.selected = selected;
    }
}