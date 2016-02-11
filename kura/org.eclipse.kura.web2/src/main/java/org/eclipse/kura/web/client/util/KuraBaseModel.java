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
