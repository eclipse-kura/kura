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
package org.eclipse.kura.web.client.network;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class ToolTipBox extends Composite implements HasText {

	private static ToolTipBoxUiBinder uiBinder = GWT
			.create(ToolTipBoxUiBinder.class);

	interface ToolTipBoxUiBinder extends UiBinder<Widget, ToolTipBox> {
	}

	public ToolTipBox() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@UiField
	HTML textArea;
	@UiField
	Label label;

	public ToolTipBox(String height) {
		initWidget(uiBinder.createAndBindUi(this));
		textArea.setHeight(height);
		label.setText("Help Text");
	}
	
	public ToolTipBox(String height, String top) {
		initWidget(uiBinder.createAndBindUi(this));
		textArea.setHeight(height);
		label.setText("Help Text");
		textArea.getElement().getStyle().setTop(66, Style.Unit.PX);
	}

	public void setText(String text) {
		textArea.setHTML(text);
	}

	public String getText() {
		return null;
	}

}
