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
 *******************************************************************************/
package org.eclipse.kura.internal.wire.db.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.InvalidSyntaxException;

@RunWith(Parameterized.class)
public class UnsuppotredTypeTest extends DbComponentsTestBase {

    @Test
    public void shouldNotEmitPropertyOfUnsupportedType()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAColumnWithData("test", 1, 2, 3, 4, 5, 6);

        whenQueryIsPerformed("SELECT MEDIAN(\"test\") FROM \"" + tableName + "\";");

        thenEmittedEnvelopeIsEmpty();
    }

    @Test
    public void shouldEmitPropertyAfterManualCastFromUnsupportedType()
            throws KuraException, InvalidSyntaxException, InterruptedException, ExecutionException, TimeoutException {
        givenAColumnWithData("test", 1, 2, 3, 4, 5);

        whenQueryIsPerformed("SELECT CAST(MEDIAN(\"test\") AS BIGINT) AS OUT FROM \"" + tableName + "\";");

        thenFilterEmitsEnvelopeWithProperty("OUT", TypedValues.newLongValue(3));
    }

    @Parameters(name = "{0} with {1}")
    public static Collection<Object[]> targets() {
        return Arrays.asList(new Object[][] {
                { WireComponentTestTarget.WIRE_RECORD_QUERY_AND_WIRE_RECORD_STORE, StoreTestTarget.H2 },
                { WireComponentTestTarget.DB_FILTER_AND_DB_STORE, StoreTestTarget.H2 } });
    }

    public UnsuppotredTypeTest(WireComponentTestTarget wireComponentTestTarget, StoreTestTarget storeTestTarget)
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, InvalidSyntaxException {
        super(wireComponentTestTarget, storeTestTarget);
        // TODO Auto-generated constructor stub
    }

}
