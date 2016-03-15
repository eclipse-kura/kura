package org.eclipse.kura.web.client.ui.Network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.RadioButton;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
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

public class TabModemGpsUi extends Composite implements NetworkTab {
	
	private static TabModemGpsUiUiBinder uiBinder = GWT.create(TabModemGpsUiUiBinder.class);
	interface TabModemGpsUiUiBinder extends UiBinder<Widget, TabModemGpsUi> {
	}
	private static final Messages MSGS = GWT.create(Messages.class);
	
	//private static final Logger logger = Logger.getLogger(TabModemGpsUi.class.getSimpleName());
	
	GwtSession session;
	boolean dirty;
	GwtModemInterfaceConfig selectedModemIfConfig;
	boolean formInitialized;
	
	@UiField
	FormLabel labelGps;
	@UiField
	RadioButton radio1, radio2;
	@UiField
	PanelHeader helpTitle;
	@UiField
	ScrollPanel helpText;
	@UiField
	FieldSet field;
	
	public TabModemGpsUi(GwtSession currentSession) {
		initWidget(uiBinder.createAndBindUi(this));
		session = currentSession;
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
	
	@Override
	public boolean isValid(){
		return true;
	}
	
	@Override
	public void setNetInterface(GwtNetInterfaceConfig config) {
		dirty = true;
		if (config instanceof GwtModemInterfaceConfig) {
			selectedModemIfConfig = (GwtModemInterfaceConfig) config;
		}
	}
	
	@Override
	public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
		GwtModemInterfaceConfig updatedModemNetIf = (GwtModemInterfaceConfig) updatedNetIf;
		if (formInitialized) {
			updatedModemNetIf.setGpsEnabled(radio1.getValue());
		} else {
			// initForm hasn't been called yet
			updatedModemNetIf.setGpsEnabled(selectedModemIfConfig.isGpsEnabled());
		}
	}

	@Override
	public void refresh() {
		if (isDirty()) {
			setDirty(false);
			if (selectedModemIfConfig == null) {
				reset();
			} else {
				update();
			}
		}
	}
	
	// ----Private Methods----
	private void initForm() {
		// ENABLE GPS
		labelGps.setText(MSGS.netModemEnableGps());
		radio1.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio1.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netModemToolTipEnableGps()));
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
					helpText.add(new Span(MSGS.netModemToolTipEnableGps()));
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
				dirty = true;
		}});
		radio2.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				dirty = true;
		}});
				
		helpTitle.setText("Help Text");		
		radio1.setText(MSGS.trueLabel());
		radio2.setText(MSGS.falseLabel());		
		radio1.setValue(true);
		radio2.setValue(false);
		formInitialized = true;
	}
	
	private void resetHelp() {
		helpText.clear();
		helpText.add(new Span("Mouse over enabled items on the left to see help text."));
	}
	
	private void update() {
		if (selectedModemIfConfig != null) {
			if (selectedModemIfConfig.isGpsEnabled()) {
				radio1.setActive(true);
				radio2.setActive(false);
			} else {
				radio1.setActive(false);
				radio2.setActive(true);
			}
		}
		refreshForm();
	}
	
	private void refreshForm() {
		if (selectedModemIfConfig.isGpsSupported()) {
			radio1.setEnabled(true);
			radio2.setEnabled(true);
		} else {
			radio1.setEnabled(false);
			radio2.setEnabled(false);
		}
	}
	
	private void reset() {
		radio1.setActive(true);
		radio2.setActive(false);
		/*
		radio1.setActive(false);
		radio2.setActive(true);
		*/
		update();
	}
}
