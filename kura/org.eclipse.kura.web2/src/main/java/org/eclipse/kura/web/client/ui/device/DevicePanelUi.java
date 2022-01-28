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

import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDockerConfigurableGenericService;
import org.eclipse.kura.web.shared.service.GwtDockerConfigurableGenericServiceAsync;
import org.eclipse.kura.web.shared.service.GwtRestrictedComponentServiceAsync;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class DevicePanelUi extends Composite {

    private static DevicePanelUiUiBinder uiBinder = GWT.create(DevicePanelUiUiBinder.class);

    private GwtSession session;

    interface DevicePanelUiUiBinder extends UiBinder<Widget, DevicePanelUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    HTMLPanel deviceIntro;

    @UiField
    TabListItem profile;
    @UiField
    TabListItem bundles;
    @UiField
    TabListItem threads;
    @UiField
    TabListItem packages;
    @UiField
    TabListItem systemProperties;
    @UiField
    TabListItem containers;

    @UiField
    ProfileTabUi profilePanel;
    @UiField
    BundlesTabUi bundlesPanel;
    @UiField
    ThreadsTabUi threadsPanel;
    @UiField
    SystemPackagesTabUi packagesPanel;
    @UiField
    SystemPropertiesTabUi systemPropertiesPanel;
    @UiField
    CommandTabUi commandPanel;
    @UiField
    LogTabUi logPanel;
    @UiField
    DockerContainersTabUi dockerContainersPanel;

    public DevicePanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.deviceIntro.add(new Span("<p>" + MSGS.deviceIntro() + "</p"));

        this.profile.addClickHandler(new Tab.RefreshHandler(this.profilePanel));
        this.bundles.addClickHandler(new Tab.RefreshHandler(this.bundlesPanel));
        this.threads.addClickHandler(new Tab.RefreshHandler(this.threadsPanel));
        this.packages.addClickHandler(new Tab.RefreshHandler(this.packagesPanel));
        this.systemProperties.addClickHandler(new Tab.RefreshHandler(this.systemPropertiesPanel));
        this.containers.addClickHandler(new Tab.RefreshHandler(this.dockerContainersPanel));
        this.dockerContainersPanel.setBackend(new DockerConfigurableGenericServiceWrapper());
    }

    public void initDevicePanel() {
        this.profilePanel.refresh();
        this.commandPanel.setSession(this.session);
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    private static class DockerConfigurableGenericServiceWrapper implements GwtRestrictedComponentServiceAsync {

        private static GwtDockerConfigurableGenericServiceAsync wrapped = GWT
                .create(GwtDockerConfigurableGenericService.class);

        @Override
        public void listFactoryPids(AsyncCallback<Set<String>> callback) {
            wrapped.listFactoryPids(callback);
        }

        @Override
        public void listServiceInstances(AsyncCallback<List<GwtComponentInstanceInfo>> callback) {
            wrapped.listServiceInstances(callback);
        }

        @Override
        public void createFactoryConfiguration(GwtXSRFToken token, String pid, String factoryPid,
                AsyncCallback<Void> callback) {
            wrapped.createFactoryConfiguration(token, pid, factoryPid, callback);
        }

        @Override
        public void getConfiguration(GwtXSRFToken token, String pid, AsyncCallback<GwtConfigComponent> callback) {
            wrapped.getConfiguration(token, pid, callback);
        }

        @Override
        public void updateConfiguration(GwtXSRFToken token, GwtConfigComponent component,
                AsyncCallback<Void> callback) {
            wrapped.updateConfiguration(token, component, callback);
        }

        @Override
        public void deleteFactoryConfiguration(GwtXSRFToken token, String pid, AsyncCallback<Void> callback) {
            wrapped.deleteFactoryConfiguration(token, pid, callback);
        }
    }
}
