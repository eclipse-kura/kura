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
import org.eclipse.kura.web.client.ui.wires.WiresPanelUi;
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

	private final StatusPanelUi statusBinder     = GWT.create(StatusPanelUi.class);
	private final DevicePanelUi deviceBinder     = GWT.create(DevicePanelUi.class);
	private final PackagesPanelUi packagesBinder = GWT.create(PackagesPanelUi.class);
	private final SettingsPanelUi settingsBinder = GWT.create(SettingsPanelUi.class);
	private final FirewallPanelUi firewallBinder = GWT.create(FirewallPanelUi.class);
	private final NetworkPanelUi networkBinder   = GWT.create(NetworkPanelUi.class);
	private final WiresPanelUi   wiresBinder     = GWT.create(WiresPanelUi.class);

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
	AnchorListItem device, network, firewall, packages, settings, wires;
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
		ui = this;
		initWidget(uiBinder.createAndBindUi(this));

		// TODO : standardize the URL?
		//		header.setUrl("eclipse/kura/icons/kura_logo_small.png");
		header.setStyleName("headerLogo");
		Date now = new Date();
		@SuppressWarnings("deprecation")
		int year = now.getYear() + 1900;
		footerLeft.setText(MSGS.copyright(String.valueOf(year)));
		footerLeft.setStyleName("copyright");
		contentPanel.setVisible(false);

		// Set client side logging
		errorLogger.addHandler(new HasWidgetsLogHandler(errorLogArea));
		errorPopup.addHideHandler(new ModalHideHandler() {
			@Override
			public void onHide(ModalHideEvent evt) {
				errorLogArea.clear();
			}
		});
		
		//
		dragDropInit(this);
		
		FailureHandler.setPopup(errorPopup);
	}

	public void setSession(GwtSession GwtSession) {
		currentSession = GwtSession;
	}

	public void setFooter(GwtSession GwtSession) {

		footerRight.setText(GwtSession.getKuraVersion());

		if (GwtSession.isDevelopMode()) {
			footerCenter.setText(MSGS.developmentMode());
		}

	}

	public void initSystemPanel(GwtSession GwtSession, boolean connectionStatus) {
		final EntryClassUi m_instanceReference= this;
		if (!GwtSession.isNetAdminAvailable()) {
			network.setVisible(false);
			firewall.setVisible(false);
		}

		// Status Panel
		updateConnectionStatusImage(connectionStatus);
		status.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Button b = new Button(MSGS.yesButton(), new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						if (modal != null) {
							modal.hide();
						}
						if (servicesUi != null) {
							servicesUi.renderForm();
						}
						contentPanel.setVisible(true);
						contentPanelHeader.setText("Status");
						contentPanelBody.clear();
						contentPanelBody.add(statusBinder);
						statusBinder.setSession(currentSession);
						statusBinder.setParent(m_instanceReference);
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
						forceTabsCleaning();
						if (modal != null ) {
							modal.hide();
						}
						if (servicesUi != null) {
							servicesUi.renderForm();
						}
						contentPanel.setVisible(true);
						contentPanelHeader.setText(MSGS.device());
						contentPanelBody.clear();
						contentPanelBody.add(deviceBinder);
						deviceBinder.setSession(currentSession);
						deviceBinder.initDevicePanel();
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
					Button b = new Button(MSGS.yesButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							forceTabsCleaning();
							if (modal != null ) {
								modal.hide();
							}
							if (servicesUi != null) {
								servicesUi.renderForm();
							}
							contentPanel.setVisible(true);
							contentPanelHeader.setText(MSGS.network());
							contentPanelBody.clear();
							contentPanelBody.add(networkBinder);
							networkBinder.setSession(currentSession);
							networkBinder.initNetworkPanel();
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
					Button b = new Button(MSGS.yesButton(),	new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							forceTabsCleaning();
							if (modal != null ) {
								modal.hide();
							}
							if (servicesUi != null) {
								servicesUi.renderForm();
							}
							contentPanel.setVisible(true);
							contentPanelHeader.setText(MSGS.firewall());
							contentPanelBody.clear();
							contentPanelBody.add(firewallBinder);
							firewallBinder.initFirewallPanel();
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
						forceTabsCleaning();
						if (modal != null ) {
							modal.hide();
						}
						if (servicesUi != null) {
							servicesUi.renderForm();
						}
						contentPanel.setVisible(true);
						contentPanelHeader.setText(MSGS.packages());
						contentPanelBody.clear();
						contentPanelBody.add(packagesBinder);
						packagesBinder.setSession(currentSession);
						packagesBinder.setMainUi(ui);
						packagesBinder.refresh();
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
						forceTabsCleaning();
						if (modal != null ) {
							modal.hide();
						}
						if (servicesUi != null) {
							servicesUi.renderForm();
						}
						contentPanel.setVisible(true);
						contentPanelHeader.setText(MSGS.settings());
						contentPanelBody.clear();
						contentPanelBody.add(settingsBinder);
						settingsBinder.setSession(currentSession);
						settingsBinder.load();
					}
				});
				renderDirtyConfigModal(b);
			}
		});

		// Wires Panel
		wires.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Button b = new Button(MSGS.yesButton(), new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						forceTabsCleaning();
						if (modal != null ) {
							modal.hide();
						}
						if (servicesUi != null) {
							servicesUi.renderForm();
						}
						contentPanel.setVisible(true);
						contentPanelHeader.setText("Kura Wires");
						contentPanelBody.clear();
						contentPanelBody.add(wiresBinder);
						wiresBinder.load();

					}
				});
				renderDirtyConfigModal(b);
			}
		});
	}

	public void initServicesTree() {
		// (Re)Fetch Available Services
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex, EntryClassUi.class.getName());
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtComponentService.findComponentConfigurations(token, new AsyncCallback<List<GwtConfigComponent>>() {
					@Override
					public void onFailure(Throwable ex) {
						logger.log(Level.SEVERE, ex.getMessage(), ex);
						FailureHandler.handle(ex, EntryClassUi.class.getName());
					}

					@Override
					public void onSuccess(List<GwtConfigComponent> result) {
						servicesMenu.clear();
						for (GwtConfigComponent pair : result) {
							servicesMenu.add(new ServicesAnchorListItem(pair, ui));
						}
					}
				});
			}
		});

	}

	public void render(GwtConfigComponent item) {
		// Do everything Content Panel related in ServicesUi
		contentPanelBody.clear();
		servicesUi = new ServicesUi(item, this);
		contentPanel.setVisible(true);
		if (item != null) {
			contentPanelHeader.setText(item.getComponentName());
		}
		contentPanelBody.add(servicesUi);
	}

	public void updateConnectionStatusImage(boolean isConnected) {

		Image img;
		String statusMessage;


		if (isConnected) {
			img= new Image(Resources.INSTANCE.greenPlug32().getSafeUri());
			statusMessage= MSGS.connectionStatusConnected();
		} else {
			img= new Image(Resources.INSTANCE.redPlug32().getSafeUri());
			statusMessage= MSGS.connectionStatusDisconnected();
		}

		StringBuilder imageSB= new StringBuilder();
		imageSB.append("<image src=\"");
		imageSB.append(img.getUrl());
		imageSB.append("\" ");
		imageSB.append("width=\"23\" height=\"23\" style=\"vertical-align: middle; float: right;\" title=\"");
		imageSB.append(statusMessage);
		imageSB.append("\"/>");

		String baseStatusHTML= status.getHTML().split("<im")[0];
		StringBuilder statusHTML= new StringBuilder(baseStatusHTML);
		statusHTML.append(imageSB.toString());
		status.setHTML(statusHTML.toString());
	}


	// create the prompt for dirty configuration before switching to another tab
	private void renderDirtyConfigModal(Button b) {
		if (servicesUi != null) {
			servicesDirty= servicesUi.isDirty();
		}

		if (network.isVisible()) {
			networkDirty= networkBinder.isDirty();
		} else {
			networkDirty= false;
		}

		if (firewall.isVisible()) {
			firewallDirty= firewallBinder.isDirty();
		} else {
			firewallDirty= false;
		}

		if (settings.isVisible()) {
			settingsDirty= settingsBinder.isDirty(); 
		}

		if (	(servicesUi!=null && servicesUi.isDirty()) || 
				networkDirty  || 
				firewallDirty || 
				settingsDirty ) {
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
		if (network.isVisible()) {
			return networkBinder.isDirty();
		} else {
			return false;
		}
	}

	public boolean isFirewallDirty(){
		if (firewall.isVisible()) {
			return firewallBinder.isDirty();
		} else {
			return false;
		}
	}

	public boolean isSettingsDirty(){
		if (settings.isVisible()) {
			return settingsBinder.isDirty();
		} else {
			return false;
		}
	}

	public void setDirty(boolean b) {
		if (servicesUi != null){
			servicesUi.setDirty(false);
		}
		if (network.isVisible()) {
			networkBinder.setDirty(false);
		}
		if (firewall.isVisible()) {
			firewallBinder.setDirty(false);
		}
		if (settings.isVisible()) {
			settingsBinder.setDirty(false);
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
		if (servicesUi != null){
			servicesUi.setDirty(false);
		}
		if (network.isVisible()) {
			networkBinder.setDirty(false);
		}
		if (firewall.isVisible()) {
			firewallBinder.setDirty(false);
		}
		if (settings.isVisible()) {
			settingsBinder.setDirty(false);
		}
	}
	
	private void eclipseMarketplaceInstall(String url) {
		
		// Construct the REST URL for Eclipse Marketplace
		String appId = url.split("=")[1];
		final String empApi = "http://marketplace.eclipse.org/node/" + appId + "/api/p";
		
		// Generate security token
		EntryClassUi.showWaitModal();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex, EntryClassUi.class.getName());
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				// Retrieve the URL of the DP via the Eclipse Marketplace API
				gwtPackageService.getMarketplaceUri(token, empApi, new AsyncCallback<String>() {

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
		String url = "/" + GWT.getModuleName()	+ "/file/deploy/url";
		final RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
		
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

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
