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
package org.eclipse.kura.web.client.util;

import org.eclipse.kura.web.client.network.ToolTipBox;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class TextFieldWithButton<D> extends TextField<D> {

	private El m_wrap;
	private Button m_button;
	private int m_buttonOffset = 0;
		
	public TextFieldWithButton(Button button, int buttonOffset) {
		m_button = button;
		m_buttonOffset = buttonOffset;
	}
	
	@Override
	protected void doAttachChildren() {
		super.doAttachChildren();
		ComponentHelper.doAttach(m_button);
	}

	@Override
	protected void doDetachChildren() {
		super.doDetachChildren();
		ComponentHelper.doDetach(m_button);
	}

	@Override
	protected El getInputEl() {
		return input;
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		m_wrap.removeStyleName(fieldStyle);
		if (GXT.isIE) {
			int y1, y2;
			if ((y1 = input.getY()) != (y2 = el().getParent().getY())) {
				int dif = y2 - y1;
				input.setTop(dif);
			}
		}
	}
	
	public void setEnabled(boolean textEnabled, boolean bottonEnabled) {
		super.setEnabled(textEnabled);
		m_button.setEnabled(bottonEnabled);
	}
	
	public void setIcon(AbstractImagePrototype icon) {
		m_button.setIcon(icon);
	}
	
	@Override
	protected void onRender(Element target, int index) {
		m_wrap = new El(DOM.createDiv());
		m_wrap.addStyleName("x-form-field-wrap");
		m_wrap.addStyleName("x-form-file-wrap");
		
		if (isPassword()) {
			input = new El(DOM.createInputPassword());
		} else {
			input = new El(DOM.createInputText());
		}
		input.addStyleName(fieldStyle);
		input.addStyleName("x-form-file-text");
		input.setStyleAttribute("color", "#000000");
		m_wrap.appendChild(input.dom);
		
		setElement(m_wrap.dom, target, index);
		super.onRender(target, index);

		m_button.addStyleName("x-form-file-btn");
		m_button.render(m_wrap.dom);

		if (width == null) {
			setWidth(150);
		}
	}

	@Override
	protected void onResize(int width, int height) {
		super.onResize(width, height);	
		input.setWidth(m_wrap.getWidth() - m_button.el().getWidth() - m_buttonOffset);
	}
}
