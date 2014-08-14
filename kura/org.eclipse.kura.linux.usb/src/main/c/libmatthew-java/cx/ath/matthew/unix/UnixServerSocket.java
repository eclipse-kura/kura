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

/**
 * Represents a listening UNIX Socket.
 */
public class UnixServerSocket
{
   static { System.loadLibrary("unix-java"); }
   private native int native_bind(String address, boolean abs) throws IOException;
   private native void native_close(int sock) throws IOException;
   private native int native_accept(int sock) throws IOException;
   private UnixSocketAddress address = null;
   private boolean bound = false;
   private boolean closed = false;
   private int sock;
   /**
    * Create an un-bound server socket.
    */
   public UnixServerSocket()
   {
   }
   /**
    * Create a server socket bound to the given address.
    * @param address Path to the socket.
    */
   public UnixServerSocket(UnixSocketAddress address) throws IOException
   {
      bind(address);
   }
   /**
    * Create a server socket bound to the given address.
    * @param address Path to the socket.
    */
   public UnixServerSocket(String address) throws IOException
   {
      this(new UnixSocketAddress(address));
   }
   /**
    * Accepts a connection on the ServerSocket.
    * @return A UnixSocket connected to the accepted connection.
    */
   public UnixSocket accept() throws IOException
   {
      int client_sock = native_accept(sock);
      return new UnixSocket(client_sock, address);
   }
   /**
    * Closes the ServerSocket.
    */
   public synchronized void close() throws IOException
   {
      native_close(sock);
      sock = 0;
      closed = true;
      bound = false;
   }
   /**
    * Binds a server socket to the given address.
    * @param address Path to the socket.
    */
   public void bind(UnixSocketAddress address) throws IOException
   {
      if (bound) close();
      sock = native_bind(address.path, address.abs);
      bound = true;
      closed = false;
      this.address = address;
   }
   /**
    * Binds a server socket to the given address.
    * @param address Path to the socket.
    */
   public void bind(String address) throws IOException
   {
      bind(new UnixSocketAddress(address));
   }   
   /**
    * Return the address this socket is bound to.
    * @return The UnixSocketAddress if bound or null if unbound.
    */
   public UnixSocketAddress getAddress()
   {
      return address;
   }
   /**
    * Check the status of the socket.
    * @return True if closed.
    */
   public boolean isClosed()
   {
      return closed;
   }
   /**
    * Check the status of the socket.
    * @return True if bound.
    */
   public boolean isBound()
   {
      return bound;
   }
}
