package org.eclipse.kura.web.client.ui.Firewall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallNatMasquerade;
import org.eclipse.kura.web.shared.model.GwtFirewallNatProtocol;
import org.eclipse.kura.web.shared.model.GwtNetProtocol;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
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
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
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

public class NatTabUi extends Composite {

	private static NatTabUiUiBinder uiBinder = GWT.create(NatTabUiUiBinder.class);

	interface NatTabUiUiBinder extends UiBinder<Widget, NatTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	
	GwtFirewallNatEntry natEntry;

	@UiField
	AnchorListItem apply, create, edit, delete;
	@UiField
	Alert notification;
	@UiField
	DataGrid<GwtFirewallNatEntry> natGrid = new DataGrid<GwtFirewallNatEntry>();
	private ListDataProvider<GwtFirewallNatEntry> natDataProvider = new ListDataProvider<GwtFirewallNatEntry>();
	final SingleSelectionModel<GwtFirewallNatEntry> selectionModel = new SingleSelectionModel<GwtFirewallNatEntry>();

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

	String REGEX_ALPHANUMERIC = "^[a-zA-Z0-9_]+$";
	String REGEX_NETWORK = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,3})";

	public NatTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		initButtons();
		initTable();
		loadData();

	}

	private void initTable() {

		TextColumn<GwtFirewallNatEntry> col1 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				return String.valueOf(object.getInInterface());
			}
		};
		col1.setCellStyleNames("status-table-row");
		natGrid.addColumn(col1, MSGS.firewallNatInInterface());

		TextColumn<GwtFirewallNatEntry> col2 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				return String.valueOf(object.getOutInterface());
			}
		};
		col2.setCellStyleNames("status-table-row");
		natGrid.addColumn(col2, MSGS.firewallNatOutInterface());

		TextColumn<GwtFirewallNatEntry> col3 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				return String.valueOf(object.getProtocol());
			}
		};
		col3.setCellStyleNames("status-table-row");
		natGrid.addColumn(col3, MSGS.firewallNatProtocol());

		TextColumn<GwtFirewallNatEntry> col4 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				return String.valueOf(object.getSourceNetwork());
			}
		};
		col4.setCellStyleNames("status-table-row");
		natGrid.addColumn(col4, MSGS.firewallNatSourceNetwork());

		TextColumn<GwtFirewallNatEntry> col5 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				return String.valueOf(object.getDestinationNetwork());
			}
		};
		col5.setCellStyleNames("status-table-row");
		natGrid.addColumn(col5, MSGS.firewallNatDestinationNetwork());

		TextColumn<GwtFirewallNatEntry> col6 = new TextColumn<GwtFirewallNatEntry>() {
			@Override
			public String getValue(GwtFirewallNatEntry object) {
				return String.valueOf(object.getMasquerade());
			}
		};
		col6.setCellStyleNames("status-table-row");
		natGrid.addColumn(col6, MSGS.firewallNatMasquerade());

		natDataProvider.addDataDisplay(natGrid);
		natGrid.setSelectionModel(selectionModel);
	}

	private void loadData() {
		natDataProvider.getList().clear();
		natGrid.setVisible(false);
		notification.setVisible(false);

		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtNetworkService.findDeficeFirewallNATs(token, new AsyncCallback<ArrayList<GwtFirewallNatEntry>>() {
					@Override
					public void onFailure(Throwable caught) {
						notification.setVisible(true);
						notification.setText(MSGS.error() + ": "
								+ caught.getLocalizedMessage());
					}

					@Override
					public void onSuccess(
							ArrayList<GwtFirewallNatEntry> result) {
						for (GwtFirewallNatEntry pair : result) {
							natDataProvider.getList().add(pair);
						}
						natDataProvider.flush();

						if (natDataProvider.getList().size() > 0) {
							natGrid.setVisible(true);
						}
						apply.setEnabled(false);
					}
				});
			}
			
		});
	}

	private void initButtons() {
		apply.setText(MSGS.firewallApply());
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final List<GwtFirewallNatEntry> updatedNatConf = natDataProvider.getList();
				if (updatedNatConf != null) {
					gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

						@Override
						public void onFailure(Throwable ex) {
							FailureHandler.handle(ex);
						}

						@Override
						public void onSuccess(GwtXSRFToken token) {
							gwtNetworkService.updateDeviceFirewallNATs(token, updatedNatConf,
									new AsyncCallback<Void>() {
										@Override
										public void onFailure(Throwable caught) {
											//Growl.growl(MSGS.error() + ": ",
											//		caught.getLocalizedMessage());
										}

										@Override
										public void onSuccess(Void result) {
											apply.setEnabled(false);
										}
									});
						}
						
					});
				}
			}
		});

		create.setText(MSGS.newButton());
		create.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				initModal(null);
			}
		});

		edit.setText(MSGS.editButton());
		edit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				GwtFirewallNatEntry selection = selectionModel
						.getSelectedObject();
				if (selection != null) {
					initModal(selection);
				}
			}
		});

		delete.setText(MSGS.deleteButton());
		delete.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final GwtFirewallNatEntry selection = selectionModel
						.getSelectedObject();
				if (selection != null) {
					final Modal confirm = new Modal();
					ModalBody confirmBody = new ModalBody();
					ModalFooter confirmFooter = new ModalFooter();

					confirm.setTitle(MSGS.confirm());
					confirmBody.add(new Span((MSGS
							.firewallNatDeleteConfirmation(selection
									.getInInterface()))));
					confirmFooter.add(new Button(MSGS.yesButton(),
							new ClickHandler() {
								@Override
								public void onClick(ClickEvent event) {
									natDataProvider.getList().remove(selection);
									natDataProvider.flush();
									apply.setEnabled(true);
									confirm.hide();

									if (natDataProvider.getList().size() > 0) {
										natGrid.setVisible(true);
									} else {
										natGrid.setVisible(false);
									}
								}

							}));

					confirmFooter.add(new Button(MSGS.noButton(),
							new ClickHandler() {
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

	private void initModal(final GwtFirewallNatEntry existingEntry) {

		if (existingEntry == null) {
			natForm.setTitle(MSGS.firewallNatFormInformation());
		} else {
			natForm.setTitle(MSGS.firewallNatFormUpdate(existingEntry
					.getOutInterface()));
		}

		// set Labels
		labelInput.setText(MSGS.firewallNatFormInInterfaceName() + "*");
		labelOutput.setText(MSGS.firewallNatFormOutInterfaceName() + "*");
		labelProtocol.setText(MSGS.firewallNatFormProtocol());
		labelSource.setText(MSGS.firewallNatFormSourceNetwork() + "*");
		labelDestination
				.setText(MSGS.firewallNatFormDestinationNetwork() + "*");
		labelEnable.setText(MSGS.firewallNatFormMasquerade());
		submit.setText(MSGS.submitButton());
		cancel.setText(MSGS.cancelButton());

		// set Tooltips
		tooltipInput.setTitle(MSGS.firewallNatFormInputInterfaceToolTip());
		tooltipOutput.setTitle(MSGS.firewallNatFormOutputInterfaceToolTip());
		tooltipProtocol.setTitle(MSGS.firewallNatFormProtocolToolTip());
		tooltipSource.setTitle(MSGS.firewallNatFormSourceNetworkToolTip());
		tooltipDestination.setTitle(MSGS
				.firewallNatFormDestinationNetworkToolTip());
		tooltipEnable.setTitle(MSGS.firewallNatFormMasqueradingToolTip());
		tooltipInput.reconfigure();
		tooltipOutput.reconfigure();
		tooltipProtocol.reconfigure();
		tooltipSource.reconfigure();
		tooltipDestination.reconfigure();
		tooltipEnable.reconfigure();

		// set ListBox
		protocol.clear();
		for (GwtFirewallNatProtocol prot : GwtFirewallNatProtocol.values()) {
			protocol.addItem(prot.name());
		}
		enable.clear();
		for (GwtFirewallNatMasquerade masquerade : GwtFirewallNatMasquerade
				.values()) {
			enable.addItem(masquerade.name());
		}

		// populate existing values
		if (existingEntry != null) {
			input.setText(existingEntry.getInInterface());
			output.setText(existingEntry.getOutInterface());
			source.setText(existingEntry.getSourceNetwork());
			destination.setText(existingEntry.getDestinationNetwork());
			for (GwtNetProtocol prot : GwtNetProtocol.values()) {
				int i = 0;
				if (existingEntry.getProtocol().equals(prot.name())) {
					protocol.setSelectedIndex(i);
					i++;
				}
			}

			for (GwtFirewallNatMasquerade masquerade : GwtFirewallNatMasquerade
					.values()) {
				int j = 0;
				if (existingEntry.getMasquerade().equals(masquerade.name())) {
					enable.setSelectedIndex(j);
					j++;
				}
			}
		}// end populate existing values

		// Set up validation

		input.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!input.getText().trim().matches(REGEX_ALPHANUMERIC)
						|| input.getText().trim().length() == 0) {
					groupInput.setValidationState(ValidationState.ERROR);
				} else {
					groupInput.setValidationState(ValidationState.NONE);
				}
			}
		});
		output.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!output.getText().trim().matches(REGEX_ALPHANUMERIC)
						|| output.getText().trim().length() == 0) {
					groupOutput.setValidationState(ValidationState.ERROR);
				} else {
					groupOutput.setValidationState(ValidationState.NONE);
				}
			}
		});
		source.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!source.getText().trim().matches(REGEX_NETWORK)) {
					groupSource.setValidationState(ValidationState.ERROR);
				} else {
					groupSource.setValidationState(ValidationState.NONE);
				}
			}
		});
		destination.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (!destination.getText().trim().matches(REGEX_NETWORK)) {
					groupDestination.setValidationState(ValidationState.ERROR);
				} else {
					groupDestination.setValidationState(ValidationState.NONE);
				}
			}
		});

		// make the required fields in error state by default
		if (input.getText() == null || input.getText().trim() == "") {
			groupInput.setValidationState(ValidationState.ERROR);
		}
		if (output.getText() == null || output.getText().trim() == "") {
			groupOutput.setValidationState(ValidationState.ERROR);
		}
		if (source.getText() == null || source.getText().trim() == "") {
			groupSource.setValidationState(ValidationState.ERROR);
		}
		if (destination.getText() == null || destination.getText().trim() == "") {
			groupDestination.setValidationState(ValidationState.ERROR);
		}

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
				if ((groupInput.getValidationState() == ValidationState.ERROR)
						|| (groupOutput.getValidationState() == ValidationState.ERROR)
						|| (groupSource.getValidationState() == ValidationState.ERROR)
						|| (groupDestination.getValidationState() == ValidationState.ERROR)) {
					//Growl.growl(MSGS.deviceConfigError());
					return;
				}
				// Fetch form data
				natEntry = new GwtFirewallNatEntry();
				natEntry.setInInterface(input.getText());
				natEntry.setOutInterface(output.getText());
				natEntry.setProtocol(protocol.getSelectedItemText());
				natEntry.setSourceNetwork(source.getText());
				natEntry.setDestinationNetwork(destination.getText());
				natEntry.setMasquerade(enable.getSelectedItemText());

				if (existingEntry == null) {
					addNewEntry(natEntry);
				} else {
					editEntry(natEntry, existingEntry);
				}
				natForm.hide();
			}

		});

		natForm.show();
	}

	protected void editEntry(GwtFirewallNatEntry natEntry,
			GwtFirewallNatEntry existingEntry) {
		if (!duplicateEntry(natEntry)) {
			natDataProvider.getList().remove(existingEntry);
			natDataProvider.getList().add(natEntry);
			natDataProvider.flush();
			if (natDataProvider.getList().size() > 0) {
				natGrid.setVisible(true);
			} else {
				natGrid.setVisible(false);
			}
		}
		apply.setEnabled(true);
	}

	private void addNewEntry(GwtFirewallNatEntry natEntry) {
		if (!duplicateEntry(natEntry)) {
			natDataProvider.getList().add(natEntry);
			natDataProvider.flush();
			if (natDataProvider.getList().size() > 0) {
				natGrid.setVisible(true);
			} else {
				natGrid.setVisible(false);
			}
		} else {
			//Growl.growl(MSGS.firewallNatFormError() + ": ",
			//		MSGS.firewallNatFormDuplicate());
		}
		apply.setEnabled(true);
	}

	private boolean duplicateEntry(GwtFirewallNatEntry firewallNatEntry) {

		boolean isDuplicateEntry = false;
		List<GwtFirewallNatEntry> entries = natDataProvider.getList();
		if (entries != null && firewallNatEntry != null) {
			for (GwtFirewallNatEntry entry : entries) {

				String sourceNetwork = (entry.getSourceNetwork() != null) ? entry
						.getSourceNetwork() : "0.0.0.0/0";
				String destinationNetwork = (entry.getDestinationNetwork() != null) ? entry
						.getDestinationNetwork() : "0.0.0.0/0";
				String newSourceNetwork = (firewallNatEntry.getSourceNetwork() != null) ? firewallNatEntry
						.getSourceNetwork() : "0.0.0.0/0";
				String newDestinationNetwork = (firewallNatEntry
						.getDestinationNetwork() != null) ? firewallNatEntry
						.getDestinationNetwork() : "0.0.0.0/0";

				if (entry.getInInterface().equals(
						firewallNatEntry.getInInterface())
						&& entry.getOutInterface().equals(
								firewallNatEntry.getOutInterface())
						&& entry.getProtocol().equals(
								firewallNatEntry.getProtocol())
						&& sourceNetwork.equals(newSourceNetwork)
						&& destinationNetwork.equals(newDestinationNetwork)) {
					isDuplicateEntry = true;
					break;
				}
			}
		}

		return isDuplicateEntry;
	}

}
