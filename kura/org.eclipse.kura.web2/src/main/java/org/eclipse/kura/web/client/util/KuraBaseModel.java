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
package org.eclipse.kura.web.client.util;

import org.eclipse.kura.web.shared.model.GwtBaseModel;

public class KuraBaseModel extends GwtBaseModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2502956403624362215L;
	
	public KuraBaseModel() {
		super();
	}
	
	@Override
	public void set(String name, Object value){
		if (value instanceof String) {
			value = (String) GwtSafeHtmlUtils.inputSanitize((String) value);
		}
		super.set(name, value);
	}

}
