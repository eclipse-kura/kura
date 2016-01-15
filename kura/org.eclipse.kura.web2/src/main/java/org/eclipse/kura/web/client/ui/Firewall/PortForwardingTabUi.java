package org.eclipse.kura.web.client.bootstrap.ui.Firewall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.ValidationUtils.FieldType;
import org.eclipse.kura.web.shared.model.GwtBSFirewallNatMasquerade;
import org.eclipse.kura.web.shared.model.GwtBSFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtBSNetProtocol;
import org.eclipse.kura.web.shared.service.GwtBSNetworkService;
import org.eclipse.kura.web.shared.service.GwtBSNetworkServiceAsync;
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
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

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

public class PortForwardingTabUi extends Composite {

	private static PortForwardingTabUiUiBinder uiBinder = GWT
			.create(PortForwardingTabUiUiBinder.class);

	interface PortForwardingTabUiUiBinder extends
			UiBinder<Widget, PortForwardingTabUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private final GwtBSNetworkServiceAsync gwtNetworkService = GWT
			.create(GwtBSNetworkService.class);
	GwtBSFirewallPortForwardEntry portForwardEntry;


	@UiField
	AnchorListItem apply, create, edit, delete;
	@UiField
	Alert notification;
	@UiField
	DataGrid<GwtBSFirewallPortForwardEntry> portForwardGrid = new DataGrid<GwtBSFirewallPortForwardEntry>();
	private ListDataProvider<GwtBSFirewallPortForwardEntry> portForwardDataProvider = new ListDataProvider<GwtBSFirewallPortForwardEntry>();
	final SingleSelectionModel<GwtBSFirewallPortForwardEntry> selectionModel = new SingleSelectionModel<GwtBSFirewallPortForwardEntry>();

	@UiField
	Modal confirm;
	@UiField
	ModalBody confirmBody;
	@UiField
	ModalFooter confirmFooter;

	@UiField
	Modal alert;
	@UiField
	Span alertBody;
	@UiField
	Button yes, no;

	@UiField
	Modal portForwardingForm;
	@UiField
	FormLabel labelInput, labelOutput, labelLan, labelProtocol, labelExternal,
			labelInternal, labelEnable, labelPermitttedNw, labelPermitttedMac,
			labelSource;
	@UiField
	FormGroup groupInput,groupOutput,groupLan,groupExternal,groupInternal,groupPermittedNw,groupPermittedMac,groupSource;
	@UiField
	Tooltip tooltipInput, tooltipOutput, tooltipLan, tooltipProtocol,
			tooltipExternal, tooltipInternal, tooltipEnable,
			tooltipPermittedNw, tooltipPermittedMac, tooltipSource;
	@UiField
	TextBox input, output, lan, external, internal, permittedNw, permittedMac,
			source;
	@UiField
	ListBox protocol, enable;
	@UiField
	Button submit, cancel;

	public PortForwardingTabUi() {
		initWidget(uiBinder.createAndBindUi(this));

		initButtons();
		initTable();
		loadData();
	}

	private void initTable() {

		TextColumn<GwtBSFirewallPortForwardEntry> col1 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getInboundInterface());
			}
		};
		col1.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col1,
				MSGS.firewallPortForwardInboundInterface());

		TextColumn<GwtBSFirewallPortForwardEntry> col2 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getOutboundInterface());
			}
		};
		col2.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col2,
				MSGS.firewallPortForwardOutboundInterface());

		TextColumn<GwtBSFirewallPortForwardEntry> col3 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getAddress());
			}
		};
		col3.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col3, MSGS.firewallPortForwardAddress());

		TextColumn<GwtBSFirewallPortForwardEntry> col4 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getProtocol());
			}
		};
		col4.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col4, MSGS.firewallPortForwardProtocol());

		TextColumn<GwtBSFirewallPortForwardEntry> col5 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getInPort());
			}
		};
		col5.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col5, MSGS.firewallPortForwardInPort());

		TextColumn<GwtBSFirewallPortForwardEntry> col6 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getOutPort());
			}
		};
		col6.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col6, MSGS.firewallPortForwardOutPort());

		TextColumn<GwtBSFirewallPortForwardEntry> col7 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getMasquerade());
			}
		};
		col7.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col7, MSGS.firewallPortForwardMasquerade());

		TextColumn<GwtBSFirewallPortForwardEntry> col8 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getPermittedNetwork());
			}
		};
		col8.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col8,
				MSGS.firewallPortForwardPermittedNetwork());

		TextColumn<GwtBSFirewallPortForwardEntry> col9 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getPermittedMAC());
			}
		};
		col9.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col9, MSGS.firewallPortForwardPermittedMac());

		TextColumn<GwtBSFirewallPortForwardEntry> col10 = new TextColumn<GwtBSFirewallPortForwardEntry>() {
			@Override
			public String getValue(GwtBSFirewallPortForwardEntry object) {
				return String.valueOf(object.getSourcePortRange());
			}
		};
		col10.setCellStyleNames("status-table-row");
		portForwardGrid.addColumn(col10,
				MSGS.firewallPortForwardSourcePortRange());

		portForwardDataProvider.addDataDisplay(portForwardGrid);
		portForwardGrid.setSelectionModel(selectionModel);
	}

	private void loadData() {
		portForwardDataProvider.getList().clear();
		portForwardGrid.setVisible(false);
		notification.setVisible(false);

		gwtNetworkService
				.findDeviceFirewallPortForwards(new AsyncCallback<ArrayList<GwtBSFirewallPortForwardEntry>>() {

					@Override
					public void onFailure(Throwable caught) {
						notification.setVisible(true);
						notification.setText(MSGS.error() + ": "
								+ caught.getLocalizedMessage());

					}

					@Override
					public void onSuccess(
							ArrayList<GwtBSFirewallPortForwardEntry> result) {
						for (GwtBSFirewallPortForwardEntry pair : result) {
							portForwardDataProvider.getList().add(pair);
						}
						portForwardDataProvider.flush();

						if (portForwardDataProvider.getList().size() > 0) {
							portForwardGrid.setVisible(true);
						}
						apply.setEnabled(false);
					}
				});
	}

	private void initButtons() {

		apply.setText(MSGS.firewallApply());
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				List<GwtBSFirewallPortForwardEntry> updatedPortForwardConf = portForwardDataProvider
						.getList();

				if (updatedPortForwardConf.size() > 0) {
					gwtNetworkService.updateDeviceFirewallPortForwards(
							updatedPortForwardConf, new AsyncCallback<Void>() {
								@Override
								public void onFailure(Throwable caught) {
									Growl.growl(MSGS.error() + ": ",
											caught.getLocalizedMessage());
								}

								@Override
								public void onSuccess(Void result) {
									apply.setEnabled(false);
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
				final GwtBSFirewallPortForwardEntry selection = selectionModel
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
				final GwtBSFirewallPortForwardEntry selection = selectionModel
						.getSelectedObject();

				if (selection != null) {

					alert.setTitle(MSGS.confirm());
					alertBody.setText(MSGS
							.firewallOpenPortDeleteConfirmation(String.valueOf(selection.getInPort())));
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
							portForwardDataProvider.getList().remove(selection);
							portForwardDataProvider.flush();
							apply.setEnabled(true);
							// Growl.growl("Delete Success");

							portForwardGrid.setVisible(false);
							notification.setVisible(false);
							if (portForwardDataProvider.getList().size() > 0) {
								portForwardGrid.setVisible(true);
							}
						}
					});
					alert.show();

				}
			}
		});
	}

	private void initModal(final GwtBSFirewallPortForwardEntry existingEntry) {
		final GwtBSFirewallPortForwardEntry oldEntry =existingEntry;
		
		if (existingEntry == null) {
			// new
			portForwardingForm.setTitle(MSGS
					.firewallPortForwardFormInformation());
		} else {
			// edit existing entry
			portForwardingForm.setTitle(MSGS
					.firewallPortForwardFormUpdate(String.valueOf(existingEntry.getInPort())));
		}

		// setLabels
		labelInput.setText(MSGS.firewallPortForwardFormInboundInterface());
		labelOutput.setText(MSGS.firewallPortForwardFormOutboundInterface());
		labelLan.setText(MSGS.firewallPortForwardFormAddress());
		labelProtocol.setText(MSGS.firewallPortForwardFormProtocol());
		labelExternal.setText(MSGS.firewallPortForwardFormOutPort());
		labelInternal.setText(MSGS.firewallPortForwardFormInPort());
		labelEnable.setText(MSGS.firewallNatFormMasquerade());
		labelPermitttedNw.setText(MSGS
				.firewallPortForwardFormPermittedNetwork());
		labelPermitttedMac.setText(MSGS.firewallPortForwardFormPermittedMac());
		labelSource.setText(MSGS.firewallPortForwardFormSourcePortRange());
		submit.setText(MSGS.submitButton());
		cancel.setText(MSGS.cancelButton());

		// set Tooltips
		tooltipInput.setTitle(MSGS
				.firewallPortForwardFormInboundInterfaceToolTip());
		tooltipOutput.setTitle(MSGS
				.firewallPortForwardFormOutboundInterfaceToolTip());
		tooltipLan.setTitle(MSGS.firewallPortForwardFormLanAddressToolTip());
		tooltipProtocol.setTitle(MSGS.firewallPortForwardFormProtocolToolTip());
		tooltipInternal.setTitle(MSGS
				.firewallPortForwardFormExternalPortToolTip());
		tooltipExternal.setTitle(MSGS
				.firewallPortForwardFormInternalPortToolTip());
		tooltipEnable.setTitle(MSGS
				.firewallPortForwardFormMasqueradingToolTip());
		tooltipPermittedNw.setTitle(MSGS
				.firewallPortForwardFormPermittedNetworkToolTip());
		tooltipPermittedMac.setTitle(MSGS
				.firewallPortForwardFormPermittedMacAddressToolTip());
		tooltipSource.setTitle(MSGS
				.firewallPortForwardFormSourcePortRangeToolTip());
		tooltipInput.reconfigure();
		tooltipOutput.reconfigure();
		tooltipLan.reconfigure();
		tooltipProtocol.reconfigure();
		tooltipExternal.reconfigure();
		tooltipInternal.reconfigure();
		tooltipEnable.reconfigure();
		tooltipPermittedNw.reconfigure();
		tooltipPermittedMac.reconfigure();
		tooltipSource.reconfigure();

		// set ListBoxes
		protocol.clear();
		for (GwtBSNetProtocol prot : GwtBSNetProtocol.values()) {
			protocol.addItem(prot.name());
		}
		enable.clear();
		for (GwtBSFirewallNatMasquerade masquerade : GwtBSFirewallNatMasquerade
				.values()) {
			enable.addItem(masquerade.name());
		}

		// populate Existing Values
		if (existingEntry != null) {
			input.setText(existingEntry.getInboundInterface());
			output.setText(existingEntry.getOutboundInterface());
			lan.setText(existingEntry.getAddress());
			external.setText(String.valueOf(existingEntry.getOutPort()));
			internal.setText(String.valueOf(existingEntry.getInPort()));
			permittedNw.setText(existingEntry.getPermittedNetwork());
			permittedMac.setText(existingEntry.getPermittedMAC());
			source.setText(existingEntry.getSourcePortRange());

			for (GwtBSNetProtocol prot : GwtBSNetProtocol.values()) {
				int i = 0;
				if (existingEntry.getProtocol().equals(prot.name())) {
					protocol.setSelectedIndex(i);
					i++;
				}
			}

			for (GwtBSFirewallNatMasquerade masquerade : GwtBSFirewallNatMasquerade
					.values()) {
				int j = 0;
				if (existingEntry.getMasquerade().equals(masquerade.name())) {
					enable.setSelectedIndex(j);
					j++;
				}
			}

		}// end existing values

		//Set validations
		input.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(input.getText().trim().length()==0 || !input.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())){
					groupInput.setValidationState(ValidationState.ERROR);
				}else{
					groupInput.setValidationState(ValidationState.NONE);
				}
			}});
		output.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(output.getText().trim().length()==0 || !output.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())){
					groupOutput.setValidationState(ValidationState.ERROR);
				}else{
					groupOutput.setValidationState(ValidationState.NONE);
				}
			}});
		lan.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(lan.getText().trim().length()==0 || !lan.getText().trim().matches(FieldType.IPv4_ADDRESS.getRegex())){
					groupLan.setValidationState(ValidationState.ERROR);
				}else{
					groupLan.setValidationState(ValidationState.NONE);
				}
			}});
		internal.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(internal.getText().trim().length()==0 || !internal.getText().trim().matches(FieldType.NUMERIC.getRegex())){
					groupInternal.setValidationState(ValidationState.ERROR);
				}else{
					groupInternal.setValidationState(ValidationState.NONE);
				}
			}});
		external.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(external.getText().trim().length()==0 || !external.getText().trim().matches(FieldType.NUMERIC.getRegex())){
					groupExternal.setValidationState(ValidationState.ERROR);
				}else{
					groupExternal.setValidationState(ValidationState.NONE);
				}
			}});
		permittedNw.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(permittedNw.getText().trim().length()!=0 && !permittedNw.getText().trim().matches(FieldType.NETWORK.getRegex())){
					groupPermittedNw.setValidationState(ValidationState.ERROR);
				}else{
					groupPermittedNw.setValidationState(ValidationState.NONE);
				}
			}});
		permittedMac.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(permittedMac.getText().trim().length()!=0 && !permittedMac.getText().trim().matches(FieldType.MAC_ADDRESS.getRegex())){
					groupPermittedMac.setValidationState(ValidationState.ERROR);
				}else{
					groupPermittedMac.setValidationState(ValidationState.NONE);
				}
			}});
		source.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(source.getText().trim().length()!=0 && !source.getText().trim().matches(FieldType.PORT_RANGE.getRegex())){
					groupSource.setValidationState(ValidationState.ERROR);
				}else{
					groupSource.setValidationState(ValidationState.NONE);
				}
			}});
		
		//set required fields in error state by default if empty
		if(input.getText()==null || input.getText().trim()==""){
			groupInput.setValidationState(ValidationState.ERROR);
		}
		if(output.getText()==null || output.getText().trim()==""){
			groupOutput.setValidationState(ValidationState.ERROR);
		}
		if(lan.getText()==null || lan.getText().trim()==""){
			groupLan.setValidationState(ValidationState.ERROR);
		}
		if(internal.getText()==null || internal.getText().trim()==""){
			groupInternal.setValidationState(ValidationState.ERROR);
		}
		if(external.getText()==null || external.getText().trim()==""){
			groupExternal.setValidationState(ValidationState.ERROR);
		}
		
		// handle buttons
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				portForwardEntry = null;
				portForwardingForm.hide();
			}
		});

		
		submit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if(groupInput.getValidationState().equals(ValidationState.ERROR)
						||groupOutput.getValidationState().equals(ValidationState.ERROR)
						||groupLan.getValidationState().equals(ValidationState.ERROR)
						||groupInternal.getValidationState().equals(ValidationState.ERROR)
						||groupExternal.getValidationState().equals(ValidationState.ERROR)
						||groupPermittedNw.getValidationState().equals(ValidationState.ERROR)
						||groupPermittedMac.getValidationState().equals(ValidationState.ERROR)
						||groupSource.getValidationState().equals(ValidationState.ERROR)){
					Growl.growl(MSGS.deviceConfigError());
					return;
				}
					
					
				portForwardEntry = new GwtBSFirewallPortForwardEntry();
				portForwardEntry.setInboundInterface(input.getText());
				portForwardEntry.setOutboundInterface(output.getText());
				portForwardEntry.setAddress(lan.getText());
				portForwardEntry.setProtocol(protocol.getSelectedItemText());
				if (internal.getText() != null && internal.getText() != "") {
					portForwardEntry.setInPort(Integer.parseInt(internal
							.getText()));
				}
				if (external.getText() != null && external.getText() != "") {
					portForwardEntry.setOutPort(Integer.parseInt(external
							.getText()));
				}
				portForwardEntry.setMasquerade(enable.getSelectedItemText());
				if (permittedNw.getText() != null) {
					portForwardEntry.setPermittedNetwork(permittedNw.getText());
				} else {
					portForwardEntry.setPermittedNetwork("0.0.0.0/0");
				}
				if (permittedMac.getText() != null
						&& permittedMac.getText() != "") {
					
					confirm.setTitle(MSGS.firewallPortForwardFormNotification());
					confirmBody.clear();
					confirmBody.add(new Span(MSGS
							.firewallPortForwardFormNotificationMacFiltering()));
					confirmFooter.clear();
					confirmFooter.add(new Button(MSGS.okButton(),
							new ClickHandler() {
								@Override
								public void onClick(ClickEvent event) {
									portForwardEntry
											.setPermittedMAC(permittedMac
													.getText());
									confirm.hide();
									portForwardEntry.setSourcePortRange(source.getText());

									
									if (oldEntry != null) {
										Growl.growl("invoking edit entry0");
										editEntry(portForwardEntry,oldEntry);
									} else {
										Growl.growl("invoking new entry0");
										addNewEntry(portForwardEntry);											 
									}
									portForwardEntry=null;
									portForwardingForm.hide();
								}
							}));
					confirm.show();
					
				} else {
					portForwardEntry.setSourcePortRange(source.getText());
					
					
					//Add values to table
					Growl.growl(String.valueOf(existingEntry==null));
					if (oldEntry != null) {
						Growl.growl("invoking edit entry1");
						editEntry(portForwardEntry,oldEntry);	
					} else {
						Growl.growl("invoking new entry1");
						addNewEntry(portForwardEntry);										 
					}
					portForwardEntry=null;
					portForwardingForm.hide();

				} //end else (mac!=null)

			}});// end submit click handler

		portForwardingForm.show();
	}// end initModal

	private boolean duplicateEntry(
			GwtBSFirewallPortForwardEntry portForwardEntry) {

		boolean isDuplicateEntry = false;
		List<GwtBSFirewallPortForwardEntry> entries = portForwardDataProvider
				.getList();
		if (entries != null && portForwardEntry != null) {
			for (GwtBSFirewallPortForwardEntry entry : entries) {
				if (entry.getInboundInterface().equals(
						portForwardEntry.getInboundInterface())
						&& entry.getOutboundInterface().equals(
								portForwardEntry.getOutboundInterface())
						&& entry.getAddress().equals(
								portForwardEntry.getAddress())
						&& entry.getProtocol().equals(
								portForwardEntry.getProtocol())
						&& entry.getOutPort()==(
								portForwardEntry.getOutPort())
						&& entry.getInPort()==(
								portForwardEntry.getInPort())) {

					String permittedNetwork = (entry.getPermittedNetwork() != null) ? entry
							.getPermittedNetwork() : "0.0.0.0/0";
					String newPermittedNetwork = (portForwardEntry
							.getPermittedNetwork() != null) ? portForwardEntry
							.getPermittedNetwork() : "0.0.0.0/0";
					String permittedMAC = (entry.getPermittedMAC() != null) ? entry
							.getPermittedMAC().toUpperCase() : "";
					String newPermittedMAC = (portForwardEntry
							.getPermittedMAC() != null) ? portForwardEntry
							.getPermittedMAC().toUpperCase() : "";
					String sourcePortRange = (entry.getSourcePortRange() != null) ? entry
							.getSourcePortRange() : "";
					String newSourcePortRange = (portForwardEntry
							.getSourcePortRange() != null) ? portForwardEntry
							.getSourcePortRange() : "";

					if (permittedNetwork.equals(newPermittedNetwork)
							&& permittedMAC.equals(newPermittedMAC)
							&& sourcePortRange.equals(newSourcePortRange)) {
						isDuplicateEntry = true;
						break;
					}
				}
			}
		}
		return isDuplicateEntry;
	}

	private void addNewEntry(GwtBSFirewallPortForwardEntry portForwardEntry){
		if (!duplicateEntry(portForwardEntry)) {
            portForwardDataProvider.getList().add(portForwardEntry);
            Growl.growl("Adding new Entry");
            portForwardDataProvider.flush();
            if(portForwardDataProvider.getList().size()>0){
                portForwardGrid.setVisible(true);
                notification.setVisible(false);
            }
            apply.setEnabled(true);
            portForwardGrid.redraw();
            portForwardEntry=null;
        } else {
            Growl.growl(MSGS.firewallPortForwardFormError()
                    + ": ",
                    MSGS.firewallPortForwardFormDuplicate());
        }
		
	}
	
	private void editEntry(GwtBSFirewallPortForwardEntry portForwardEntry, GwtBSFirewallPortForwardEntry existingEntry){
		if(!duplicateEntry(portForwardEntry)){
			Growl.growl("Adding edited Entry");
            portForwardDataProvider.getList().remove(existingEntry);
            portForwardDataProvider.flush();
            portForwardDataProvider.getList().add(portForwardEntry);
            portForwardDataProvider.flush();
            if(portForwardDataProvider.getList().size()>0){
                portForwardGrid.setVisible(true);
                notification.setVisible(false);
            }
            apply.setEnabled(true);
            portForwardGrid.redraw();
        }
	}

}