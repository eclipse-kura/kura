/*******************************************************************************
 * Copyright (c) 2019, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.servlet;

import static java.util.Objects.isNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.PrivilegedExecutorService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.RequiredPermissions.Mode;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogServlet extends AuditServlet {

    private static final long serialVersionUID = 3969980124054250070L;

    private static Logger logger = LoggerFactory.getLogger(LogServlet.class);
    private static final String KURA_JOURNAL_LOG_FILE_NAME = "kura_journal.log";
    private static final String SYSTEM_JOURNAL_LOG_FILE_NAME = "system_journal.log";
    private static final String TEMP_ZIP_FILE_NAME = "Kura_Logs.zip";
    private static final String ARCHIVE_NAME_PREFIX = "Kura_Logs";
    private static final String ARCHIVE_NAME_EXTENSION = ".zip";
    private static final DateTimeFormatter ARCHIVE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern UNSAFE_FILE_NAME_CHARS = Pattern.compile("[^A-Za-z0-9._-]+");
    private static final int MAX_DEVICE_NAME_LENGTH = 40;
    private static final String TEMP_DIR_PREFIX = "kura_logs_";
    private static final String JOURNALCTL_CMD = "journalctl";
    private static final String COOKIE_NAME_PREFIX = "LogsDownload-";
    private static final Pattern NONCE_PATTERN = Pattern.compile("-?[0-9]{1,32}");

    public LogServlet() {
        super("UI Log Download", "Download device logs");
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException {
        KuraRemoteServiceServlet.requirePermissions(httpServletRequest, Mode.ALL,
                new String[] { KuraPermission.DEVICE });

        // BEGIN XSRF - Servlet dependent code
        try {
            GwtXSRFToken token = new GwtXSRFToken(httpServletRequest.getParameter("xsrfToken"));
            KuraRemoteServiceServlet.checkXSRFToken(httpServletRequest, token);
        } catch (Exception e) {
            throw new ServletException("Security error: please retry this operation correctly.", e);
        }
        // END XSRF security check

        String nonce = httpServletRequest.getParameter("nonce");

        SystemService ss = null;
        ServiceLocator locator = ServiceLocator.getInstance();
        try {
            ss = locator.getService(SystemService.class);
        } catch (GwtKuraException e1) {
            logger.warn("Unable to get service");
            releaseClient(httpServletResponse, nonce);
            return;
        }

        PrivilegedExecutorService pes = null;
        try {
            pes = locator.getService(PrivilegedExecutorService.class);
        } catch (GwtKuraException e1) {
            logger.warn("Unable to get service");
            releaseClient(httpServletResponse, nonce);
            return;
        }

        // Every request works in its own private temporary directory: concurrent downloads must not
        // truncate, overwrite or delete each other's journal dumps and archives.
        Path tempDir = null;
        try {
            tempDir = createPrivateTempDirectory();

            List<File> fileList = collectLogFiles(ss);
            fileList.addAll(collectJournalLogs(ss, pes, tempDir));

            String archiveName = buildArchiveName(ss.getDeviceName(), LocalDateTime.now());
            createReply(httpServletResponse, fileList, tempDir, nonce, archiveName);
        } catch (IOException e) {
            logger.warn("Unable to create zip file containing log resources", e);
            releaseClient(httpServletResponse, nonce);
        } finally {
            deleteTempDirectory(tempDir);
        }
    }

    private List<File> collectLogFiles(SystemService ss) {
        List<String> paths = new ArrayList<>();

        String logSourcesVal = ss.getProperties().getProperty("kura.log.download.sources", "/var/log");
        if (logSourcesVal != null && !logSourcesVal.trim().isEmpty()) {
            String[] logSources = logSourcesVal.split(",");
            paths.addAll(Arrays.asList(logSources));
        }

        List<File> fileList = new ArrayList<>();
        paths.stream().forEach(path -> {
            try (Stream<Path> kuraLogDirStream = Files.list(Paths.get(path));) {
                fileList.addAll(kuraLogDirStream.filter(filePath -> filePath.toFile().isFile()).map(Path::toFile)
                        .collect(Collectors.toList()));
            } catch (IOException e) {
                logger.warn("Unable to fetch log files for {}", path);
            }
        });

        return fileList;
    }

    private List<File> collectJournalLogs(SystemService ss, PrivilegedExecutorService pes, Path tempDir) {
        String outputFields = ss.getProperties().getProperty("kura.log.download.journal.fields",
                "SYSLOG_IDENTIFIER,PRIORITY,MESSAGE,STACKTRACE");

        List<File> journalFiles = new ArrayList<>();

        Path kuraJournalLogFile = tempDir.resolve(KURA_JOURNAL_LOG_FILE_NAME);
        if (writeJournalLog(pes, outputFields, kuraJournalLogFile.toString(), "kura")
                && Files.isRegularFile(kuraJournalLogFile)) {
            journalFiles.add(kuraJournalLogFile.toFile());
        } else {
            logger.warn("Error producing: {}", kuraJournalLogFile);
        }

        Path systemJournalLogFile = tempDir.resolve(SYSTEM_JOURNAL_LOG_FILE_NAME);
        if (writeJournalLog(pes, outputFields, systemJournalLogFile.toString())
                && Files.isRegularFile(systemJournalLogFile)) {
            journalFiles.add(systemJournalLogFile.toFile());
        } else {
            logger.warn("Error producing: {}", systemJournalLogFile);
        }

        return journalFiles;
    }

    private void createReply(HttpServletResponse httpServletResponse, List<File> fileList, Path tempDir, String nonce,
            String archiveName) throws IOException {

        // the archive is assembled on disk instead of in memory: /var/log can be arbitrarily large,
        // and the completion cookie must be sent only once the archive is actually ready
        Path zipPath = tempDir.resolve(TEMP_ZIP_FILE_NAME);
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
            zipFiles(zos, fileList);
        }

        addDownloadCompletedCookie(httpServletResponse, nonce);
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + archiveName + "\"");
        httpServletResponse.setContentLengthLong(Files.size(zipPath));

        ServletOutputStream sos = httpServletResponse.getOutputStream();
        Files.copy(zipPath, sos);
        sos.flush();
    }

    /**
     * Names the downloaded archive after the device and the moment it was taken, so that logs
     * collected from several gateways, or from the same one at different times, stay distinguishable
     * once downloaded.
     */
    String buildArchiveName(String deviceName, LocalDateTime timestamp) {
        StringBuilder archiveName = new StringBuilder(ARCHIVE_NAME_PREFIX);

        String sanitizedDeviceName = sanitizeForFileName(deviceName);
        if (!sanitizedDeviceName.isEmpty()) {
            archiveName.append('_').append(sanitizedDeviceName);
        }

        archiveName.append('_').append(ARCHIVE_NAME_TIMESTAMP.format(timestamp));

        return archiveName.append(ARCHIVE_NAME_EXTENSION).toString();
    }

    String sanitizeForFileName(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = UNSAFE_FILE_NAME_CHARS.matcher(value.trim()).replaceAll("_");

        int start = 0;
        while (start < sanitized.length() && isTrimmableFileNameChar(sanitized.charAt(start))) {
            start++;
        }
        int end = sanitized.length();
        while (end > start && isTrimmableFileNameChar(sanitized.charAt(end - 1))) {
            end--;
        }
        sanitized = sanitized.substring(start, end);

        return sanitized.length() > MAX_DEVICE_NAME_LENGTH ? sanitized.substring(0, MAX_DEVICE_NAME_LENGTH)
                : sanitized;
    }

    private static boolean isTrimmableFileNameChar(char c) {
        return c == '.' || c == '_' || c == '-';
    }

    /**
     * Lets the client stop waiting when the archive cannot be produced: without the completion cookie
     * the browser keeps the wait modal up until its own retry limit expires.
     */
    private void releaseClient(HttpServletResponse httpServletResponse, String nonce) {
        if (httpServletResponse.isCommitted()) {
            return;
        }

        addDownloadCompletedCookie(httpServletResponse, nonce);
        httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private void addDownloadCompletedCookie(HttpServletResponse httpServletResponse, String nonce) {
        // the nonce ends up in a cookie name: an unexpected value would make the Cookie constructor throw
        if (nonce == null || !NONCE_PATTERN.matcher(nonce).matches()) {
            logger.warn("Invalid download nonce, the completion cookie will not be set");
            return;
        }

        Cookie downloadedCookie = new Cookie(COOKIE_NAME_PREFIX + nonce, "finished");
        downloadedCookie.setPath("/");
        httpServletResponse.addCookie(downloadedCookie);
    }

    void zipFiles(ZipOutputStream zos, List<File> files) throws IOException {
        byte[] bytes = new byte[2048];
        Set<String> usedEntryNames = new HashSet<>();

        for (File file : files) {
            InputStream fileInput;
            try {
                fileInput = new BufferedInputStream(new FileInputStream(file));
            } catch (IOException e) {
                // a rotated or unreadable log file must not compromise the whole archive
                logger.warn("Unable to read log file {}, skipping it", file.getAbsolutePath());
                continue;
            }

            try (InputStream bis = fileInput) {
                zos.putNextEntry(new ZipEntry(uniqueEntryName(usedEntryNames, file.getName())));

                int bytesRead;
                while ((bytesRead = bis.read(bytes)) != -1) {
                    zos.write(bytes, 0, bytesRead);
                }
                zos.closeEntry();
            }
        }
    }

    String uniqueEntryName(Set<String> usedEntryNames, String fileName) {
        if (usedEntryNames.add(fileName)) {
            return fileName;
        }

        // same file name coming from two different source directories
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        int suffix = 1;
        String candidate;
        do {
            candidate = baseName + "_" + suffix + extension;
            suffix++;
        } while (!usedEntryNames.add(candidate));

        return candidate;
    }

    private boolean writeJournalLog(PrivilegedExecutorService pes, String outputFields, String outputFile) {
        return writeJournalLog(pes, outputFields, outputFile, null);
    }

    private boolean writeJournalLog(PrivilegedExecutorService pes, String outputFields, String outputFile,
            String unit) {

        List<String> commandSequence = new ArrayList<>();

        commandSequence.add(JOURNALCTL_CMD);
        commandSequence.add("--no-pager");

        if (!isNull(unit)) {
            commandSequence.add("-u");
            commandSequence.add(unit);
        }

        commandSequence.add("-o");
        commandSequence.add("verbose");
        commandSequence.add("--output-fields=" + outputFields);
        commandSequence.add(">");
        commandSequence.add(outputFile);

        Command command = new Command(commandSequence.toArray(new String[commandSequence.size()]));
        if (logger.isDebugEnabled()) {
            logger.debug("Executing command: {}", String.join(" ", command.getCommandLine()));
        }
        command.setExecuteInAShell(true);
        CommandStatus status = pes.execute(command);

        return status.getExitStatus().isSuccessful();
    }

    Path createPrivateTempDirectory() throws IOException {
        return Files.createTempDirectory(TEMP_DIR_PREFIX,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    }

    void deleteTempDirectory(Path tempDir) {
        if (tempDir == null) {
            return;
        }

        try (Stream<Path> tempDirStream = Files.walk(tempDir)) {
            tempDirStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    logger.warn("Unable to delete temporary log file {}", path, e);
                }
            });
        } catch (IOException e) {
            logger.warn("Unable to delete temporary log directory {}", tempDir, e);
        }
    }
}
