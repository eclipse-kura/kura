/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.IOUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.util.AssetConfigValidator;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileServlet extends HttpServlet {

    private static final String FAILED_TO_MANAGE_ASSET_CONFIGURATION_UPLOAD_FOR_USER_SESSION_CAUSE = "UI Asset - Failure - Failed to manage asset configuration upload for user: {}, session: {}. Cause: ";

    private static final String CANNOT_CLOSE_INPUT_STREAM = "Cannot close input stream";

    private static final String CANNOT_CLOSE_OUTPUT_STREAM = "Cannot close output stream";

    private static final String XSRF_TOKEN = "xsrfToken";

    private static final String ERROR_PARSING_THE_FILE_UPLOAD_REQUEST = "Error parsing the file upload request";

    private static final String ERROR_PARSING_QUERY_STRING = "Error parsing query string.";

    private static final String REQUEST_PATH_INFO_NOT_FOUND = "Request path info not found";

    private static final long serialVersionUID = -5016170117606322129L;

    private static Logger logger = LoggerFactory.getLogger(FileServlet.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static final String EXPECTED_1_FILE_PATTERN = "expected 1 file item but found {}";

    private static final int BUFFER = 1024;
    private static int tooBig = 0x6400000; // Max size of unzipped data, 100MB
    private static int tooMany = 1024;     // Max number of files

    private DiskFileItemFactory diskFileItemFactory;
    private FileCleaningTracker fileCleaningTracker;

    private final BundleContext bundleContext;

    public FileServlet() {
        this.bundleContext = FrameworkUtil.getBundle(FileServlet.class).getBundleContext();
    }

    @Override
    public void destroy() {
        super.destroy();

        logger.info("Servlet {} destroyed", getServletName());

        if (this.fileCleaningTracker != null) {
            logger.info("Number of temporary files tracked: {}", this.fileCleaningTracker.getTrackCount());
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();

        logger.info("Servlet {} initialized", getServletName());

        ServletContext ctx = getServletContext();
        this.fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(ctx);

        getZipUploadSizeMax();
        getZipUploadCountMax();

        int sizeThreshold = getFileUploadInMemorySizeThreshold();
        File repository = new File(System.getProperty(JAVA_IO_TMPDIR));

        logger.info("DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD: {}", DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        logger.info("DiskFileItemFactory: using size threshold of: {}", sizeThreshold);

        this.diskFileItemFactory = new DiskFileItemFactory(sizeThreshold, repository);
        this.diskFileItemFactory.setFileCleaningTracker(this.fileCleaningTracker);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("image/jpeg");

        String reqPathInfo = req.getPathInfo();

        if (reqPathInfo == null) {
            logger.error(REQUEST_PATH_INFO_NOT_FOUND);
            throw new ServletException(REQUEST_PATH_INFO_NOT_FOUND);
        }

        logger.debug("req.getRequestURI(): {}", req.getRequestURI());
        logger.debug("req.getRequestURL(): {}", req.getRequestURL());
        logger.debug("req.getPathInfo(): {}", req.getPathInfo());

        if (reqPathInfo.startsWith("/icon")) {
            doGetIcon(req, resp);
        } else {
            logger.error("Unknown request path info: {}", reqPathInfo);
            throw new ServletException("Unknown request path info: " + reqPathInfo);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        HttpSession session = req.getSession(false);

        String reqPathInfo = req.getPathInfo();
        if (reqPathInfo == null) {
            logger.error(REQUEST_PATH_INFO_NOT_FOUND);
            auditLogger.warn(
                    FAILED_TO_MANAGE_ASSET_CONFIGURATION_UPLOAD_FOR_USER_SESSION_CAUSE + REQUEST_PATH_INFO_NOT_FOUND,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new ServletException(REQUEST_PATH_INFO_NOT_FOUND);
        }

        logger.debug("req.getRequestURI(): {}", req.getRequestURI());
        logger.debug("req.getRequestURL(): {}", req.getRequestURL());
        logger.debug("req.getPathInfo(): {}", req.getPathInfo());

        if (reqPathInfo.startsWith("/deploy")) {
            doPostDeploy(req);
        } else if (reqPathInfo.equals("/configuration/snapshot")) {
            doPostConfigurationSnapshot(req);
        } else if (reqPathInfo.equals("/command")) {
            doPostCommand(req);
        } else if (reqPathInfo.equals("/asset")) {
            doPostAsset(req, resp);
        } else {
            logger.error("Unknown request path info: {}", reqPathInfo);

            auditLogger.warn(
                    "UI Asset - Failure - Failed to manage asset configuration upload for user: {}, session: {}. Cause: Unknown request path info: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), reqPathInfo);
            throw new ServletException("Unknown request path info: " + reqPathInfo);
        }
    }

    private void doGetIcon(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String queryString = req.getQueryString();

        if (queryString == null) {
            logger.error(ERROR_PARSING_QUERY_STRING);
            throw new ServletException(ERROR_PARSING_QUERY_STRING);
        }

        // Parse the query string
        Map<String, String> pairs;
        try {
            pairs = parseQueryString(queryString);
        } catch (UnsupportedEncodingException e) {
            logger.error(ERROR_PARSING_QUERY_STRING, e);
            throw new ServletException("Error parsing query string: " + e.getLocalizedMessage());
        }

        // Check for malformed request
        if (pairs == null || pairs.size() != 1) {
            logger.error(ERROR_PARSING_QUERY_STRING);
            throw new ServletException(ERROR_PARSING_QUERY_STRING);
        }

        final boolean factories;
        final String pid;
        if (pairs.containsKey("factoryId")) {
            factories = true;
            pid = pairs.get("factoryId");
        } else {
            factories = false;
            pid = pairs.get("pid");
        }

        if (pid != null && pid.length() > 0) {
            Bundle[] bundles = this.bundleContext.getBundles();
            ServiceLocator locator = ServiceLocator.getInstance();

            final MetaTypeService mts;
            try {
                mts = locator.getService(MetaTypeService.class);
            } catch (GwtKuraException e) {
                logger.error("Error getting MetaTypeService", e);
                throw new ServletException("Error getting MetaTypeService", e);
            }

            // Iterate over bundles to find PID
            for (Bundle b : bundles) {
                MetaTypeInformation mti = mts.getMetaTypeInformation(b);

                if (mti == null) {
                    continue;
                }

                final String[] ids = factories ? mti.getFactoryPids() : mti.getPids();

                for (String p : ids) {
                    if (p.equals(pid)) {
                        try (InputStream is = mti.getObjectClassDefinition(pid, null).getIcon(32)) {
                            if (is == null) {
                                logger.error("Error reading icon file.");
                                throw new ServletException("Error reading icon file.");
                            }
                            try (OutputStream os = resp.getOutputStream()) {
                                IOUtils.copy(is, os);
                            }
                        }

                        // break for loop ... only send one icon
                        break;
                    }
                }
            }
        } else {
            logger.error(ERROR_PARSING_QUERY_STRING);
            throw new ServletException(ERROR_PARSING_QUERY_STRING);
        }

    }

    private Map<String, String> parseQueryString(String queryString) throws UnsupportedEncodingException {
        Map<String, String> qp = new HashMap<>();

        String[] pairs = queryString.split("&");
        for (String p : pairs) {
            int index = p.indexOf('=');
            qp.put(URLDecoder.decode(p.substring(0, index), StandardCharsets.UTF_8.name()),
                    URLDecoder.decode(p.substring(index + 1), StandardCharsets.UTF_8.name()));
        }
        return qp;
    }

    private void doPostCommand(HttpServletRequest req) throws ServletException, IOException {
        UploadRequest upload = new UploadRequest(this.diskFileItemFactory);

        HttpSession session = req.getSession(false);

        try {
            upload.parse(req);
        } catch (FileUploadException e) {
            logger.error(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST);
            auditLogger.warn(
                    "UI Command - Failure - Failed to execute command file upload for user: {}, session: {}. Cause: "
                            + ERROR_PARSING_THE_FILE_UPLOAD_REQUEST,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new ServletException(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST);
        }

        // BEGIN XSRF - Servlet dependent code
        Map<String, String> formFields = upload.getFormFields();

        try {
            GwtXSRFToken token = new GwtXSRFToken(formFields.get(XSRF_TOKEN));
            KuraRemoteServiceServlet.checkXSRFToken(req, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.");
        }
        // END XSRF security check

        List<FileItem> fileItems = null;
        InputStream is = null;
        File localFolder = new File(System.getProperty(JAVA_IO_TMPDIR));
        OutputStream os = null;

        try {
            fileItems = upload.getFileItems();

            if (!fileItems.isEmpty()) {
                FileItem item = fileItems.get(0);
                is = item.getInputStream();

                byte[] bytes = IOUtils.toByteArray(is);
                ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));

                int entries = 0;
                long total = 0;
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    byte[] buffer = new byte[BUFFER];

                    String expectedFilePath = new StringBuilder(localFolder.getPath()).append(File.separator)
                            .append(ze.getName()).toString();
                    String fileName = validateFileName(expectedFilePath, localFolder.getPath());
                    File newFile = new File(fileName);
                    if (expectedFilePath.endsWith("/")) {
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
                    while (total + BUFFER <= tooBig && (len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                        total += len;
                    }
                    fos.flush();
                    fos.close();

                    entries++;
                    if (entries > tooMany) {
                        auditLogger.warn(
                                "UI Command - Failure - Failed to execute command file upload for user: {}, session: {}. Cause: Too many files to unzip.",
                                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
                        throw new IllegalStateException("Too many files to unzip.");
                    }
                    if (total > tooBig) {
                        auditLogger.warn(
                                "UI Command - Failure - Failed to execute command file upload for user: {}, session: {}. Cause: the unzipped file is too big.",
                                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
                        throw new IllegalStateException("File being unzipped is too big.");
                    }

                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();
            }
            auditLogger.info(
                    "UI Command - Success - Successfully executed command file upload for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        } catch (IOException e) {
            auditLogger.warn("UI Command - Failure - Failed to execute command file upload for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            throw e;
        } catch (GwtKuraException e) {
            auditLogger.warn("UI Command - Failure - Failed to execute command file upload for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            throw new ServletException("File is outside extraction target directory.");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.warn(CANNOT_CLOSE_OUTPUT_STREAM, e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.warn(CANNOT_CLOSE_INPUT_STREAM, e);
                }
            }
            if (fileItems != null) {
                for (FileItem fileItem : fileItems) {
                    fileItem.delete();
                }
            }
        }
    }

    private String validateFileName(String zipFileName, String intendedDir) throws IOException, GwtKuraException {
        File zipFile = new File(zipFileName);
        String filePath = zipFile.getCanonicalPath();

        File iD = new File(intendedDir);
        String canonicalID = iD.getCanonicalPath();

        if (filePath.startsWith(canonicalID)) {
            return filePath;
        } else {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ACCESS);
        }
    }

    private void doPostAsset(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UploadRequest upload = new UploadRequest(this.diskFileItemFactory);
        List<String> errors = new ArrayList<>();

        HttpSession session = req.getSession(false);

        try {
            upload.parse(req);
        } catch (FileUploadException e) {
            errors.add(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST);
            resp.getWriter().write("Error parsing the file upload request.");
            auditLogger.warn(
                    FAILED_TO_MANAGE_ASSET_CONFIGURATION_UPLOAD_FOR_USER_SESSION_CAUSE
                            + ERROR_PARSING_THE_FILE_UPLOAD_REQUEST,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            return;
        }

        Map<String, String> formFields = upload.getFormFields();
        try {
            // BEGIN XSRF - Servlet dependent code
            GwtXSRFToken token = new GwtXSRFToken(formFields.get(XSRF_TOKEN));
            KuraRemoteServiceServlet.checkXSRFToken(req, token);
            // END XSRF security check

            List<FileItem> fileItems = upload.getFileItems();
            int fileItemsSize = fileItems.size();
            if (fileItemsSize != 1) {
                logger.error(EXPECTED_1_FILE_PATTERN, fileItemsSize);
                errors.add("Security error: please retry this operation.");

                auditLogger.warn(
                        FAILED_TO_MANAGE_ASSET_CONFIGURATION_UPLOAD_FOR_USER_SESSION_CAUSE + EXPECTED_1_FILE_PATTERN,
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), fileItemsSize);
                throw new ServletException();
            }

            FileItem fileItem = fileItems.get(0);
            byte[] data = fileItem.get();
            String csvString = new String(data, StandardCharsets.UTF_8);
            String assetPid = formFields.get("assetPid");
            String driverPid = formFields.get("driverPid");

            List<GwtConfigParameter> parametersFromCsv = AssetConfigValidator.get().validateCsv(csvString, driverPid,
                    errors);

            final GwtConfigComponent config = new GwtConfigComponent();
            config.setParameters(parametersFromCsv);

            session.setAttribute("kura.csv.config." + assetPid, config);

            auditLogger.info(
                    "UI Asset - Success - Successfully parsed CSV asset configuration for user: {}, session: {}, asset PID: {}, driver PID: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), assetPid, driverPid);
        } catch (KuraException | GwtKuraException | InterruptedException e) {
            logger.error("Error updating device configuration", e);
            auditLogger.warn(
                    "UI Asset - Failure - Failed to manage asset configuration upload for user: {}, session: {}. Cause: Error updating device configuration",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
        } catch (ServletException ex) {
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                errors.forEach(line -> sb.append(line).append("\n"));
                try {
                    resp.getWriter().write(sb.toString());
                } catch (Exception e) {
                    logger.error("Error while writing output", e);
                }
            } else {
                logger.error("Servlet exception.", ex);
            }
        } catch (Exception ex2) {
            logger.warn("Security error: please retry this operation correctly.", ex2);
            resp.getWriter().write("Security error: please retry this operation.");
            auditLogger.warn(
                    "UI Asset - Failure - Failed to manage asset configuration upload for user: {}, session: {}. Cause: Security error, please retry this operation correctly.",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), ex2);
        }

    }

    private void doPostConfigurationSnapshot(HttpServletRequest req) throws ServletException {

        UploadRequest upload = new UploadRequest(this.diskFileItemFactory);

        HttpSession session = req.getSession(false);

        try {
            upload.parse(req);
        } catch (FileUploadException e) {
            logger.error(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST);
            auditLogger.warn(
                    "UI Deploy Snapshots - Failure - Failed to upload snapshot for user: {}, session: {}. Cause: "
                            + ERROR_PARSING_THE_FILE_UPLOAD_REQUEST,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            throw new ServletException(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST);
        }

        // BEGIN XSRF - Servlet dependent code
        Map<String, String> formFields = upload.getFormFields();

        try {
            GwtXSRFToken token = new GwtXSRFToken(formFields.get(XSRF_TOKEN));
            KuraRemoteServiceServlet.checkXSRFToken(req, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        List<FileItem> fileItems = upload.getFileItems();
        int fileItemsSize = fileItems.size();
        if (fileItemsSize != 1) {
            logger.error(EXPECTED_1_FILE_PATTERN, fileItemsSize);
            auditLogger.warn(
                    "UI Deploy Snapshots - Failure - Failed to upload snapshot for user: {}, session: {}. Cause: "
                            + EXPECTED_1_FILE_PATTERN,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), fileItemsSize);
            throw new ServletException("Wrong number of file items");
        }

        FileItem fileItem = fileItems.get(0);
        byte[] data = fileItem.get();
        String xmlString = new String(data, StandardCharsets.UTF_8);
        XmlComponentConfigurations xmlConfigs;
        try {
            xmlConfigs = unmarshal(xmlString, XmlComponentConfigurations.class);
        } catch (Exception e) {
            logger.error("Error unmarshaling device configuration");
            auditLogger.warn(
                    "UI Deploy Snapshots - Failure - Failed to upload snapshot for user: {}, session: {}. Cause: Error unmarshaling device configuration",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            throw new ServletException("Error unmarshaling device configuration");
        }

        ServiceLocator locator = ServiceLocator.getInstance();
        try {

            ConfigurationService cs = locator.getService(ConfigurationService.class);
            List<ComponentConfiguration> configImpls = xmlConfigs.getConfigurations();

            List<ComponentConfiguration> configs = new ArrayList<>();
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
            auditLogger.info(
                    "UI Deploy Snapshots - Success - Successfully uploaded and applied snapshot for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        } catch (Exception e) {
            logger.error("Error updating device configuration");
            auditLogger.warn(
                    "UI Deploy Snapshots - Failure - Failed to upload snapshot for user: {}, session: {}. Cause: Error updating device configuration",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            throw new ServletException("Error updating device configuration");
        }
    }

    private void doPostDeployUpload(HttpServletRequest req, HttpSession session) throws ServletException, IOException {
        ServiceLocator locator = ServiceLocator.getInstance();
        DeploymentAgentService deploymentAgentService;
        try {
            deploymentAgentService = locator.getService(DeploymentAgentService.class);
        } catch (GwtKuraException e) {
            logger.error("Error locating DeploymentAgentService");
            throw new ServletException("Error locating DeploymentAgentService", e);
        }

        // Check that we have a file upload request
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        if (!isMultipart) {
            logger.error("Not a file upload request");
            throw new ServletException("Not a file upload request");
        }

        UploadRequest upload = new UploadRequest(this.diskFileItemFactory);

        try {
            upload.parse(req);
        } catch (FileUploadException e) {
            logger.error(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST);
            throw new ServletException(ERROR_PARSING_THE_FILE_UPLOAD_REQUEST, e);
        }

        // BEGIN XSRF - Servlet dependent code
        Map<String, String> formFields = upload.getFormFields();

        try {
            GwtXSRFToken token = new GwtXSRFToken(formFields.get(XSRF_TOKEN));
            KuraRemoteServiceServlet.checkXSRFToken(req, token);
        } catch (Exception e) {
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

            int fileItemsSize = fileItems.size();
            if (fileItemsSize != 1) {
                logger.error(EXPECTED_1_FILE_PATTERN, fileItemsSize);
                auditLogger.warn(
                        "UI Deploy - Failure - Failed to upload and install package for user: {}, session: {}. Cause: "
                                + EXPECTED_1_FILE_PATTERN,
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), fileItemsSize);
                throw new ServletException("Wrong number of file items");
            }

            FileItem item = fileItems.get(0);
            String filename = item.getName();
            is = item.getInputStream();

            String filePath = System.getProperty(JAVA_IO_TMPDIR) + File.separator + UUID.randomUUID() + ".dp";

            localFile = new File(filePath);
            if (localFile.exists() && !localFile.delete()) {
                logger.error("Cannot delete file: {}", filePath);
                auditLogger.warn(
                        "UI Deploy - Failure - Failed to upload and install package for user: {}, session: {}. Cause: Cannot delete file: {}",
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), filePath);
                throw new ServletException("Cannot delete file: " + filePath);
            }

            try {
                localFile.createNewFile();
                localFile.deleteOnExit();
            } catch (IOException e) {
                logger.error("Cannot create file: {}", filePath);
                auditLogger.warn(
                        "UI Deploy - Failure - Failed to upload and install package for user: {}, session: {}. Cause: Cannot create file: {}",
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), filename, e);
                throw new ServletException("Cannot create file: " + filePath);
            }

            try {
                os = new FileOutputStream(localFile);
            } catch (FileNotFoundException e) {
                logger.error("Cannot find file: {}", filePath);
                auditLogger.warn(
                        "UI Deploy - Failure - Failed to upload and install package for user: {}, session: {}. Cause: Cannot find file: {}",
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), filename, e);
                throw new ServletException("Cannot find file: " + filePath);
            }

            logger.info("Copying uploaded package file to file: {}", filePath);

            try {
                IOUtils.copy(is, os);
            } catch (IOException e) {
                logger.error("Failed to copy deployment package file: {}", filename);
                auditLogger.warn(
                        "UI Deploy - Failure - Failed to upload and install package for user: {}, session: {}. Cause: Failed to copy deployment package file: {}",
                        session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), filename, e);
                throw new ServletException("Failed to copy deployment package file: " + filename);
            }

            try {
                os.close();
            } catch (IOException e) {
                logger.warn(CANNOT_CLOSE_OUTPUT_STREAM, e);
            }

            URL url = localFile.toURI().toURL();
            String sUrl = url.toString();

            logger.info("Installing package...");
            try {
                deploymentAgentService.installDeploymentPackageAsync(sUrl);
                successful = true;
            } catch (Exception e) {
                logger.error("Package installation failed");
                throw new ServletException("Package installation failed", e);
            }

            auditLogger.info(
                    "UI Deploy - Success - Successfully uploaded and installed package for user: {}, session: {}, URL: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), sUrl);
        } catch (IOException e) {
            auditLogger.warn("UI Deploy - Failure - Failed to upload and install package for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            throw e;
        } catch (ServletException e) {
            auditLogger.warn("UI Deploy - Failure - Failed to upload and install package for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), e);
            throw e;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.warn(CANNOT_CLOSE_OUTPUT_STREAM, e);
                }
            }
            if (localFile != null && !successful) {
                try {
                    localFile.delete();
                } catch (Exception e) {
                    logger.warn("Cannot delete file");
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.warn(CANNOT_CLOSE_INPUT_STREAM, e);
                }
            }
            if (fileItems != null) {
                for (FileItem fileItem : fileItems) {
                    fileItem.delete();
                }
            }
        }
    }

    private void doPostDeploy(HttpServletRequest req) throws ServletException, IOException {

        ServiceLocator locator = ServiceLocator.getInstance();
        DeploymentAgentService deploymentAgentService;
        try {
            deploymentAgentService = locator.getService(DeploymentAgentService.class);
        } catch (GwtKuraException e) {
            logger.error("Error locating DeploymentAgentService");
            throw new ServletException("Error locating DeploymentAgentService", e);
        }

        HttpSession session = req.getSession(false);

        String reqPathInfo = req.getPathInfo();
        if (reqPathInfo.endsWith("url")) {

            String packageDownloadUrl = req.getParameter("packageUrl");
            if (packageDownloadUrl == null) {
                logger.error("Deployment package URL parameter missing");
                throw new ServletException("Deployment package URL parameter missing");
            }

            // BEGIN XSRF - Servlet dependent code
            String tokenId = req.getParameter(XSRF_TOKEN);

            try {
                GwtXSRFToken token = new GwtXSRFToken(tokenId);
                KuraRemoteServiceServlet.checkXSRFToken(req, token);
            } catch (Exception e) {
                throw new ServletException("Security error: please retry this operation correctly.", e);
            }
            // END XSRF security check

            try {
                logger.info("Installing package...");
                deploymentAgentService.installDeploymentPackageAsync(packageDownloadUrl);
            } catch (Exception e) {
                logger.error("Failed to install package at URL {}", packageDownloadUrl);
                throw new ServletException("Error installing deployment package", e);
            }

            auditLogger.info(
                    "UI Deploy - Success - Successfully deployed package from URL for user: {}, session: {}, package URL: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), packageDownloadUrl);
        } else if (reqPathInfo.endsWith("upload")) {
            doPostDeployUpload(req, session);
        } else {
            logger.error("Unsupported package deployment request");
            auditLogger.warn(
                    "UI Snapshots - Failure - Failed to deploy new package for user: {}, session: {}. Cause: Unsupported package deployment request",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new ServletException("Unsupported package deployment request");
        }
    }

    private void getZipUploadSizeMax() {
        ServiceLocator locator = ServiceLocator.getInstance();
        try {
            SystemService systemService = locator.getService(SystemService.class);
            int sizeInMB = systemService.getFileCommandZipMaxUploadSize();
            int sizeInBytes = sizeInMB * 1024 * 1024;
            tooBig = sizeInBytes;
        } catch (GwtKuraException e) {
            logger.error("Error locating SystemService", e);
        }
    }

    private void getZipUploadCountMax() {
        ServiceLocator locator = ServiceLocator.getInstance();
        try {
            SystemService systemService = locator.getService(SystemService.class);
            tooMany = systemService.getFileCommandZipMaxUploadNumber();
        } catch (GwtKuraException e) {
            logger.error("Error locating SystemService", e);
        }
    }

    static long getFileUploadSizeMax() {
        ServiceLocator locator = ServiceLocator.getInstance();

        long sizeMax = -1;
        try {
            SystemService systemService = locator.getService(SystemService.class);
            sizeMax = Long.parseLong(systemService.getProperties().getProperty("file.upload.size.max", "-1"));
        } catch (GwtKuraException e) {
            logger.error("Error locating SystemService", e);
        }

        return sizeMax;
    }

    private static int getFileUploadInMemorySizeThreshold() {
        ServiceLocator locator = ServiceLocator.getInstance();

        int sizeThreshold = DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD;
        try {
            SystemService systemService = locator.getService(SystemService.class);
            sizeThreshold = Integer
                    .parseInt(systemService.getProperties().getProperty("file.upload.in.memory.size.threshold",
                            String.valueOf(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD)));
        } catch (GwtKuraException e) {
            logger.error("Error locating SystemService", e);
        }

        return sizeThreshold;
    }

    private ServiceReference<Unmarshaller>[] getXmlUnmarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.xml.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(FrameworkUtil.getBundle(FileServlet.class).getBundleContext(),
                Unmarshaller.class, filterString);
    }

    private void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(FrameworkUtil.getBundle(FileServlet.class).getBundleContext(), refs);
    }

    protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
        T result = null;
        ServiceReference<Unmarshaller>[] unmarshallerSRs = getXmlUnmarshallers();
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                Unmarshaller unmarshaller = FrameworkUtil.getBundle(FileServlet.class).getBundleContext()
                        .getService(unmarshallerSR);
                result = unmarshaller.unmarshal(xmlString, clazz);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract persisted configuration.");
        } finally {
            ungetServiceReferences(unmarshallerSRs);
        }
        if (result == null) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, "configuration");
        }
        return result;
    }
}

class UploadRequest extends ServletFileUpload {

    private static Logger logger = LoggerFactory.getLogger(UploadRequest.class);

    Map<String, String> formFields;
    List<FileItem> fileItems;

    public UploadRequest(DiskFileItemFactory diskFileItemFactory) {
        super(diskFileItemFactory);
        setSizeMax(FileServlet.getFileUploadSizeMax());
        this.formFields = new HashMap<>();
        this.fileItems = new ArrayList<>();
    }

    public void parse(HttpServletRequest req) throws FileUploadException {

        logger.debug("upload.getFileSizeMax(): {}", getFileSizeMax());
        logger.debug("upload.getSizeMax(): {}", getSizeMax());

        // Parse the request
        List<FileItem> items = null;
        items = parseRequest(req);

        // Process the uploaded items
        Iterator<FileItem> iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = iter.next();

            if (item.isFormField()) {
                String name = item.getFieldName();
                String value = item.getString();

                logger.debug("Form field item name: {}, value: {}", name, value);

                this.formFields.put(name, value);
            } else {
                String fieldName = item.getFieldName();
                String fileName = item.getName();
                String contentType = item.getContentType();
                boolean isInMemory = item.isInMemory();
                long sizeInBytes = item.getSize();

                logger.debug("File upload item name: {}, fileName: {}, contentType: {}, isInMemory: {}, size: {}",
                        new Object[] { fieldName, fileName, contentType, isInMemory, sizeInBytes });

                this.fileItems.add(item);
            }
        }
    }

    public Map<String, String> getFormFields() {
        return this.formFields;
    }

    public List<FileItem> getFileItems() {
        return this.fileItems;
    }
}