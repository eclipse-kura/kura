package org.eclipse.kura.web.client.ui.Settings;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class SettingsPanelUi extends Composite {

	private static SettingsPanelUiUiBinder uiBinder = GWT.create(SettingsPanelUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(SettingsPanelUi.class.getSimpleName());

	interface SettingsPanelUiUiBinder extends UiBinder<Widget, SettingsPanelUi> {
	}
	private static SnapshotsTabUi snapshotsBinder = GWT.create(SnapshotsTabUi.class);
	private static SslCertificatesTabUi sslBinder = GWT.create(SslCertificatesTabUi.class);
	
	GwtSession Session;
	@UiField
	AnchorListItem snapshots, ssl;
	@UiField
	Well content;

	public SettingsPanelUi() {
		logger.log(Level.FINER, "Initiating SettingsPanelUI...");
		initWidget(uiBinder.createAndBindUi(this));
		setSelectedActive(snapshots);
		content.clear();
		content.add(snapshotsBinder);
		
		snapshots.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(snapshots);
				content.clear();
				content.add(snapshotsBinder);
				snapshotsBinder.refresh();
				
			}});
		
		ssl.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(ssl);
				content.clear();
				content.add(sslBinder);
			}});
	
	}

	public void load() {
		setSelectedActive(snapshots);
		content.clear();
		content.add(snapshotsBinder);
		snapshotsBinder.refresh();
	}
	
	public void setSession(GwtSession currentSession) {
		Session = currentSession;
	}

	public void setSelectedActive(AnchorListItem item){
		snapshots.setActive(false);
		ssl.setActive(false);
		item.setActive(true);
		
	}

}
