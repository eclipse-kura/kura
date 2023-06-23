/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.db.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.InvalidSyntaxException;

@RunWith(Parameterized.class)
public class DbWireComponentsTest extends DbComponentsTestBase {

    @Test
    public void shouldSupportInteger()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newIntegerValue(23));
    }

    @Test
    public void shouldSupportLong()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newLongValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newLongValue(23));
    }

    @Test
    public void shouldSupportBoolean()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newBooleanValue(true), "bar",
                TypedValues.newBooleanValue(false));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newBooleanValue(true));
        thenFilterEmitsEnvelopeWithProperty("bar", TypedValues.newBooleanValue(false));
    }

    @Test
    public void shouldSupportDouble()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newDoubleValue(1234.5d));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newDoubleValue(1234.5d));
    }

    @Test
    public void shouldSupportFloat()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newFloatValue(1234.5f));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newDoubleValue(1234.5d));
    }

    @Test
    public void shouldSupportString()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newStringValue("bar"));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newStringValue("bar"));
    }

    @Test
    public void shouldSupportByteArray()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newByteArrayValue(new byte[] { 1, 2, 3 }));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithByteArrayProperty("foo", new byte[] { 1, 2, 3 });
    }

    @Test
    public void shouldSupportMultipleEnvelopes()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(24));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(25));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty(0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(1, "foo", TypedValues.newIntegerValue(24));
        thenFilterEmitsEnvelopeWithProperty(2, "foo", TypedValues.newIntegerValue(25));
    }

    @Test
    public void shouldEmitEmptyEnvelopesByDefault()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" LIMIT 0;");

        thenFilterEmitsEmptyEnvelope();
    }

    @Test
    public void shouldNotEmitEmptyEnvelopesIfConfigured()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenFilterWithConfig(this.wireComponentTestTarget.filterEmitOnEmptyResultKey(), false);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" LIMIT 0;");

        thenFilterEmitNoEnvelope();
    }

    @Test
    public void shouldSupportCacheExpirationInterval()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenFilterWithConfig(this.wireComponentTestTarget.filterCacheExpirationIntervalKey(), 5);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));
        givenPerformedQuery("SELECT * FROM \"" + tableName + "\";");
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(24));
        givenPerformedQuery("SELECT * FROM \"" + tableName + "\";");

        whenTimePasses(6, TimeUnit.SECONDS);
        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" ORDER BY ID ASC;");

        thenFilterEmitsEnvelopeWithProperty(0, 0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(1, 0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(2, 0, "foo", TypedValues.newIntegerValue(23));
        thenFilterEmitsEnvelopeWithProperty(2, 1, "foo", TypedValues.newIntegerValue(24));
    }

    @Test
    public void shouldSupportMaximumTableSize()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenStoreWithConfig(this.wireComponentTestTarget.storeMaximumSizeKey(), 5);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(1));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(2));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(3));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(4));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(5));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(6));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(7));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" ORDER BY ID DESC;");

        thenEnvelopeRecordCountIs(0, 5);
        thenFilterEmitsEnvelopeWithProperty(0, 0, "foo", TypedValues.newIntegerValue(7));
        thenFilterEmitsEnvelopeWithProperty(0, 1, "foo", TypedValues.newIntegerValue(6));
        thenFilterEmitsEnvelopeWithProperty(0, 2, "foo", TypedValues.newIntegerValue(5));
        thenFilterEmitsEnvelopeWithProperty(0, 3, "foo", TypedValues.newIntegerValue(4));
        thenFilterEmitsEnvelopeWithProperty(0, 4, "foo", TypedValues.newIntegerValue(3));
    }

    @Test
    public void shouldSupportMaximumTableSize1()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenStoreWithConfig(this.wireComponentTestTarget.storeMaximumSizeKey(), 5);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(1));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(2));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(3));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(4));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(5));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(6));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(7));
        givenStoreWithConfig(this.wireComponentTestTarget.storeMaximumSizeKey(), 1);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(8));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" ORDER BY ID DESC;");

        thenEnvelopeRecordCountIs(0, 1);
        thenFilterEmitsEnvelopeWithProperty(0, 0, "foo", TypedValues.newIntegerValue(8));
    }

    @Test
    public void shouldSupportCleanupRecordKeep()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        givenStoreWithConfig(this.wireComponentTestTarget.storeMaximumSizeKey(), 5,
                this.wireComponentTestTarget.storeCleanupRecordsKeepKey(), 2);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(1));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(2));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(3));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(4));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(5));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(6));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\" ORDER BY ID DESC;");

        thenEnvelopeRecordCountIs(0, 2);
        thenFilterEmitsEnvelopeWithProperty(0, 0, "foo", TypedValues.newIntegerValue(6));
        thenFilterEmitsEnvelopeWithProperty(0, 1, "foo", TypedValues.newIntegerValue(5));
    }

    @Test
    public void shouldSupportStoreReconfiguration()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));

        whenDatabaseIsReconfigured();
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(24));
        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("foo", TypedValues.newIntegerValue(24));
    }

    @Test
    public void shouldNotResetIdIfTableIsEmpty()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenStoreWithConfig(this.wireComponentTestTarget.storeMaximumSizeKey(), 1);
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(23));
        givenAnEnvelopeReceivedByStore("foo", TypedValues.newIntegerValue(24));

        whenQueryIsPerformed("SELECT * FROM \"" + tableName + "\";");

        thenEnvelopeRecordCountIs(0, 1);
        thenFilterEmitsEnvelopeWithProperty("ID", TypedValues.newLongValue(2));
    }

    @Parameters(name = "{0} with {1}")
    public static Collection<Object[]> targets() {
        return Arrays.asList(new Object[][] {
                { WireComponentTestTarget.WIRE_RECORD_QUERY_AND_WIRE_RECORD_STORE, StoreTestTarget.H2 },
                { WireComponentTestTarget.WIRE_RECORD_QUERY_AND_WIRE_RECORD_STORE, StoreTestTarget.SQLITE },
                { WireComponentTestTarget.DB_FILTER_AND_DB_STORE, StoreTestTarget.H2 } });
    }

    public DbWireComponentsTest(final WireComponentTestTarget wireComponentTestTarget,
            final StoreTestTarget storeTestTarget)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        super(wireComponentTestTarget, storeTestTarget);
    }

}
