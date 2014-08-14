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
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class testclient
{
   public static void main(String args[]) throws IOException
   {
      UnixSocket s = new UnixSocket(new UnixSocketAddress("testsock", true));
      OutputStream os = s.getOutputStream();
      PrintWriter o = new PrintWriter(os);
      BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
      String l;
      while (null != (l = r.readLine())) {
         byte[] buf = (l+"\n").getBytes();
         os.write(buf, 0, buf.length);
      }
      s.close();
   }
}
