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
