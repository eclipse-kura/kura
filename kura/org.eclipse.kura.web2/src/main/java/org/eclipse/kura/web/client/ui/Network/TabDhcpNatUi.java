package org.eclipse.kura.web.client.bootstrap.ui.Network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.client.util.ValidationUtils.FieldType;
import org.eclipse.kura.web.shared.model.GwtBSNetIfType;
import org.eclipse.kura.web.shared.model.GwtBSNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtBSSession;
import org.eclipse.kura.web.shared.model.GwtBSWifiWirelessMode;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.RadioButton;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
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
import com.google.gwt.user.client.ui.Widget;

public class TabDhcpNatUi extends Composite implements Tab {

	private static TabDhcpNatUiUiBinder uiBinder = GWT
			.create(TabDhcpNatUiUiBinder.class);

	interface TabDhcpNatUiUiBinder extends UiBinder<Widget, TabDhcpNatUi> {
	}

	private static final String REGEX_IPV4 = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
	private static final Messages MSGS = GWT.create(Messages.class);
	GwtBSSession session;
	TabTcpIpUi tcpTab;
	TabWirelessUi wirelessTab;
	Boolean dirty;
	GwtBSNetInterfaceConfig selectedNetIfConfig;

	@UiField
	Form form;
	@UiField
	FormLabel labelRouter, labelBegin, labelEnd, labelSubnet, labelDefaultL,
			labelMax, labelPass;
	@UiField
	ListBox router;
	@UiField
	TextBox begin, end, subnet, defaultL, max;
	@UiField
	RadioButton radio1, radio2;
	@UiField
	FormGroup groupRouter, groupBegin, groupEnd, groupSubnet, groupDefaultL,
			groupMax;
	@UiField
	HelpBlock helpRouter;
	@UiField
	PanelHeader helpTitle;
	@UiField
	PanelBody helpText;

	public TabDhcpNatUi(GwtBSSession currentSession, TabTcpIpUi tcp,
			TabWirelessUi wireless) {
		initWidget(uiBinder.createAndBindUi(this));
		tcpTab = tcp;
		wirelessTab = wireless;
		session = currentSession;
		setDirty(false);
		initForm();
	}

	@Override
	public void setDirty(boolean flag) {
		dirty = flag;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public boolean isValid() {
		if (groupRouter.getValidationState().equals(ValidationState.ERROR)
				|| groupBegin.getValidationState()
						.equals(ValidationState.ERROR)
				|| groupEnd.getValidationState().equals(ValidationState.ERROR)
				|| groupSubnet.getValidationState().equals(
						ValidationState.ERROR)
				|| groupDefaultL.getValidationState().equals(
						ValidationState.ERROR)
				|| groupMax.getValidationState().equals(ValidationState.ERROR)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void setNetInterface(GwtBSNetInterfaceConfig config) {
		selectedNetIfConfig = config;
	}

	@Override
	public void refresh() {
		if (isDirty()) {
			setDirty(false);
			if (selectedNetIfConfig == null) {
				reset();
			} else {
				update();
			}
		}
	}

	public void getUpdatedNetInterface(GwtBSNetInterfaceConfig updatedNetIf) {
		if (session != null) {

			for (GwtBSNetRouterMode mode : GwtBSNetRouterMode.values()) {
				if (MessageUtils.get(mode.name()).equals(
						router.getSelectedItemText())) {
					updatedNetIf.setRouterMode(mode.name());
				}
			}
			updatedNetIf.setRouterDhcpBeginAddress(begin.getText());
			updatedNetIf.setRouterDhcpEndAddress(end.getText());
			updatedNetIf.setRouterDhcpSubnetMask(subnet.getText());
			if (defaultL.getText() != null) {
				updatedNetIf.setRouterDhcpDefaultLease(Integer
						.parseInt(defaultL.getText()));
			}
			if (max.getText() != null) {
				updatedNetIf.setRouterDhcpMaxLease(Integer.parseInt(max
						.getText()));
			}
			updatedNetIf.setRouterDnsPass(radio1.isActive());

		}
	}

	// ----------PRIVATE METHODS-------------

	private void update() {
		if (selectedNetIfConfig != null) {
			for (int i = 0; i < router.getItemCount(); i++) {
				if (router.getItemText(i).equals(
						MessageUtils.get(selectedNetIfConfig.getRouterMode()))) {
					router.setSelectedIndex(i);
				}
			}
			begin.setText(selectedNetIfConfig.getRouterDhcpBeginAddress());
			end.setText(selectedNetIfConfig.getRouterDhcpEndAddress());
			subnet.setText(selectedNetIfConfig.getRouterDhcpSubnetMask());
			defaultL.setText(String.valueOf(selectedNetIfConfig
					.getRouterDhcpDefaultLease()));
			max.setText(String.valueOf(selectedNetIfConfig
					.getRouterDhcpMaxLease()));
			radio1.setActive(selectedNetIfConfig.getRouterDnsPass());
			radio2.setActive(!selectedNetIfConfig.getRouterDnsPass());

		}
		refreshForm();
	}

	// enable/disable fields depending on values in other tabs
	private void refreshForm() {
		if (!tcpTab.isLanEnabled()) {
			router.setEnabled(false);
			begin.setEnabled(false);
			end.setEnabled(false);
			subnet.setEnabled(false);
			defaultL.setEnabled(false);
			max.setEnabled(false);
			radio1.setEnabled(false);
			radio2.setEnabled(false);
		} else {
			GwtBSWifiWirelessMode wirelessMode = wirelessTab.getWirelessMode();
			if (selectedNetIfConfig.getHwTypeEnum() == GwtBSNetIfType.WIFI
					&& (wirelessMode == GwtBSWifiWirelessMode.netWifiWirelessModeStation || wirelessMode == GwtBSWifiWirelessMode.netWifiWirelessModeDisabled)) {
				router.setEnabled(false);
				begin.setEnabled(false);
				end.setEnabled(false);
				subnet.setEnabled(false);
				defaultL.setEnabled(false);
				max.setEnabled(false);
				radio1.setEnabled(false);
				radio2.setEnabled(false);
			} else {
				router.setEnabled(true);
				begin.setEnabled(true);
				end.setEnabled(true);
				subnet.setEnabled(true);
				defaultL.setEnabled(true);
				max.setEnabled(true);
				radio1.setEnabled(true);
				radio2.setEnabled(true);

				String modeValue = router.getSelectedItemText();
				if (modeValue == MessageUtils
						.get(GwtBSNetRouterMode.netRouterNat.name())
						|| modeValue == MessageUtils
								.get(GwtBSNetRouterMode.netRouterOff.name())) {
					router.setEnabled(true);
					begin.setEnabled(false);
					end.setEnabled(false);
					subnet.setEnabled(false);
					defaultL.setEnabled(false);
					max.setEnabled(false);
					radio1.setEnabled(false);
					radio2.setEnabled(false);
				} else {
					router.setEnabled(true);
					begin.setEnabled(true);
					end.setEnabled(true);
					subnet.setEnabled(true);
					defaultL.setEnabled(true);
					max.setEnabled(true);
					radio1.setEnabled(true);
					radio2.setEnabled(true);
				}
			}
		}
	}

	private void reset() {
		router.setSelectedIndex(0);
		begin.setText("");
		end.setText("");
		subnet.setText("");
		defaultL.setText("");
		max.setText("");
		radio1.setActive(true);
		radio2.setActive(false);
		update();
	}

	private void initForm() {
		// Router Mode
		labelRouter.setText(MSGS.netRouterMode());
		int i = 0;
		for (GwtBSNetRouterMode mode : GwtBSNetRouterMode.values()) {
			router.addItem(MessageUtils.get(mode.name()));
			if (tcpTab.isDhcp() && mode.equals(GwtBSNetRouterMode.netRouterOff)) {
				router.setSelectedIndex(i);
			}
			i++;
		}
		router.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netRouterToolTipMode()));
				}
			}
		});
		router.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		router.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				ListBox box = (ListBox) event.getSource();
				if (tcpTab.isDhcp()
						&& !box.getSelectedItemText().equals(
								MessageUtils
										.get(GwtBSNetRouterMode.netRouterOff
												.toString()))) {
					groupRouter.setValidationState(ValidationState.ERROR);
					helpRouter.setText(MSGS.netRouterConfiguredForDhcpError());
					helpRouter.setColor("red");
				} else {
					groupRouter.setValidationState(ValidationState.NONE);
					helpRouter.setText("");
				}
			}
		});

		// DHCP Beginning Address
		labelBegin.setText(MSGS.netRouterDhcpBeginningAddress());
		begin.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netRouterToolTipDhcpBeginAddr()));
				}
			}
		});
		begin.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		begin.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				if (!begin.getText().matches(REGEX_IPV4)) {
					groupBegin.setValidationState(ValidationState.ERROR);
				} else {
					groupBegin.setValidationState(ValidationState.NONE);
				}
			}
		});

		// DHCP Ending Address
		labelEnd.setText(MSGS.netRouterDhcpEndingAddress());
		end.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netRouterToolTipDhcpEndAddr()));
				}
			}
		});
		end.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		end.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				if (!end.getText().matches(REGEX_IPV4)) {
					groupEnd.setValidationState(ValidationState.ERROR);
				} else {
					groupEnd.setValidationState(ValidationState.NONE);
				}
			}
		});

		// DHCP Subnet Mask
		labelSubnet.setText(MSGS.netRouterDhcpSubnetMask());
		subnet.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netRouterToolTipDhcpSubnet()));
				}
			}
		});
		subnet.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		subnet.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				if (!subnet.getText().matches(REGEX_IPV4)) {
					groupSubnet.setValidationState(ValidationState.ERROR);
				} else {
					groupSubnet.setValidationState(ValidationState.NONE);
				}
			}
		});

		// DHCP Default Lease
		labelDefaultL.setText(MSGS.netRouterDhcpDefaultLease());
		defaultL.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS
							.netRouterToolTipDhcpDefaultLeaseTime()));
				}
			}
		});
		defaultL.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		defaultL.addValueChangeHandler(new ValueChangeHandler<String>(){

			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				if(defaultL.getText().trim().matches(FieldType.NUMERIC.getRegex())){
					groupDefaultL.setValidationState(ValidationState.ERROR);
				}else{
					groupDefaultL.setValidationState(ValidationState.NONE);
				}
			}});
		
		// DHCP Max Lease
		labelMax.setText(MSGS.netRouterDhcpMaxLease());
		max.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS
							.netRouterToolTipDhcpMaxLeaseTime()));
				}
			}
		});
		max.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		max.addValueChangeHandler(new ValueChangeHandler<String>(){
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setDirty(true);
				if(max.getText().trim().matches(FieldType.NUMERIC.getRegex())){
					groupMax.setValidationState(ValidationState.ERROR);
				}else{
					groupMax.setValidationState(ValidationState.NONE);
				}
			}});

		// Pass DNS
		labelPass.setText(MSGS.netRouterPassDns());
		radio1.setText(MSGS.trueLabel());
		radio2.setText(MSGS.falseLabel());
		radio1.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netRouterToolTipPassDns()));
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
				if (router.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netRouterToolTipPassDns()));
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

		
		helpTitle.setText("Help Text");
	}

	private void resetHelp() {
		helpText.clear();
		helpText.add(new Span(
				"Mouse over enabled items on the left to see help text."));
	}
}
