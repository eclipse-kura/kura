/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.db.sqlite.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.db.BaseDbService;
import org.junit.After;
import org.osgi.framework.InvalidSyntaxException;

public class SqliteDbServiceTestBase {

    private static final String SQLITE_DB_SERVICE_FACTORY_PID = "org.eclipse.kura.db.SQLiteDbService";
    private static final AtomicInteger currentId = new AtomicInteger(0);

    // Settle window for the DS factory-configuration component. In the bnd/equinox test runtime the
    // newly created component can be briefly deactivated/reactivated by the ConfigurationAdmin event
    // queue right after creation (and felix.scr replaces the component instance on reactivation), so the
    // service object resolved at creation time may transiently report "Database is not initialized" or be
    // stale. We re-resolve the current service for the pid and poll getConnection() until it succeeds,
    // bounded by this timeout (a permanently-unusable instance, e.g. a second instance on the same db
    // path, simply exhausts the timeout and the test then asserts on its expected failure).
    private static final long DB_READY_TIMEOUT_MS = 10_000;
    private static final long DB_READY_POLL_INTERVAL_MS = 50;

    private final ConfigurationService configurationService;
    private final List<String> createdPids = new ArrayList<>();
    private final Map<Path, Long> fileSize = new HashMap<>();
    private BaseDbService dbService;
    private Optional<Path> temporaryDirectory = Optional.empty();
    protected Optional<Exception> exception = Optional.empty();

    public SqliteDbServiceTestBase() throws InterruptedException, ExecutionException, TimeoutException {
        this.configurationService = ServiceUtil.trackService(ConfigurationService.class, Optional.empty()).get(60,
                TimeUnit.SECONDS);
    }

    @After
    public void cleanup() throws InterruptedException, ExecutionException, TimeoutException {
        for (final String pid : createdPids) {
            ServiceUtil.deleteFactoryConfiguration(configurationService, pid).get(60, TimeUnit.SECONDS);
        }
    }

    protected void givenSqliteDbService(final Map<String, Object> properties)
            throws InterruptedException, ExecutionException, TimeoutException {

        final String pid = "testDb" + currentId.incrementAndGet();

        this.dbService = ServiceUtil.createFactoryConfiguration(configurationService, BaseDbService.class, pid,
                SQLITE_DB_SERVICE_FACTORY_PID, properties).get(30, TimeUnit.SECONDS);

        createdPids.add(pid);

        awaitDbServiceReady(pid);
    }

    /**
     * Waits for the {@link BaseDbService} registered with the given pid to become usable, re-resolving the
     * current service instance (which felix.scr may replace on reactivation) and polling getConnection()
     * until it succeeds or {@link #DB_READY_TIMEOUT_MS} elapses. Failures are swallowed: a component that
     * is intentionally never usable (e.g. a second instance claiming an already-open db file) just exhausts
     * the timeout, leaving {@link #dbService} pointing at the current instance so the test can assert on the
     * expected error.
     */
    private void awaitDbServiceReady(final String pid) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + DB_READY_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            final Optional<BaseDbService> current = currentDbService(pid);

            if (current.isPresent()) {
                this.dbService = current.get();
                try (final Connection conn = current.get().getConnection()) {
                    return;
                } catch (final Exception e) {
                    // component still (re)activating or intentionally unusable - retry until the deadline
                }
            }

            Thread.sleep(DB_READY_POLL_INTERVAL_MS);
        }

        currentDbService(pid).ifPresent(svc -> this.dbService = svc);
    }

    private Optional<BaseDbService> currentDbService(final String pid) {
        try {
            return Optional.ofNullable(ServiceUtil
                    .trackService(BaseDbService.class, Optional.of("(kura.service.pid=" + pid + ")"))
                    .get(30, TimeUnit.SECONDS));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    protected void givenExecutedQuery(final String query) {
        whenQueryIsPerformed(query);
        thenNoExceptionIsThrown();
    }

    protected void givenInitialFileSize(final String pathStr) throws IOException {
        final Path path = new File(pathStr).toPath();

        this.fileSize.put(path, Files.size(path));
    }

    protected void whenAConnectionIsRequested() {
        try (final Connection conn = dbService.getConnection()) {
        } catch (final Exception e) {
            this.exception = Optional.of(e);
        }
    }

    protected void whenQueryIsPerformed(final String query) {
        try (final Connection conn = dbService.getConnection(); final Statement statement = conn.createStatement()) {
            statement.executeUpdate(query);
        } catch (final Exception e) {
            this.exception = Optional.of(e);
        }
    }

    protected void whenSqliteDbServiceIsUpdated(final Map<String, Object> properties)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {

        final String pid = "testDb" + currentId.get();

        ServiceUtil.updateComponentConfiguration(configurationService, pid, properties).get(30, TimeUnit.SECONDS);

    }

    protected void whenTimePasses(final long duration, final TimeUnit timeUnit) throws InterruptedException {
        Thread.sleep(timeUnit.toMillis(duration));
    }

    protected void thenFileExists(final String path) {
        assertTrue(new File(path).exists());
    }

    protected void thenFileSizeIs(final String path, final int size) throws IOException {
        assertEquals(size, Files.size(new File(path).toPath()));
    }

    protected void thenFileSizeIsNotZero(final String path) throws IOException {
        assertNotEquals(0, Files.size(new File(path).toPath()));
    }

    protected void thenFileSizeDecreased(final String pathStr) throws IOException {
        final Path path = new File(pathStr).toPath();

        final long before = this.fileSize.get(path);
        final long after = Files.size(path);

        assertTrue("file size did not decrease, before " + before + " after " + after, after < before);
    }

    protected void thenFileSizeDidNotChange(String pathStr) throws IOException {
        final Path path = new File(pathStr).toPath();

        final long before = this.fileSize.get(path);
        final long after = Files.size(path);

        assertEquals("file size decreased, before " + before + " after " + after, before, after);

    }

    protected void thenFileDoesNotExist(final String path) {
        assertFalse(new File(path).exists());
    }

    protected void thenNoExceptionIsThrown() {
        assertEquals(Optional.empty(), this.exception);
    }

    protected void thenExceptionIsThrown() {
        assertTrue(this.exception.isPresent());
        this.exception = Optional.empty();
    }

    protected <E extends Exception> void thenExceptionIsThrown(final Class<E> clazz, final String messageSubstring) {
        assertTrue(this.exception.isPresent());
        assertTrue(clazz.isInstance(this.exception.get()));
        assertTrue(this.exception.get().getMessage().contains(messageSubstring));
        this.exception = Optional.empty();
    }

    protected Path getOrCreateTemporaryDirectory() throws IOException {
        if (temporaryDirectory.isPresent()) {
            return temporaryDirectory.get();
        } else {
            temporaryDirectory = Optional.of(Files.createTempDirectory(null).toAbsolutePath());
            return getOrCreateTemporaryDirectory();
        }
    }

    protected String temporaryDirectory() throws IOException {
        return getOrCreateTemporaryDirectory().toString();
    }

    protected String dbServicePid() {
        return "testDb" + currentId.get();
    }

    protected String largeText(final int size) {
        final StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < size; i++) {
            stringBuilder.append('a');
        }

        return stringBuilder.toString();
    }

    protected Map<String, Object> map(final Object... objects) {
        final Map<String, Object> result = new HashMap<>();

        final Iterator<Object> iter = Arrays.asList(objects).iterator();

        while (iter.hasNext()) {
            final String key = (String) iter.next();
            final Object value = iter.next();

            result.put(key, value);
        }

        return result;
    }
}
