/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package cx.ath.matthew.cgi;

public class TextArea extends Field
{
   String defval;
   int cols;
   int rows;
   public TextArea(String name, String label, String defval)
   {
      this(name, label, defval, 30, 4);
   }
   public TextArea(String name, String label, String defval, int cols, int rows)
   {
      this.name = name;
      this.label = label;
      if (null == defval)
         this.defval = "";
      else
         this.defval = defval;
      this.cols = cols;
      this.rows = rows;
   }
   protected String print()
   {
      return "<textarea name='"+name+"' cols='"+cols+"' rows='"+rows+"'>"+defval+"</textarea>";
   }
}


