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
package cx.ath.matthew.unix;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class testserver
{
   public static void main(String args[]) throws IOException
   {
      UnixServerSocket ss = new UnixServerSocket(new UnixSocketAddress("testsock", true));
      UnixSocket s = ss.accept();
      BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
      String l;
      while (null != (l = r.readLine()))
         System.out.println(l);/*
      InputStream is = s.getInputStream();
      int r;
      do {
         r = is.read();
         System.out.print((char)r);
      } while (-1 != r);*/
      s.close();
      ss.close();
   }
}
