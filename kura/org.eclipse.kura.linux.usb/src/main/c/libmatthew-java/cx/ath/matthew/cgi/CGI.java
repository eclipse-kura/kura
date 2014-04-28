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
package cx.ath.matthew.cgi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * This is the main class you have to extend with your CGI program.
 * You should implement the cgi() method.
 *
 * @author Matthew Johnson &lt;src@matthew.ath.cx&gt;
 */
abstract public class CGI
{
   private CGIErrorHandler errorhandler = new DefaultErrorHandler();
   private boolean headers_sent = false;
   private HashMap headers = new HashMap();
   private Vector cookies = new Vector();
   private LinkedList pagedata = new LinkedList();
   private LinkedList rawdata = new LinkedList();
   
   private native String getenv(String var);
   /** MUST pass String.class and ALWAYS returns a String[] */
   private native Object[] getfullenv(Class c);
   private native void setenv(String var, String value);
   {
      System.loadLibrary("cgi-java");
   }

   /**
    * Called by CGIs to send a header to the output
    *
    * @param variable The header variable to set.
    * @param value The value of the variable.
    *
    * @throws CGIHeaderSentException if the headers have already been sent.
    *
    * @see #flush
    */
   public final void header(String variable, String value) throws CGIHeaderSentException
   {
      // only send headers once
      if (headers_sent) throw new CGIHeaderSentException();

      // buffer the variable (Map so that each header is only set once)
      headers.put(variable.toLowerCase(), value);
   }

   /**
    * Sets a Cookie in the web browser, with extended attributes.
    * Calls header() so must be called before sending any output.
    *
    * A parameter will not be sent if it is null.
    * 
    * @param variable The cookie variable to set.
    * @param value The value of the variable.
    * @param path The path that the cookie will be returned for.
    * @param domain The domain that the cookie will be returned for.
    * @param expires The expiry date of the cookie.
    * @param secure Will only send the cookie over HTTPS if this is true.
    *
    * @throws CGIHeaderSentException if the headers have already been sent.
    *
    * @see #flush
    * @see #header
    */
   public final void setcookie(String variable, String value, String path, String domain, Date expires, boolean secure) throws CGIHeaderSentException
   {
      if (headers_sent) throw new CGIHeaderSentException();

      //Set-Cookie: NAME=VALUE; expires=DATE;
      //path=PATH; domain=DOMAIN_NAME; secure
      //Wdy, DD-Mon-YYYY HH:MM:SS GMT
      DateFormat df = new SimpleDateFormat("E, dd-MMM-yyyy HH:mm:ss zzz");
      String cookie = variable+"="+value;
      if (null != path) cookie += "; path="+path;
      if (null != domain) cookie += "; domain="+domain;
      if (null != expires) cookie += "; expires="+df.format(expires);
      if (secure) cookie += "; secure";
      cookies.add("Set-Cookie: "+ cookie);
   }
 
   /**
    * Sets a Cookie in the web browser.
    * Calls header() so must be called before sending any output.
    * 
    * @param variable The cookie variable to set.
    * @param value The value of the variable.
    *
    * @throws CGIHeaderSentException if the headers have already been sent.
    *
    * @see #flush
    * @see #header
    */
   public final void setcookie(String variable, String value) throws CGIHeaderSentException
   {
      if (headers_sent) throw new CGIHeaderSentException();

      //Set-Cookie: NAME=VALUE; expires=DATE;
      //path=PATH; domain=DOMAIN_NAME; secure
      cookies.add("Set-Cookie: "+ variable+"="+value);
   }
    
   /**
    * Called by CGIs to send byte data to the output.
    * The data is buffered until the CGI exits, or a call of flush.
    *
    * @param data The page data.
    * @throws CGIInvalidContentFormatException if text data has already been sent.
    *
    * @see #flush
    */
   public final void out(byte[] data) throws CGIInvalidContentFormatException
   {
      if (pagedata.size() > 0) throw new CGIInvalidContentFormatException();
      rawdata.add(data);
   }
  
   /**
    * Called by CGIs to send a string to the output.
    * The data is buffered until the CGI exits, or a call of flush.
    *
    * @param data The page data.
    * @throws CGIInvalidContentFormatException if raw data has already been sent.
    *
    * @see #flush
    */
   public final void out(String data) throws CGIInvalidContentFormatException
   {
      if (rawdata.size() > 0) throw new CGIInvalidContentFormatException();
      pagedata.add(data);
   }

   /**
    * This will return an OutputStream that you can write data
    * directly to. Calling this method will cause the output to be 
    * flushed and the Headers sent. At the moment this is not buffered
    * and will be sent directly to the client. Subsequent calls
    * to out() will appear after data written to the output stream.
    *
    * @see #out
    * @return an OutputStream
    */
   public final OutputStream getOutputStream() throws IOException
   {
      flush();
      return System.out;
   }

   /**
    * Flushes the output. 
    * Note that you cannot send a header after a flush.
    * If you want to send both text and binary data in a page 
    * you may do so either side of a flush.
    *
    * @see #header
    */
   public final void flush() throws IOException
   {
      if (!headers_sent) {
         // don't send headers again
         headers_sent = true;
         // send headers
         Iterator i = headers.keySet().iterator();
         while (i.hasNext()) {
            String key = (String) i.next();
            String value = (String) headers.get(key);
            System.out.println(key + ": " + value);
         }
         // send cookies
         i = cookies.iterator();
         while (i.hasNext()) {
            System.out.println((String) i.next());
         }
         System.out.println();
      }

      // send data
      if (pagedata.size() >0) {
         Iterator j = pagedata.iterator();
         while (j.hasNext()) {
            System.out.println((String) j.next());
         }
         pagedata.clear();
      } else if (rawdata.size() > 0) {
         Iterator j = rawdata.iterator();
         while (j.hasNext()) {
            System.out.write((byte[]) j.next());
         }
         pagedata.clear();
      }
         System.out.flush();
   }

   /**
    * Sets a custom exception handler.
    * Gets called when an exception is thrown. 
    * The default error handler prints the error nicely in HTML
    * and then exits gracefully.
    *
    * @param handler The new exception handler
    */
   protected final void setErrorHandler(CGIErrorHandler handler)
   {
      errorhandler = handler;
   }
   
   /**
    * Override this method in your CGI program.
    *
    * @param POST A Map of variable =$gt; value for the POST variables.
    * @param GET A Map of variable =$gt; value for the GET variables.
    * @param ENV A Map of variable =$gt; value for the Webserver environment variables.
    * @param COOKIES A Map of variable =$gt; value for the browser-sent cookies.
    * @param params An array of parameters passed to the CGI (GET with no variable assignments)
    *
    * @throws Exception You can throw anything, it will be caught by the error handler.
    */
   abstract protected void cgi(Map POST, Map GET, Map ENV, Map COOKIES, String[] params) throws Exception;

   /**
    * Reads variables from a String like a=b&amp;c=d to a Map {a =&gt; b, c =&gt; d}.
    *
    * @param s String to read from.
    * @param seperator seperator character between variables (eg &amp;)
    * @param values whether or not this string has values for variables
    *
    * @return a Map with values, a Vector without
    */
   private Object readVariables(String s, char seperator, boolean values)
   {
      HashMap vars = new HashMap();
      Vector varv = new Vector();
      String temp = "";
      String variable = null;
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == seperator) { // new variable
            if (null != temp) temp = temp.trim();
            if (values) {
               if (variable == null) {variable = temp; temp = "";}
               else variable.trim();
               if (!variable.equals("")) {
                  Object o = vars.get(variable);
                  if (o == null)
                     vars.put(variable.trim(), temp);
                  else if (o instanceof String) {
                     LinkedList l = new LinkedList();
                     l.add(o);
                     l.add(temp);
                     vars.put(variable.trim(), l);
                  } else if (o instanceof LinkedList) 
                     ((LinkedList) o).add(temp);
               }
               temp = "";
            }
            else {
               varv.add(temp);
               temp = "";
            }
            variable = null;
            continue;
         }
         if (values && c == '=') {
            variable = temp;
            temp = "";
            continue;
         }
         switch (c) {
            case '%': // escaped character   
               try {
                  char a = s.charAt(++i);
                  char b = s.charAt(++i);
                  int ch = 0;
                  switch (a) {
                     case '0':
                     case '1':
                     case '2':
                     case '3':
                     case '4':
                     case '5':
                     case '6':
                     case '7':
                     case '8':
                     case '9':
                        ch += 0x10 * (a - '0');
                        break;
                     case 'a':
                     case 'b':
                     case 'c':
                     case 'd':
                     case 'e':
                     case 'f':
                        ch += 0x10 * (a - 'a' + 0xa);
                        break;
                     case 'A':
                     case 'B':
                     case 'C':
                     case 'D':
                     case 'E':
                     case 'F':
                        ch += 0x10 * (a - 'A' + 0xA);
                        break;
                  }
                  switch (b) {
                     case '0':
                     case '1':
                     case '2':
                     case '3':
                     case '4':
                     case '5':
                     case '6':
                     case '7':
                     case '8':
                     case '9':
                        ch += (b - '0');
                        break;
                     case 'a':
                     case 'b':
                     case 'c':
                     case 'd':
                     case 'e':
                     case 'f':
                        ch += (b - 'a' + 0xa);
                        break;
                     case 'A':
                     case 'B':
                     case 'C':
                     case 'D':
                     case 'E':
                     case 'F':
                        ch += (b - 'A' + 0xA);
                        break;
                  }
                  temp += (char) ch;
               } catch (StringIndexOutOfBoundsException SIOOBe) {
                  // this means someone has included an invalid escape sequence.
                  // Invalid URIs can just be thrown on the floor.
               }
               break;
            // + is a space
            case '+':
               temp += ' ';
               break;
            default:
               temp += c;               
         }
      }
      if (values) {
         if (variable == null) {variable = temp; temp = "";}
         else variable.trim();
         //out("DEBUG variable read: "+variable+"/"+temp);
         if (!variable.equals("")) {
            Object o = vars.get(variable);
            if (o == null)
               vars.put(variable.trim(), temp);
            else if (o instanceof String) {
               LinkedList l = new LinkedList();
               l.add(o);
               l.add(temp);
               vars.put(variable.trim(), l);
            } else if (o instanceof LinkedList) 
               ((LinkedList) o).add(temp);
         }

         return vars;
      }
      else {
         varv.add(temp);
         return varv;
      }
   }
   
   /**
    * Sets up the POST variables
    */
   private Map getPOST() 
   {
      try {
         String s = "";
         while(System.in.available() > 0)
            s += (char) System.in.read();
         //out("DEBUG: POST STRING: "+s);
         return (Map) readVariables(s, '&', true);
      } catch (IOException IOe) {
         try {
         out("ERROR: IOException: "+IOe);
         } catch (CGIInvalidContentFormatException CGIICFe) {
            System.err.println("ERROR: IOException: "+IOe);
         }
         return new HashMap();
      }
   }
 
   /**
    * Sets up the COOKIEs
    */
   private Map getCOOKIE()
   {
      String s = getenv("HTTP_COOKIE");
      if (null == s)
         return new HashMap();
      else
         return (Map) readVariables(s, ';', true);
   }
  
   /**
    * Sets up the GET variables
    */
   private Map getGET()
   {
      String s = getenv("QUERY_STRING");
      if (null == s)
         return new HashMap();
      else
         return (Map) readVariables(s, '&', true);
   }
  
   /**
    * Sets up the ENV variables
    */
   private  Map getENV()
   {
      Map m = new HashMap();
      String[] env = (String[]) getfullenv(String.class);
      for (int i = 0; i < env.length; i++){
         if (null == env[i]) continue;
         String[] e = env[i].split("=");
         if (1 == e.length)
            m.put(e[0], "");
         else
            m.put(e[0], e[1]);
      }
         
/*
      m.put("SERVER_SOFTWARE", getenv("SERVER_SOFTWARE"));
      m.put("SERVER_NAME", getenv("SERVER_NAME"));
      m.put("GATEWAY_INTERFACE", getenv("GATEWAY_INTERFACE"));
      m.put("SERVER_PROTOCOL", getenv("SERVER_PROTOCOL"));
      m.put("SERVER_PORT", getenv("SERVER_PORT"));
      m.put("REQUEST_METHOD", getenv("REQUEST_METHOD"));
      m.put("PATH_INFO", getenv("PATH_INFO"));
      m.put("PATH_TRANSLATED", getenv("PATH_TRANSLATED"));
      m.put("SCRIPT_NAME", getenv("SCRIPT_NAME"));
      m.put("QUERY_STRING", getenv("QUERY_STRING"));
      m.put("REMOTE_HOST", getenv("REMOTE_HOST"));
      m.put("REMOTE_ADDR", getenv("REMOTE_ADDR"));
      m.put("AUTH_TYPE", getenv("AUTH_TYPE"));
      m.put("REMOTE_USER", getenv("REMOTE_USER"));
      m.put("REMOTE_IDENT", getenv("REMOTE_IDENT"));
      m.put("CONTENT_TYPE", getenv("CONTENT_TYPE"));
      m.put("CONTENT_LENGTH", getenv("CONTENT_LENGTH"));
      m.put("HTTP_ACCEPT", getenv("HTTP_ACCEPT"));
      m.put("HTTP_USER_AGENT", getenv("HTTP_USER_AGENT"));
      m.put("HTTP_COOKIE", getenv("HTTP_COOKIE"));
      m.put("HTTP_ACCEPT_CHARSET", getenv("HTTP_ACCEPT_CHARSET"));
      m.put("HTTP_ACCEPT_ENCODING", getenv("HTTP_ACCEPT_ENCODING"));
      m.put("HTTP_CACHE_CONTROL", getenv("HTTP_CACHE_CONTROL"));
      m.put("HTTP_REFERER", getenv("HTTP_REFERER"));
      m.put("HTTP_X_FORWARDED_FOR", getenv("HTTP_X_FORWARDED_FOR"));
      m.put("HTTP_HOST", getenv("HTTP_HOST"));
      m.put("REQUEST_URI", getenv("REQUEST_URI"));
      m.put("DOCUMENT_ROOT", getenv("DOCUMENT_ROOT"));
      m.put("PATH", getenv("PATH"));
      m.put("SERVER_ADDR", getenv("SERVER_ADDR"));
      m.put("SCRIPT_FILENAME", getenv("SCRIPT_FILENAME"));
      m.put("HTTP_COOKIE2", getenv("HTTP_COOKIE2"));
      m.put("HTTP_CONNECTION", getenv("HTTP_CONNECTION"));
      m.put("LANG", getenv("LANG"));
      m.put("REDIRECT_LANG", getenv("REDIRECT_LANG"));
  */    
      return m;
   }
  
   /**
    * Sets up the param variables
    */
   private  String[] getParams(String args)
   {
      Vector v = (Vector) readVariables(args, ',', false);
      String[] params = new String[v.size()];
      Iterator i = v.iterator();
      for (int j = 0; j < params.length; j++) 
         params[j] = (String) i.next();
      return params;
   }

   /**
    * This method sets up all the CGI variables and calls the cgi() method, then writes out the page data.
    */
   public final void doCGI(String[] args)
   {
      CGI cgiclass = null;
      // wrap everything in a try, we need to handle all our own errors.
      try {
         // setup the CGI variables
         Map POST = getPOST();
         Map GET = getGET();
         Map ENV = getENV();
         Map COOKIE = getCOOKIE();
         String[] params = new String[] {};
         if (args.length >= 1)
            params = getParams(args[0]);

         // instantiate CGI class
         /*   Class c = Class.forName(args[0]);
         cgiclass = (CGI) c.newInstance();        */ 

         // set default headers
         /*cgiclass.*/header("Content-type", "text/html");
         
         // execute the CGI
         /*cgiclass.*/cgi(POST, GET, ENV, COOKIE, params);
         
         // send the output / remaining output
         /*cgiclass.*/flush();
      } 
      
      // yes, we really want to do this. CGI programs can't send errors. Print nicely to the screen.
      catch (Exception e) {
         errorhandler.print(/*null == cgiclass ? false : cgiclass.*/headers_sent, e);
      }
      catch (Throwable t) {
         t.printStackTrace(); // this is bad enough to produce stderr errors
         errorhandler.print(/*null == cgiclass ? false : cgiclass.*/headers_sent, new Exception(t.toString()));
      }
   }
}


