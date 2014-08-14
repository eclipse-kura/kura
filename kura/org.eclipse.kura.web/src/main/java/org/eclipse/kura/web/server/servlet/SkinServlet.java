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
package org.eclipse.kura.web.server.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkinServlet extends HttpServlet {

	private static final long serialVersionUID = -556598856721497972L;
	
	private static Logger s_logger = LoggerFactory.getLogger(SkinServlet.class);
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String resourceName = request.getPathInfo();
		if (resourceName.endsWith(".css")) {
			response.setContentType("text/css");
			streamText(resourceName, response.getWriter());
		}
		else if (resourceName.endsWith(".js")) {
			response.setContentType("text/javascript");
			streamText(resourceName, response.getWriter());
		}
		else if (resourceName.endsWith(".jpg") || resourceName.endsWith(".png")) {
			response.setContentType("image/png");
			streamBinary(resourceName, response.getOutputStream());
		}
		
		
		
	}
	
	private void streamText(String resourceName, PrintWriter w) throws ServletException, IOException {
		FileReader fr = null;
		
		try {
			// check to see if we have an external resource directory configured
			SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
			
			File fResourceDir = checkDir(systemService.getKuraStyleDirectory());
			if (fResourceDir == null) return;
			
			File fResourceFile = checkFile(fResourceDir, resourceName);
			if (fResourceFile == null) return;
				
			// write the requested resource
			fr = new FileReader(fResourceFile);
			char[] buffer = new char[1024];
			int iRead = fr.read(buffer);
			while (iRead != -1) {
				w.write(buffer, 0, iRead);
				iRead = fr.read(buffer);
			}
		}
		catch (Exception e) {
			s_logger.error("Error loading skin resource", e);			
		} 
		finally {
			if (fr != null)
				fr.close();
			if (w != null)
				w.close();
		}
		
	}
	
	private void streamBinary(String resourceName, OutputStream o) throws ServletException, IOException {
		FileInputStream in = null;
		
		try {
			// check to see if we have an external resource directory configured
			SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
			
			File fResourceDir = checkDir(systemService.getKuraStyleDirectory());
			if (fResourceDir == null) return;
			
			File fResourceFile = checkFile(fResourceDir, resourceName);
			if (fResourceFile == null) return;
				
			// write the requested resource
			in = new FileInputStream(fResourceFile);
			byte[] buf  = new byte[1024];
			int len = 0;
			while ((len = in.read(buf)) >= 0) {
				o.write(buf, 0, len);
			}
			
		}
		catch (Exception e) {
			s_logger.error("Error loading skin resource", e);			
		} 
		finally {
			if (in != null)
				in.close();
			if (o != null)
				o.close();
		}
		
	}
	
	private File checkDir(String resourceDir) {
		File fResourceDir = null;
		
		if (resourceDir != null && resourceDir.trim().length() != 0) {
			
			fResourceDir = new File(resourceDir);
			if (!fResourceDir.exists()) {
				s_logger.warn("Resource Directory {} does not exist", fResourceDir.getAbsolutePath());
				fResourceDir = null;
				
				return fResourceDir;
			}
		}
		return fResourceDir;
	}
	
	private File checkFile(File resourceDir, String resourceName) {
		File fResourceFile = new File(resourceDir, resourceName);
		
		if (!fResourceFile.exists()) {
			s_logger.warn("Resource File {} does not exist", fResourceFile.getAbsolutePath());
			fResourceFile = null;
			
			return fResourceFile;
		}
		
		return fResourceFile;
	}
	

}
