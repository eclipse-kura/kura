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
/*
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package cx.ath.matthew.cgi;

/**
 * @author Agent
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SubmitButton extends Field {

	public SubmitButton(String name, String label) {
		this.name = name;
		this.label = label;
	}
	/* (non-Javadoc)
	 * @see cx.ath.matthew.cgi.Field#print()
	 */
	protected String print() {
		return "<input type='submit' name='"+name+"' value='"+CGITools.escapeChar(label,'\'')+"' />";
	}

}
