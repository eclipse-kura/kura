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
package cx.ath.matthew.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies from an input stream to an output stream using a Thread.
 * example:
 *
 * <pre>
 * InputStream a = getInputStream();
 * OutputStream b = getOutputStream();
 * InOutCopier copier = new InOutCopier(a, b);
 * copier.start();
 * &lt;do stuff that writes to the inputstream&gt;
 * </pre>
 */
public class InOutCopier extends Thread
{
   private static final int BUFSIZE=1024;
   private static final int POLLTIME=100;
   private BufferedInputStream is;
   private OutputStream os;
   private boolean enable;
   /**
    * Create a copier from an inputstream to an outputstream
    * @param is The stream to copy from
    * @param os the stream to copy to
    */
   public InOutCopier(InputStream is, OutputStream os) throws IOException
   {
      this.is = new BufferedInputStream(is);
      this.os = os;
      this.enable = true;
   }
   /**
    * Force close the stream without waiting for EOF on the source
    */
   public void close()
   {
      enable = false;
      interrupt();
   }
   /**
    * Flush the outputstream
    */
   public void flush() throws IOException
   {
      os.flush();
   }
   /** Start the thread and wait to make sure its really started */
   public synchronized void start()
   {
      super.start();
      try {
         wait();
      } catch (InterruptedException Ie) {}
   }
   /**
    * Copies from the inputstream to the outputstream
    * until EOF on the inputstream or explicitly closed
    * @see #close()
    */
   public void run() 
   {
      byte[] buf = new byte[BUFSIZE];
      synchronized (this) {
         notifyAll();
      }
      while (enable)
         try {
            int n = is.read(buf);
            if (0 > n)
               break;
            if (0 < n) {
               os.write(buf, 0, (n> BUFSIZE? BUFSIZE:n));
               os.flush();
            }
         } catch (IOException IOe) { 
            break;
         }
      try { os.close(); } catch (IOException IOe) {}
   }
}

