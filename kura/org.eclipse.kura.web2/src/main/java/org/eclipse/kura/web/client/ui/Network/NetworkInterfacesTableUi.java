package org.eclipse.kura.web.client.bootstrap.ui.Network;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtBSNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSSession;
import org.eclipse.kura.web.shared.service.GwtBSNetworkService;
import org.eclipse.kura.web.shared.service.GwtBSNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class NetworkInterfacesTableUi extends Composite {

	private static NetworkInterfacesTableUiUiBinder uiBinder = GWT
			.create(NetworkInterfacesTableUiUiBinder.class);

	interface NetworkInterfacesTableUiUiBinder extends
			UiBinder<Widget, NetworkInterfacesTableUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private final GwtBSNetworkServiceAsync gwtNetworkService = GWT
			.create(GwtBSNetworkService.class);
	GwtBSSession session;
	NetworkTabsUi tabs;
	GwtBSNetInterfaceConfig selection;

	@UiField
	Alert notification;
	@UiField
	DataGrid<GwtBSNetInterfaceConfig> interfacesGrid = new DataGrid<GwtBSNetInterfaceConfig>();
	private ListDataProvider<GwtBSNetInterfaceConfig> interfacesProvider = new ListDataProvider<GwtBSNetInterfaceConfig>();
	final SingleSelectionModel<GwtBSNetInterfaceConfig> selectionModel = new SingleSelectionModel<GwtBSNetInterfaceConfig>();

	public NetworkInterfacesTableUi(GwtBSSession session,
			NetworkTabsUi tabsPanel) {
		initWidget(uiBinder.createAndBindUi(this));
		this.session = session;
		this.tabs = tabsPanel;
		initTable();
		loadData();

		selectionModel
				.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

					@Override
					public void onSelectionChange(SelectionChangeEvent event) {
						if (selection != null && tabs.isDirty()) {
							// there was an earlier selection, changes have not
							// been saved						
							final Modal confirm = new Modal();
							ModalBody confirmBody = new ModalBody();
							ModalFooter confirmFooter = new ModalFooter();

							confirm.setTitle(MSGS.confirm());
							confirmBody.add(new Span(MSGS.deviceConfigDirty()));
							confirmFooter.add(new Button(MSGS.yesButton(),
									new ClickHandler() {
										@Override
										public void onClick(ClickEvent event) {
											confirm.hide();
											selection = selectionModel
													.getSelectedObject();
											if (selection != null) {
												tabs.setNetInterface(selection);
												tabs.setDirty(false);
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

						} else {
							// no unsaved changes
							selection = selectionModel.getSelectedObject();
							if (selection != null) {
								tabs.setNetInterface(selection);
							}
						}
					}
				});

	}

	
	public void refresh() {
		if (selection != null && tabs.isDirty()) {
			// there was an earlier selection, changes have not been saved
			final Modal confirm = new Modal();
			ModalBody confirmBody = new ModalBody();
			ModalFooter confirmFooter = new ModalFooter();

			confirm.setTitle(MSGS.confirm());
			confirmBody.add(new Span(MSGS.deviceConfigDirty()));
			confirmFooter.add(new Button(MSGS.yesButton(),
					new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							confirm.hide();
							//selection = null;
							tabs.setDirty(false);
							loadData();
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
		} else {
			tabs.setDirty(false);
			loadData();
		}
	}
	
	
	
	/*--------------------------------------
	 * -------Private methods---------------
	 --------------------------------------*/
	
	private void initTable() {
		TextColumn<GwtBSNetInterfaceConfig> col1 = new TextColumn<GwtBSNetInterfaceConfig>() {
			@Override
			public String getValue(GwtBSNetInterfaceConfig object) {
				return object.getName();
			}
		};
		col1.setCellStyleNames("status-table-row");
		interfacesGrid.addColumn(col1, MSGS.netInterfaceName());

		interfacesProvider.addDataDisplay(interfacesGrid);
		interfacesGrid.setSelectionModel(selectionModel);

	}

	private void loadData() {
		interfacesProvider.getList().clear();
		gwtNetworkService
				.findNetInterfaceConfigurations(new AsyncCallback<ArrayList<GwtBSNetInterfaceConfig>>() {
					;
					@Override
					public void onFailure(Throwable caught) {
						Growl.growl(MSGS.error() + ": "
								+ caught.getLocalizedMessage());
					}

					@Override
					public void onSuccess(
							ArrayList<GwtBSNetInterfaceConfig> result) {
						for (GwtBSNetInterfaceConfig pair : result) {
							interfacesProvider.getList().add(pair);
						}
					}
				});

		GwtBSNetInterfaceConfig ex = new GwtBSNetInterfaceConfig();
		ex.setName("Wifi");
		ex.setHwName("1-2-1-2");
		interfacesProvider.getList().add(ex);
		GwtBSNetInterfaceConfig ex2 = new GwtBSNetInterfaceConfig();
		ex2.setName("eth0");
		interfacesProvider.getList().add(ex2);
		interfacesProvider.flush();

		if (interfacesProvider.getList().size() > 0) {
			interfacesGrid.setVisible(true);
			notification.setVisible(false);
			selectionModel.setSelected(interfacesProvider.getList().get(0),
					true);
			interfacesGrid.getSelectionModel().setSelected(
					interfacesProvider.getList().get(0), true);

		} else {
			interfacesGrid.setVisible(false);
			notification.setVisible(true);
			notification.setText(MSGS.netTableNoInterfaces());
		}
	}

}
