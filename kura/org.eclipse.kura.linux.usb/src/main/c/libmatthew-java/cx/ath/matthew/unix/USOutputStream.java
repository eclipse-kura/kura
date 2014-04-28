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

import java.io.IOException;
import java.io.OutputStream;

public class USOutputStream extends OutputStream
{
   private native int native_send(int sock, byte[] b, int off, int len) throws IOException;
   private native int native_send(int sock, byte[][] b) throws IOException;
   private int sock;
   boolean closed = false;
   private byte[] onebuf = new byte[1];
   private UnixSocket us;
   public USOutputStream(int sock, UnixSocket us)
   {
      this.sock = sock;
      this.us = us;
   }
   public void close() throws IOException
   {
      closed = true;
      us.close();
   }
   public void flush() {} // no-op, we do not buffer
   public void write(byte[][] b) throws IOException
   {
      if (closed) throw new NotConnectedException();
      native_send(sock, b);
   }
   public void write(byte[] b, int off, int len) throws IOException
   {
      if (closed) throw new NotConnectedException();
      native_send(sock, b, off, len);
   }
   public void write(int b) throws IOException
   {
      onebuf[0] = (byte) (b % 0x7F);
      if (1 == (b % 0x80)) onebuf[0] = (byte) -onebuf[0];
      write(onebuf);
   }
   public boolean isClosed() { return closed; }
   public UnixSocket getSocket() { return us; }
}
