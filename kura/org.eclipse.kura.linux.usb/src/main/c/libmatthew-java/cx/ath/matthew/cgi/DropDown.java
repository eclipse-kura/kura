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

import java.util.List;

public class DropDown extends Field
{
   Object[] values;
   Object defval;
   boolean indexed = false;
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, Object[] values, Object defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = values;
      this.indexed = indexed;
      this.defval = defval;
   }
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, Object[] values, int defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = values;
      if (defval < 0)
         this.defval = null;
      else
         this.defval = values[defval];
      this.indexed = indexed;
   }
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, List values, Object defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = (Object[]) values.toArray(new Object[] {});
      this.defval = defval;
      this.indexed = indexed;
   }
   /**
    * Create a new DropDown list.
    *
    * @param name The HTML field name.
    * @param label The label to display
    * @param values The values for the drop down list
    * @param defval If this parameter is set then this element will be selected by default.
    * @param indexed If this is set to true, then indexes will be returned, rather than values.
    */
   public DropDown(String name, String label, List values, int defval, boolean indexed)
   {
      this.name = name;
      this.label = label;
      this.values = (Object[]) values.toArray(new Object[] {});
      if (defval < 0)
         this.defval = null;
      else
         this.defval = values.get(defval);
      this.indexed = indexed;
   }
   protected String print()
   {
      String s = "";
      s += "<select name='"+name+"'>\n";
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


