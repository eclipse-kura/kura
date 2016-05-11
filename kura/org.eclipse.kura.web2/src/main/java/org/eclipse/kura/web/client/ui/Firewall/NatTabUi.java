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
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallNatMasquerade;
import org.eclipse.kura.web.shared.model.GwtFirewallNatProtocol;
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
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Tooltip;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
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

public class NatTabUi extends Composite implements Tab {

	private static NatTabUiUiBinder uiBinder = GWT.create(NatTabUiUiBinder.class);

	interface NatTabUiUiBinder extends UiBinder<Widget, NatTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

	private ListDataProvider<GwtFirewallNatEntry> natDataProvider = new ListDataProvider<GwtFirewallNatEntry>();
	final SingleSelectionModel<GwtFirewallNatEntry> selectionModel = new SingleSelectionModel<GwtFirewallNatEntry>();

	private boolean m_dirty;

	private GwtFirewallNatEntry newNatEntry;
	private GwtFirewallNatEntry editNatEntry;

	@UiField
	Button apply, create, edit, delete;
	@UiField
	Alert notification;
	@UiField
	CellTable<GwtFirewallNatEntry> natGrid = new CellTable<GwtFirewallNatEntry>();

	@UiField
	Modal natForm;
	@UiField
	FormGroup groupInput, groupOutput, groupProtocol, groupSource,
	groupDestination, groupEnable;
	@UiField
	FormLabel labelInput, labelOutput, labelProtocol, labelSource,
	labelDestination, labelEnable;
	@UiField
	Tooltip tooltipInput, tooltipOutput, tooltipProtocol, tooltipSource,
	tooltipDestination, tooltipEnable;
	@UiField
	TextBox input, output, source, destination;
	@UiField
	ListBox protocol, enable;
	@UiField
	Button submit, cancel;


	public NatTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initButtons();
		initTable();
		initModal();
	}


	//
	// Public methods
	//
	@Override
	public void refresh() {
		EntryClassUi.showWaitModal();
		natDataProvider.getList().clear();
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
			@Override
			public void onFailure(Throwable ex) {
				EntryClassUi.hideWaitModal();
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtNetworkService.findDeficeFirewallNATs(token, new AsyncCallback<ArrayList<GwtFirewallNatEntry>>() {
					@Override
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(ArrayList<GwtFirewallNatEntry> result) {
						for (GwtFirewallNatEntry pair : result) {
							natDataProvider.getList().add(pair);
						}
						int size = natDataProvider.getList().size();
						natGrid.setVisibleRange(0, size);
						natDataProvider.flush();

						if(natDataProvider.getList().isEmpty()){
							natGrid.setVisible(false);
							notification.setVisible(true);
							notification.setText(MSGS.firewallPortForwardTableNoPorts());
						} else {
							natGrid.setVisible(true);
							notification.setVisible(false);
						}

						apply.setEnabled(false);
						EntryClassUi.hideWaitModal();
					}
				});
			}
		});
	}

	@Override
	public boolean isDirty() {
		return m_dirty;
	}

	@Override
	public void setDirty(boolean b) {
		m_dirty = b;
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
	

	//
	// Private methods
	//
	private void initTable() {

		TextColumn<GwtFirewallNatEntry> col1 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				if (object.getInInterface() != null) {
					return String.valueOf(object.getInInterface());
				} else {
					return "";
				}
			}
		};
		col1.setCellStyleNames("status-table-row");
		natGrid.addColumn(col1, MSGS.firewallNatInInterface());

		TextColumn<GwtFirewallNatEntry> col2 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				if (object.getOutInterface() != null) {
					return String.valueOf(object.getOutInterface());
				} else {
					return "";
				}
			}
		};
		col2.setCellStyleNames("status-table-row");
		natGrid.addColumn(col2, MSGS.firewallNatOutInterface());

		TextColumn<GwtFirewallNatEntry> col3 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				if (object.getProtocol() != null) {
					return String.valueOf(object.getProtocol());
				} else {
					return "";
				}
			}
		};
		col3.setCellStyleNames("status-table-row");
		natGrid.addColumn(col3, MSGS.firewallNatProtocol());

		TextColumn<GwtFirewallNatEntry> col4 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				if (object.getSourceNetwork() != null) {
					return String.valueOf(object.getSourceNetwork());
				} else {
					return "";
				}
			}
		};
		col4.setCellStyleNames("status-table-row");
		natGrid.addColumn(col4, MSGS.firewallNatSourceNetwork());

		TextColumn<GwtFirewallNatEntry> col5 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				if (object.getDestinationNetwork() != null) {
					return String.valueOf(object.getDestinationNetwork());
				} else {
					return "";
				}
			}
		};
		col5.setCellStyleNames("status-table-row");
		natGrid.addColumn(col5, MSGS.firewallNatDestinationNetwork());

		TextColumn<GwtFirewallNatEntry> col6 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				if (object.getMasquerade() != null) {
					return String.valueOf(object.getMasquerade());
				} else {
					return "";
				}
			}
		};
		col6.setCellStyleNames("status-table-row");
		natGrid.addColumn(col6, MSGS.firewallNatMasquerade());

		natDataProvider.addDataDisplay(natGrid);
		natGrid.setSelectionModel(selectionModel);
	}

	//Initialize tab buttons
	private void initButtons() {
		initApplyButton();

		initCreateButton();

		initEditButton();

		initDeleteButton();
	}

	private void initDeleteButton() {
		delete.setText(MSGS.deleteButton());
		delete.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final GwtFirewallNatEntry selection = selectionModel.getSelectedObject();
				if (selection != null) { //TODO: this part should be structured the same as the other firewall tabs
					final Modal confirm = new Modal();
					ModalBody confirmBody = new ModalBody();
					ModalFooter confirmFooter = new ModalFooter();

					confirm.setTitle(MSGS.confirm());
					confirmBody.add(new Span((MSGS.firewallNatDeleteConfirmation(selection.getInInterface()))));
					confirmFooter.add(new Button(MSGS.yesButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							natDataProvider.getList().remove(selection);
							natDataProvider.flush();
							apply.setEnabled(true);
							confirm.hide();
							setDirty(true);
						}
					}));

					confirmFooter.add(new Button(MSGS.noButton(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							confirm.hide();
						}
					}));

					confirm.add(confirmBody);
					confirm.add(confirmFooter);
					confirm.show();
				}
			}
		});
	}

	private void initEditButton() {
		edit.setText(MSGS.editButton());
		edit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				GwtFirewallNatEntry selection = selectionModel.getSelectedObject();
				if (selection != null) {
					showModal(selection);
				}
			}
		});
		natForm.addHideHandler(new ModalHideHandler() {
			@Override
			public void onHide(ModalHideEvent evt) {
				if (editNatEntry != null) { 
					GwtFirewallNatEntry oldEntry= selectionModel.getSelectedObject();
					natDataProvider.getList().remove(oldEntry);
					if (!duplicateEntry(editNatEntry)) {
						natDataProvider.getList().add(editNatEntry);
						natDataProvider.flush();
						apply.setEnabled(true);
						editNatEntry= null;
					} else {	//end duplicate
						natDataProvider.getList().add(oldEntry);
						natDataProvider.flush();
					}
				}
			}
		});
	}

	private void initCreateButton() {
		create.setText(MSGS.newButton());
		create.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				showModal(null);
			}
		});
		natForm.addHideHandler(new ModalHideHandler() {
			@Override
			public void onHide(ModalHideEvent evt) {
				if (newNatEntry != null && !duplicateEntry(newNatEntry)) {
					natDataProvider.getList().add(newNatEntry);
					int size = natDataProvider.getList().size();
					natGrid.setVisibleRange(0, size);
					natDataProvider.flush();
					apply.setEnabled(true);
					natGrid.redraw();
					newNatEntry= null;
				}
			}
		});
	}

	private void initApplyButton() {
		apply.setText(MSGS.firewallApply());
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				List<GwtFirewallNatEntry> intermediateList = natDataProvider.getList();
				ArrayList<GwtFirewallNatEntry> tempList = new ArrayList<GwtFirewallNatEntry>();
				final List<GwtFirewallNatEntry> updatedNatConf = tempList;
				for (GwtFirewallNatEntry entry: intermediateList) {
					tempList.add(entry);
				}

				if (updatedNatConf != null) {
					EntryClassUi.showWaitModal();
					gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
						@Override
						public void onFailure(Throwable ex) {
							EntryClassUi.hideWaitModal();
							FailureHandler.handle(ex);
						}

						@Override
						public void onSuccess(GwtXSRFToken token) {
							gwtNetworkService.updateDeviceFirewallNATs(token, updatedNatConf, new AsyncCallback<Void>() {
								@Override
								public void onFailure(Throwable caught) {
									EntryClassUi.hideWaitModal();
									FailureHandler.handle(caught);
								}

								@Override
								public void onSuccess(Void result) {
									setDirty(false);
									apply.setEnabled(false);
									EntryClassUi.hideWaitModal();
								}
							});
						}
					});
				}
			}
		});
	}

	private void initModal() {
		// Handle Buttons
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				natForm.hide();
			}
		});

		submit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				checkFieldsValues();

				if (    groupInput.getValidationState() == ValidationState.ERROR  || 
						groupOutput.getValidationState() == ValidationState.ERROR || 
						groupSource.getValidationState() == ValidationState.ERROR || 
						groupDestination.getValidationState() == ValidationState.ERROR) {
					return;
				}
				// Fetch form data
				GwtFirewallNatEntry natEntry = new GwtFirewallNatEntry();
				natEntry.setInInterface(input.getText());
				natEntry.setOutInterface(output.getText());
				natEntry.setProtocol(protocol.getSelectedItemText());
				natEntry.setSourceNetwork(source.getText());
				natEntry.setDestinationNetwork(destination.getText());
				natEntry.setMasquerade(enable.getSelectedItemText());

				if (submit.getId().equals("new")) {
					newNatEntry= natEntry;
					editNatEntry= null;
				} else if (submit.getId().equals("edit")) {
					editNatEntry= natEntry;
					newNatEntry= null;
				}
				natForm.hide();

				setDirty(true);
			}

		});
	}

	private void showModal(final GwtFirewallNatEntry existingEntry) {
		if (existingEntry == null) {
			natForm.setTitle(MSGS.firewallNatFormInformation());
		} else {
			natForm.setTitle(MSGS.firewallNatFormUpdate(existingEntry.getOutInterface()));
		}

		setModalFieldsLabels();

		setModalFieldsValues(existingEntry);

		setModalFieldsTooltips();

		setModalFieldsHandlers();

		if (existingEntry == null) {
			submit.setId("new");
		} else {
			submit.setId("edit");
		}

		natForm.show();
	}

	private void setModalFieldsHandlers() {
		// Set up validation
		input.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!input.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex()) || 
						input.getText().trim().isEmpty()) {
					groupInput.setValidationState(ValidationState.ERROR);
				} else {
					groupInput.setValidationState(ValidationState.NONE);
				}
			}
		});
		output.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!output.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
						|| output.getText().trim().isEmpty()) {
					groupOutput.setValidationState(ValidationState.ERROR);
				} else {
					groupOutput.setValidationState(ValidationState.NONE);
				}
			}
		});
		source.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!source.getText().trim().isEmpty() && !source.getText().trim().matches(FieldType.NETWORK.getRegex())) {
					groupSource.setValidationState(ValidationState.ERROR);
				} else {
					groupSource.setValidationState(ValidationState.NONE);
				}
			}
		});
		destination.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!destination.getText().trim().isEmpty() && !destination.getText().trim().matches(FieldType.NETWORK.getRegex())) {
					groupDestination.setValidationState(ValidationState.ERROR);
				} else {
					groupDestination.setValidationState(ValidationState.NONE);
				}
			}
		});
	}

	private void setModalFieldsValues(final GwtFirewallNatEntry existingEntry) {
		// populate existing values
		if (existingEntry != null) {
			input.setText(existingEntry.getInInterface());
			output.setText(existingEntry.getOutInterface());
			source.setText(existingEntry.getSourceNetwork());
			destination.setText(existingEntry.getDestinationNetwork());
			for (int i = 0; i < protocol.getItemCount(); i++) {
				if (existingEntry.getProtocol().equals(protocol.getItemText(i))) {
					protocol.setSelectedIndex(i);
					break;
				}
			}

			for (int i = 0; i < enable.getItemCount(); i++) {
				if (existingEntry.getMasquerade().equals(enable.getItemText(i))) {
					enable.setSelectedIndex(i);
					break;
				}
			}
		} else {
			input.setText("");
			output.setText("");
			source.setText("");
			destination.setText("");

			protocol.setSelectedIndex(0);
			enable.setSelectedIndex(0);
		}
	}

	private void setModalFieldsTooltips() {
		// set Tooltips
		tooltipInput.setTitle(MSGS.firewallNatFormInputInterfaceToolTip());
		tooltipOutput.setTitle(MSGS.firewallNatFormOutputInterfaceToolTip());
		tooltipProtocol.setTitle(MSGS.firewallNatFormProtocolToolTip());
		tooltipSource.setTitle(MSGS.firewallNatFormSourceNetworkToolTip());
		tooltipDestination.setTitle(MSGS.firewallNatFormDestinationNetworkToolTip());
		tooltipEnable.setTitle(MSGS.firewallNatFormMasqueradingToolTip());
		tooltipInput.reconfigure();
		tooltipOutput.reconfigure();
		tooltipProtocol.reconfigure();
		tooltipSource.reconfigure();
		tooltipDestination.reconfigure();
		tooltipEnable.reconfigure();
	}

	private void setModalFieldsLabels() {
		// set Labels
		labelInput.setText(MSGS.firewallNatFormInInterfaceName() + "*");
		labelOutput.setText(MSGS.firewallNatFormOutInterfaceName() + "*");
		labelProtocol.setText(MSGS.firewallNatFormProtocol());
		labelSource.setText(MSGS.firewallNatFormSourceNetwork());
		labelDestination.setText(MSGS.firewallNatFormDestinationNetwork());
		labelEnable.setText(MSGS.firewallNatFormMasquerade());
		submit.setText(MSGS.submitButton());
		cancel.setText(MSGS.cancelButton());

		// set ListBox
		protocol.clear();
		for (GwtFirewallNatProtocol prot : GwtFirewallNatProtocol.values()) {
			protocol.addItem(prot.name());
		}
		enable.clear();
		for (GwtFirewallNatMasquerade masquerade : GwtFirewallNatMasquerade.values()) {
			enable.addItem(masquerade.name());
		}
	}

	private boolean duplicateEntry(GwtFirewallNatEntry firewallNatEntry) {
		boolean isDuplicateEntry = false;
		List<GwtFirewallNatEntry> entries = natDataProvider.getList();
		if (entries != null && firewallNatEntry != null) {
			for (GwtFirewallNatEntry entry : entries) {
				String sourceNetwork = (entry.getSourceNetwork() != null) ? entry.getSourceNetwork() : "0.0.0.0/0";
				String destinationNetwork = (entry.getDestinationNetwork() != null) ? entry.getDestinationNetwork() : "0.0.0.0/0";
				String newSourceNetwork = (firewallNatEntry.getSourceNetwork() != null) ? firewallNatEntry.getSourceNetwork() : "0.0.0.0/0";
				String newDestinationNetwork = (firewallNatEntry.getDestinationNetwork() != null) ? firewallNatEntry.getDestinationNetwork() : "0.0.0.0/0";

				if (    entry.getInInterface().equals(firewallNatEntry.getInInterface()) && 
						entry.getOutInterface().equals(firewallNatEntry.getOutInterface()) && 
						entry.getProtocol().equals(firewallNatEntry.getProtocol()) && 
						sourceNetwork.equals(newSourceNetwork) && 
						destinationNetwork.equals(newDestinationNetwork)) {
					isDuplicateEntry = true;
					break;
				}
			}
		}

		return isDuplicateEntry;
	}

	private void checkFieldsValues() {
		// make the required fields in error state by default
		if (input.getText() == null || "".equals(input.getText().trim())) {
			groupInput.setValidationState(ValidationState.ERROR);
		}
		if (output.getText() == null || "".equals(output.getText().trim())) {
			groupOutput.setValidationState(ValidationState.ERROR);
		}
	}
}
