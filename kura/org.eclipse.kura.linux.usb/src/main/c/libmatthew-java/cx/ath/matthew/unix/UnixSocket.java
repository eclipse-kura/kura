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
package cx.ath.matthew.unix;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import cx.ath.matthew.debug.Debug;

/**
 * Represents a UnixSocket.
 */
public class UnixSocket
{
   static { System.loadLibrary("unix-java"); }
   private native void native_set_pass_cred(int sock, boolean passcred) throws IOException;
   private native int native_connect(String address, boolean abs) throws IOException;
   private native void native_close(int sock) throws IOException;
   private native int native_getPID(int sock);
   private native int native_getUID(int sock);
   private native int native_getGID(int sock);
   private native void native_send_creds(int sock, byte data) throws IOException;
   private native byte native_recv_creds(int sock, int[] creds) throws IOException;

   private UnixSocketAddress address = null;
   private USOutputStream os = null;
   private USInputStream is = null;
   private boolean closed = false;
   private boolean connected = false;
   private boolean passcred = false;
   private int sock = 0;
   private boolean blocking = true;
   private int uid = -1;
   private int pid = -1;
   private int gid = -1;
   UnixSocket(int sock, UnixSocketAddress address)
   {
      this.sock = sock;
      this.address = address;
      this.connected = true;
      this.os = new USOutputStream(sock, this);
      this.is = new USInputStream(sock, this);
   }
   /**
    * Create an unconnected socket.
    */
   public UnixSocket()
   {
   }
   /**
    * Create a socket connected to the given address.
    * @param address The Unix Socket address to connect to
    */
   public UnixSocket(UnixSocketAddress address) throws IOException
   {
      connect(address);
   }
   /**
    * Create a socket connected to the given address.
    * @param address The Unix Socket address to connect to
    */
   public UnixSocket(String address) throws IOException
   {
      this(new UnixSocketAddress(address));
   }
   /**
    * Connect the socket to this address.
    * @param address The Unix Socket address to connect to
    */
   public void connect(UnixSocketAddress address) throws IOException
   {
      if (connected) close();
      this.sock = native_connect(address.path, address.abs);
      this.os = new USOutputStream(this.sock, this);
      this.is = new USInputStream(this.sock, this);
      this.address = address;
      this.connected = true;
      this.closed = false;
      this.is.setBlocking(blocking);
   }
   /**
    * Connect the socket to this address.
    * @param address The Unix Socket address to connect to
    */
   public void connect(String address) throws IOException
   {
      connect(new UnixSocketAddress(address));
   }
   public void finalize()
   {
      try { 
         close();
      } catch (IOException IOe) {}
   }
   /**
    * Closes the connection.
    */
   public synchronized void close() throws IOException
   {
      if (Debug.debug) Debug.print(Debug.INFO, "Closing socket");
      native_close(sock);
      sock = 0;
      this.closed = true;
      this.connected = false;
      os = null;
      is = null;
   }
   /**
    * Returns an InputStream for reading from the socket.
    * @return An InputStream connected to this socket.
    */
   public InputStream getInputStream()
   {
      return is;
   }
   /**
    * Returns an OutputStream for writing to the socket.
    * @return An OutputStream connected to this socket.
    */
   public OutputStream getOutputStream()
   {
      return os;
   }
   /**
    * Returns the address this socket is connected to.
    * Returns null if the socket is unconnected.
    * @return The UnixSocketAddress the socket is connected to
    */
   public UnixSocketAddress getAddress()
   { 
      return address;
   }
   /**
    * Send a single byte of data with credentials.
    * (Works on BSDs)
    * @param data The byte of data to send.
    */
   public void sendCredentialByte(byte data) throws IOException
   {
      if (!connected) throw new NotConnectedException();
      native_send_creds(sock, data);
   }
   /**
    * Receive a single byte of data, with credentials.
    * (Works on BSDs)
    * @see getPeerUID
    * @see getPeerPID
    * @see getPeerGID
    * @param data The byte of data to send.
    */
   public byte recvCredentialByte() throws IOException
   {
      if (!connected) throw new NotConnectedException();
      int[] creds = new int[] { -1, -1, -1 };
      byte data = native_recv_creds(sock, creds);
      pid = creds[0];
      uid = creds[1];
      gid = creds[2];
      return data;
   }
   /**
    * Get the credential passing status.
    * (only effective on linux)
    * @return The current status of credential passing.
    * @see setPassCred
    */
   public boolean getPassCred()
   {
      return passcred;
   }
   /**
    * Return the uid of the remote process.
    * Some data must have been received on the socket to do this.
    * Either setPassCred must be called on Linux first, or recvCredentialByte
    * on BSD.
    * @return the UID or -1 if it is not available
    */
   public int getPeerUID()
   {
      if (-1 == uid)
         uid = native_getUID(sock);
      return uid;
   }
   /**
    * Return the gid of the remote process.
    * Some data must have been received on the socket to do this.
    * Either setPassCred must be called on Linux first, or recvCredentialByte
    * on BSD.
    * @return the GID or -1 if it is not available
    */
   public int getPeerGID()
   {
      if (-1 == gid)
         gid = native_getGID(sock);
      return gid;
   }
   /**
    * Return the pid of the remote process.
    * Some data must have been received on the socket to do this.
    * Either setPassCred must be called on Linux first, or recvCredentialByte
    * on BSD.
    * @return the PID or -1 if it is not available
    */
   public int getPeerPID()
   {
      if (-1 == pid)
         pid = native_getPID(sock);
      return pid;
   }
   /**
    * Set the credential passing status.
    * (Only does anything on linux, for other OS, you need
    * to use send/recv credentials)
    * @param enable Set to true for credentials to be passed.
    */
   public void setPassCred(boolean enable) throws IOException
   {
      native_set_pass_cred(sock, enable);
      passcred = enable;
   }
   /**
    * Get the blocking mode.
    * @return true if reads are blocking.
    * @see setBlocking
    */
   public boolean getBlocking()
   {
      return blocking;
   }
   /**
    * Set the blocking mode.
    * @param enable Set to false for non-blocking reads.
    */
   public void setBlocking(boolean enable)
   {
      blocking = enable;
      if (null != is) is.setBlocking(enable);
   }

   /**
    * Check the socket status.
    * @return true if closed.
    */
   public boolean isClosed()
   {
      return closed;
   }
   /**
    * Check the socket status.
    * @return true if connected.
    */
   public boolean isConnected()
   {
      return connected;
   }
   /**
    * Check the socket status.
    * @return true if the input stream has been shutdown
    */
   public boolean isInputShutdown()
   {
      return is.isClosed();
   }
   /**
    * Check the socket status.
    * @return true if the output stream has been shutdown
    */
   public boolean isOutputShutdown()
   {
      return os.isClosed();
   }
   /**
    * Shuts down the input stream.
    * Subsequent reads on the associated InputStream will fail.
    */
   public void shutdownInput()
   {
      is.closed = true;
   }
   /**
    * Shuts down the output stream.
    * Subsequent writes to the associated OutputStream will fail.
    */
   public void shutdownOutput()
   {
      os.closed = true;
   }
   /**
    * Set timeout of read requests.
    */
   public void setSoTimeout(int timeout)
   {
      is.setSoTimeout(timeout);
   }
}
