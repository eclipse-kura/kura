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

public class HiddenField extends Field
{
   String value;
   public HiddenField(String name, String value)
   {
      this.name = name;
      this.label = "";
      this.value = value;
   }
   protected String print()
   {
      return "<input type=\"hidden\" name=\""+name+"\" value=\""+CGITools.escapeChar(value, '"')+"\" />";
   }
}


