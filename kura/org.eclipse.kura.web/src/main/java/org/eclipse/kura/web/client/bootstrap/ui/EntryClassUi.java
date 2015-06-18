package org.eclipse.kura.web.client.bootstrap.ui;

import java.util.ArrayList;

import org.eclipse.kura.web.client.bootstrap.ui.Device.DevicePanelUi;
import org.eclipse.kura.web.client.bootstrap.ui.Firewall.FirewallPanelUi;
import org.eclipse.kura.web.client.bootstrap.ui.Network.NetworkPanelUi;
import org.eclipse.kura.web.client.bootstrap.ui.Packages.PackagesPanelUi;
import org.eclipse.kura.web.client.bootstrap.ui.Settings.SettingsPanelUi;
import org.eclipse.kura.web.client.bootstrap.ui.Status.StatusPanelUi;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtBSConfigComponent;
import org.eclipse.kura.web.shared.model.GwtBSSession;
import org.eclipse.kura.web.shared.service.GwtBSComponentService;
import org.eclipse.kura.web.shared.service.GwtBSComponentServiceAsync;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Image;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class EntryClassUi extends Composite {

	private static EntryClassUIUiBinder uiBinder = GWT
			.create(EntryClassUIUiBinder.class);
	private static final Messages MSGS = GWT.create(Messages.class);
	private final GwtBSComponentServiceAsync gwtComponentService = GWT
			.create(GwtBSComponentService.class);

	public GwtBSConfigComponent selected = null;

	interface EntryClassUIUiBinder extends UiBinder<Widget, EntryClassUi> {
	}

	GwtBSSession currentSession;
	AnchorListItem service;
	GwtBSConfigComponent addedItem;
	EntryClassUi ui;
	Modal modal;
	boolean servicesDirty,networkDirty;

	private final StatusPanelUi statusBinder = GWT.create(StatusPanelUi.class);
	private final DevicePanelUi deviceBinder = GWT.create(DevicePanelUi.class);
	private final PackagesPanelUi packagesBinder = GWT
			.create(PackagesPanelUi.class);
	private final SettingsPanelUi settingsBinder = GWT
			.create(SettingsPanelUi.class);
	private final FirewallPanelUi firewallBinder = GWT
			.create(FirewallPanelUi.class);
	private final NetworkPanelUi networkBinder = GWT
			.create(NetworkPanelUi.class);
	ServicesUi servicesUi;

	@UiField
	Image header;
	@UiField
	Label footerLeft, footerRight;
	@UiField
	Panel contentPanel;
	@UiField
	PanelHeader contentPanelHeader;
	@UiField
	PanelBody contentPanelBody;
	@UiField
	AnchorListItem status, device, network, firewall, packages, settings;
	@UiField
	ScrollPanel servicesPanel;
	@UiField
	NavPills servicesMenu;

	public EntryClassUi() {
		Growl.growl("1----------------");
		Log.debug("Initiating UiBinder");
		ui = this;
		Growl.growl("2----------------");
		initWidget(uiBinder.createAndBindUi(this));
		Growl.growl("3----------------");
		// TODO : standardize the URL?
		header.setUrl("eclipse/kura/icons/kura_logo_small.png");
		footerLeft.setText(MSGS.copyright());
		contentPanel.setVisible(false);
	}

	public void setSession(GwtBSSession gwtBSSession) {
		currentSession = gwtBSSession;
	}

	public void setFooter(GwtBSSession gwtBSSession) {

		footerRight.setText(gwtBSSession.getKuraVersion());

	}

	public void initSystemPanel(GwtBSSession gwtBSSession) {
		if (!gwtBSSession.isNetAdminAvailable()) {
			Growl.growl("4----------------");
			//network.setVisible(false);
			//firewall.setVisible(false);
		}
			// Status Panel
			status.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Button b = new Button(MSGS.yesButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) { 
							if (modal!=null ) {
								modal.hide();
							}
							if (servicesUi != null) {
								servicesUi.renderForm();
							}
							status.setIconSpin(true);
							contentPanel.setVisible(true);
							contentPanelHeader.setText("Status");
							contentPanelBody.clear();
							contentPanelBody.add(statusBinder);
							statusBinder.setSession(currentSession);
							Timer timer = new Timer() {
								@Override
								public void run() {
									status.setIconSpin(false);
								}
							};
							timer.schedule(2000);
							statusBinder.loadStatusData();
						}
					});
					renderDirtyConfigModal(b);
				}
			});

			// Device Panel
			device.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					Button b = new Button(MSGS.yesButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							if (modal!=null ) {
								modal.hide();
							}
							if (servicesUi != null) {
								servicesUi.renderForm();
							}
							device.setIconSpin(true);
							contentPanel.setVisible(true);
							contentPanelHeader.setText(MSGS.device());
							contentPanelBody.clear();
							contentPanelBody.add(deviceBinder);
							Timer timer = new Timer() {
								@Override
								public void run() {
									device.setIconSpin(false);
								}
							};
							timer.schedule(2000);
						}
					});
					renderDirtyConfigModal(b);
				}
			});

			// Network Panel
			if (network.isVisible()) {
				network.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						Button b = new Button(MSGS.yesButton(),
								new ClickHandler() {
									@Override
									public void onClick(ClickEvent event) {
										if (modal!=null ) {
											modal.hide();
										}
										if (servicesUi != null) {
											servicesUi.renderForm();
										}
										network.setIconSpin(true);
										contentPanel.setVisible(true);
										contentPanelHeader.setText(MSGS
												.network());
										contentPanelBody.clear();
										contentPanelBody.add(networkBinder);
										networkBinder
												.setSession(currentSession);
										Timer timer = new Timer() {
											@Override
											public void run() {
												network.setIconSpin(false);
											}
										};
										timer.schedule(2000);
									}
								});
						renderDirtyConfigModal(b);
					}
				});
			}

			// Firewall Panel
			if (firewall.isVisible()) {
				firewall.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						Button b = new Button(MSGS.yesButton(),
								new ClickHandler() {
									@Override
									public void onClick(ClickEvent event) {
										if (modal!=null ) {
											modal.hide();
										}
										if (servicesUi != null) {
											servicesUi.renderForm();
										}
										firewall.setIconSpin(true);
										contentPanel.setVisible(true);
										contentPanelHeader.setText(MSGS
												.firewall());
										contentPanelBody.clear();
										contentPanelBody.add(firewallBinder);
										Timer timer = new Timer() {
											@Override
											public void run() {
												firewall.setIconSpin(false);
											}
										};
										timer.schedule(2000);

									}
								});
						renderDirtyConfigModal(b);
					}
				});
			}

			// Packages Panel
			packages.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Button b = new Button(MSGS.yesButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							if (modal!=null ) {
								modal.hide();
							}
							if (servicesUi != null) {
								servicesUi.renderForm();
							}
							packages.setIconSpin(true);
							contentPanel.setVisible(true);
							contentPanelHeader.setText(MSGS.packages());
							contentPanelBody.clear();
							contentPanelBody.add(packagesBinder);
							packagesBinder.setSession(currentSession);
							Timer timer = new Timer() {
								@Override
								public void run() {
									packages.setIconSpin(false);
								}
							};
							timer.schedule(2000);

						}
					});
					renderDirtyConfigModal(b);
				}
			});

			// Settings Panel
			settings.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Button b = new Button(MSGS.yesButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							if (modal!=null ) {
								modal.hide();
							}
							if (servicesUi != null) {
								servicesUi.renderForm();
							}
							settings.setIconSpin(true);
							contentPanel.setVisible(true);
							contentPanelHeader.setText(MSGS.settings());
							contentPanelBody.clear();
							contentPanelBody.add(settingsBinder);
							settingsBinder.setSession(currentSession);
							Timer timer = new Timer() {
								@Override
								public void run() {
									settings.setIconSpin(false);
								}
							};
							timer.schedule(2000);

						}
					});
					renderDirtyConfigModal(b);
				}
			});
	}

	public void initServicesTree() {
		// (Re)Fetch Available Services

		gwtComponentService
				.findComponentConfigurations(new AsyncCallback<ArrayList<GwtBSConfigComponent>>() {
					@Override
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
						Growl.growl(MSGS.error() + ": ",
								caught.getLocalizedMessage());
					}

					@Override
					public void onSuccess(ArrayList<GwtBSConfigComponent> result) {
						servicesMenu.clear();
						for (GwtBSConfigComponent pair : result) {
							servicesMenu.add(new ServicesAnchorListItem(pair,
									ui));
						}
					}
				});
	}

	public void render(GwtBSConfigComponent item) {
		// Do everything Content Panel related in ServicesUi
		contentPanelBody.clear();
		servicesUi = new ServicesUi(item, this);
		contentPanel.setVisible(true);
		if (item != null) {
			contentPanelHeader.setText(item.getComponentName());
		}
		contentPanelBody.add(servicesUi);
	}

	// create the prompt for dirty configuration before switching to another tab
	private void renderDirtyConfigModal(Button b) {
		if(servicesUi!=null){
			servicesDirty=servicesUi.isDirty();
		}
		networkDirty = networkBinder.isDirty();
		
		if ((servicesUi!=null && servicesUi.isDirty()) || networkBinder.isDirty()) {
			if(servicesUi!=null){
				servicesUi.setDirty(false);
			}
			networkBinder.setDirty(false);
			modal = new Modal();

			ModalHeader header = new ModalHeader();
			header.setTitle(MSGS.warning());
			modal.add(header);

			ModalBody body = new ModalBody();
			body.add(new Span(MSGS.deviceConfigDirty()));
			modal.add(body);

			ModalFooter footer = new ModalFooter();
			footer.add(b);
			footer.add(new Button(MSGS.noButton(), new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					//reset sevices and networks Dirty flags to their original values
					if(servicesUi!=null){
						servicesUi.setDirty(servicesDirty);
					}
					networkBinder.setDirty(networkDirty);
					modal.hide();
				}
			}));
			modal.add(footer);
			modal.show();
			
		} else {
			b.click();
		}

	}

	public boolean isNetworkDirty(){
		return networkBinder.isDirty();
	}
	
	public void setDirty(boolean b) {
		if(servicesUi!=null){
			servicesUi.setDirty(false);
		}
		networkBinder.setDirty(false);
	}
}