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
package org.eclipse.kura.web.client.ui.Firewall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtNetProtocol;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.shared.event.ModalHideEvent;
import org.gwtbootstrap3.client.shared.event.ModalHideHandler;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Tooltip;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class OpenPortsTabUi extends Composite {

	private static OpenPortsTabUiUiBinder uiBinder = GWT.create(OpenPortsTabUiUiBinder.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

	private static final Messages MSGS = GWT.create(Messages.class);

	interface OpenPortsTabUiUiBinder extends UiBinder<Widget, OpenPortsTabUi> {
	}
	
	private ListDataProvider<GwtFirewallOpenPortEntry> openPortsDataProvider = new ListDataProvider<GwtFirewallOpenPortEntry>();
	final SingleSelectionModel<GwtFirewallOpenPortEntry> selectionModel = new SingleSelectionModel<GwtFirewallOpenPortEntry>();

	private boolean m_dirty;

	GwtFirewallOpenPortEntry editOpenPortEntry, newOpenPortEntry, openPortEntry;

	@UiField
	Button apply, create, edit, delete;
	@UiField
	Alert notification;

	@UiField
	Modal openPortsForm, alert;
	@UiField
	FormGroup groupPort, groupPermittedNw,groupPermittedI,groupUnpermittedI,groupPermittedMac,groupSource;
	@UiField
	FormLabel labelPort, labelProtocol, labelPermitttedNw, labelPermitttedI,
	labelUnPermitttedI, labelPermitttedMac, labelsource;
	@UiField
	TextBox port, permittedNw, permittedI, unpermittedI, permittedMac, source;
	@UiField
	Tooltip tooltipPermittedI, tooltipUnpermittedI;
	@UiField
	Button submit, cancel, yes, no;
	@UiField
	ListBox protocol;

	@UiField
	Span alertBody;

	@UiField
	CellTable<GwtFirewallOpenPortEntry> openPortsGrid = new CellTable<GwtFirewallOpenPortEntry>();

	
	public OpenPortsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		apply.setText(MSGS.firewallApply());
		create.setText(MSGS.newButton());
		edit.setText(MSGS.editButton());
		delete.setText(MSGS.deleteButton());
		openPortsGrid.setSelectionModel(selectionModel);

		initButtons();
		initTable();	
	}

	//
	// Public methods
	//
	public void loadData() {
		EntryClassUi.showWaitModal();
		openPortsDataProvider.getList().clear();
		notification.setVisible(false);
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtNetworkService.findDeviceFirewallOpenPorts(token, new AsyncCallback<ArrayList<GwtFirewallOpenPortEntry>>() {
					@Override
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught, gwtNetworkService.getClass().getSimpleName());
					}

					@Override
					public void onSuccess(ArrayList<GwtFirewallOpenPortEntry> result) {
						for (GwtFirewallOpenPortEntry pair : result) {
							openPortsDataProvider.getList().add(pair);
						}
						int size = openPortsDataProvider.getList().size();
						openPortsGrid.setVisibleRange(0, size);
						openPortsDataProvider.flush();
						setVisibility();
						apply.setEnabled(false);
						EntryClassUi.hideWaitModal();
					}
				});
			}

		});
	}

	public GwtFirewallOpenPortEntry getNewOpenPortEntry() {
		return newOpenPortEntry;
	}

	public GwtFirewallOpenPortEntry getEditOpenPortEntry() {
		return editOpenPortEntry;
	}
	
	public boolean isDirty() {
		return m_dirty;
	}
	
	public void setDirty(boolean b) {
		m_dirty = b;
	}


	//
	//Private methods
	//
	private void initTable() {

		TextColumn<GwtFirewallOpenPortEntry> col1 = new TextColumn<GwtFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtFirewallOpenPortEntry object) {
				if (object.getPortRange() != null) {
					return String.valueOf(object.getPortRange());
				} else {
					return "";
				}
			}
		};
		col1.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col1, MSGS.firewallOpenPort());

		TextColumn<GwtFirewallOpenPortEntry> col2 = new TextColumn<GwtFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtFirewallOpenPortEntry object) {
				if (object.getProtocol() != null) {
					return String.valueOf(object.getProtocol());
				} else {
					return "";
				}
			}
		};
		col2.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col2, MSGS.firewallOpenPortProtocol());

		TextColumn<GwtFirewallOpenPortEntry> col3 = new TextColumn<GwtFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtFirewallOpenPortEntry object) {
				if (object.getPermittedNetwork() != null) {
					return String.valueOf(object.getPermittedNetwork());
				} else {
					return "";
				}
			}
		};
		col3.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col3, MSGS.firewallOpenPortPermittedNetwork());

		TextColumn<GwtFirewallOpenPortEntry> col4 = new TextColumn<GwtFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtFirewallOpenPortEntry object) {
				if (object.getPermittedInterfaceName() != null) {
					return String.valueOf(object.getPermittedInterfaceName());
				} else {
					return "";
				}
			}
		};
		col4.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col4, MSGS.firewallOpenPortPermittedInterfaceName());

		TextColumn<GwtFirewallOpenPortEntry> col5 = new TextColumn<GwtFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtFirewallOpenPortEntry object) {
				if (object.getUnpermittedInterfaceName() != null) {
					return String.valueOf(object.getUnpermittedInterfaceName());
				} else {
					return "";
				}
			}
		};
		col5.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col5, MSGS.firewallOpenPortUnpermittedInterfaceName());

		TextColumn<GwtFirewallOpenPortEntry> col6 = new TextColumn<GwtFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtFirewallOpenPortEntry object) {
				if (object.getPermittedMAC() != null) {
					return String.valueOf(object.getPermittedMAC());
				} else {
					return "";
				}
			}
		};
		col6.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col6, MSGS.firewallOpenPortPermittedMac());

		TextColumn<GwtFirewallOpenPortEntry> col7 = new TextColumn<GwtFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtFirewallOpenPortEntry object) {
				if (object.getSourcePortRange() != null) {
					return String.valueOf(object.getSourcePortRange());
				} else {
					return "";
				}
			}
		};
		col7.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col7, MSGS.firewallOpenPortSourcePortRange());

		openPortsDataProvider.addDataDisplay(openPortsGrid);
	}

	//Initialize tab buttons
	private void initButtons() {
		initApplyButton();

		initCreateButton();

		initEditButton();

		initDeleteButton();
	}

	private void initApplyButton() {
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				List<GwtFirewallOpenPortEntry> intermediateList = openPortsDataProvider.getList();
				ArrayList<GwtFirewallOpenPortEntry> tempList = new ArrayList<GwtFirewallOpenPortEntry>();
				final List<GwtFirewallOpenPortEntry> updatedOpenPortConf = tempList;
				for (GwtFirewallOpenPortEntry entry: intermediateList) {
					tempList.add(entry);
				}


				if (updatedOpenPortConf != null) {
					EntryClassUi.showWaitModal();
					gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

						@Override
						public void onFailure(Throwable ex) {
							EntryClassUi.hideWaitModal();
							FailureHandler.handle(ex, gwtXSRFService.getClass().getName());
						}

						@Override
						public void onSuccess(GwtXSRFToken token) {
							gwtNetworkService.updateDeviceFirewallOpenPorts(token, updatedOpenPortConf, new AsyncCallback<Void>() {
								@Override
								public void onFailure(Throwable caught) {
									EntryClassUi.hideWaitModal();
									FailureHandler.handle(caught, gwtNetworkService.getClass().getSimpleName());
								}

								@Override
								public void onSuccess(Void result) {
									apply.setEnabled(false);
									EntryClassUi.hideWaitModal();
									setDirty(false);
								}
							});
						}
					});
				}
			}
		});
	}

	private void initCreateButton() {
		create.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				initModal(null);
				// TODO add warnings for port 80 and 22
				openPortsForm.addHideHandler(new ModalHideHandler() {
					@Override
					public void onHide(ModalHideEvent evt) {

						if (getNewOpenPortEntry()!= null) {
							GwtFirewallOpenPortEntry newEntry = getNewOpenPortEntry();
							if (!duplicateEntry(newEntry)) {
								openPortsDataProvider.getList().add(newEntry);
								int size = openPortsDataProvider.getList().size();
								openPortsGrid.setVisibleRange(0, size);
								openPortsDataProvider.flush();
								apply.setEnabled(true);
								setVisibility();
								openPortsGrid.redraw();
							} else {
								//Growl.growl(MSGS.firewallOpenPortFormError()
								//		+ ": ",
								//		MSGS.firewallOpenPortFormDuplicate());
							}
						}
					}
				});

			}
		});
	}

	private void initEditButton() {
		edit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final GwtFirewallOpenPortEntry selection = selectionModel.getSelectedObject();
				if (selection != null) {
					if (selection.getPortRange().equals("22")) {
						// show warning
						alertBody.setText(MSGS.firewallOpenPorts22());
						yes.setText(MSGS.yesButton());
						no.setText(MSGS.noButton());
						no.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								alert.hide();
							}
						});
						yes.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								initModal(selection);
								alert.hide();
							}
						});
						alert.show();

					} else if (selection.getPortRange().equals("80")) {
						// show warning
						alertBody.setText(MSGS.firewallOpenPorts80());
						yes.setText(MSGS.yesButton());
						no.setText(MSGS.noButton());
						no.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								alert.hide();
							}
						});
						yes.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								initModal(selection);
								alert.hide();
							}
						});
						alert.show();

					} else {
						initModal(selection);
					}

					openPortsForm.addHideHandler(new ModalHideHandler() {
						@Override
						public void onHide(ModalHideEvent evt) {

							if (getEditOpenPortEntry() != null) {
								final GwtFirewallOpenPortEntry editEntry = getEditOpenPortEntry();
								if (!duplicateEntry(getEditOpenPortEntry())) {
									openPortsDataProvider.getList().remove(selection);
									openPortsDataProvider.getList().add(editEntry);
									openPortsDataProvider.flush();
									apply.setEnabled(true);
									setVisibility();
								}	//end duplicate

							}//end !=null
						}//end onHide
					});
				}
			}
		});
	}

	private void initDeleteButton() {
		delete.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final GwtFirewallOpenPortEntry selection = selectionModel.getSelectedObject();
				if (selection != null) {
					alert.setTitle(MSGS.confirm());
					alertBody.setText(MSGS.firewallOpenPortDeleteConfirmation(String.valueOf(selection.getPortRange())));
					yes.setText(MSGS.yesButton());
					no.setText(MSGS.noButton());
					no.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							alert.hide();
						}
					});
					yes.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {							
							alert.hide();
							openPortsDataProvider.getList().remove(selection);
							openPortsDataProvider.flush();
							apply.setEnabled(true);
							setVisibility();
							
							setDirty(true);
						}
					});
					alert.show();
				}
			}
		});
	}

	private void initModal(final GwtFirewallOpenPortEntry existingEntry) {

		if (existingEntry == null) {
			// new
			openPortsForm.setTitle(MSGS.firewallOpenPortFormInformation());
		} else {
			// edit existing entry
			openPortsForm.setTitle(MSGS.firewallOpenPortFormUpdate(String.valueOf(existingEntry.getPortRange())));
		}

		setModalFieldsLabels();

		setModalFieldsValues(existingEntry);

		setModalFieldsTooltips();

		setModalFieldsHandlers();


		cancel.setText(MSGS.cancelButton());
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				openPortsForm.hide();
				openPortEntry = null;
				editOpenPortEntry=null;
				newOpenPortEntry=null;
			}
		});

		submit.setText(MSGS.submitButton());
		submit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				checkFieldsValues();

				if(     groupPort.getValidationState() == ValidationState.ERROR       ||
						groupPermittedNw.getValidationState()==ValidationState.ERROR  ||
						groupPermittedI.getValidationState()==ValidationState.ERROR   ||
						groupUnpermittedI.getValidationState()==ValidationState.ERROR ||
						groupPermittedMac.getValidationState()==ValidationState.ERROR ||
						groupSource.getValidationState()==ValidationState.ERROR){
					return;
				}

				// create a new entry
				openPortEntry = new GwtFirewallOpenPortEntry();
				openPortEntry.setPortRange(port.getText());
				openPortEntry.setProtocol(protocol.getSelectedItemText());
				if (permittedNw.getText() != null && 
						!"".equals(permittedNw.getText().trim())) {
					openPortEntry.setPermittedNetwork(permittedNw.getText());
				} else {
					openPortEntry.setPermittedNetwork("0.0.0.0/0");
				}
				if (permittedI.getText() != null && 
						!"".equals(permittedI.getText().trim())) {
					openPortEntry.setPermittedInterfaceName(permittedI.getText());
				}
				if (unpermittedI.getText() != null && 
						!"".equals(unpermittedI.getText().trim())) {
					openPortEntry.setUnpermittedInterfaceName(unpermittedI.getText());
				}
				if (permittedMac.getText() != null && 
						!"".equals(permittedMac.getText().trim())) {
					openPortEntry.setPermittedMAC(permittedMac.getText());
				}
				if (source.getText() != null && 
						!"".equals(source.getText().trim())) {
					openPortEntry.setSourcePortRange(source.getText());
				}
				
				editOpenPortEntry = null;
				newOpenPortEntry = null;

				if (existingEntry == null) {
					newOpenPortEntry = openPortEntry;
				} else {
					editOpenPortEntry = openPortEntry;
				}
				
				setDirty(true);
				
				openPortsForm.hide();
			}
		});

		openPortsForm.show();
	}

	private void setModalFieldsHandlers() {
		permittedI.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (permittedI.getText() != null && !"".equals(permittedI.getText())) {
					unpermittedI.clear();
					unpermittedI.setEnabled(false);
				} else {
					unpermittedI.setEnabled(true);
				}
			}
		});

		unpermittedI.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (unpermittedI.getText() != null && !"".equals(unpermittedI.getText())) {
					permittedI.clear();
					permittedI.setEnabled(false);
				} else {
					permittedI.setEnabled(true);
				}
			}
		});

		//set up validation
		//groupPort, groupPermittedNw,groupPermittedI,grourUnpermittedI,groupPermittedMac,groupSource;
		port.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if((!port.getText().trim().matches(FieldType.NUMERIC.getRegex()) && port.getText().trim().length() != 0) || 
						(port.getText()==null || "".equals(port.getText().trim()))){
					groupPort.setValidationState(ValidationState.ERROR);
				}else{					
					groupPort.setValidationState(ValidationState.NONE);
				}
			}});

		permittedNw.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!permittedNw.getText().trim().matches(FieldType.NETWORK.getRegex()) && 
						permittedNw.getText().trim().length() > 0){					
					groupPermittedNw.setValidationState(ValidationState.ERROR);
				}else{				
					groupPermittedNw.setValidationState(ValidationState.NONE);
				}
			}});
		permittedI.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!permittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex()) && 
						permittedI.getText().trim().length() > 0){
					groupPermittedI.setValidationState(ValidationState.ERROR);
				}else{					
					groupPermittedI.setValidationState(ValidationState.NONE);
				}
			}});
		unpermittedI.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!unpermittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex()) && 
						unpermittedI.getText().trim().length() > 0){
					groupUnpermittedI.setValidationState(ValidationState.ERROR);
				}else{					
					groupPermittedI.setValidationState(ValidationState.NONE);
				}
			}});
		permittedMac.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!permittedMac.getText().trim().matches(FieldType.MAC_ADDRESS.getRegex()) && 
						permittedMac.getText().trim().length() > 0){
					groupPermittedMac.setValidationState(ValidationState.ERROR);
				}else{					
					groupPermittedMac.setValidationState(ValidationState.NONE);
				}
			}});
		source.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!source.getText().trim().matches(FieldType.PORT_RANGE.getRegex()) && 
						source.getText().trim().length() > 0){
					groupSource.setValidationState(ValidationState.ERROR);
				}else{					
					groupSource.setValidationState(ValidationState.NONE);
				}
			}});
	}

	private void setModalFieldsTooltips() {
		// Permitted Interface config
		tooltipPermittedI.setTitle(MSGS.firewallOpenPortFormPermittedInterfaceToolTip());
		tooltipPermittedI.reconfigure();

		// UnPermitted Interface config
		tooltipUnpermittedI.setTitle(MSGS.firewallOpenPortFormUnpermittedInterfaceToolTip());
		tooltipUnpermittedI.reconfigure();
	}

	private void setModalFieldsValues(final GwtFirewallOpenPortEntry existingEntry) {
		// populate existing values
		if (existingEntry != null) {
			port.setText(String.valueOf(existingEntry.getPortRange()));
			protocol.setSelectedIndex(existingEntry.getProtocol().equals(GwtNetProtocol.tcp.name()) ? 0 : 1);

			permittedNw.setText(existingEntry.getPermittedNetwork());
			permittedI.setText(existingEntry.getPermittedInterfaceName());
			unpermittedI.setText(existingEntry.getUnpermittedInterfaceName());
			permittedMac.setText(existingEntry.getPermittedMAC());
			source.setText(existingEntry.getSourcePortRange());
		} else {
			port.setText("");
			protocol.setSelectedIndex(0);

			permittedNw.setText("");
			permittedI.setText("");
			unpermittedI.setText("");
			permittedMac.setText("");
			source.setText("");
		}
	}

	private void setModalFieldsLabels() {
		// set Labels
		labelPort.setText(MSGS.firewallOpenPortFormPort()+"*");
		labelProtocol.setText(MSGS.firewallOpenPortFormProtocol());
		protocol.clear();
		protocol.addItem(GwtNetProtocol.tcp.name());
		protocol.addItem(GwtNetProtocol.udp.name());
		labelPermitttedNw.setText(MSGS.firewallOpenPortFormPermittedNetwork());
		labelPermitttedI.setText(MSGS.firewallOpenPortFormPermittedInterfaceName());
		labelUnPermitttedI.setText(MSGS.firewallOpenPortFormUnpermittedInterfaceName());
		labelPermitttedMac.setText(MSGS.firewallOpenPortFormPermittedMac());
		labelsource.setText(MSGS.firewallOpenPortFormSourcePortRange());
	}

	private void checkFieldsValues() {
		if (port.getText().trim().isEmpty()) {
			groupPort.setValidationState(ValidationState.ERROR);
		}
	}

	private boolean duplicateEntry(GwtFirewallOpenPortEntry openPortEntry) {
		List<GwtFirewallOpenPortEntry> entries = openPortsDataProvider.getList();
		if (entries != null && openPortEntry != null) {
			for (GwtFirewallOpenPortEntry entry : entries) {
				if (entry.getPortRange().equals(openPortEntry.getPortRange()) && 
						entry.getPermittedNetwork().equals(openPortEntry.getPermittedNetwork())) {
					return true;
				}
			}
		}
		return false;
	}

	private void setVisibility() {
		if (openPortsDataProvider.getList().isEmpty()) {
			openPortsGrid.setVisible(false);
			notification.setVisible(true);
			notification.setText(MSGS.firewallOpenPortTableNoPorts());
		} else {
			openPortsGrid.setVisible(true);
			notification.setVisible(false);
		}
	}
}
