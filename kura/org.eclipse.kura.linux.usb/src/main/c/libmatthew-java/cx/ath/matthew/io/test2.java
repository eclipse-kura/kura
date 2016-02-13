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
package cx.ath.matthew.io;
import java.io.BufferedReader;
import java.io.InputStreamReader;
class test2
{
   public static void main(String[] args) throws Exception
   {
      BufferedReader in = new BufferedReader(new InputStreamReader(new ExecInputStream(System.in, "xsltproc mcr.xsl -")));
      String s;
      while (null != (s = in.readLine())) System.out.println(s);
   }
}
