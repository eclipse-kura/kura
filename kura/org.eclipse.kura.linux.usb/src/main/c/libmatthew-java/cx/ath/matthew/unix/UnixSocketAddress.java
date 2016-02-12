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

/**
 * Represents an address for a Unix Socket
 */
public class UnixSocketAddress
{
   String path;
   boolean abs;
  /**
    * Create the address.
    * @param path The path to the Unix Socket.
    * @param abs True if this should be an abstract socket.
    */
   public UnixSocketAddress(String path, boolean abs)
   {
      this.path = path;
      this.abs = abs;
   }
   /**
    * Create the address.
    * @param path The path to the Unix Socket.
    */
   public UnixSocketAddress(String path)
   {
      this.path = path;
      this.abs = false;
   }
   /**
    * Return the path.
    */
   public String getPath()
   {
      return path;
   }
   /**
    * Returns true if this an address for an abstract socket.
    */
   public boolean isAbstract()
   {
      return abs;
   }
   /**
    * Return the Address as a String.
    */
   public String toString()
   {
      return "unix"+(abs?":abstract":"")+":path="+path;
   }
   public boolean equals(Object o)
   {
      if (!(o instanceof UnixSocketAddress)) return false;
      return ((UnixSocketAddress) o).path.equals(this.path);
   }
   public int hashCode()
   {
      return path.hashCode();
   }
}
