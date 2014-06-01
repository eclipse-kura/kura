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
}
