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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to copy a stream to a file or another stream as it is being sent through a stream pipe
 * E.g.
 * <pre>
 *    Reader r = new InputStreamReader(new TeeInputStream(new FileInputStream("file"), new File("otherfile")));
 * </pre>
 */
public class TeeInputStream extends FilterInputStream
{
   private InputStream in;
   private OutputStream fos;
   /**
    * Create a new TeeInputStream on the given InputStream
    * and copy the stream to the given File.
    * @param is Reads from this InputStream
    * @param tos Write to this OutputStream
    */
   public TeeInputStream(InputStream is, OutputStream tos) throws IOException
   {
      super(is);
      this.in = is;
      this.fos = tos;
   }
   /**
    * Create a new TeeInputStream on the given InputStream
    * and copy the stream to the given File.
    * @param is Reads from this InputStream
    * @param f Write to this File
    * @param append Append to file not overwrite
    */
   public TeeInputStream(InputStream is, File f, boolean append) throws IOException
   {
      super(is);
      this.in = is;
      this.fos = new FileOutputStream(f, append);
   }
   /**
    * Create a new TeeInputStream on the given InputStream
    * and copy the stream to the given File.
    * @param is Reads from this InputStream
    * @param f Write to this File
    */
   public TeeInputStream(InputStream is, File f) throws IOException
   {
      super(is);
      this.in = is;
      this.fos = new FileOutputStream(f);
   }
   /**
    * Create a new TeeInputStream on the given InputStream
    * and copy the stream to the given File.
    * @param is Reads from this InputStream
    * @param f Write to this File
    * @param append Append to file not overwrite
    */
   public TeeInputStream(InputStream is, String f, boolean append) throws IOException
   {
      this(is, new File(f), append);
   }
   /**
    * Create a new TeeInputStream on the given InputStream
    * and copy the stream to the given File.
    * @param is Reads from this InputStream
    * @param f Write to this File
    */
   public TeeInputStream(InputStream is, String f) throws IOException
   {
      this(is, new File(f));
   }
   public void close() throws IOException
   {
      in.close();
      fos.close();
   }
   public void flush() throws IOException
   {
      fos.flush();
   }
   public int	available() throws IOException
   {
      return in.available();
   } 
   public int	read() throws IOException
   {
      int i = in.read();
      if (-1 != i) fos.write(i);
      return i;
   }
   public int	read(byte[] b) throws IOException
   {
      int c = in.read(b);
      if (-1 != c) fos.write(b, 0, c);
      return c;
   }
   public int	read(byte[] b, int off, int len) throws IOException
   {  
      int c = in.read(b, off, len);
      if (-1 != c) fos.write(b, off, c);
      return c;
   }
   public long	skip(long n) throws IOException
   { return in.skip(n); }
   public void	mark(int readlimit)
   {}
   public boolean	markSupported()
   { return false; }
   public void	reset() throws IOException
   { in.reset(); }

   public void finalize()
   {
      try {
         close();
      } catch (Exception e) {}
   }
}



