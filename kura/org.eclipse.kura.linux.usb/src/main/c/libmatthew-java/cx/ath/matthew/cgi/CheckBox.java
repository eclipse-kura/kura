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

public class CheckBox extends Field
{
   boolean checked;
   public CheckBox(String name, String label, boolean checked)
   {
      this.name = name;
      this.label = label;
      this.checked = checked;
   }
   protected String print()
   {
      return "<input type='checkbox' name='"+name+"'"+(checked?" checked='checked'":"")+" />";
   }
}


