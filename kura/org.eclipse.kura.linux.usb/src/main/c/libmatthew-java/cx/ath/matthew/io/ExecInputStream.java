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

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to pipe an InputStream through a command using stdin/stdout.
 * E.g.
 * <pre>
 *    Reader r = new InputStreamReader(new ExecInputStream(new FileInputStream("file"), "command"));
 * </pre>
 */
public class ExecInputStream extends FilterInputStream
{
   private Process proc;
   private InputStream stdout;
   private OutputStream stdin;
   private InOutCopier copy;

   /**
    * Create a new ExecInputStream on the given InputStream
    * using the process to filter the stream.
    * @param is Reads from this InputStream
    * @param p Filters data through stdin/out on this Process
    */
   public ExecInputStream(InputStream is, Process p) throws IOException
   {
      super(is);
      proc = p;
      stdin = p.getOutputStream();
      stdout = p.getInputStream();
      copy = new InOutCopier(in, stdin);
      copy.start();
   }
   /**
    * Create a new ExecInputStream on the given InputStream
    * using the process to filter the stream.
    * @param is Reads from this InputStream
    * @param cmd Creates a Process from this string to filter data through stdin/out 
    */
   public ExecInputStream(InputStream is, String cmd) throws IOException
   { this(is, Runtime.getRuntime().exec(cmd)); }
   /**
    * Create a new ExecInputStream on the given InputStream
    * using the process to filter the stream.
    * @param is Reads from this InputStream
    * @param cmd Creates a Process from this string array (command, arg, ...) to filter data through stdin/out 
    */
   public ExecInputStream(InputStream is, String[] cmd) throws IOException
   { this(is, Runtime.getRuntime().exec(cmd)); }
   /**
    * Create a new ExecInputStream on the given InputStream
    * using the process to filter the stream.
    * @param is Reads from this InputStream
    * @param cmd Creates a Process from this string to filter data through stdin/out 
    * @param env Setup the environment for the command
    */
   public ExecInputStream(InputStream is, String cmd, String[] env) throws IOException
   { this(is, Runtime.getRuntime().exec(cmd, env)); }
   /**
    * Create a new ExecInputStream on the given InputStream
    * using the process to filter the stream.
    * @param is Reads from this InputStream
    * @param cmd Creates a Process from this string array (command, arg, ...) to filter data through stdin/out 
    * @param env Setup the environment for the command
    */
   public ExecInputStream(InputStream is, String[] cmd, String[] env) throws IOException
   { this(is, Runtime.getRuntime().exec(cmd, env)); }

   public void close() throws IOException
   {
      try {
         proc.waitFor();
      } catch (InterruptedException Ie)  {}
      //copy.close();
      try {
         copy.join();
      } catch (InterruptedException Ie)  {}
      stdin.close();
      in.close();
      stdout.close();
   }
   public void flush() throws IOException
   {
      copy.flush();
   }
   public int	available() throws IOException
   { return stdout.available(); } 
   public int	read() throws IOException
   { return stdout.read(); }
   public int	read(byte[] b) throws IOException
   { return stdout.read(b); }
   public int	read(byte[] b, int off, int len) throws IOException
   { return stdout.read(b, off, len); }
   public long	skip(long n) throws IOException
   { return stdout.skip(n); }
   public void	mark(int readlimit)
   {}
   public boolean	markSupported()
   { return false; }
   public void	reset() 
   {}

   public void finalize()
   {
      try {
         close();
      } catch (Exception e) {}
   }
}



