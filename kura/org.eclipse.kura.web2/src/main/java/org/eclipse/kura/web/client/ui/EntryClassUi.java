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
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Device.DevicePanelUi;
import org.eclipse.kura.web.client.ui.Firewall.FirewallPanelUi;
import org.eclipse.kura.web.client.ui.Network.NetworkPanelUi;
import org.eclipse.kura.web.client.ui.Packages.PackagesPanelUi;
import org.eclipse.kura.web.client.ui.Settings.SettingsPanelUi;
import org.eclipse.kura.web.client.ui.Status.StatusPanelUi;
import org.eclipse.kura.web.client.ui.resources.Resources;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
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
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.logging.client.HasWidgetsLogHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class EntryClassUi extends Composite {

    interface EntryClassUIUiBinder extends UiBinder<Widget, EntryClassUi> {
    }

    private static EntryClassUIUiBinder uiBinder = GWT.create(EntryClassUIUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);
    private static final Logger logger = Logger.getLogger(EntryClassUi.class.getSimpleName());
    private static Logger errorLogger = Logger.getLogger("ErrorLogger");

    private final StatusPanelUi statusBinder = GWT.create(StatusPanelUi.class);
    private final DevicePanelUi deviceBinder = GWT.create(DevicePanelUi.class);
    private final PackagesPanelUi packagesBinder = GWT.create(PackagesPanelUi.class);
    private final SettingsPanelUi settingsBinder = GWT.create(SettingsPanelUi.class);
    private final FirewallPanelUi firewallBinder = GWT.create(FirewallPanelUi.class);
    private final NetworkPanelUi networkBinder = GWT.create(NetworkPanelUi.class);
    // private final WiresPanelUi wiresBinder = GWT.create(WiresPanelUi.class);

    private final GwtPackageServiceAsync gwtPackageService = GWT.create(GwtPackageService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    public GwtConfigComponent selected = null;

    static PopupPanel m_waitModal;
    GwtSession currentSession;
    AnchorListItem service;
    GwtConfigComponent addedItem;
    EntryClassUi ui;
    Modal modal;
    ServicesUi servicesUi;

    private boolean servicesDirty;
    private boolean networkDirty;
    private boolean firewallDirty;
    private boolean settingsDirty;

    @UiField
    Panel header;
    @UiField
    Label footerLeft, footerCenter, footerRight;
    @UiField
    Panel contentPanel;
    @UiField
    PanelHeader contentPanelHeader;
    @UiField
    PanelBody contentPanelBody;
    @UiField
    TabListItem status;
    @UiField
    AnchorListItem device, network, firewall, packages, settings;
    @UiField
    ScrollPanel servicesPanel;
    @UiField
    NavPills servicesMenu;
    @UiField
    VerticalPanel errorLogArea;
    @UiField
    Modal errorPopup;

    public EntryClassUi() {
        logger.log(Level.FINER, "Initiating UiBinder");
        this.ui = this;
        initWidget(uiBinder.createAndBindUi(this));

        // TODO : standardize the URL?
        // header.setUrl("eclipse/kura/icons/kura_logo_small.png");
        this.header.setStyleName("headerLogo");
        Date now = new Date();
        @SuppressWarnings("deprecation")
        int year = now.getYear() + 1900;
        this.footerLeft.setText(MSGS.copyright(String.valueOf(year)));
        this.footerLeft.setStyleName("copyright");
        this.contentPanel.setVisible(false);

        // Set client side logging
        errorLogger.addHandler(new HasWidgetsLogHandler(this.errorLogArea));
        this.errorPopup.addHideHandler(new ModalHideHandler() {

            @Override
            public void onHide(ModalHideEvent evt) {
                EntryClassUi.this.errorLogArea.clear();
            }
        });

        //
        dragDropInit(this);

        FailureHandler.setPopup(this.errorPopup);
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

    public void initSystemPanel(GwtSession GwtSession, boolean connectionStatus) {
        final EntryClassUi m_instanceReference = this;
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
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText("Status");
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.statusBinder);
                        EntryClassUi.this.statusBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.statusBinder.setParent(m_instanceReference);
                        EntryClassUi.this.statusBinder.loadStatusData();
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
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.device());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.deviceBinder);
                        EntryClassUi.this.deviceBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.deviceBinder.initDevicePanel();
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
                            EntryClassUi.this.contentPanel.setVisible(true);
                            EntryClassUi.this.contentPanelHeader.setText(MSGS.network());
                            EntryClassUi.this.contentPanelBody.clear();
                            EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.networkBinder);
                            EntryClassUi.this.networkBinder.setSession(EntryClassUi.this.currentSession);
                            EntryClassUi.this.networkBinder.initNetworkPanel();
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
                            EntryClassUi.this.contentPanel.setVisible(true);
                            EntryClassUi.this.contentPanelHeader.setText(MSGS.firewall());
                            EntryClassUi.this.contentPanelBody.clear();
                            EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.firewallBinder);
                            EntryClassUi.this.firewallBinder.initFirewallPanel();
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
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.packages());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.packagesBinder);
                        EntryClassUi.this.packagesBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.packagesBinder.setMainUi(EntryClassUi.this.ui);
                        EntryClassUi.this.packagesBinder.refresh();
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
                        EntryClassUi.this.contentPanel.setVisible(true);
                        EntryClassUi.this.contentPanelHeader.setText(MSGS.settings());
                        EntryClassUi.this.contentPanelBody.clear();
                        EntryClassUi.this.contentPanelBody.add(EntryClassUi.this.settingsBinder);
                        EntryClassUi.this.settingsBinder.setSession(EntryClassUi.this.currentSession);
                        EntryClassUi.this.settingsBinder.load();
                    }
                });
                renderDirtyConfigModal(b);
            }
        });
    }

    public void initServicesTree() {
        // (Re)Fetch Available Services
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                EntryClassUi.this.gwtComponentService.findComponentConfigurations(token,
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
                            EntryClassUi.this.servicesMenu.add(new ServicesAnchorListItem(pair, EntryClassUi.this.ui));
                        }
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

        Image img;
        String statusMessage;

        if (isConnected) {
            img = new Image(Resources.INSTANCE.greenPlug32().getSafeUri());
            statusMessage = MSGS.connectionStatusConnected();
        } else {
            img = new Image(Resources.INSTANCE.redPlug32().getSafeUri());
            statusMessage = MSGS.connectionStatusDisconnected();
        }

        StringBuilder imageSB = new StringBuilder();
        imageSB.append("<image src=\"");
        imageSB.append(img.getUrl());
        imageSB.append("\" ");
        imageSB.append("width=\"23\" height=\"23\" style=\"vertical-align: middle; float: right;\" title=\"");
        imageSB.append(statusMessage);
        imageSB.append("\"/>");

        String baseStatusHTML = this.status.getHTML().split("<im")[0];
        StringBuilder statusHTML = new StringBuilder(baseStatusHTML);
        statusHTML.append(imageSB.toString());
        this.status.setHTML(statusHTML.toString());
    }

    // create the prompt for dirty configuration before switching to another tab
    private void renderDirtyConfigModal(Button b) {
        if (this.servicesUi != null) {
            this.servicesDirty = this.servicesUi.isDirty();
        }

        if (this.network.isVisible()) {
            this.networkDirty = this.networkBinder.isDirty();
        } else {
            this.networkDirty = false;
        }

        if (this.firewall.isVisible()) {
            this.firewallDirty = this.firewallBinder.isDirty();
        } else {
            this.firewallDirty = false;
        }

        if (this.settings.isVisible()) {
            this.settingsDirty = this.settingsBinder.isDirty();
        }

        if (this.servicesUi != null && this.servicesUi.isDirty() || this.networkDirty || this.firewallDirty
                || this.settingsDirty) {
            this.modal = new Modal();

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.warning());
            this.modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigDirty()));
            this.modal.add(body);

            ModalFooter footer = new ModalFooter();
            footer.add(b);
            footer.add(new Button(MSGS.noButton(), new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    EntryClassUi.this.modal.hide();
                }
            }));
            this.modal.add(footer);
            this.modal.show();

        } else {
            b.click();
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

    public static void showWaitModal() {
        m_waitModal = new PopupPanel(false, true);
        Icon icon = new Icon();
        icon.setType(IconType.COG);
        icon.setSize(IconSize.TIMES4);
        icon.setSpin(true);
        m_waitModal.setWidget(icon);
        m_waitModal.setGlassEnabled(true);
        m_waitModal.center();
        m_waitModal.show();
    }

    public static void hideWaitModal() {
        if (m_waitModal != null) {
            m_waitModal.hide();
        }
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
    }

    private void eclipseMarketplaceInstall(String url) {

        // Construct the REST URL for Eclipse Marketplace
        String appId = url.split("=")[1];
        final String empApi = "http://marketplace.eclipse.org/node/" + appId + "/api/p";

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
                EntryClassUi.this.gwtPackageService.getMarketplaceUri(token, empApi, new AsyncCallback<String>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(String result) {
                        installMarketplaceDp(result);
                        Timer timer = new Timer() {

                            @Override
                            public void run() {
                                initServicesTree();
                                EntryClassUi.hideWaitModal();
                            }
                        };
                        timer.schedule(2000);
                    }
                });

            }
        });
    }

    private void installMarketplaceDp(final String uri) {
        String url = "/" + GWT.getModuleName() + "/file/deploy/url";
        final RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                StringBuilder sb = new StringBuilder();
                sb.append("xsrfToken=" + token.getToken());
                sb.append("&packageUrl=" + uri);

                builder.setHeader("Content-type", "application/x-www-form-urlencoded");
                try {
                    builder.sendRequest(sb.toString(), new RequestCallback() {

                        @Override
                        public void onResponseReceived(Request request, Response response) {
                            logger.info(response.getText());
                        }

                        @Override
                        public void onError(Request request, Throwable ex) {
                            logger.log(Level.SEVERE, ex.getMessage(), ex);
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                    });
                } catch (RequestException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    FailureHandler.handle(e, EntryClassUi.class.getName());
                }
            }
        });
    }

    public static native void dragDropInit(EntryClassUi ecu) /*-{
                                                             $wnd.$("html").on("dragover", function(event) {
                                                             event.preventDefault();
                                                             event.stopPropagation();
                                                             });

                                                             $wnd.$("html").on("dragleave", function(event) {
                                                             event.preventDefault();
                                                             event.stopPropagation();
                                                             });

                                                             $wnd.$("html").on("drop", function(event) {
                                                             event.preventDefault();
                                                             event.stopPropagation();
                                                             console.log(event.originalEvent.dataTransfer.getData("text"));
                                                             if (confirm("Install file?") == true) {
                                                             ecu.@org.eclipse.kura.web.client.ui.EntryClassUi::eclipseMarketplaceInstall(Ljava/lang/String;)(event.originalEvent.dataTransfer.getData("text"));
                                                             }
                                                             });
                                                             }-*/;
}