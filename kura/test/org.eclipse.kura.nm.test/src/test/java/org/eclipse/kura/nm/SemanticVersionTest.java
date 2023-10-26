/*******************************************************************************
 * Copyright (c) 2023 Areti and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Areti
 *******************************************************************************/
package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import org.junit.Test;

public class SemanticVersionTest {

    private SemanticVersion testVersion;
    
    private SemanticVersion comparedVersion;
    private Exception thrownException;
    
    @Test
    public void parseShouldWorkWithMajor() {
        givenSemanticVersionParseWith("1");
        thenNoExceptionHasBeenThrown();
        thenParsedVersionIs("1.0.0");
    }
    
    @Test
    public void parseShouldWorkWithMajorMinor() {
        givenSemanticVersionParseWith("1.34");
        thenNoExceptionHasBeenThrown();
        thenParsedVersionIs("1.34.0");
    }
    
    @Test
    public void parseShouldWorkWithMajorMinorRevision() {
        givenSemanticVersionParseWith("4.2.1");
        thenNoExceptionHasBeenThrown();
        thenParsedVersionIs("4.2.1");
    }
    
    @Test
    public void parseShouldWorkWithNull() {
        givenSemanticVersionParseWith(null);
        thenNoExceptionHasBeenThrown();
        thenParsedVersionIs("0.0.0");
    }
    
    @Test
    public void parseShouldWorkWithDashQualifier() {
        givenSemanticVersionParseWith("1.2.3-STAR");
        thenNoExceptionHasBeenThrown();
        thenParsedVersionIs("1.2.3");
    }
    
    @Test
    public void parseShouldWorkWithDotQualifier() {
        givenSemanticVersionParseWith("3.2.1.BOOM");
        thenNoExceptionHasBeenThrown();
        thenParsedVersionIs("3.2.1");
    }
    
    @Test
    public void parseShouldWorkWithNonNumericString() {
        givenSemanticVersionParseWith("HiImaString");
        thenNoExceptionHasBeenThrown();
        thenParsedVersionIs("0.0.0");
    }
    
    @Test
    public void compareShouldWorkWithEqual() {
        givenSemanticVersionParseWith("3");
        givenCompareSemanticVersionParseWith("3.0");
        thenParsedVersionIs("3.0.0");
        thenComparedVersionIsEqual();
        thenNoExceptionHasBeenThrown();
    }
    
    @Test
    public void compareShouldWorkWithOlder() {
        givenSemanticVersionParseWith("2.9.0");
        givenCompareSemanticVersionParseWith("2.8.999");
        thenParsedVersionIs("2.9.0");
        thenComparedVersionIsOlder();
        thenNoExceptionHasBeenThrown();
    }
    
    @Test
    public void compareShouldWorkWithNewer() {
        givenSemanticVersionParseWith("1.22.10-1");
        givenCompareSemanticVersionParseWith("1.40.10");
        thenParsedVersionIs("1.22.10");
        thenComparedVersionIsNewer();
        thenNoExceptionHasBeenThrown();
    }
    
    @Test
    public void compareShouldThrowWithNull() {
        givenSemanticVersionParseWith("1.22.10-1");
        thenParsedVersionIs("1.22.10");
        thenComparedVersionIsEqual();
        thenExceptionHasBeenThrown(NullPointerException.class);
    }
    
    private void givenSemanticVersionParseWith(String version) {
        try {
            this.testVersion = SemanticVersion.parse(version);
        } catch (Exception e) {
            this.thrownException = e;
        }
    }
    
    private void givenCompareSemanticVersionParseWith(String version) {
        try {
            this.comparedVersion = SemanticVersion.parse(version);
        } catch (Exception e) {
            this.thrownException = e;
        }
    }
    
    private void thenNoExceptionHasBeenThrown() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.thrownException)) {
            StringWriter sw = new StringWriter();
            this.thrownException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.thrownException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.thrownException);
    }
    
    private void thenExceptionHasBeenThrown(Class<? extends Exception> expectedException) {
        assertNotNull(this.thrownException);
        assertTrue(expectedException.isInstance(this.thrownException));
    }
    
    private void thenParsedVersionIs(String expectedResult) {
        assertTrue(this.testVersion.asStringValue().equals(expectedResult));
    }
    
    private void thenComparedVersionIsOlder() {
        try {
            int compare = this.testVersion.compareTo(this.comparedVersion);
            assertEquals(1, compare);
        } catch (Exception e) {
            this.thrownException = e;          
        } 
    }
    
    private void thenComparedVersionIsNewer() {
        try {
            int compare = this.testVersion.compareTo(this.comparedVersion);
            assertEquals(-1, compare);
        } catch (Exception e) {
            this.thrownException = e;          
        }        
    }
    
    private void thenComparedVersionIsEqual() {
        try {
            int compare = this.testVersion.compareTo(this.comparedVersion);
            assertEquals(0, compare);
        } catch (Exception e) {
            this.thrownException = e;           
        }
    }

}
