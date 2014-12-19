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

import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.widget.Component;

public class FormUtils 
{
	public static void addDirtyFieldIcon(Component c)
	{
		El elem = c.el().findParent(".x-form-element", 6);
		El dirtyIcon= elem.createChild("");
		dirtyIcon.setStyleName("x-grid3-dirty-cell");
		dirtyIcon.setStyleAttribute("top", "0");
		dirtyIcon.setStyleAttribute("position", "absolute");
		dirtyIcon.setSize(10, 10);
		dirtyIcon.show();		
	}

	
	public static void removeDirtyFieldIcon(Component c)
	{
		El elem = c.el().findParent(".x-form-element", 6);
		El dirtyIcon= elem.child(".x-grid3-dirty-cell");
		if (dirtyIcon != null) {
			dirtyIcon.removeFromParent();
		}
	}
	
	public static void addWarningFieldIcon(Component c, String title) {
		El elem = c.el();
		El warningIcon = elem.createChild("");
		warningIcon.setStyleName("x-form-invalid-msg");
		warningIcon.setStyleAttribute("position", "absolute");
		warningIcon.setStyleAttribute("top", "0");
		warningIcon.setStyleAttribute("left", "252px");
		warningIcon.setTitle(title);
		warningIcon.setSize(100, 100);
		warningIcon.show();
	}
	
	public static void removeWarningFieldIcon(Component c) {
		El elem = c.el();
		El warningIcon = elem.child(".x-form-invalid-msg");
		if (warningIcon != null) {
			warningIcon.removeFromParent();
		}
	}
}
