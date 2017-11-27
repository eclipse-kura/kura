package org.eclipse.kura.web.client.ui.wires;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.TextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresDialogs extends Composite {

    interface WiresDialogsUiBinder extends UiBinder<Widget, WiresDialogs> {
    }

    private static final WiresDialogsUiBinder uiBinder = GWT.create(WiresDialogsUiBinder.class);

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    ListBox assetInstance;
    @UiField
    Button buttonSelectAssetCancel;
    @UiField
    Button buttonNewAsset;
    @UiField
    Button buttonSelectAssetOk;
    @UiField
    Modal selectDriverModal;
    @UiField
    ListBox driverInstance;
    @UiField
    Button buttonSelectDriverCancel;
    @UiField
    Button buttonNewDriver;
    @UiField
    Button buttonSelectDriverOk;
    @UiField
    Modal newAssetModal;
    @UiField
    TextBox newAssetName;
    @UiField
    TextBox newAssetDriverInstance;
    @UiField
    Button newAssetOk;
    @UiField
    Modal newDriverModal;
    @UiField
    TextBox newDriverName;
    @UiField
    ListBox newDriverFactory;
    @UiField
    Button newDriverCancel;
    @UiField
    Button newDriverOk;
    @UiField
    Modal selectAssetModal;
    @UiField
    ModalHeader newAssetModalHeader;
    @UiField
    FormLabel componentNameLabel;
    @UiField
    Modal genericCompModal;
    @UiField
    Button btnComponentModalYes;
    @UiField
    Button btnComponentModalNo;
    @UiField
    TextBox componentName;

    private Listener listener;

    public WiresDialogs() {
        initWidget(uiBinder.createAndBindUi(this));

        initSelectAssetModal();
        initSelectDriverModal();
        initAssetModal();
        initNewAssetModal();
        initNewDriverModal();
    }

    private void initSelectAssetModal() {

        this.buttonNewAsset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                EntryClassUi.showWaitModal();

                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(final Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(final GwtXSRFToken token) {
                        gwtWireService.getDriverInstances(token, new AsyncCallback<List<String>>() {

                            @Override
                            public void onFailure(final Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(final List<String> result) {
                                if (result.isEmpty()) {
                                    WiresDialogs.this.driverInstance.setEnabled(false);
                                    WiresDialogs.this.buttonSelectDriverOk.setEnabled(false);
                                } else {
                                    WiresDialogs.this.driverInstance.setEnabled(true);
                                    WiresDialogs.this.buttonSelectDriverOk.setEnabled(true);
                                }

                                WiresDialogs.this.driverInstance.clear();
                                for (String driverPid : result) {
                                    WiresDialogs.this.driverInstance.addItem(driverPid);
                                }
                                EntryClassUi.hideWaitModal();

                                WiresDialogs.this.selectDriverModal.show();
                            }
                        });
                    }
                });
            }
        });

        this.buttonSelectAssetOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (listener != null) {
                    listener.onNewAssetCreated(WiresDialogs.this.assetInstance.getSelectedValue(),
                            WiresDialogs.this.driverInstance.getSelectedValue());
                }
            }
        });
        this.buttonSelectAssetCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        });
    }

    private void initSelectDriverModal() {
        this.buttonNewDriver.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresDialogs.this.newDriverName.setValue("");
                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        gwtComponentService.getDriverFactoriesList(token, new AsyncCallback<List<String>>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(final List<String> result) {
                                if (result.isEmpty()) {
                                    WiresDialogs.this.newDriverFactory.setEnabled(false);
                                    WiresDialogs.this.newDriverOk.setEnabled(false);
                                } else {
                                    WiresDialogs.this.newDriverFactory.setEnabled(true);
                                    WiresDialogs.this.newDriverOk.setEnabled(true);
                                }

                                WiresDialogs.this.newDriverFactory.clear();
                                for (String driverFactoryPid : result) {
                                    WiresDialogs.this.newDriverFactory.addItem(driverFactoryPid);
                                }
                                WiresDialogs.this.newDriverName.setText("");
                                WiresDialogs.this.newDriverModal.show();
                            }
                        });
                    }
                });
            }
        });

        this.buttonSelectDriverOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String driverPid = WiresDialogs.this.driverInstance.getSelectedValue();
                WiresDialogs.this.newAssetDriverInstance.setText(driverPid);
                WiresDialogs.this.newAssetName.setText("");
                WiresDialogs.this.newAssetModal.show();
            }
        });
        this.buttonSelectDriverCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        });
    }

    private void initNewAssetModal() {
        this.newAssetDriverInstance.setReadOnly(true);

        this.newAssetOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String wireAssetPid = WiresDialogs.this.newAssetName.getText();
                String driverPid = WiresDialogs.this.newAssetDriverInstance.getText();
                WiresDialogs.this.newAssetModal.hide();
                if (listener != null) {
                    listener.onNewAssetCreated(wireAssetPid, driverPid);
                }
            }
        });
    }

    private void initNewDriverModal() {

        this.newDriverOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (WiresDialogs.this.newDriverName.validate()) {
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            final String driverFactoryPid = WiresDialogs.this.newDriverFactory.getSelectedValue();
                            final String pid = WiresDialogs.this.newDriverName.getValue();

                            gwtComponentService.createFactoryComponent(token, driverFactoryPid, pid,
                                    new AsyncCallback<Void>() {

                                        @Override
                                        public void onFailure(Throwable ex) {
                                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                                        }

                                        @Override
                                        public void onSuccess(Void result) {
                                            WiresDialogs.this.newDriverModal.hide();
                                            WiresDialogs.this.newAssetDriverInstance.setText(pid);
                                            WiresDialogs.this.newAssetName.setText("");
                                            WiresDialogs.this.newAssetModal.show();
                                        }
                                    });
                        }
                    });
                }
            }
        });
        this.newDriverCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        });
    }

    private void initAssetModal() {
        this.componentNameLabel.setText(MSGS.wiresComponentName());
        this.btnComponentModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String value = WiresDialogs.this.componentName.getValue();
                if (value != null && !value.isEmpty()) {
                    if (listener != null) {
                        listener.onNewComponentCreated(value);
                    }
                    genericCompModal.hide();
                }
                componentName.clear();
            }
        });
        this.btnComponentModalNo.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void pickComponent(String factoryPid, Listener listener) {
        this.listener = listener;
        if (factoryPid.contains(WiresPanelUi.WIRE_ASSET)) {
            this.selectAssetModal.show();
        } else {
            this.newAssetModalHeader.setTitle(MSGS.wiresComponentNew());
            this.componentNameLabel.setText(MSGS.wiresComponentName());

            this.genericCompModal.show();
        }
    }

    public interface Listener {

        public void onNewAssetCreated(String pid, String driverPid);

        public void onNewComponentCreated(String pid);

        public void onCancel();
    }

}
