/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.resources.Resources;
import org.eclipse.kura.web.client.util.ConfirmPasswordFieldValidator;
import org.eclipse.kura.web.client.util.Constants;
import org.eclipse.kura.web.client.util.TextFieldValidator;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtSettings;
import org.eclipse.kura.web.shared.service.GwtSettingService;
import org.eclipse.kura.web.shared.service.GwtSettingServiceAsync;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class AdminPasswordTab extends LayoutContainer 
{
	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtSettingServiceAsync gwtSettingService = GWT.create(GwtSettingService.class);
	
    @SuppressWarnings("unused")
	private GwtSession        m_currentSession;

	private FormPanel         m_formPanel;

    private ToolBar           m_toolBar;
    private Button            m_apply;

    private TextField<String> m_currentPassword;
	private TextField<String> m_newPassword;

	
    public AdminPasswordTab(GwtSession currentSession) {
    	m_currentSession = currentSession;
    }

	protected void onRender(final Element parent, int index) {
		
		super.onRender(parent, index);
        
		//
		// Borderlayout that expands to the whole screen
		setLayout(new FitLayout());
		setBorders(false);
		setId("settings-admin-password");
		
        LayoutContainer mf = new LayoutContainer();
        mf.setLayout(new BorderLayout());
		
		//
		// Center Panel: Open Ports Table
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER, 1F);
		centerData.setMargins(new Margins(0, 0, 0, 0));
		centerData.setSplit(true);  
		centerData.setMinSize(0);
		
		ContentPanel openPortsTablePanel = new ContentPanel();
		openPortsTablePanel.setBorders(false);
		openPortsTablePanel.setBodyBorder(false);
		openPortsTablePanel.setHeaderVisible(false);
		openPortsTablePanel.setScrollMode(Scroll.AUTO);
		openPortsTablePanel.setLayout(new FitLayout());
		
		initToolBar();
		initAdminPasswordForm();
        
		openPortsTablePanel.setTopComponent(m_toolBar);
		openPortsTablePanel.add(m_formPanel);
		mf.add(openPortsTablePanel, centerData);
		
        add(mf);

		refresh();
	}

	
    public void refresh() {
    	// Nothing to refresh in this case
    	// The current password must be supplied in 
    	// order to be able to change it to a new one.
    }

    
    public boolean isDirty() {
    	if (m_formPanel != null) {
    		return m_formPanel.isDirty();
    	}
    	return false;
    }
    
    
    private void initToolBar() 
    {	
        m_toolBar = new ToolBar();
        m_toolBar.setBorders(true);
        m_toolBar.setId("settings-admin-password-toolbar");
        
        m_apply = new Button(MSGS.apply(), 
        		AbstractImagePrototype.create(Resources.INSTANCE.accept()),
                new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent ce) {
                apply();
            }
        });

        m_toolBar.add(m_apply);
        m_toolBar.add(new SeparatorToolItem());
    }

    
    private void initAdminPasswordForm()
    {
        FormData formData = new FormData("-30");

        m_formPanel = new FormPanel();
        m_formPanel.setFrame(false);
        m_formPanel.setBodyBorder(false);
        m_formPanel.setHeaderVisible(false);
        m_formPanel.setScrollMode(Scroll.AUTOY);
        m_formPanel.setLayout(new FlowLayout());

        FieldSet fieldSet = new FieldSet();
        fieldSet.setHeading(MSGS.settingsChangeAdmin());
        FormLayout layoutAccount = new FormLayout();
        layoutAccount.setLabelWidth(Constants.LABEL_WIDTH_FORM);
        fieldSet.setLayout(layoutAccount);
        
        //
    	// current password
        //
        m_currentPassword = new TextField<String>();
        m_currentPassword.setAllowBlank(false);
        m_currentPassword.setName("currentPassword");
        m_currentPassword.setFieldLabel("* "+MSGS.settingsAdminPwdCurrent());
        m_currentPassword.setPassword(true);
        fieldSet.add(m_currentPassword, formData);

        //
    	// new password
        //
        m_newPassword = new TextField<String>();
        m_newPassword.setAllowBlank(false);
        m_newPassword.setName("newPassword");
        m_newPassword.setFieldLabel("* "+MSGS.settingsAdminPwdNew());
        m_newPassword.setValidator(new TextFieldValidator(m_newPassword, FieldType.PASSWORD));
        m_newPassword.setPassword(true);
        fieldSet.add(m_newPassword, formData);

        //
    	// confirm password
        //
        final TextField<String> confirmPassword = new TextField<String>();
        confirmPassword.setAllowBlank(false);
        confirmPassword.setName("confirmPassword");
        confirmPassword.setFieldLabel("* "+MSGS.settingsAdminPwdConfirm());
        confirmPassword.setValidator(new ConfirmPasswordFieldValidator(confirmPassword, m_newPassword));
        confirmPassword.setPassword(true);
        fieldSet.add(confirmPassword, formData);

        m_formPanel.add(fieldSet);
    }

    
    private void apply()
    {
    	// make sure all visible fields are valid before performing the action
    	for (Field<?> field : m_formPanel.getFields()) {
    		if (field.isVisible() && !field.isValid()) {
        		return;
    		}
    	}
                    	
    	GwtSettings settings = new GwtSettings();
    	settings.setPasswordCurrent(m_currentPassword.getValue());
    	settings.setPasswordNew(m_newPassword.getValue());    
    	
    	gwtSettingService.updateSettings(settings, new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				if (caught instanceof GwtKuraException) {
					GwtKuraException gwtExc = (GwtKuraException) caught;
					if (GwtKuraErrorCode.CURRENT_ADMIN_PASSWORD_DOES_NOT_MATCH.equals(gwtExc.getCode())) {
						Info.display(MSGS.error(), MSGS.settingsPasswoedNotMatchError());
						return;
					}
				}
				Info.display(MSGS.error(), caught.getMessage());								
			}
			public void onSuccess(Void result) {
				Info.display(MSGS.info(), MSGS.settingsApplied());
				reset();
			}                    		
    	});
    }
    
    
    private void reset()
    {
    	// make sure all visible fields are valid before performing the action
    	for (Field<?> field : m_formPanel.getFields()) {
    		field.reset();
    	}
    }
}
