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
package org.eclipse.kura.web.client.ui.Network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.RadioButton;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabModemUi extends Composite implements NetworkTab {

	private static final String MODEM_AUTH_NONE_MESSAGE = MessageUtils.get(GwtModemAuthType.netModemAuthNONE.name());
	private static TabModemUiUiBinder uiBinder = GWT.create(TabModemUiUiBinder.class);

	interface TabModemUiUiBinder extends UiBinder<Widget, TabModemUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private static final String REGEX_NUM = "(?:\\d*)?\\d+";

	private GwtSession session;
	private TabTcpIpUi tcpTab;
	private boolean dirty;
	private GwtModemInterfaceConfig selectedNetIfConfig;
	private final Map<String, String> defaultDialString = new HashMap<String, String>();
	private String dialString;

	@UiField
	FormGroup groupReset, groupMaxfail, groupIdle, groupInterval, groupFailure,
	groupNumber, groupDial,groupApn;
	@UiField
	FormLabel labelModel, labelNetwork, labelService, labelModem, labelNumber,
	labelDial, labelApn, labelAuth, labelUsername, labelPassword,
	labelReset, labelPersist, labelMaxfail, labelIdle, labelActive,
	labelInterval, labelFailure;
	@UiField
	HelpBlock helpReset, helpMaxfail, helpIdle, helpInterval, helpFailure, helpNumber;

	@UiField
	ListBox network, auth;
	@UiField
	TextBox modem, number, dial, apn, username, reset, maxfail, idle, active,
	interval, failure;
	@UiField
	FormControlStatic model, service;
	@UiField
	Input password;
	@UiField
	RadioButton radio1, radio2;
	@UiField
	PanelHeader helpTitle;
	@UiField
	ScrollPanel helpText;
	@UiField
	FieldSet field;

	public TabModemUi(GwtSession currentSession, TabTcpIpUi tcp) {
		initWidget(uiBinder.createAndBindUi(this));
		session = currentSession;
		tcpTab = tcp;

		defaultDialString.put("HE910", "atd*99***1#");
		defaultDialString.put("DE910", "atd#777");
		initForm();

		tcpTab.status.addChangeHandler(new ChangeHandler(){
			@Override
			public void onChange(ChangeEvent event) {
				update();
			}});
	}

	@Override
	public void setDirty(boolean flag) {
		dirty = flag;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public boolean isValid(){
		if (number.getText() == null || "".equals(number.getText().trim())) {
			groupNumber.setValidationState(ValidationState.ERROR);
		}
		if (dial.getText() == null || "".equals(dial.getText().trim())) {
			groupDial.setValidationState(ValidationState.ERROR);
		}
		if (apn.getText() == null || "".equals(apn.getText().trim())) {
			if (apn.isEnabled()) {
				groupApn.setValidationState(ValidationState.ERROR);
			}
		}
		if (maxfail.getText() == null || "".equals(maxfail.getText().trim())) {
			groupMaxfail.setValidationState(ValidationState.ERROR);
		}
		if (idle.getText() == null || "".equals(idle.getText().trim())) {
			groupIdle.setValidationState(ValidationState.ERROR);
		}
		if (interval.getText() == null || "".equals(interval.getText().trim())) {
			groupInterval.setValidationState(ValidationState.ERROR);
		}
		if (failure.getText() == null || "".equals(failure.getText().trim())) {
			groupFailure.setValidationState(ValidationState.ERROR);
		}

		if( groupNumber.getValidationState().equals(ValidationState.ERROR)   ||
				groupDial.getValidationState().equals(ValidationState.ERROR)     ||
				groupApn.getValidationState().equals(ValidationState.ERROR)      || 
				groupMaxfail.getValidationState().equals(ValidationState.ERROR)  ||
				groupIdle.getValidationState().equals(ValidationState.ERROR)     ||
				groupInterval.getValidationState().equals(ValidationState.ERROR) ||
				groupFailure.getValidationState().equals(ValidationState.ERROR)  ){
			return false;
		}else{
			return true;
		}
	}

	@Override
	public void setNetInterface(GwtNetInterfaceConfig config) {
		setDirty(true);
		if (config instanceof GwtModemInterfaceConfig) {
			selectedNetIfConfig = (GwtModemInterfaceConfig) config;
		}
	}

	@Override
	public void refresh() {
		if (isDirty()) {
			setDirty(false);
			resetValidations();
			if (selectedNetIfConfig == null) {
				reset();
			} else {
				update();
			}
		}
	}

	public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
		GwtModemInterfaceConfig updatedModemNetIf = (GwtModemInterfaceConfig) updatedNetIf;

		if (model.getText() != null && service.getText() != null) {
			// note - status is set in tcp/ip tab
			updatedModemNetIf.setPppNum(Integer.parseInt(number.getText()));
			updatedModemNetIf.setModemId((modem.getText().trim() != null) ? modem.getText().trim() : "");
			updatedModemNetIf.setDialString((dial.getText().trim() != null) ? dial.getText().trim() : "");
			updatedModemNetIf.setApn((apn.getText().trim() != null) ? apn.getText().trim() : "");

			String authValue = auth.getSelectedValue();
			for (GwtModemAuthType authT : GwtModemAuthType.values()) {
				if (MessageUtils.get(authT.name()).equals(authValue)) {
					updatedModemNetIf.setAuthType(authT);
				}
			}

			if (updatedModemNetIf.getAuthType() != GwtModemAuthType.netModemAuthNONE) {
				updatedModemNetIf.setUsername((username.getText().trim() != null) ? username.getText().trim() : "");
				updatedModemNetIf.setPassword((password.getText().trim() != null) ? password.getText().trim() : "");
			}

			updatedModemNetIf.setResetTimeout(Integer.parseInt(reset.getValue().trim()));
			updatedModemNetIf.setPersist(radio1.getValue());
			updatedModemNetIf.setMaxFail(Integer.parseInt(maxfail.getText().trim()));
			updatedModemNetIf.setIdle(Integer.parseInt(idle.getText().trim()));
			updatedModemNetIf.setActiveFilter((active.getText() != "") ? active.getText().trim() : "");
			updatedModemNetIf.setLcpEchoInterval(Integer.parseInt(interval.getText().trim()));
			updatedModemNetIf.setLcpEchoFailure(Integer.parseInt(failure.getText().trim()));
			// ---
		} else {
			// initForm hasn't been called yet

			updatedModemNetIf.setPppNum(selectedNetIfConfig.getPppNum());
			updatedModemNetIf.setModemId(selectedNetIfConfig.getModemId());
			updatedModemNetIf.setDialString(selectedNetIfConfig.getDialString());
			updatedModemNetIf.setApn(selectedNetIfConfig.getApn());
			updatedModemNetIf.setAuthType(selectedNetIfConfig.getAuthType());
			updatedModemNetIf.setUsername(selectedNetIfConfig.getUsername());
			updatedModemNetIf.setPassword(selectedNetIfConfig.getPassword());
			updatedModemNetIf.setResetTimeout(selectedNetIfConfig.getResetTimeout());
			updatedModemNetIf.setPersist(selectedNetIfConfig.isPersist());
			updatedModemNetIf.setMaxFail(selectedNetIfConfig.getMaxFail());
			updatedModemNetIf.setIdle(selectedNetIfConfig.getIdle());
			updatedModemNetIf.setActiveFilter(selectedNetIfConfig.getActiveFilter());
			updatedModemNetIf.setLcpEchoInterval(selectedNetIfConfig.getLcpEchoInterval());
			updatedModemNetIf.setLcpEchoFailure(selectedNetIfConfig.getLcpEchoFailure());
		}

	}

	// ----Private Methods----
	private void initForm() {

		// MODEL
		labelModel.setText(MSGS.netModemModel());

		// NETWORK TECHNOLOGY
		labelNetwork.setText(MSGS.netModemNetworkTechnology());
		network.addItem(MSGS.unknown());
		network.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (network.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipNetworkTopology()));
				}
			}
		});
		network.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		network.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				refreshForm();
			}
		});

		// CONNECTION TYPE
		labelService.setText(MSGS.netModemConnectionType());

		// MODEM IDENTIFIER
		labelModem.setText(MSGS.netModemIdentifier());
		modem.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (modem.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipModemIndentifier()));
				}
			}
		});
		modem.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		modem.addValueChangeHandler(new ValueChangeHandler<String>(){

			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
			}});

		// INTERFACE NUMBER
		labelNumber.setText(MSGS.netModemInterfaceNum() + "*");
		number.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if ( number.getText().trim() != null && 
						( !number.getText().trim().matches(REGEX_NUM) || 
								Integer.parseInt(number.getText()) < 0) ) {
					helpNumber.setText("This Field requires a numeric input");
					groupNumber.setValidationState(ValidationState.ERROR);
				}else{
					helpNumber.setText("");
					groupNumber.setValidationState(ValidationState.NONE);
				}
			}
		});
		number.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (number.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipModemInterfaceNumber()));
				}
			}
		});
		number.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});

		// DIAL STRING
		labelDial.setText(MSGS.netModemDialString()+"*");

		dial.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		dialString = "";
		String model = "";
		if (selectedNetIfConfig != null) {
			model = selectedNetIfConfig.getModel();
			if (model != null && model.length() > 0) {
				if (model.contains("HE910")) {
					dialString = defaultDialString.get("HE910");
				} else if (model.contains("DE910")) {
					dialString = defaultDialString.get("DE910");
				} else {
					dialString = "";
				}
			}
		}
		dial.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (dial.isEnabled()) {
					helpText.clear();
					if (dialString.equals("")) {
						helpText.add(new Span(MSGS.netModemToolTipDialStringDefault()));
					} else {
						helpText.add(new Span(MSGS.netModemToolTipDialString(dial.getText())));
					}
				}
			}
		});
		dial.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if (dial.getText() == null || "".equals(dial.getText().trim())) {
					groupDial.setValidationState(ValidationState.ERROR);
				}else{
					groupDial.setValidationState(ValidationState.NONE);
				}
			}});

		// APN
		labelApn.setText(MSGS.netModemAPN()+"*");
		apn.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (apn.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipApn()));
				}
			}
		});
		apn.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		apn.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if(apn.getText() == null || "".equals(apn.getText().trim())){
					if (apn.isEnabled()) {
						groupApn.setValidationState(ValidationState.ERROR);
					} else {
						groupApn.setValidationState(ValidationState.NONE);
					}
				} else{
					groupApn.setValidationState(ValidationState.NONE);
				}
			}});

		// AUTH TYPE
		labelAuth.setText(MSGS.netModemAuthType());
		for (GwtModemAuthType a : GwtModemAuthType.values()) {
			auth.addItem(MessageUtils.get(a.name()));
		}
		auth.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (auth.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipAuthentication()));
				}
			}
		});
		auth.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		auth.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				refreshForm();
			}
		});

		// USERNAME
		labelUsername.setText(MSGS.netModemUsername());
		username.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (username.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipUsername()));
				}
			}
		});
		username.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		username.addValueChangeHandler(new ValueChangeHandler<String>(){
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
			}});

		// PASSWORD
		labelPassword.setText(MSGS.netModemPassword());
		password.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (network.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipPassword()));
				}
			}
		});
		password.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		password.addValueChangeHandler(new ValueChangeHandler<String>(){
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
			}});

		// MODEM RESET TIMEOUT
		labelReset.setText(MSGS.netModemResetTimeout()+"*");
		reset.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (reset.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipResetTimeout()));
				}
			}
		});
		reset.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		reset.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if ( reset.getText().trim() != null && 
						( !reset.getText().trim().matches(REGEX_NUM) || 
								( Integer.parseInt(reset.getText()) < 0 || 
										Integer.parseInt(reset.getText()) == 1)) ) {
					helpReset.setText(MSGS.netModemInvalidResetTimeout());
					groupReset.setValidationState(ValidationState.ERROR);
				} else {
					helpReset.setText("");
					groupReset.setValidationState(ValidationState.NONE);
				}
			}
		});

		// REOPEN CONNECTION ON TERMINATION
		labelPersist.setText(MSGS.netModemPersist());
		radio1.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio1.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipPersist()));
				}
			}
		});
		radio1.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		radio2.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio2.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipPersist()));
				}
			}
		});
		radio2.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});

		radio1.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				setDirty(true);
			}});
		radio2.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				setDirty(true);
			}});


		// CONNECTION ATTEMPTS
		labelMaxfail.setText(MSGS.netModemMaxFail()+"*");
		maxfail.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (maxfail.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipMaxFail()));
				}
			}
		});
		maxfail.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		maxfail.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if ( maxfail.getText().trim() != null && 
						( !maxfail.getText().trim().matches(REGEX_NUM) || 
								Integer.parseInt(maxfail.getText() ) <= 0) || 
						maxfail.getText().trim().length() <= 0 ) {
					helpMaxfail.setText(MSGS.netModemInvalidMaxFail());
					groupMaxfail.setValidationState(ValidationState.ERROR);
				} else {
					helpMaxfail.setText("");
					groupMaxfail.setValidationState(ValidationState.NONE);
				}
			}
		});

		// DISCONNET IF IDLE
		labelIdle.setText(MSGS.netModemIdle()+"*");
		idle.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (idle.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipIdle()));
				}
			}
		});
		idle.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		idle.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if ( idle.getText().trim() != null && 
						( !idle.getText().trim().matches(REGEX_NUM) || 
								Integer.parseInt(idle.getText()) < 0) ) {
					helpIdle.setText(MSGS.netModemInvalidIdle());
					groupIdle.setValidationState(ValidationState.ERROR);
				} else {
					helpIdle.setText("");
					groupIdle.setValidationState(ValidationState.NONE);
				}
			}
		});

		// ACTIVE FILTER
		labelActive.setText(MSGS.netModemActiveFilter());
		active.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (active.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipActiveFilter()));
				}
			}
		});
		active.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		active.addValueChangeHandler(new ValueChangeHandler<String>(){
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
			}});

		// LCP ECHO INTERVAL
		labelInterval.setText(MSGS.netModemLcpEchoInterval()+"*");
		interval.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (interval.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipLcpEchoInterval()));
				}
			}
		});
		interval.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		interval.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if ( interval.getText().trim() != null && 
						( !interval.getText().trim().matches(REGEX_NUM) || 
								Integer.parseInt(interval.getText()) < 0) ) {
					helpInterval.setText(MSGS.netModemInvalidLcpEchoInterval());
					groupInterval.setValidationState(ValidationState.ERROR);
				}else{
					groupInterval.setValidationState(ValidationState.NONE);
				}
			}
		});

		// LCP ECHO FAILURE
		labelFailure.setText(MSGS.netModemLcpEchoFailure()+"*");
		failure.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (failure.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipLcpEchoFailure()));
				}
			}
		});
		failure.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		failure.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				setDirty(true);
				if ( failure.getText().trim() != null && 
						( !failure.getText().trim().matches(REGEX_NUM) || 
								Integer.parseInt(failure.getText()) < 0) ) {
					helpFailure.setText(MSGS.netModemInvalidLcpEchoFailure());
					groupFailure.setValidationState(ValidationState.ERROR);
				} else {
					helpFailure.setText("");
					groupFailure.setValidationState(ValidationState.NONE);
				}
			}
		});

		helpTitle.setText(MSGS.netHelpTitle());
		radio1.setText(MSGS.trueLabel());
		radio2.setText(MSGS.falseLabel());

		radio1.setValue(true);
		radio2.setValue(false);
	}
	
	private void resetValidations() {
		groupApn.setValidationState(ValidationState.NONE);
		groupDial.setValidationState(ValidationState.NONE);
		groupFailure.setValidationState(ValidationState.NONE);
		groupIdle.setValidationState(ValidationState.NONE);
		groupInterval.setValidationState(ValidationState.NONE);
		groupMaxfail.setValidationState(ValidationState.NONE);
		groupNumber.setValidationState(ValidationState.NONE);
		groupReset.setValidationState(ValidationState.NONE);
		
		helpReset.setText(""); 
		helpMaxfail.setText(""); 
		helpIdle.setText(""); 
		helpInterval.setText(""); 
		helpFailure.setText(""); 
		helpNumber.setText("");
	}

	private void resetHelp() {
		helpText.clear();
		helpText.add(new Span(MSGS.netHelpDefaultHint()));
	}

	private void update() {
		if (selectedNetIfConfig != null) {
			model.setText(selectedNetIfConfig.getManufacturer() + "-" + selectedNetIfConfig.getModel());
			network.clear();
			List<String> networkTechnologies = selectedNetIfConfig.getNetworkTechnology();
			if (networkTechnologies != null && !networkTechnologies.isEmpty()) {
				for (String techType : selectedNetIfConfig.getNetworkTechnology()) {
					network.addItem(techType);
				}
			} else {
				network.addItem(MSGS.unknown());
			}
			service.setText(selectedNetIfConfig.getConnectionType());
			modem.setText(selectedNetIfConfig.getModemId());
			number.setText(String.valueOf(selectedNetIfConfig.getPppNum()));
			dial.setText(selectedNetIfConfig.getDialString());
			apn.setText(selectedNetIfConfig.getApn());

			GwtModemAuthType authType = GwtModemAuthType.netModemAuthNONE;
			if (selectedNetIfConfig.getAuthType() != null) {
				authType = selectedNetIfConfig.getAuthType();
			}
			for (int i = 0; i < auth.getItemCount(); i++) {
				if (auth.getItemText(i).equals(MessageUtils.get(authType.name()))) {
					auth.setSelectedIndex(i);
				}
			}

			username.setText(selectedNetIfConfig.getUsername());
			password.setText(selectedNetIfConfig.getPassword());
			reset.setText(String.valueOf(selectedNetIfConfig.getResetTimeout()));

			if (selectedNetIfConfig.isPersist()) {
				radio1.setActive(true);
				radio2.setActive(false);
			} else {
				radio1.setActive(false);
				radio2.setActive(true);
			}

			maxfail.setText(String.valueOf(selectedNetIfConfig.getMaxFail()));
			idle.setText(String.valueOf(selectedNetIfConfig.getIdle()));
			active.setText(selectedNetIfConfig.getActiveFilter());
			interval.setText(String.valueOf(selectedNetIfConfig.getLcpEchoInterval()));
			failure.setText(String.valueOf(selectedNetIfConfig.getLcpEchoFailure()));
		}
		refreshForm();
	}

	private void refreshForm() {
		network.setEnabled(true);
		modem.setEnabled(true);
		number.setEnabled(true);
		dial.setEnabled(true);
		apn.setEnabled(true);
		auth.setEnabled(true);
		username.setEnabled(true);
		password.setEnabled(true);
		reset.setEnabled(true);
		radio1.setEnabled(true);
		radio2.setEnabled(true);
		maxfail.setEnabled(true);
		idle.setEnabled(true);
		active.setEnabled(true);
		interval.setEnabled(true);
		failure.setEnabled(true);

		String authTypeVal = auth.getSelectedItemText().trim();

		if ( authTypeVal == null || 
				authTypeVal.equalsIgnoreCase(MODEM_AUTH_NONE_MESSAGE) ) {
			username.setEnabled(false);
			password.setEnabled(false);
		} else {
			username.setEnabled(true);
			password.setEnabled(true);
		}

		/*
		if (selectedNetIfConfig.isGpsSupported()) {
			radio1.setEnabled(true);
			radio2.setEnabled(true);
		} else {
			radio1.setEnabled(false);
			radio2.setEnabled(false);
		}
		 */

		if (selectedNetIfConfig != null) {
			for (String techType : selectedNetIfConfig.getNetworkTechnology()) {
				if (techType.equals("EVDO") || techType.equals("CDMA")) {
					apn.setEnabled(false);
					auth.setEnabled(false);
					username.setEnabled(false);
					password.setEnabled(false);
				}
			}
		}
	}

	private void reset() {
		model.setText(null);
		network.setSelectedIndex(0);
		service.setText(null);
		modem.setText(null);
		number.setText(null);
		dial.setText(null);
		apn.setText(null);
		auth.setSelectedIndex(1);
		username.setText(null);
		password.setText(null);
		reset.setText(null);
		radio1.setActive(true);
		radio2.setActive(false);
		maxfail.setText(null);
		idle.setText(null);
		active.setText(null);
		interval.setText(null);
		failure.setText(null);
		update();
	}
}
