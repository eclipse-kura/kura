/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import com.extjs.gxt.ui.client.data.BaseModel;

public class KuraBaseModel extends BaseModel{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2502956403624362215L;
	
	public KuraBaseModel() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <X> X set(String name, X value){
		if (value instanceof String) {
			value = (X) GwtSafeHtmlUtils.inputSanitize((String) value);
		}
		return super.set(name, value);
	}

}
