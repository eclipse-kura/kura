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
package org.eclipse.kura.web.client;

import org.eclipse.kura.web.client.util.UserAgentUtils;
import org.eclipse.kura.web.shared.model.GwtSession;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.layout.TableLayout;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class NorthView extends LayoutContainer 
{

	@SuppressWarnings("unused")
	private GwtSession currentSession;
	
	public NorthView(GwtSession currentSession) {
		this.currentSession = currentSession;
		setId("north-panel-wrapper");
	}
	
    protected void onRender(Element parent, int index) {
    	
        super.onRender(parent, index);
        
        ContentPanel panel = new ContentPanel();
        panel.setBorders(true);
        panel.setBodyBorder(false);
        panel.setStyleAttribute("backgroundColor", "white");
        panel.setHeaderVisible(false);
        
        TableLayout layout = new TableLayout(3);
        layout.setWidth("100%");
        panel.setLayout(layout);
        
        panel.add(getKuraHeader(), new TableData(Style.HorizontalAlignment.LEFT,  Style.VerticalAlignment.MIDDLE));
        //panel.add(getWelcome(),        new TableData(Style.HorizontalAlignment.RIGHT, Style.VerticalAlignment.TOP));
        
        add(panel);
    }
    
    private Widget getKuraHeader() {
    	SimplePanel logo = new SimplePanel();
    	if (!UserAgentUtils.isIE() || UserAgentUtils.getIEDocumentMode() > 8) {
    		logo.setStyleName("headerLogo");
    	} else {
    		logo.setStyleName("headerLogo-ie8");
    	}
    	return logo;
    }
}
