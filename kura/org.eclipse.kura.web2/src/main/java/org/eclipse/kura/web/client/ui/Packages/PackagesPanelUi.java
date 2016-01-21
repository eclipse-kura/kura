package org.eclipse.kura.web.client.ui.Packages;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtPackageService;
import org.eclipse.kura.web.shared.service.GwtPackageServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class PackagesPanelUi extends Composite {

	private static PackagesPanelUiUiBinder uiBinder = GWT.create(PackagesPanelUiUiBinder.class);

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtPackageServiceAsync gwtPackageService = GWT.create(GwtPackageService.class);

	private final static String SERVLET_URL = "/" + GWT.getModuleName()
			+ "/file/deploy";
	private static final Messages MSGS = GWT.create(Messages.class);

	interface PackagesPanelUiUiBinder extends UiBinder<Widget, PackagesPanelUi> {
	}

	@UiField
	Modal uploadModal;
	@UiField
	FormPanel packagesFormFile, packagesFormUrl;
	@UiField
	Button fileCancel, fileSubmit, urlCancel, urlSubmit;
	@UiField
	TabListItem fileLabel;
	
	@UiField
	Alert notification;
	@UiField
	AnchorListItem packagesRefresh, packagesInstall, packagesUninstall;
	@UiField
	DataGrid<GwtDeploymentPackage> packagesGrid = new DataGrid<GwtDeploymentPackage>();
	private ListDataProvider<GwtDeploymentPackage> packagesDataProvider = new ListDataProvider<GwtDeploymentPackage>();
	final SingleSelectionModel<GwtDeploymentPackage> selectionModel = new SingleSelectionModel<GwtDeploymentPackage>();

	GwtSession gwtSession;
	GwtDeploymentPackage selected;

	public PackagesPanelUi() {

		// TODO - ServiceTree
		initWidget(uiBinder.createAndBindUi(this));
		packagesGrid.setSelectionModel(selectionModel);
		initTable();

		// Refresh Button
		packagesRefresh.setText(MSGS.refreshButton());
		packagesRefresh.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {

				refresh();
			}
		});
		// Install Button
		packagesInstall.setText(MSGS.packageAddButton());
		packagesInstall.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				upload();
			}
		});
		
		// Uninstall Button
		packagesUninstall.setText(MSGS.packageDeleteButton());
		packagesUninstall.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				selected = selectionModel.getSelectedObject();
				if (selected != null && selected.getVersion()!=null) {
				final Modal modal = new Modal();
				ModalBody modalBody = new ModalBody();
				ModalFooter modalFooter = new ModalFooter();
				modal.setClosable(true);
				modal.setTitle(MSGS.confirm());
				modalBody.add(new Span(MSGS.deviceUninstallPackage(selected.getName())));
				modalFooter.add(new Button("Yes", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						modal.hide();
						uninstall(selected);
					}
				}));
				modalFooter.add(new Button("No", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						modal.hide();
					}
				}));
				

				modal.add(modalBody);
				modal.add(modalFooter);
				modal.show();
				}// end if null
			}// end on click
		});
	}

	public void setSession(GwtSession currentSession) {
		gwtSession = currentSession;
	}

	public void refresh() {
		refresh(100);
	}	

	private void refresh(int delay) {
			Timer timer = new Timer() {
				@Override
				public void run() {
					loadPackagesData();
				}
			};
			timer.schedule(delay);
	}

	private void upload() {
		uploadModal.show();
		
		//******File TAB ****//
		fileLabel.setText(MSGS.fileLabel());
		packagesFormFile.setAction(SERVLET_URL + "/upload");
		packagesFormFile.setEncoding(FormPanel.ENCODING_MULTIPART);
		packagesFormFile.setMethod(FormPanel.METHOD_POST);
		packagesFormFile.addSubmitCompleteHandler(new SubmitCompleteHandler() {
			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				String result = event.getResults();
				if (result == null || result.isEmpty()) {
					//Growl.growl(MSGS.information()+": ", MSGS.fileUploadSuccess());
					uploadModal.hide();
					refresh(2500);
				} else {
					//Growl.growl(MSGS.information()+": ", MSGS.fileUploadFailure());
				}
			}
		});
		
		fileSubmit.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				packagesFormFile.submit();
			}});
		fileCancel.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {			
				uploadModal.hide();
			}});
		
		
		//******URL TAB ****//
		packagesFormUrl.setAction(SERVLET_URL + "/url");
		packagesFormUrl.setMethod(FormPanel.METHOD_POST);
		packagesFormUrl.addSubmitCompleteHandler(new SubmitCompleteHandler() {
			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				String result = event.getResults();
				if (result == null || result.isEmpty()) {
					//Growl.growl(MSGS.information()+": ", MSGS.fileDownloadSuccess());
					uploadModal.hide();
					refresh(2500);
				} else {
					String errMsg = result;
					int startIdx = result.indexOf("<pre>");
					int endIndex = result.indexOf("</pre>");
					if (startIdx != -1 && endIndex != -1) {
						errMsg = result.substring(startIdx+5, endIndex);
					}
					//Growl.growl(MSGS.error()+": ", MSGS.fileDownloadFailure()+": "+errMsg);
				}
			}
		});
		
		urlSubmit.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				packagesFormUrl.submit();
			}});
		urlCancel.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {			
				uploadModal.hide();
			}});

		
	}

	private void uninstall(final GwtDeploymentPackage selected) {
		
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtPackageService.uninstallDeploymentPackage(token, selected.getName(), new AsyncCallback<Void>() {

					@Override
					public void onFailure(Throwable caught) {
						// TODO Auto-generated method stub
						//Growl.growl("Uninstall Failed: ",caught.getLocalizedMessage());
						refresh(2500);
					}

					@Override
					public void onSuccess(Void result) {
						// TODO Auto-generated method stub
						//Growl.growl("Uninstalled!");
						refresh(2500);
					}});
			}
			
		});
	}

	private void initTable() {

		TextColumn<GwtDeploymentPackage> col1 = new TextColumn<GwtDeploymentPackage>() {
			@Override
			public String getValue(GwtDeploymentPackage object) {
				return object.getName();
			}
		};
		col1.setCellStyleNames("status-table-row");
		packagesGrid.addColumn(col1, "Name");

		TextColumn<GwtDeploymentPackage> col2 = new TextColumn<GwtDeploymentPackage>() {
			@Override
			public String getValue(GwtDeploymentPackage object) {
				return object.getName();
			}
		};
		col2.setCellStyleNames("status-table-row");
		packagesGrid.addColumn(col2, "Version");
		
		packagesDataProvider.addDataDisplay(packagesGrid);
	}

	private void loadPackagesData() {
		packagesDataProvider.getList().clear();
		
		gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

			@Override
			public void onFailure(Throwable ex) {
				FailureHandler.handle(ex);
			}

			@Override
			public void onSuccess(GwtXSRFToken token) {
				gwtPackageService.findDeviceDeploymentPackages(token, new AsyncCallback<List<GwtDeploymentPackage>>() {
					@Override
					public void onFailure(Throwable caught) {
						GwtDeploymentPackage pkg = new GwtDeploymentPackage();
						pkg.setName("Unavailable! Please click refresh");
						pkg.setVersion(caught.getLocalizedMessage());
						packagesDataProvider.getList().add(pkg);
					}

					@Override
					public void onSuccess(List<GwtDeploymentPackage> result) {
						for(GwtDeploymentPackage pair : result){
							packagesDataProvider.getList().add(pair);
						}
						packagesDataProvider.flush();
					}});
				
			}
			
		});
			
		if(packagesDataProvider.getList().size()==0){
			packagesGrid.setVisible(false);
			notification.setVisible(true);
			notification.setText(MSGS.devicePackagesNone());
		}else {
			packagesGrid.setVisible(true);
			notification.setVisible(false);
		}
			
		
	}
}