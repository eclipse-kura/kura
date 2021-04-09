/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.ServicesUi;
import org.eclipse.kura.web.client.ui.ServicesUi.ValidationResult;
import org.eclipse.kura.web.client.ui.ServicesUi.Validator;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtSecurityCapabilities;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.Widget;

public class SecurityPanelUi extends Composite {

    private static SecurityPanelUiUiBinder uiBinder = GWT.create(SecurityPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(SecurityPanelUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    interface SecurityPanelUiUiBinder extends UiBinder<Widget, SecurityPanelUi> {
    }

    GwtSession session;

    @UiField
    CertificateListTabUi certificateListPanel;
    @UiField
    SslManagerServicesUi sslPanel;
    @UiField
    TabPane httpServicePanel;
    @UiField
    TabPane consolePanel;
    @UiField
    ThreatManagerTabUi threatManagerPanel;
    @UiField
    TamperDetectionTabUi tamperDetectionPanel;
    @UiField
    SecurityTabUi securityPanel;

    @UiField
    TabListItem certificateList;
    @UiField
    TabListItem ssl;
    @UiField
    TabListItem httpService;
    @UiField
    TabListItem console;
    @UiField
    TabListItem threatManager;
    @UiField
    TabListItem tamperDetection;
    @UiField
    TabListItem security;

    @UiField
    TabContent tabContent;
    @UiField
    NavTabs navTabs;

    @UiField
    HTMLPanel securityIntro;

    @UiField
    AlertDialog alertDialog;

    private TabListItem selected = certificateList;

    public SecurityPanelUi(final GwtUserData userData, final GwtSecurityCapabilities capabilities) {
        logger.log(Level.FINER, "Initiating SecurityPanelUI...");

        initWidget(uiBinder.createAndBindUi(this));
        Paragraph description = new Paragraph();
        description.setText(MSGS.securityIntro());
        this.securityIntro.add(description);

        final boolean hasAdminPermission = userData.checkPermission(KuraPermission.ADMIN);
        final boolean hasMaintenancePermission = userData.checkPermission(KuraPermission.MAINTENANCE);

        if (hasAdminPermission) {

            this.certificateList.setVisible(true);
            this.ssl.setVisible(true);
            this.httpService.setVisible(true);
            this.console.setVisible(true);
            this.security.setVisible(capabilities.isSecurityServiceAvailable());
            this.threatManager.setVisible(capabilities.isThreatManagerAvailable());

            this.certificateList.addClickHandler(addDirtyCheck(new Tab.RefreshHandler(this.certificateListPanel)));
            this.ssl.addClickHandler(addDirtyCheck(new Tab.RefreshHandler(this.sslPanel)));
            this.httpService.addClickHandler(
                    addDirtyCheck(e -> this.loadServiceConfig("org.eclipse.kura.http.server.manager.HttpService", //
                            httpServicePanel, //
                            getHttpServiceOptionsValidator(), //
                            ui -> ui.onApply(err -> {
                                ui.setDirty(false);
                                Window.Location.reload();
                            }))));
            this.console.addClickHandler(addDirtyCheck(e -> this.loadServiceConfig("org.eclipse.kura.web.Console",
                    consolePanel, getConsoleOptionsValidator())));
            this.threatManager.addClickHandler(addDirtyCheck(new Tab.RefreshHandler(this.threatManagerPanel)));
            this.tamperDetection
                    .addClickHandler(addDirtyCheck(new Tab.RefreshHandler(this.tamperDetectionPanel, true)));
            this.security.addClickHandler(addDirtyCheck(new Tab.RefreshHandler(this.securityPanel)));
        }

        if (hasAdminPermission || hasMaintenancePermission) {
            this.tamperDetection.setVisible(capabilities.isTamperDetectionAvailable());
        }

        if (hasAdminPermission) {
            this.certificateList.setActive(true);
            ((TabPane) this.certificateListPanel.getParent()).setActive(true);
        } else {
            this.tamperDetection.setActive(true);
            ((TabPane) this.tamperDetectionPanel.getParent()).setActive(true);
        }

    }

    public void loadServiceConfig(final String pid, final TabPane panel, final ServicesUi.Validator validator) {
        loadServiceConfig(pid, panel, validator, ui -> {
        });
    }

    public void loadServiceConfig(final String pid, final TabPane panel, final ServicesUi.Validator validator,
            final Consumer<ServicesUi> customizer) {
        RequestQueue.submit(c -> gwtXSRFService.generateSecurityToken(c.callback(
                token -> gwtComponentService.findFilteredComponentConfiguration(token, pid, c.callback(result -> {
                    for (GwtConfigComponent config : result) {
                        panel.clear();

                        final ServicesUi ui = new ServicesUi(config, Optional.empty(), Optional.of(validator));

                        customizer.accept(ui);

                        panel.add(ui);
                    }
                })))));
    }

    public void load() {
        if (this.certificateList.isActive()) {
            this.certificateListPanel.refresh();
        } else if (this.tamperDetection.isActive()) {
            this.tamperDetectionPanel.refresh();
        }
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public boolean isDirty() {
        boolean certListDirty = this.certificateListPanel.isDirty();
        boolean sslConfigDirty = this.sslPanel.isDirty();
        boolean securityDirty = this.securityPanel.isDirty();
        boolean threatManagerDirty = this.threatManagerPanel.isDirty();
        boolean webConsoleDirty = isServicesUiDirty(this.consolePanel);
        boolean httpServiceDirty = isServicesUiDirty(this.httpServicePanel);

        return certListDirty || sslConfigDirty || threatManagerDirty || securityDirty || webConsoleDirty
                || httpServiceDirty;
    }

    public void addTab(final String name, final WidgetFactory widgetFactory) {

        final TabPane tabPane = new TabPane();
        tabPane.setId("__extension__" + name);

        final TabListItem item = new TabListItem(name);
        item.setDataTarget("#__extension__" + name);

        item.addClickHandler(e -> {
            tabPane.clear();
            tabPane.add(widgetFactory.buildWidget());
        });

        this.navTabs.add(item);
        this.tabContent.add(tabPane);
    }

    public void setDirty(boolean b) {
        this.certificateListPanel.setDirty(b);
        this.sslPanel.setDirty(b);
        this.securityPanel.setDirty(b);
        this.threatManagerPanel.setDirty(b);
        getServicesUi(this.httpServicePanel).ifPresent(u -> u.setDirty(b));
        getServicesUi(this.consolePanel).ifPresent(u -> u.setDirty(b));
    }

    private ClickHandler addDirtyCheck(final ClickHandler handler) {
        return e -> {
            final TabListItem newSelection = (TabListItem) ((Anchor) e.getSource()).getParent();
            if (isDirty()) {
                this.alertDialog.show(MSGS.deviceConfigDirty(), accepted -> {
                    if (accepted) {
                        setDirty(false);
                        handler.onClick(e);
                        selected = newSelection;
                    } else {
                        selected.showTab();
                    }
                });
            } else {
                handler.onClick(e);
                selected = newSelection;
            }
        };
    }

    private static boolean isEnabledBooleanParameter(final GwtConfigParameter param, final String prefix) {
        return param.getId().startsWith(prefix) && "true".equals(param.getValue());
    }

    private static boolean isEmptyParameter(final GwtConfigParameter param, final String prefix) {
        return param.getId().startsWith(prefix) && (param.getValue() == null || param.getValue().trim().isEmpty());
    }

    private Validator getConsoleOptionsValidator() {
        return config -> {
            final List<ValidationResult> result = new ArrayList<>();
            if (config.getParameters().stream().noneMatch(p -> isEnabledBooleanParameter(p, "auth.method"))) {
                result.add(ValidationResult.warning(MSGS.securityAllAuthMethodsDisabled()));
            }
            return result;
        };
    }

    private Validator getHttpServiceOptionsValidator() {
        return config -> {
            final List<ValidationResult> result = new ArrayList<>();

            final Set<Integer> ports = new HashSet<>();
            final Set<Integer> httpPorts = new HashSet<>();
            final Set<Integer> httpsPorts = new HashSet<>();
            final Set<Integer> httpsClientAuthPorts = new HashSet<>();
            boolean haveHttpsKeystorePath = true;
            boolean haveHttpsKeystorePassword = true;

            for (final GwtConfigParameter p : config.getParameters()) {
                if (p.getId().endsWith("ports")) {
                    getIntegerFieldValues(p, port -> {
                        if (ports.contains(port)) {
                            result.add(ValidationResult.error(MSGS.securityPortConflictOrInvalidPort()));
                        }
                        ports.add(port);
                        if (p.getId().startsWith("http.")) {
                            httpPorts.add(port);
                        } else if (p.getId().startsWith("https.ports")) {
                            httpsPorts.add(port);
                        } else if (p.getId().startsWith("https.client")) {
                            httpsClientAuthPorts.add(port);
                        }
                    });
                }
                if (isEmptyParameter(p, "https.keystore.path")) {
                    haveHttpsKeystorePath = false;
                }
                if (isEmptyParameter(p, "https.keystore.password")) {
                    haveHttpsKeystorePassword = false;
                }
            }

            if (httpPorts.isEmpty() && httpsPorts.isEmpty() && httpsClientAuthPorts.isEmpty()) {
                result.add(ValidationResult.warning(MSGS.securityAllConnectorsDisabled()));
            }

            if ((!httpsPorts.isEmpty() || !httpsClientAuthPorts.isEmpty())
                    && (!haveHttpsKeystorePath || !haveHttpsKeystorePassword)) {
                result.add(ValidationResult.error(MSGS.securityHttpsEnabledButKeystoreParametersMissing()));
            }

            if (result.isEmpty()) {
                result.add(ValidationResult.warning(MSGS.securityHttpServiceUiReloadWarning()));
            }

            return result;
        };
    }

    private void getIntegerFieldValues(final GwtConfigParameter param, final IntConsumer consumer) {
        final String[] values = param.getValues();

        if (values == null) {
            return;
        }

        for (final String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            consumer.accept(Integer.parseInt(value));
        }
    }

    public Optional<ServicesUi> getServicesUi(final IndexedPanel parent) {

        for (int i = 0; i < parent.getWidgetCount(); i++) {
            final Widget child = parent.getWidget(i);

            if (child instanceof ServicesUi) {
                return Optional.of((ServicesUi) child);
            }
        }

        return Optional.empty();
    }

    public boolean isServicesUiDirty(final IndexedPanel parent) {
        return getServicesUi(parent).map(ServicesUi::isDirty).orElse(false);
    }
}
