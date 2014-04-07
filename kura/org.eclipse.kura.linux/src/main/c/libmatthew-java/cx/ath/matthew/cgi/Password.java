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

public class Password extends Field
{
   String defval;
   public Password(String name, String label, String defval)
   {
      this.name = name;
      this.label = label;
      this.defval = defval;
   }
   protected String print()
   {
      return "<input type='password' name='"+name+"' value='"+CGITools.escapeChar(defval, '\'')+"' />";
   }
}


