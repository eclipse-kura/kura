package org.eclipse.kura.web.client.bootstrap.ui.Firewall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.ValidationUtils.FieldType;
import org.eclipse.kura.web.shared.model.GwtBSFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtBSNetProtocol;
import org.eclipse.kura.web.shared.service.GwtBSNetworkService;
import org.eclipse.kura.web.shared.service.GwtBSNetworkServiceAsync;
import org.gwtbootstrap3.client.shared.event.ModalHideEvent;
import org.gwtbootstrap3.client.shared.event.ModalHideHandler;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Tooltip;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

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

	private static OpenPortsTabUiUiBinder uiBinder = GWT
			.create(OpenPortsTabUiUiBinder.class);

	private final GwtBSNetworkServiceAsync gwtNetworkService = GWT
			.create(GwtBSNetworkService.class);
	private static final Messages MSGS = GWT.create(Messages.class);

	interface OpenPortsTabUiUiBinder extends UiBinder<Widget, OpenPortsTabUi> {
	}

	GwtBSFirewallOpenPortEntry editOpenPortEntry, newOpenPortEntry,
			openPortEntry;
	@UiField
	AnchorListItem apply, create, edit, delete;
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
	DataGrid<GwtBSFirewallOpenPortEntry> openPortsGrid = new DataGrid<GwtBSFirewallOpenPortEntry>();
	private ListDataProvider<GwtBSFirewallOpenPortEntry> openPortsDataProvider = new ListDataProvider<GwtBSFirewallOpenPortEntry>();
	final SingleSelectionModel<GwtBSFirewallOpenPortEntry> selectionModel = new SingleSelectionModel<GwtBSFirewallOpenPortEntry>();

	public OpenPortsTabUi() {
		initWidget(uiBinder.createAndBindUi(this));
		apply.setText(MSGS.firewallApply());
		create.setText(MSGS.newButton());
		edit.setText(MSGS.editButton());
		delete.setText(MSGS.deleteButton());
		openPortsGrid.setSelectionModel(selectionModel);

		initButtons();
		initTable();
		loadData();		
	}

	private void initTable() {

		TextColumn<GwtBSFirewallOpenPortEntry> col1 = new TextColumn<GwtBSFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtBSFirewallOpenPortEntry object) {
				return String.valueOf(object.getPort());
			}
		};
		col1.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col1, MSGS.firewallOpenPort());

		TextColumn<GwtBSFirewallOpenPortEntry> col2 = new TextColumn<GwtBSFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtBSFirewallOpenPortEntry object) {
				return String.valueOf(object.getProtocol());
			}
		};
		col2.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col2, MSGS.firewallOpenPortProtocol());

		TextColumn<GwtBSFirewallOpenPortEntry> col3 = new TextColumn<GwtBSFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtBSFirewallOpenPortEntry object) {
				return String.valueOf(object.getPermittedNetwork());
			}
		};
		col3.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col3, MSGS.firewallOpenPortPermittedNetwork());

		TextColumn<GwtBSFirewallOpenPortEntry> col4 = new TextColumn<GwtBSFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtBSFirewallOpenPortEntry object) {
				return String.valueOf(object.getPermittedInterfaceName());
			}
		};
		col4.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col4,
				MSGS.firewallOpenPortPermittedInterfaceName());

		TextColumn<GwtBSFirewallOpenPortEntry> col5 = new TextColumn<GwtBSFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtBSFirewallOpenPortEntry object) {
				return String.valueOf(object.getUnpermittedInterfaceName());
			}
		};
		col5.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col5,
				MSGS.firewallOpenPortUnpermittedInterfaceName());

		TextColumn<GwtBSFirewallOpenPortEntry> col6 = new TextColumn<GwtBSFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtBSFirewallOpenPortEntry object) {
				return String.valueOf(object.getPermittedMAC());
			}
		};
		col6.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col6, MSGS.firewallOpenPortPermittedMac());

		TextColumn<GwtBSFirewallOpenPortEntry> col7 = new TextColumn<GwtBSFirewallOpenPortEntry>() {
			@Override
			public String getValue(GwtBSFirewallOpenPortEntry object) {
				return String.valueOf(object.getSourcePortRange());
			}
		};
		col7.setCellStyleNames("status-table-row");
		openPortsGrid.addColumn(col7, MSGS.firewallOpenPortSourcePortRange());

		openPortsDataProvider.addDataDisplay(openPortsGrid);
	}

	private void loadData() {
		openPortsDataProvider.getList().clear();
		gwtNetworkService
				.findDeviceFirewallOpenPorts(new AsyncCallback<ArrayList<GwtBSFirewallOpenPortEntry>>() {
					@Override
					public void onFailure(Throwable caught) {
						openPortsGrid.setVisible(false);
						notification.setVisible(true);
						notification.setText(MSGS.error() + ": "
								+ caught.getLocalizedMessage());
					}

					@Override
					public void onSuccess(
							ArrayList<GwtBSFirewallOpenPortEntry> result) {
						for (GwtBSFirewallOpenPortEntry pair : result) {
							openPortsDataProvider.getList().add(pair);
						}
						openPortsDataProvider.flush();
						setVisibility();
						apply.setEnabled(false);
					}
				});

	}

	private void initButtons() {

		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				List<GwtBSFirewallOpenPortEntry> updatedOpenPortConf = openPortsDataProvider
						.getList();

				if (updatedOpenPortConf.size() > 0) {
					gwtNetworkService.updateDeviceFirewallOpenPorts(
							updatedOpenPortConf, new AsyncCallback<Void>() {
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

		create.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				initModal(null);
				// TODO add warnings for port 80 and 22
				openPortsForm.addHideHandler(new ModalHideHandler() {
					@Override
					public void onHide(ModalHideEvent evt) {
						
						if (getNewOpenPortEntry()!= null) {
							GwtBSFirewallOpenPortEntry newEntry = getNewOpenPortEntry();
							if (!duplicateEntry(newEntry)) {
								openPortsDataProvider.getList().add(newEntry);
								openPortsDataProvider.flush();
								apply.setEnabled(true);
								setVisibility();
								openPortsGrid.redraw();
							} else {
								Growl.growl(MSGS.firewallOpenPortFormError()
										+ ": ",
										MSGS.firewallOpenPortFormDuplicate());
							}
						}
					}
				});

			}
		});

		edit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final GwtBSFirewallOpenPortEntry selection = selectionModel
						.getSelectedObject();
				if (selection != null) {
					if (selection.getPort()==22) {
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

					} else if (selection.getPort()==80) {
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
								final GwtBSFirewallOpenPortEntry editEntry = getEditOpenPortEntry();
								if (!duplicateEntry(getEditOpenPortEntry())) {
									openPortsDataProvider.getList().remove(
											selection);
									openPortsDataProvider.getList().add(
											editEntry);
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

		delete.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final GwtBSFirewallOpenPortEntry selection = selectionModel
						.getSelectedObject();
				if (selection != null) {
					
					alert.setTitle(MSGS.confirm());
					alertBody.setText(MSGS.firewallOpenPortDeleteConfirmation(String.valueOf(selection.getPort())));
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
							// Growl.growl("Delete Success");
							setVisibility();
						}
					});
					alert.show();

					
					
				}
			}
		});

	}

	private void initModal(final GwtBSFirewallOpenPortEntry existingEntry) {

		if (existingEntry == null) {
			// new
			openPortsForm.setTitle(MSGS.firewallOpenPortFormInformation());
		} else {
			// edit existing entry
			openPortsForm.setTitle(MSGS
					.firewallOpenPortFormUpdate(String.valueOf(existingEntry.getPort())));
		}

		// set Labels
		labelPort.setText(MSGS.firewallOpenPortFormPort()+"*");
		labelProtocol.setText(MSGS.firewallOpenPortFormProtocol());
		protocol.clear();
		protocol.addItem(GwtBSNetProtocol.tcp.name());
		protocol.addItem(GwtBSNetProtocol.udp.name());
		labelPermitttedNw.setText(MSGS.firewallOpenPortFormPermittedNetwork());
		labelPermitttedI.setText(MSGS
				.firewallOpenPortFormPermittedInterfaceName());
		labelUnPermitttedI.setText(MSGS
				.firewallOpenPortFormUnpermittedInterfaceName());
		labelPermitttedMac.setText(MSGS.firewallOpenPortFormPermittedMac());
		labelsource.setText(MSGS.firewallOpenPortFormSourcePortRange());

		// Permitted Interface config
		tooltipPermittedI.setTitle(MSGS
				.firewallOpenPortFormPermittedInterfaceToolTip());
		tooltipPermittedI.reconfigure();
		permittedI.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (permittedI.getText() != null && permittedI.getText() != "") {
					unpermittedI.clear();
					unpermittedI.setEnabled(false);
				} else {
					unpermittedI.setEnabled(true);
				}
			}
		});

		// UnPermitted Interface config
		tooltipUnpermittedI.setTitle(MSGS
				.firewallOpenPortFormUnpermittedInterfaceToolTip());
		tooltipUnpermittedI.reconfigure();
		unpermittedI.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (unpermittedI.getText() != null
						&& unpermittedI.getText() != "") {
					permittedI.clear();
					permittedI.setEnabled(false);
				} else {
					permittedI.setEnabled(true);
				}
			}
		});

		// populate existing values
		if (existingEntry != null) {
			port.setText(String.valueOf(existingEntry.getPort()));
			protocol.setSelectedIndex(existingEntry.getProtocol() == GwtBSNetProtocol.tcp
					.name() ? 0 : 1);

			permittedNw.setText(existingEntry.getPermittedNetwork());
			permittedI.setText(existingEntry.getPermittedInterfaceName());
			unpermittedI.setText(existingEntry.getUnpermittedInterfaceName());
			permittedMac.setText(existingEntry.getPermittedMAC());
			source.setText(existingEntry.getSourcePortRange());
		}

		
		//set up validation
		//groupPort, groupPermittedNw,groupPermittedI,grourUnpermittedI,groupPermittedMac,groupSource;
		port.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if((!port.getText().trim().matches(FieldType.NUMERIC.getRegex()) && port.getText().trim().length()!=0)
						|| (port.getText()==null || port.getText().trim()=="")){
					groupPort.setValidationState(ValidationState.ERROR);
				}else{					
					groupPort.setValidationState(ValidationState.NONE);
				}
			}});
		//set Port to be in error state by default, if empty
		if(port.getText()==null || port.getText().trim()==""){
			groupPort.setValidationState(ValidationState.ERROR);
		}
		
		permittedNw.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!permittedNw.getText().trim().matches(FieldType.NETWORK.getRegex()) && permittedNw.getText().trim().length()>0){					
					groupPermittedNw.setValidationState(ValidationState.ERROR);
				}else{				
					groupPermittedNw.setValidationState(ValidationState.NONE);
				}
			}});
		permittedI.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!permittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex()) && permittedI.getText().trim().length()>0){
					groupPermittedI.setValidationState(ValidationState.ERROR);
				}else{					
					groupPermittedI.setValidationState(ValidationState.NONE);
				}
			}});
		unpermittedI.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!unpermittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex()) && unpermittedI.getText().trim().length()>0){
					groupUnpermittedI.setValidationState(ValidationState.ERROR);
				}else{					
					groupPermittedI.setValidationState(ValidationState.NONE);
				}
			}});
		permittedMac.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!permittedMac.getText().trim().matches(FieldType.MAC_ADDRESS.getRegex()) && permittedMac.getText().trim().length()>0){
					groupPermittedMac.setValidationState(ValidationState.ERROR);
				}else{					
					groupPermittedMac.setValidationState(ValidationState.NONE);
				}
			}});
		source.addBlurHandler(new BlurHandler(){
			@Override
			public void onBlur(BlurEvent event) {
				if(!source.getText().trim().matches(FieldType.PORT_RANGE.getRegex()) && source.getText().trim().length()>0){
					groupSource.setValidationState(ValidationState.ERROR);
				}else{					
					groupSource.setValidationState(ValidationState.NONE);
				}
			}});
		
		
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

				if((groupPort.getValidationState()==ValidationState.ERROR)||
					(groupPermittedNw.getValidationState()==ValidationState.ERROR)||
					(groupPermittedI.getValidationState()==ValidationState.ERROR)||
					(groupUnpermittedI.getValidationState()==ValidationState.ERROR)||
					(groupPermittedMac.getValidationState()==ValidationState.ERROR)||
					(groupSource.getValidationState()==ValidationState.ERROR)){
					Growl.growl(MSGS.deviceConfigError());
					return;
				}
				// create a new entry
				openPortEntry = new GwtBSFirewallOpenPortEntry();
				openPortEntry.setPort(Integer.parseInt(port.getText()));
				openPortEntry.setProtocol(protocol.getSelectedItemText());
				if (permittedNw.getText() != null
						&& permittedNw.getText() != "") {
					openPortEntry.setPermittedNetwork(permittedNw.getText());
				} else {
					openPortEntry.setPermittedNetwork("0.0.0.0/0");
				}
				if (permittedI.getText() != null && permittedI.getText() != "") {
					openPortEntry.setPermittedInterfaceName(permittedI
							.getText());
				}
				if (unpermittedI.getText() != null
						&& unpermittedI.getText() != "") {
					openPortEntry.setUnpermittedInterfaceName(unpermittedI
							.getText());
				}
				if (permittedMac.getText() != null
						&& permittedMac.getText() != "") {
					openPortEntry.setPermittedMAC(permittedMac.getText());
				}
				if (source.getText() != null && source.getText() != "") {
					openPortEntry.setSourcePortRange(source.getText());
				}
				openPortsForm.hide();

				editOpenPortEntry = null;
				newOpenPortEntry = null;
				
				if (existingEntry == null) {
					newOpenPortEntry = openPortEntry;
				} else {
					editOpenPortEntry = openPortEntry;
				}
			}
		});

		openPortsForm.show();
	}

	public GwtBSFirewallOpenPortEntry getNewOpenPortEntry() {
		return newOpenPortEntry;
	}

	public GwtBSFirewallOpenPortEntry getEditOpenPortEntry() {
		return editOpenPortEntry;
	}

	private boolean duplicateEntry(GwtBSFirewallOpenPortEntry openPortEntry) {
		List<GwtBSFirewallOpenPortEntry> entries = openPortsDataProvider
				.getList();
		if (entries != null && openPortEntry != null) {
			for (GwtBSFirewallOpenPortEntry entry : entries) {
				if (entry.getPort()==(openPortEntry.getPort())
						&& entry.getPermittedNetwork().equals(
								openPortEntry.getPermittedNetwork())) {
					return true;
				}
			}
		}
		return false;
	}

	private void setVisibility() {
		if (openPortsDataProvider.getList().size() == 0) {
			openPortsGrid.setVisible(false);
			notification.setVisible(true);
			notification.setText(MSGS.firewallOpenPortTableNoPorts());
		} else {
			openPortsGrid.setVisible(true);
			notification.setVisible(false);
		}
	}

}
