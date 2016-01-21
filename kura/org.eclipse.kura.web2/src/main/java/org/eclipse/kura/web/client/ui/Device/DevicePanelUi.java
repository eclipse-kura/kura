package org.eclipse.kura.web.client.ui.Device;


import org.eclipse.kura.web.client.messages.Messages;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class DevicePanelUi extends Composite {

	private static DevicePanelUiUiBinder uiBinder = GWT.create(DevicePanelUiUiBinder.class);
	private static ProfileTabUi profileBinder     = GWT.create(ProfileTabUi.class);
	private static BundlesTabUi bundlesBinder     = GWT.create(BundlesTabUi.class);
	private static ThreadsTabUi threadsBinder     = GWT.create(ThreadsTabUi.class);
	
	private static SystemPropertiesTabUi systemPropertiesBinder = GWT.create(SystemPropertiesTabUi.class);
	private static CommandTabUi commandBinder = GWT.create(CommandTabUi.class);


	interface DevicePanelUiUiBinder extends UiBinder<Widget, DevicePanelUi> {
	}
	
	private static final Messages MSGS = GWT.create(Messages.class);

	@UiField
	Well content;
	@UiField
	HTMLPanel deviceIntro;
	
	@UiField
	AnchorListItem profile, bundles, threads, systemProperties, command;

	public DevicePanelUi() {
		initWidget(uiBinder.createAndBindUi(this));
		//Profile selected by Default
		deviceIntro.add(new Span("<p>"+MSGS.deviceIntro()+"</p"));
		content.clear();
		setSelectedActive(profile);
		content.add(profileBinder);
		
		profile.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(profile);
				content.clear();
				content.add(profileBinder);
				profileBinder.loadProfileData();
			}
		});

		bundles.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(bundles);
				content.clear();
				content.add(bundlesBinder);
				bundlesBinder.loadBundlesData();
			}
		});
		
		threads.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(threads);
				content.clear();
				content.add(threadsBinder);
				threadsBinder.loadThreadsData();
			}
		});
		
		systemProperties.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(systemProperties);
				content.clear();
				content.add(systemPropertiesBinder);
				systemPropertiesBinder.loadSystemPropertiesData();
			}
		});
		
		command.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				setSelectedActive(command);
				content.clear();
				content.add(commandBinder);
			}
		});
		


	}
	
	public void initDevicePanel() {
		profileBinder.loadProfileData();
	}
	
	public void setSelectedActive(AnchorListItem item){
		profile.setActive(false);
		bundles.setActive(false);
		threads.setActive(false);
		systemProperties.setActive(false);
		command.setActive(false);
		item.setActive(true);
		
	}


}
