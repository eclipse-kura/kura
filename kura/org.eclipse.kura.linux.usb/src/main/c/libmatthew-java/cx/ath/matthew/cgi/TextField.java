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


public class TextField extends Field
{
   String defval;
   int length;
   public TextField(String name, String label)
   {
      this.name = name;
      this.label = label;
      this.defval = "";
      this.length = 0;
   }
   public TextField(String name, String label, String defval)
   {
      this.name = name;
      this.label = label;
      if (null == defval)
         this.defval = "";
      else
         this.defval = defval;
      this.length = 0;
   }
   public TextField(String name, String label, String defval, int length)
   {
      this.name = name;
      this.label = label;
      if (null == defval)
         this.defval = "";
      else
         this.defval = defval;
      this.length = length;
   }
   protected String print()
   {
      return "<input type=\"text\" name=\""+name+"\" value=\""+CGITools.escapeChar(defval,'"')+"\" "+(length==0?"":"size=\""+length+"\"")+" />";
   }
}


