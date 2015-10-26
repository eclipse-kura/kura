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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.IOUtils;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileServlet extends HttpServlet {

	private static final long serialVersionUID = -5016170117606322129L;

	private static Logger s_logger = LoggerFactory.getLogger(FileServlet.class);

	private static final int BUFFER = 1024;
	private static int tooBig = 0x6400000; // Max size of unzipped data, 100MB
	private static int tooMany = 1024;     // Max number of files

	private DiskFileItemFactory m_diskFileItemFactory;
	private FileCleaningTracker m_fileCleaningTracker;


	@Override
	public void destroy() {
		super.destroy();

		s_logger.info("Servlet {} destroyed", getServletName());

		if (m_fileCleaningTracker != null) {
			s_logger.info("Number of temporary files tracked: " + m_fileCleaningTracker.getTrackCount());
		}
	}


	@Override
	public void init() throws ServletException {
		super.init();

		s_logger.info("Servlet {} initialized", getServletName());

		ServletContext ctx = getServletContext();
		m_fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(ctx);
		
		getZipUploadSizeMax();
		getZipUploadCountMax();

		int sizeThreshold = getFileUploadInMemorySizeThreshold();
		File repository = new File(System.getProperty("java.io.tmpdir"));

		s_logger.info("DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD: {}", DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
		s_logger.info("DiskFileItemFactory: using size threshold of: {}", sizeThreshold);

		m_diskFileItemFactory = new DiskFileItemFactory(sizeThreshold, repository);
		m_diskFileItemFactory.setFileCleaningTracker(m_fileCleaningTracker);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("image/jpeg");

		String reqPathInfo = req.getPathInfo();
		
		if (reqPathInfo == null) {
			s_logger.error("Request path info not found");
			throw new ServletException("Request path info not found");
		}
		
		s_logger.debug("req.getRequestURI(): {}", req.getRequestURI());
		s_logger.debug("req.getRequestURL(): {}", req.getRequestURL());
		s_logger.debug("req.getPathInfo(): {}", req.getPathInfo());
		
		if (reqPathInfo.startsWith("/icon")) {
			doGetIcon(req, resp);
		}
		else {
			s_logger.error("Unknown request path info: " + reqPathInfo);
			throw new ServletException("Unknown request path info: " + reqPathInfo);			
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("text/html");

		String reqPathInfo = req.getPathInfo();
		if (reqPathInfo == null) {
			s_logger.error("Request path info not found");
			throw new ServletException("Request path info not found");
		}

		s_logger.debug("req.getRequestURI(): {}", req.getRequestURI());
		s_logger.debug("req.getRequestURL(): {}", req.getRequestURL());
		s_logger.debug("req.getPathInfo(): {}", req.getPathInfo());


		if (reqPathInfo.startsWith("/deploy")) {
			doPostDeploy(req, resp);
		}
		else if (reqPathInfo.equals("/configuration/snapshot")) {
			doPostConfigurationSnapshot(req, resp);
		}
		else if (reqPathInfo.equals("/command")) {
			doPostCommand(req, resp);
		}
		else if (reqPathInfo.equals("/certificate")) {
			return;
		}
		else {
			s_logger.error("Unknown request path info: " + reqPathInfo);
			throw new ServletException("Unknown request path info: " + reqPathInfo);			
		}
	}

	private void doGetIcon(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String queryString = req.getQueryString();
		
		if (queryString == null) {
			s_logger.error("Error parsing query string.");
			throw new ServletException("Error parsing query string.");
		}
		
		// Parse the query string
		Map<String, String> pairs;
		try {
			pairs = parseQueryString(queryString);
		} catch (UnsupportedEncodingException e) {
			s_logger.error("Error parsing query string.");
			throw new ServletException("Error parsing query string: " + e.getLocalizedMessage());
		}
		
		// Check for malformed request
		if (pairs == null || pairs.size() != 1) {
			s_logger.error("Error parsing query string.");
			throw new ServletException("Error parsing query string.");
		}
		
		String pid = pairs.get("pid");
		if (pid != null && pid.length() > 0) {
			BundleContext ctx = Console.getBundleContext();
			Bundle[] bundles = ctx.getBundles();
			ServiceLocator  locator = ServiceLocator.getInstance();
			
			// Iterate over bundles to find PID
			for (Bundle b : bundles) {
				MetaTypeService mts;
				try {
					mts = locator.getService(MetaTypeService.class);
				} catch (GwtKuraException e1) {
					s_logger.error("Error parsing query string.");
					throw new ServletException("Error parsing query string.");
				}
				MetaTypeInformation mti = mts.getMetaTypeInformation(b);
				
				String[] pids = mti.getPids();
				for (String p : pids) {
					if (p.equals(pid)) {
						try {
							InputStream is = mti.getObjectClassDefinition(pid, null).getIcon(32);
							if (is == null) {
								s_logger.error("Error reading icon file.");
								throw new ServletException("Error reading icon file.");
							}
							OutputStream os = resp.getOutputStream();
							byte[] buffer = new byte[1024];
							for (int length = 0; (length = is.read(buffer)) > 0;) {
						        os.write(buffer, 0, length);
						    }
							is.close();
							os.close();
							
						} catch (IOException e) {
							s_logger.error("Error reading icon file.");
							throw new IOException("Error reading icon file.");
						}
					}
				}
			}
		}
		else {
			s_logger.error("Error parsing query string.");
			throw new ServletException("Error parsing query string.");
		}
		
		
	}
	
	private Map<String, String> parseQueryString(String queryString) throws UnsupportedEncodingException {
		Map<String, String> qp = new HashMap<String, String>();
		
		String[] pairs = queryString.split("&");
		for (String p : pairs) {
			int index = p.indexOf("=");
			qp.put(URLDecoder.decode(p.substring(0, index), "UTF-8"), URLDecoder.decode(p.substring(index + 1), "UTF-8"));
		}
		return qp;
	}
	
	private void doPostCommand(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		UploadRequest upload = new UploadRequest(m_diskFileItemFactory);

		try {
			upload.parse(req);
		} catch (FileUploadException e) {
			s_logger.error("Error parsing the file upload request");
			throw new ServletException("Error parsing the file upload request", e);			
		}

		// BEGIN XSRF - Servlet dependent code
		Map<String,String> formFields= upload.getFormFields();

		try {
			GwtXSRFToken token = new GwtXSRFToken(formFields.get("xsrfToken"));
			KuraRemoteServiceServlet.checkXSRFToken(req, token);
		}
		catch (Exception e) {
			throw new ServletException("Security error: please retry this operation correctly.", e);
		}
		// END XSRF security check

		List<FileItem> fileItems = null;
		InputStream is = null;
		File localFolder = new File(System.getProperty("java.io.tmpdir"));
		OutputStream os = null;

		try {
			fileItems = upload.getFileItems();

			if (fileItems.size() > 0) {
				FileItem item = fileItems.get(0);
				is = item.getInputStream();

				byte[] bytes = IOUtils.toByteArray(is);
				ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));

				int entries = 0;
				long total = 0;
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					byte[] buffer = new byte[BUFFER];

					String expectedFilePath = new StringBuilder(localFolder.getPath()).append(File.separator).append(ze.getName()).toString();
					String fileName = validateFileName(expectedFilePath, localFolder.getPath());
					File newFile = new File(fileName);
					if (newFile.isDirectory()) {
						newFile.mkdirs();
						ze = zis.getNextEntry();
						continue;
					}
					if (newFile.getParent() != null) {
						File parent = new File(newFile.getParent());
						parent.mkdirs();
					}

					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((total + BUFFER <= tooBig) && (len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
						total += len;
					}
					fos.flush();
					fos.close();

					entries++;
					if (entries > tooMany) {
						throw new IllegalStateException("Too many files to unzip.");
					}
					if (total > tooBig) {
						throw new IllegalStateException("File being unzipped is too big.");
					}

					ze = zis.getNextEntry();
				}

				zis.closeEntry();
				zis.close();
			}
		} catch (IOException e) {
			throw e;
		} catch (GwtKuraException e) {
			throw new ServletException("File is outside extraction target directory.");
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					s_logger.warn("Cannot close output stream", e);
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					s_logger.warn("Cannot close input stream", e);
				}
			}
			if (fileItems != null) {
				for (FileItem fileItem : fileItems) {
					fileItem.delete();
				}
			}
		}
	}

	private String validateFileName(String zipFileName, String intendedDir) throws IOException, GwtKuraException{
		File zipFile = new File(zipFileName);
		String filePath = zipFile.getCanonicalPath();

		File iD = new File(intendedDir);
		String canonicalID = iD.getCanonicalPath();

		if (filePath.contains(canonicalID)) {
			return filePath;
		} else {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ACCESS);
		}
	}

	private void doPostConfigurationSnapshot(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		UploadRequest upload = new UploadRequest(m_diskFileItemFactory);

		try {
			upload.parse(req);
		} catch (FileUploadException e) {
			s_logger.error("Error parsing the file upload request");
			throw new ServletException("Error parsing the file upload request", e);			
		}

		// BEGIN XSRF - Servlet dependent code
		Map<String,String> formFields= upload.getFormFields();

		try {
			GwtXSRFToken token = new GwtXSRFToken(formFields.get("xsrfToken"));
			KuraRemoteServiceServlet.checkXSRFToken(req, token);
		}
		catch (Exception e) {
			throw new ServletException("Security error: please retry this operation correctly.", e);
		}
		// END XSRF security check

		List<FileItem> fileItems = upload.getFileItems();
		if (fileItems.size() != 1) {
			s_logger.error("expected 1 file item but found {}", fileItems.size());
			throw new ServletException("Wrong number of file items");
		}

		FileItem fileItem = fileItems.get(0);
		byte[] data = fileItem.get();
		String xmlString = new String(data, "UTF-8");
		XmlComponentConfigurations xmlConfigs;
		try {
			xmlConfigs = XmlUtil.unmarshal(xmlString, XmlComponentConfigurations.class);
		} catch (Exception e) {
			s_logger.error("Error unmarshaling device configuration", e);
			throw new ServletException("Error unmarshaling device configuration", e);
		}		

		ServiceLocator  locator = ServiceLocator.getInstance();
		try {

			ConfigurationService cs = locator.getService(ConfigurationService.class); 
			List<ComponentConfigurationImpl> configImpls = xmlConfigs.getConfigurations();

			List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();
			configs.addAll(configImpls);

			cs.updateConfigurations(configs);

			//
			// Add an additional delay after the configuration update
			// to give the time to the device to apply the received 
			// configuration            
			SystemService ss = locator.getService(SystemService.class);
			long delay = Long.parseLong(ss.getProperties().getProperty("console.updateConfigDelay", "5000"));
			if (delay > 0) {
				Thread.sleep(delay);
			}		
		} 
		catch (Exception e) {
			s_logger.error("Error updating device configuration: {}", e);
			throw new ServletException("Error updating device configuration", e);
		}
	}

	private void doPostDeployUpload (HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		ServiceLocator locator = ServiceLocator.getInstance();
		DeploymentAgentService deploymentAgentService;
		try {
			deploymentAgentService = locator.getService(DeploymentAgentService.class);
		} catch (GwtKuraException e) {
			s_logger.error("Error locating DeploymentAgentService", e);
			throw new ServletException("Error locating DeploymentAgentService", e);
		}

		// Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if (!isMultipart) {
			s_logger.error("Not a file upload request");
			throw new ServletException("Not a file upload request");
		}

		UploadRequest upload = new UploadRequest(m_diskFileItemFactory);

		try {
			upload.parse(req);
		} catch (FileUploadException e) {
			s_logger.error("Error parsing the file upload request", e);
			throw new ServletException("Error parsing the file upload request", e);			
		}

		// BEGIN XSRF - Servlet dependent code
		Map<String,String> formFields= upload.getFormFields();

		try {
			GwtXSRFToken token = new GwtXSRFToken(formFields.get("xsrfToken"));
			KuraRemoteServiceServlet.checkXSRFToken(req, token);
		}
		catch (Exception e) {
			throw new ServletException("Security error: please retry this operation correctly.", e);
		}
		// END XSRF security check

		List<FileItem> fileItems = null;
		InputStream is = null;
		File localFile = null;
		OutputStream os = null;
		boolean successful = false;

		try {
			fileItems = upload.getFileItems();

			if (fileItems.size() != 1) {
				s_logger.error("expected 1 file item but found {}", fileItems.size());
				throw new ServletException("Wrong number of file items");
			}

			FileItem item = fileItems.get(0);
			String filename = item.getName();
			is = item.getInputStream();

			String filePath = System.getProperty("java.io.tmpdir") + File.separator + filename;

			localFile = new File(filePath);
			if (localFile.exists()) {
				if (localFile.delete()) {
					s_logger.error("Cannot delete file: {}", filePath);
					throw new ServletException("Cannot delete file: " + filePath);
				}
			}

			try {
				localFile.createNewFile();
				localFile.deleteOnExit();
			} catch (IOException e) {
				s_logger.error("Cannot create file: {}", filePath, e);
				throw new ServletException("Cannot create file: " + filePath);				
			}

			try {
				os = new FileOutputStream(localFile);
			} catch (FileNotFoundException e) {
				s_logger.error("Cannot find file: {}", filePath, e);
				throw new ServletException("Cannot find file: " + filePath, e);				
			}

			s_logger.info("Copying uploaded package file to file: {}", filePath);

			try {
				IOUtils.copy(is, os);
			} catch (IOException e) {
				s_logger.error("Failed to copy deployment package file: {}", filename, e);
				throw new ServletException("Failed to copy deployment package file: " + filename, e);
			}

			try {
				os.close();
			} catch (IOException e) {
				s_logger.warn("Cannot close output stream", e);
			}

			URL url = localFile.toURI().toURL();
			String sUrl = url.toString();

			s_logger.info("Installing package...");
			try {
				deploymentAgentService.installDeploymentPackageAsync(sUrl);
				successful = true;
			} catch (Exception e) {
				s_logger.error("Package installation failed", e);
				throw new ServletException("Package installation failed", e);
			}
		} catch (IOException e) {
			throw e;
		} catch (ServletException e) {
			throw e;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					s_logger.warn("Cannot close output stream", e);
				}
			}
			if (localFile != null && !successful) {
				try {
					localFile.delete();
				} catch (Exception e) {
					s_logger.warn("Cannot delete file");
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					s_logger.warn("Cannot close input stream", e);
				}
			}
			if (fileItems != null) {
				for (FileItem fileItem : fileItems) {
					fileItem.delete();
				}
			}
		}
	}

	private void doPostDeploy(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		ServiceLocator locator = ServiceLocator.getInstance();
		DeploymentAgentService deploymentAgentService;
		try {
			deploymentAgentService = locator.getService(DeploymentAgentService.class);
		} catch (GwtKuraException e) {
			s_logger.error("Error locating DeploymentAgentService", e);
			throw new ServletException("Error locating DeploymentAgentService", e);
		}

		String reqPathInfo = req.getPathInfo();
		if (reqPathInfo.endsWith("url")) {

			String packageDownloadUrl = req.getParameter("packageUrl");
			if (packageDownloadUrl == null) {
				s_logger.error("Deployment package URL parameter missing");
				throw new ServletException("Deployment package URL parameter missing");	
			}

			// BEGIN XSRF - Servlet dependent code
			String tokenId= req.getParameter("xsrfToken");

			try {
				GwtXSRFToken token = new GwtXSRFToken(tokenId);
				KuraRemoteServiceServlet.checkXSRFToken(req, token);
			}
			catch (Exception e) {
				throw new ServletException("Security error: please retry this operation correctly.", e);
			}
			// END XSRF security check

			try {
				s_logger.info("Installing package...");
				deploymentAgentService.installDeploymentPackageAsync(packageDownloadUrl);
			} catch (Exception e) {
				s_logger.error("Failed to install package at URL {}", packageDownloadUrl, e);
				throw new ServletException("Error installing deployment package", e);
			}			
		} else if (reqPathInfo.endsWith("upload")) {
			doPostDeployUpload(req, resp);
		} else {
			s_logger.error("Unsupported package deployment request");
			throw new ServletException("Unsupported package deployment request");			
		}
	}
	
	private void getZipUploadSizeMax(){
		ServiceLocator locator = ServiceLocator.getInstance();
		try {
			SystemService systemService = locator.getService(SystemService.class);
			int sizeInMB= systemService.getFileCommandZipMaxUploadSize();
			int sizeInBytes= sizeInMB * 1024 * 1024;
			tooBig = sizeInBytes;
		} catch (GwtKuraException e) {
			s_logger.error("Error locating SystemService", e);
		}
	}
	
	private void getZipUploadCountMax(){
		ServiceLocator locator = ServiceLocator.getInstance();
		try {
			SystemService systemService = locator.getService(SystemService.class);
			tooMany = systemService.getFileCommandZipMaxUploadNumber();
		} catch (GwtKuraException e) {
			s_logger.error("Error locating SystemService", e);
		}
	}

	static long getFileUploadSizeMax() {
		ServiceLocator locator = ServiceLocator.getInstance();

		long sizeMax = -1;
		try {
			SystemService systemService = locator.getService(SystemService.class);
			sizeMax = Long.parseLong(
					systemService.getProperties().getProperty("file.upload.size.max", "-1"));
		} catch (GwtKuraException e) {
			s_logger.error("Error locating SystemService", e);
		}

		return sizeMax;
	}

	static private int getFileUploadInMemorySizeThreshold() {
		ServiceLocator locator = ServiceLocator.getInstance();

		int sizeThreshold = DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD;
		try {
			SystemService systemService = locator.getService(SystemService.class);
			sizeThreshold = Integer.parseInt(
					systemService.getProperties().getProperty("file.upload.in.memory.size.threshold",
							String.valueOf(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD)));
		} catch (GwtKuraException e) {
			s_logger.error("Error locating SystemService", e);
		}

		return sizeThreshold;
	}
}

class UploadRequest extends ServletFileUpload {

	private static Logger s_logger = LoggerFactory.getLogger(UploadRequest.class);

	Map<String, String> formFields;
	List<FileItem> fileItems;

	public UploadRequest(DiskFileItemFactory diskFileItemFactory) {
		super(diskFileItemFactory);
		setSizeMax(FileServlet.getFileUploadSizeMax());
		formFields = new HashMap<String, String>();
		fileItems = new ArrayList<FileItem>();
	}

	@SuppressWarnings("unchecked")
	public void parse(HttpServletRequest req) throws FileUploadException {

		s_logger.debug("upload.getFileSizeMax(): {}", getFileSizeMax());
		s_logger.debug("upload.getSizeMax(): {}", getSizeMax());

		// Parse the request
		List<FileItem> items = null;
		items = parseRequest(req);

		// Process the uploaded items
		Iterator<FileItem> iter = items.iterator();
		while (iter.hasNext()) {
			FileItem item = (FileItem) iter.next();

			if (item.isFormField()) {
				String name = item.getFieldName();
				String value = item.getString();

				s_logger.debug("Form field item name: {}, value: {}", name, value);

				formFields.put(name, value);
			} else {
				String fieldName = item.getFieldName();
				String fileName = item.getName();
				String contentType = item.getContentType();
				boolean isInMemory = item.isInMemory();
				long sizeInBytes = item.getSize();

				s_logger.debug("File upload item name: {}, fileName: {}, contentType: {}, isInMemory: {}, size: {}",
						new Object[] {fieldName, fileName, contentType, isInMemory, sizeInBytes});

				fileItems.add(item);
			}
		}
	}

	public Map<String, String> getFormFields() {
		return formFields;
	}

	public List<FileItem> getFileItems() {
		return fileItems;
	}
}