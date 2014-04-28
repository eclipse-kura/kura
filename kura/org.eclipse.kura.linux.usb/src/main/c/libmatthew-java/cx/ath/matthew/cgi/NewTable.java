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
package cx.ath.matthew.cgi;
public class NewTable extends Field {
	
	private String name;
	private String cssClass;
	
	public NewTable (String name, String css) {
		this.name = name;
		this.cssClass = css;
	}
	
	protected String print() {
		return "</table>\n<table class=\""+cssClass+"\">";
	}
}
