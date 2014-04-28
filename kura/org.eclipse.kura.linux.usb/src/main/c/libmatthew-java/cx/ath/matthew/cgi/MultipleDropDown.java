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

import java.util.List;

/**
 * @author Agent
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MultipleDropDown extends DropDown {

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, String[] values,
			String defval, boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, String[] values,
			int defval, boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, List values,
			String defval, boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param label
	 * @param values
	 * @param defval
	 * @param indexed
	 */
	public MultipleDropDown(String name, String label, List values, int defval,
			boolean indexed) {
		super(name, label, values, defval, indexed);
		// TODO Auto-generated constructor stub
	}
	
	protected String print()
	   {
	      String s = "";
	      s += "<select name='"+name+"' multiple='multiple' size='"+values.length+"'>\n";
	      for (int i=0; i<values.length; i++) {
	         if (indexed)
	            s += "   <option value='"+i+"'";
	         else
	            s += "   <option";
	         if (values[i].equals(defval))
	            s += " selected='selected'>"+values[i]+"</option>\n";
	         else
	            s += ">"+values[i]+"</option>\n";
	      }
	      s += "</select>\n";
	      return s;
	   }

}
