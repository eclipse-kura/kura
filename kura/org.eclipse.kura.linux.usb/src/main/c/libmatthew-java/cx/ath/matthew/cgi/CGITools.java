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

abstract class CGITools
{
   /**
    * Escape a character in a string.
    * @param in String to escape in.
    * @param c Character to escape.
    * @return in with c replaced with \c
    */
   public static String escapeChar(String in, char c)
   {
      String out = "";
      for (int i = 0; i < in.length(); i++) {
         if (in.charAt(i) == c) out += '\\';
         out += in.charAt(i);
      }
      return out;
   }
}

