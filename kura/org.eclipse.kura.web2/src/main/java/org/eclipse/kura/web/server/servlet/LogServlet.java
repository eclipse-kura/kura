/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.PrivilegedExecutorService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogServlet extends AuditServlet {

    private static final long serialVersionUID = 3969980124054250070L;

    private static Logger logger = LoggerFactory.getLogger(LogServlet.class);
    private static final String KURA_JOURNAL_LOG_FILE = "/tmp/kura_journal.log";
    private static final String SYSTEM_JOURNAL_LOG_FILE = "/tmp/system_journal.log";
    private static final String JOURNALCTL_CMD = "journalctl";

    public LogServlet() {
        super("UI Log Download", "Download device logs");
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        SystemService ss = null;
        ServiceLocator locator = ServiceLocator.getInstance();
        try {
            ss = locator.getService(SystemService.class);
        } catch (GwtKuraException e1) {
            logger.warn("Unable to get service");
            return;
        }

        PrivilegedExecutorService pes = null;
        try {
            pes = locator.getService(PrivilegedExecutorService.class);
        } catch (GwtKuraException e1) {
            logger.warn("Unable to get service");
            return;
        }

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

        String outputFields = ss.getProperties().getProperty("kura.log.download.journal.fields",
                "SYSLOG_IDENTIFIER,PRIORITY,MESSAGE,STACKTRACE");

        if (writeJournalLog(pes, outputFields, KURA_JOURNAL_LOG_FILE, "kura")) {
            fileList.add(new File(KURA_JOURNAL_LOG_FILE));
        } else {
            logger.warn("Error producing: {}", KURA_JOURNAL_LOG_FILE);
        }

        if (writeJournalLog(pes, outputFields, SYSTEM_JOURNAL_LOG_FILE)) {
            fileList.add(new File(SYSTEM_JOURNAL_LOG_FILE));
        } else {
            logger.warn("Error producing: {}", SYSTEM_JOURNAL_LOG_FILE);
        }

        createReply(httpServletResponse, fileList);
        removeTmpFiles();
    }

    private void createReply(HttpServletResponse httpServletResponse, List<File> fileList) {
        try {
            byte[] zip = zipFiles(fileList);
            ServletOutputStream sos = httpServletResponse.getOutputStream();
            httpServletResponse.setContentType("application/zip");
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"Kura_Logs.zip\"");

            sos.write(zip);
            sos.flush();
        } catch (IOException e) {
            logger.warn("Unable to create zip file containing log resources");
        }
    }

    private byte[] zipFiles(List<File> files) throws IOException {
        byte[] bytes = new byte[2048];

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos);) {
            for (File file : files) {
                zipFile(bytes, zos, file);
            }
            zos.flush();
            zos.close();
            baos.flush();
            return baos.toByteArray();
        }

    }

    private void zipFile(byte[] bytes, ZipOutputStream zos, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file.getCanonicalPath());
                BufferedInputStream bis = new BufferedInputStream(fis);) {

            zos.putNextEntry(new ZipEntry(file.getName()));

            int bytesRead;
            while ((bytesRead = bis.read(bytes)) != -1) {
                zos.write(bytes, 0, bytesRead);
            }
            zos.closeEntry();
        }
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

    private void removeTmpFiles() {
        try {
            Files.deleteIfExists(new File(KURA_JOURNAL_LOG_FILE).toPath());
            Files.deleteIfExists(new File(SYSTEM_JOURNAL_LOG_FILE).toPath());
        } catch (IOException e) {
            logger.warn("Unable to delete temporary log files", e);
        }
    }
}
