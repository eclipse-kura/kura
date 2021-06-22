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
 *  Amit Kumar Mondal
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import static org.eclipse.kura.web.client.util.FilterBuilder.not;
import static org.eclipse.kura.web.client.util.FilterBuilder.or;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.kura.core.configuration.ConfigurationChangeEvent;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.client.ui.AlertDialog.Severity;
import org.eclipse.kura.web.client.ui.cloudconnection.CloudConnectionsUi;
import org.eclipse.kura.web.client.ui.device.DevicePanelUi;
import org.eclipse.kura.web.client.ui.drivers.assets.DriversAndAssetsUi;
import org.eclipse.kura.web.client.ui.firewall.FirewallPanelUi;
import org.eclipse.kura.web.client.ui.network.NetworkPanelUi;
import org.eclipse.kura.web.client.ui.packages.PackagesPanelUi;
import org.eclipse.kura.web.client.ui.security.SecurityPanelUi;
import org.eclipse.kura.web.client.ui.settings.SettingsPanelUi;
import org.eclipse.kura.web.client.ui.status.StatusPanelUi;
import org.eclipse.kura.web.client.ui.users.UsersPanelUi;
import org.eclipse.kura.web.client.ui.wires.WiresPanelUi;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.FilterBuilder;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.client.util.request.Request;
import org.eclipse.kura.web.client.util.request.RequestContext;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.model.GwtSecurityCapabilities;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSessionService;
import org.eclipse.kura.web.shared.service.GwtSessionServiceAsync;
import org.eclipse.kura.web2.ext.AlertSeverity;
import org.eclipse.kura.web2.ext.AuthenticationHandler;
import org.eclipse.kura.web2.ext.Context;
import org.eclipse.kura.web2.ext.ExtensionRegistry;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class EntryClassUi extends Composite implements Context, ServicesUi.Listener {

    interface EntryClassUIUiBinder extends UiBinder<Widget, EntryClassUi> {
    }

    // UI elements definition
    @UiField
    static Modal errorModal;
    @UiField
    Label footerLeft;
    @UiField
    Row footerCenterRow;
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
    AnchorListItem security;
    @UiField
    AnchorListItem users;
    @UiField
    AnchorListItem wires;
    @UiField
    AnchorListItem cloudServices;
    @UiField
    AnchorListItem driversAndAssetsServices;
    @UiField
    ScrollPanel servicesPanel;
    @UiField
    TextBox textSearch;
    @UiField
    NavPills servicesMenu;
    @UiField
    Modal errorPopup;
    @UiField
    Label errorMessage;
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
    PidTextBox componentName;
    @UiField
    Button sidenavButton;
    @UiField
    Column sidenav;
    @UiField
    Panel sidenavOverlay;
    @UiField
    Button logoutButton;
    @UiField
    Button headerLogoutButton;
    @UiField
    NavPills sidenavPills;
    @UiField
    AlertDialog alertDialog;
    @UiField
    Widget servicesContainer;
    @UiField
    Span userNameLarge;
    @UiField
    Span userNameSmall;
    @UiField
    Row mainContainer;
    @UiField
    DropdownNotification dropdownNotification;

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final EntryClassUIUiBinder uiBinder = GWT.create(EntryClassUIUiBinder.class);

    private static final String SELECT_COMPONENT = MSGS.servicesComponentFactorySelectorIdle();
    private static final String SIDENAV_HIDDEN_STYLE_NAME = "sidenav-hidden";
    private static final String SELECTED_ANCHOR_LIST_ITEM_STYLE_NAME = "selected-item";
    private static final String NOT_SCROLLABLE_STYLE_NAME = "not-scrollable";
    private static final String SERVICES_FILTER = FilterBuilder.of(not(or("service.pid=*SystemPropertiesService",
            "service.pid=*NetworkAdminService", "service.pid=*NetworkConfigurationService",
            "service.pid=*SslManagerService", "service.pid=*FirewallConfigurationService",
            "service.pid=*WireGraphService", "objectClass=org.eclipse.kura.wire.WireComponent",
            "objectClass=org.eclipse.kura.driver.Driver", "kura.ui.service.hide=true")));

    private static PopupPanel waitModal;

    private final StatusPanelUi statusBinder = GWT.create(StatusPanelUi.class);
    private final DevicePanelUi deviceBinder = GWT.create(DevicePanelUi.class);
    private final PackagesPanelUi packagesBinder = GWT.create(PackagesPanelUi.class);
    private final SettingsPanelUi settingsBinder = GWT.create(SettingsPanelUi.class);
    private final SecurityPanelUi securityBinder;
    private final UsersPanelUi usersBinder = GWT.create(UsersPanelUi.class);
    private final FirewallPanelUi firewallBinder = GWT.create(FirewallPanelUi.class);
    private final NetworkPanelUi networkBinder = GWT.create(NetworkPanelUi.class);
    private final CloudConnectionsUi cloudServicesBinder = GWT.create(CloudConnectionsUi.class);
    private final WiresPanelUi wiresBinder = GWT.create(WiresPanelUi.class);
    private final DriversAndAssetsUi driversAndTwinsBinder = GWT.create(DriversAndAssetsUi.class);

    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtSessionServiceAsync gwtSessionService = GWT.create(GwtSessionService.class);

    private final KeyUpHandler searchBoxChangeHandler = event -> {
        TextBox searchBox = (TextBox) event.getSource();
        String searchedValue = searchBox.getValue();
        if (searchedValue != null) {
            filterAvailableServices(searchedValue);
        }
    };

    private final EntryClassUi ui;
    private GwtUserData userData;
    private final GwtSecurityCapabilities securityCapabilities;

    private GwtSession currentSession;
    private GwtConfigComponent selected = null;

    private ServicesUi servicesUi;
    private AnchorListItem selectedAnchorListItem;

    private final Timer userConfigReloadTimer = new Timer() {

        @Override
        public void run() {
            RequestQueue.submit(c -> EntryClassUi.this.gwtXSRFService.generateSecurityToken(
                    c.callback(token -> EntryClassUi.this.gwtSessionService.getUserConfig(token, c.callback(config -> {
                        if (config == null) {
                            logout();
                            return;
                        }

                        if (!config.equals(EntryClassUi.this.userData)) {
                            EntryClassUi.this.alertDialog.show(
                                    MSGS.usersIdentityConfigChanged(EntryClassUi.this.userData.getUserName()),
                                    AlertDialog.Severity.ALERT, (ConfirmListener) null);
                        }

                        EntryClassUi.this.userData = config;
                    })))), false);
        }
    };

    private static GwtConsoleUserOptions userOptions;

    public EntryClassUi(final GwtUserData gwtUserData, final GwtSecurityCapabilities securityCapabilities,
            final GwtSession session) {
        initWidget(uiBinder.createAndBindUi(this));

        this.ui = this;
        this.userData = gwtUserData;
        this.securityCapabilities = securityCapabilities;
        this.securityBinder = new SecurityPanelUi(gwtUserData, securityCapabilities);

        setFooter(session);
        initSystemPanel(session);
        setSession(session);

        initWaitModal();
        initNewComponentErrorModal();
        initExceptionReportModal();
        this.contentPanelHeader.setId("contentPanelHeader");

        Date now = new Date();
        @SuppressWarnings("deprecation")
        int year = now.getYear() + 1900;
        this.footerLeft.setText(MSGS.copyright(String.valueOf(year)));
        this.footerLeft.setStyleName("copyright");
        this.contentPanel.setVisible(false);

        // Add handler for sidenav show/hide button
        this.sidenavButton.addClickHandler(event -> {
            if (EntryClassUi.this.sidenav.getStyleName().contains(SIDENAV_HIDDEN_STYLE_NAME)) {
                showSidenav();
            } else {
                hideSidenav();
            }
        });

        initLogoutButtons();
        initServicesTree();
        initExtensions();
    }

    private void initExtensions() {

        ExtensionRegistry.get().addExtensionConsumer(e -> {
            e.onLoad(this);
        });

    }

    private void initExceptionReportModal() {
        this.errorPopup.setTitle(MSGS.warning());
        FailureHandler.setBackend((title, message, stackTrace) -> {
            this.errorPopup.setTitle(title);

            this.errorMessage.setText(message);
            this.errorPopup.show();
        });
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

    public void setSession(GwtSession gwtSession) {
        this.currentSession = gwtSession;
    }

    public void setFooter(GwtSession gwtSession) {

        this.footerRight.setText(gwtSession.getKuraVersion());

        this.footerCenterRow.setVisible(false);
        if (gwtSession.isDevelopMode()) {
            this.footerCenter.setText(MSGS.developmentMode());
            this.footerCenterRow.setVisible(true);
        }
    }

    public void initSystemPanel(GwtSession gwtSession) {
        final EntryClassUi instanceReference = this;
        if (!gwtSession.isNetAdminAvailable()
                || !this.userData.checkPermissions(Collections.singleton(KuraPermission.NETWORK_ADMIN))) {
            this.network.setVisible(false);
            this.firewall.setVisible(false);
        }

        initStatusPanel(instanceReference);

        initDevicePanel();

        initNetworkPanel();

        initFirewallPanel();

        initPackagesPanel();

        initSettingsPanel();

        initSecurityPanel();

        initUsersPanel();

        initCloudServicesPanel();

        initWiresPanel();

        initDriversAndAssetsPanel();

    }

    private void initDriversAndAssetsPanel() {
        if (!this.userData.checkPermission(KuraPermission.WIRES_ADMIN)) {
            this.driversAndAssetsServices.setVisible(false);
            return;
        }

        this.driversAndAssetsServices.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.driversAndAssetsServices) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.driversAndAssetsServices);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.driversAndAssetsServices(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.driversAndTwinsBinder);
                EntryClassUi.this.driversAndTwinsBinder.refresh();
            });
        });
    }

    private void initWiresPanel() {
        if (!this.userData.checkPermission(KuraPermission.WIRES_ADMIN)) {
            this.wires.setVisible(false);
            return;
        }

        this.wires.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.wires) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.wires);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.wires(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.wiresBinder);
                EntryClassUi.this.wiresBinder.load();
            });
        });
    }

    private void initCloudServicesPanel() {
        if (!this.userData.checkPermission(KuraPermission.CLOUD_CONNECTION_ADMIN)) {
            this.cloudServices.setVisible(false);
            return;
        }

        this.cloudServices.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.cloudServices) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.cloudServices);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.cloudServices(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.cloudServicesBinder);
                EntryClassUi.this.cloudServicesBinder.refresh();

            });
        });
    }

    private void initSettingsPanel() {
        if (!this.userData.checkPermission(KuraPermission.ADMIN)) {
            this.settings.setVisible(false);
            return;
        }

        this.settings.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.settings) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.settings);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.settings(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.settingsBinder);
                EntryClassUi.this.settingsBinder.setSession(EntryClassUi.this.currentSession);
                EntryClassUi.this.settingsBinder.load();
            });
        });
    }

    private void initSecurityPanel() {
        final boolean isVisible = this.userData.checkPermission(KuraPermission.ADMIN)
                || this.userData.checkPermission(KuraPermission.MAINTENANCE)
                        && this.securityCapabilities.isTamperDetectionAvailable();

        if (!isVisible) {
            this.security.setVisible(false);
            return;
        }

        this.security.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.security) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.security);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.security(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.securityBinder);
                EntryClassUi.this.securityBinder.setSession(EntryClassUi.this.currentSession);
                EntryClassUi.this.securityBinder.load();
            });
        });
    }

    private void initUsersPanel() {
        if (!this.userData.checkPermission(KuraPermission.ADMIN)) {
            this.users.setVisible(false);
            return;
        }

        this.users.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.users) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.users);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.users(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.usersBinder);
                EntryClassUi.this.usersBinder.refresh();
            });
        });
    }

    private void initPackagesPanel() {
        if (!this.userData.checkPermission(KuraPermission.PACKAGES_ADMIN)) {
            this.packages.setVisible(false);
            return;
        }

        this.packages.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.packages) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.packages);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.packages(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.packagesBinder);
                EntryClassUi.this.packagesBinder.setSession(EntryClassUi.this.currentSession);
                EntryClassUi.this.packagesBinder.setMainUi(EntryClassUi.this.ui);
                EntryClassUi.this.packagesBinder.refresh();
            });
        });
    }

    private void initFirewallPanel() {
        if (this.firewall.isVisible()) {
            this.firewall.addClickHandler(event -> {
                if (this.selectedAnchorListItem == this.firewall) {
                    return;
                }
                confirmIfUiDirty(() -> {
                    EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.firewall);
                    EntryClassUi.this.contentPanel.setVisible(true);
                    setHeader(MSGS.firewall(), null);
                    EntryClassUi.this.contentPanelBody.clear();
                    EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.firewallBinder);
                    EntryClassUi.this.firewallBinder.initFirewallPanel();
                });
            });
        }
    }

    private void initNetworkPanel() {
        if (this.network.isVisible()) {
            this.network.addClickHandler(event -> {
                if (this.selectedAnchorListItem == this.network) {
                    return;
                }
                confirmIfUiDirty(() -> {
                    EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.network);
                    EntryClassUi.this.contentPanel.setVisible(true);
                    setHeader(MSGS.network(), null);
                    EntryClassUi.this.contentPanelBody.clear();
                    EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.networkBinder);
                    EntryClassUi.this.networkBinder.setSession(EntryClassUi.this.currentSession);
                    EntryClassUi.this.networkBinder.initNetworkPanel();
                });
            });
        }
    }

    private void initDevicePanel() {
        if (!this.userData.checkPermission(KuraPermission.DEVICE)) {
            this.device.setVisible(false);
            return;
        }

        this.device.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.device) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.device);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader(MSGS.device(), null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.deviceBinder);
                EntryClassUi.this.deviceBinder.setSession(EntryClassUi.this.currentSession);
                EntryClassUi.this.deviceBinder.initDevicePanel();
            });
        });
    }

    private void initStatusPanel(final EntryClassUi instanceReference) {
        if (!this.userData.checkPermission(KuraPermission.DEVICE)) {
            this.status.setVisible(false);
            return;
        }

        this.status.addClickHandler(event -> {
            if (this.selectedAnchorListItem == this.status) {
                return;
            }
            confirmIfUiDirty(() -> {
                EntryClassUi.this.setSelectedAnchorListItem(EntryClassUi.this.status);
                EntryClassUi.this.contentPanel.setVisible(true);
                setHeader("Status", null);
                EntryClassUi.this.contentPanelBody.clear();
                EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.statusBinder);
                EntryClassUi.this.statusBinder.setSession(EntryClassUi.this.currentSession);
                EntryClassUi.this.statusBinder.setParent(instanceReference);
                EntryClassUi.this.statusBinder.loadStatusData();
            });
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

            sl.setVisible(tmpServiceName == null || sl.getServiceName().toLowerCase().indexOf(tmpServiceName) != -1);
        }
    }

    private void sortConfigurationsByName(List<GwtConfigComponent> configs) {
        Collections.sort(configs, (arg0, arg1) -> {
            String name0;
            String pid0 = arg0.getComponentId();
            String pid1 = arg1.getComponentId();
            int start = pid0.lastIndexOf('.');
            int substringIndex = start + 1;
            if (start != -1 && substringIndex < pid0.length()) {
                name0 = pid0.substring(substringIndex);
            } else {
                name0 = pid0;
            }

            String name1;
            start = pid1.lastIndexOf('.');
            substringIndex = start + 1;
            if (start != -1 && substringIndex < pid1.length()) {
                name1 = pid1.substring(substringIndex);
            } else {
                name1 = pid1;
            }
            return name0.compareTo(name1);
        });
    }

    public void fetchUserOptions() {
        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(token -> {
            this.gwtSessionService.getUserOptions(token, c.callback(options -> userOptions = options));
        })));
    }

    public void fetchAvailableServices() {
        if (!this.userData.checkPermission(KuraPermission.ADMIN)) {
            return;
        }

        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(token -> this.gwtComponentService
                .findComponentConfigurations(token, SERVICES_FILTER, c.callback(result -> {
                    sortConfigurationsByName(result);
                    EntryClassUi.this.servicesMenu.clear();
                    for (GwtConfigComponent configComponent : result) {
                        if (!configComponent.isWireComponent()) {
                            final ServicesAnchorListItem item = new ServicesAnchorListItem(configComponent);
                            item.addClickHandler(e -> confirmIfUiDirty(() -> {
                                setSelected(configComponent);
                                setSelectedAnchorListItem(item);
                                render(configComponent);
                            }));
                            EntryClassUi.this.servicesMenu.add(item);
                        }
                    }
                    filterAvailableServices(EntryClassUi.this.textSearch.getValue());
                })))));

    }

    private void initLogoutButtons() {
        final ClickHandler logoutHandler = e -> confirmIfUiDirty(() -> logout());

        this.logoutButton.addClickHandler(logoutHandler);
        this.headerLogoutButton.addClickHandler(logoutHandler);
    }

    private void logout() {
        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(
                c.callback(token -> this.gwtSessionService.logout(token, c.callback(ok -> Window.Location.reload())))));
    }

    private void initServicesTree() {
        if (!this.userData.checkPermission(KuraPermission.ADMIN)) {
            this.servicesContainer.setVisible(false);
            return;
        }

        // Keypress handler
        this.textSearch.addKeyUpHandler(this.searchBoxChangeHandler);

        initNewFactoryComponentModal();

        this.factoriesButton.addClickHandler(event -> {
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
        });
    }

    private void initNewFactoryComponentModal() {
        this.newFactoryComponentModal.setTitle(MSGS.servicesComponentFactoryNew());
        this.newFactoryComponentFormLabel.setText(MSGS.servicesComponentFactoryFactory());
        this.componentInstanceNameLabel.setText(MSGS.servicesComponentFactoryName());
        this.componentName.setPlaceholder(MSGS.servicesComponentFactoryNamePlaceholder());
        this.buttonNewComponent.setText(MSGS.submitButton());
        this.buttonNewComponentCancel.setText(MSGS.cancelButton());

        // New factory configuration handler
        this.buttonNewComponent.addClickHandler(event -> {

            String factoryPid = EntryClassUi.this.factoriesList.getSelectedValue();
            String pid = EntryClassUi.this.componentName.getPid();
            if (pid == null) {
                return;
            }
            if (SELECT_COMPONENT.equalsIgnoreCase(factoryPid) || "".equals(pid)) {
                EntryClassUi.this.errorAlertText.setText(MSGS.servicesComponentFactoryAlertNotSelected());
                errorModal.show();
                return;
            }

            RequestQueue.submit(
                    context -> EntryClassUi.this.gwtXSRFService.generateSecurityToken(context.callback(token -> {

                        EntryClassUi.this.newFactoryComponentModal.hide();
                        EntryClassUi.this.gwtComponentService.createFactoryComponent(token, factoryPid, pid,
                                context.callback(new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable ex) {
                                        FailureHandler.showErrorMessage(MSGS.errorCreatingFactoryComponent());
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        context.defer(2000, () -> fetchAvailableServices());
                                    }
                                }));
                    })));
        });
    }

    private void setHeader(final String title, final String subTitle) {
        this.contentPanelHeader.setText(title);
    }

    public void render(GwtConfigComponent item) {
        // Do everything Content Panel related in ServicesUi
        this.contentPanelBody.clear();
        this.servicesUi = new ServicesUi(item, Optional.of(this));
        this.contentPanel.setVisible(true);

        if (item != null) {
            setHeader(item.getOCDComponentHeader(), item.getComponentDescription());
        }

        this.contentPanelBody.add(this.servicesUi);
    }

    public void confirmIfUiDirty(final Runnable action) {
        if (isUiDirty()) {
            this.alertDialog.show(MSGS.deviceConfigDirty(), () -> {
                forceTabsCleaning();
                action.run();
            });
        } else {
            action.run();
        }
    }

    public boolean isUiDirty() {
        return isServicesUiDirty() || isNetworkDirty() || isFirewallDirty() || isSettingsDirty()
                || isCloudServicesDirty() || isWiresDirty() || isDriversAndTwinsDirty() || isUsersDirty()
                || isSecurityDirty();
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

    public boolean isSecurityDirty() {
        if (this.security.isVisible()) {
            return this.securityBinder.isDirty();
        } else {
            return false;
        }
    }

    public boolean isUsersDirty() {
        if (this.users.isVisible()) {
            return this.usersBinder.isDirty();
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
            return this.wiresBinder.isDirty();
        } else {
            return false;
        }
    }

    public boolean isDriversAndTwinsDirty() {
        if (this.driversAndTwinsBinder.isVisible()) {
            return this.driversAndTwinsBinder.isDirty();
        } else {
            return false;
        }
    }

    private static void initWaitModal() {
        waitModal = new PopupPanel(false, true);
        Icon icon = new Icon();
        icon.setId("cog");
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
        waitModal.center();
        waitModal.show();
    }

    public static void hideWaitModal() {
        waitModal.hide();
    }

    public static GwtConsoleUserOptions getUserOptions() {
        return new GwtConsoleUserOptions(userOptions);
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
        if (this.security.isVisible()) {
            this.securityBinder.setDirty(false);
        }
        if (this.wires.isVisible()) {
            this.wiresBinder.clearDirtyState();
            this.wiresBinder.unload();
        }
    }

    GwtConfigComponent getSelected() {
        return this.selected;
    }

    void setSelected(GwtConfigComponent selected) {
        this.selected = selected;
    }

    public void init() {
        this.userNameLarge.setText(this.userData.getUserNameEllipsed());
        this.userNameSmall.setText(this.userData.getUserNameEllipsed());

        final EventService.Handler userAdminEventHandler = e -> {
            this.userConfigReloadTimer.schedule(1000);
        };

        EventService.subscribe(ForwardedEventTopic.ROLE_CHANGED, userAdminEventHandler);
        EventService.subscribe(ForwardedEventTopic.ROLE_CREATED, userAdminEventHandler);
        EventService.subscribe(ForwardedEventTopic.ROLE_CHANGED, userAdminEventHandler);
        EventService.subscribe(ForwardedEventTopic.ROLE_CHANGED_SHORT, userAdminEventHandler);
        EventService.subscribe(ForwardedEventTopic.ROLE_CREATED_SHORT, userAdminEventHandler);
        EventService.subscribe(ForwardedEventTopic.ROLE_CHANGED_SHORT, userAdminEventHandler);

        if (this.userData.getPermissions().isEmpty()) {
            this.alertDialog.show("The current user has no permissions", Severity.ALERT, (ConfirmListener) null);
            // hamburger button and sidenav are not more useful since menus not contain anything
            this.mainContainer.addStyleName("no-sidenav");
            this.sidenavButton.removeFromParent();
            return;
        }

        if (this.userData.checkPermission(KuraPermission.ADMIN)) {
            fetchAvailableServices();
        }
        if (this.userData.checkPermission(KuraPermission.DEVICE)) {
            showStatusPanel();
        }
        fetchUserOptions();

        Window.addWindowClosingHandler(e -> {
            if (isUiDirty()) {
                e.setMessage(MSGS.deviceConfigDirty());
            }
        });

        EventService.subscribe(ForwardedEventTopic.CONF_CHANGE_EVENT, this::handleConcurrencyEvent);
    }

    private void handleConcurrencyEvent(GwtEventInfo eventInfo) {
        String eventPid = (String) eventInfo.getProperties().get(ConfigurationChangeEvent.CONF_CHANGE_EVENT_PID_PROP);
        if (eventPid.length() == 0) {
            this.dropdownNotification.show(MSGS.configurationChangeEventNotificationGeneric());
        } else {
            this.dropdownNotification.show(MSGS.configurationChangeEventNotification(eventPid));
        }
    }

    private void showStatusPanel() {
        setSelectedAnchorListItem(this.status);
        this.contentPanel.setVisible(true);
        this.contentPanelHeader.setText("Status");
        this.contentPanelBody.clear();
        this.contentPanelBody.add(EntryClassUi.this.statusBinder);
        this.statusBinder.setSession(EntryClassUi.this.currentSession);
        this.statusBinder.setParent(this);
        this.statusBinder.loadStatusData();
    }

    @Override
    public void addSidenavComponent(final String name, final String icon, final WidgetFactory widgetFactory) {
        final AnchorListItem item = new AnchorListItem(name);

        try {
            item.setIcon(IconType.valueOf(icon));
        } catch (final Exception e) {
            // do nothing
        }

        item.addClickHandler(evt -> confirmIfUiDirty(() -> {
            EntryClassUi.this.contentPanelBody.clear();

            forceTabsCleaning();
            EntryClassUi.this.setSelectedAnchorListItem(item);
            EntryClassUi.this.contentPanel.setVisible(true);
            setHeader(name, null);
            EntryClassUi.this.contentPanelBody.clear();

            EntryClassUi.this.contentPanelBody.add(widgetFactory.buildWidget());

            EntryClassUi.this.deviceBinder.setSession(EntryClassUi.this.currentSession);
            EntryClassUi.this.deviceBinder.initDevicePanel();
        }));

        this.sidenavPills.add(item);
    }

    @Override
    public void addSettingsComponent(final String name, final WidgetFactory factory) {
        this.settingsBinder.addTab(name, factory);
    }

    @Override
    public void addAuthenticationHandler(final AuthenticationHandler authenticationHandler) {
        // unsupported
    }

    @Override
    public void getXSRFToken(Callback<String, String> callback) {
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onSuccess(GwtXSRFToken result) {
                callback.onSuccess(result.getToken());
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught.getMessage());
            }

        });

    }

    private class WrapperRequest implements Callback<Void, String>, Request {

        private AsyncCallback<Void> wrapped;

        @Override
        public void onSuccess(Void result) {
            this.wrapped.onSuccess(null);
        }

        @Override
        public void onFailure(String reason) {
            this.wrapped.onFailure(new RuntimeException(reason));
        }

        @Override
        public void run(RequestContext context) {
            this.wrapped = context.callback();
        }

    }

    @Override
    public Callback<Void, String> startLongRunningOperation() {

        final WrapperRequest callback = new WrapperRequest();

        RequestQueue.submit(callback);

        return callback;

    }

    @Override
    public void showAlertDialog(final String message, final AlertSeverity severity, final Consumer<Boolean> callback) {
        this.alertDialog.show(message,
                severity == AlertSeverity.INFO ? AlertDialog.Severity.INFO : AlertDialog.Severity.ALERT,
                callback::accept);
    }

    @Override
    public void onConfigurationChanged() {
        fetchAvailableServices();
    }
}