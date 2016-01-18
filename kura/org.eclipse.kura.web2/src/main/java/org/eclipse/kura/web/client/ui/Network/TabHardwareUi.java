package org.eclipse.kura.web.client.ui.Network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class TabHardwareUi extends Composite implements Tab {

	private static TabHardwareUiUiBinder uiBinder = GWT
			.create(TabHardwareUiUiBinder.class);

	interface TabHardwareUiUiBinder extends UiBinder<Widget, TabHardwareUi>  {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	GwtSession session;
	GwtNetInterfaceConfig SelectednetIfConfig;
	
	@UiField
	FormLabel labelState,labelName,labelType,labelHardware,labelSerial,labelDriver,labelVersion,labelFirmware,labelMtu,labelUsb,labelRssi;
	@UiField
	FormControlStatic state,name,type,hardware,serial,driver,version,firmware,mtu,usb,rssi;
	
	public TabHardwareUi(GwtSession currentSession) {
		initWidget(uiBinder.createAndBindUi(this));
		session=currentSession;
		setDirty(false);
		
		//Set Labels
		labelState.setText(MSGS.netHwState());
		labelName.setText(MSGS.netHwName());
		labelType.setText(MSGS.netHwType());
		labelHardware.setText(MSGS.netHwAddress());
		labelSerial.setText(MSGS.netHwSerial());
		labelDriver.setText(MSGS.netHwDriver());
		labelVersion.setText(MSGS.netHwVersion());
		labelFirmware.setText(MSGS.netHwFirmware());
		labelMtu.setText(MSGS.netHwMTU());
		labelUsb.setText(MSGS.netHwUSBDevice());
		labelRssi.setText(MSGS.netHwSignalStrength());
	}

	//Dirty flag not needed here since this tab is not modifiable
	public void setDirty(boolean flag){	
	}
	
	public boolean isDirty(){
		return false;
	}
	
	public boolean isValid(){
		return true;
	}
	
	public void setNetInterface(GwtNetInterfaceConfig config){
		SelectednetIfConfig=config;
	}

	public void refresh(){
		if(SelectednetIfConfig!=null){
			loadData();
		}else {
			reset();
		}
	}
	
	
	
	/*********Private Methods********/
	
	private void loadData(){		
		state.setText(SelectednetIfConfig.getHwState());
		name.setText(SelectednetIfConfig.getHwName());
		type.setText(SelectednetIfConfig.getHwType());
		hardware.setText(SelectednetIfConfig.getHwAddress());
		serial.setText(SelectednetIfConfig.getHwSerial());
		driver.setText(SelectednetIfConfig.getHwDriver());
		version.setText(SelectednetIfConfig.getHwDriverVersion());
		firmware.setText(SelectednetIfConfig.getHwFirmware());
		mtu.setText(String.valueOf(SelectednetIfConfig.getHwMTU()));
		usb.setText(SelectednetIfConfig.getHwUsbDevice());
		rssi.setText(SelectednetIfConfig.getHwRssi());
	}
	
	private void reset(){
		state.setText("");
		name.setText("");
		type.setText("");
		hardware.setText("");
		serial.setText("");
		driver.setText("");
		version.setText("");
		firmware.setText("");
		mtu.setText("");
		usb.setText("");
		rssi.setText("");
	}

	public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
		if(session!=null){
			updatedNetIf.setHwState(state.getText());
			updatedNetIf.setHwName(name.getText());
			updatedNetIf.setHwType(type.getText());
			updatedNetIf.setHwAddress(hardware.getText());
			updatedNetIf.setHwSerial(serial.getText());
			updatedNetIf.setHwDriver(driver.getText());
			updatedNetIf.setHwDriverVersion(version.getText());
			updatedNetIf.setHwFirmware(firmware.getText());
			if(mtu.getText()!=null){
				updatedNetIf.setHwMTU(Integer.parseInt(mtu.getText()));
			}
			updatedNetIf.setHwUsbDevice(usb.getText());
			updatedNetIf.setHwRssi(rssi.getText());
		}
	}
}
