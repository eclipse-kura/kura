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
package cx.ath.matthew.cgi;

import java.util.Iterator;
import java.util.Map;

class testcgi extends CGI
{
   protected void cgi(Map POST, Map GET, Map ENV, Map COOKIE, String[] params) throws Exception
   {
      header("Content-type", "text/plain");
      setcookie("testcgi", "You have visited us already");
      out("This is a test CGI program");
      out("These are the params:");
      for (int i=0; i < params.length; i++)
         out("-- "+params[i]);
      
      out("These are the POST vars:");
      Iterator i = POST.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+POST.get(s));
      }
      
      out("These are the GET vars:");
      i = GET.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+GET.get(s));
      }
        
      out("These are the ENV vars:");
      i = ENV.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+ENV.get(s));
      }
      
      out("These are the COOKIEs:");
      i = COOKIE.keySet().iterator();
      while (i.hasNext()) {
         String s = (String) i.next();
         out("-- "+s+" => "+COOKIE.get(s));
      }   
   }

   public static void main(String[] args)
   {
      CGI cgi = new testcgi();
      cgi.doCGI(args);
   }
}
