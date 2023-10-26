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

import java.util.Objects;

public class SemanticVersion implements Comparable<SemanticVersion> {

    private final int major;
    private final int minor;
    private final int revision;

    private SemanticVersion(int major, int minor, int revision) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    public boolean isGreaterEqualThan(String version) {
        return isGreaterEqualThan(SemanticVersion.parse(version));
    }

    public boolean isGreaterEqualThan(SemanticVersion other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int majorComp = Integer.compare(this.major, other.getMajor());
        if (majorComp != 0) {
            return majorComp;
        }
        int minorComp = Integer.compare(this.minor, other.getMinor());
        if (minorComp != 0) {
            return minorComp;
        }
        return Integer.compare(this.revision, other.getRevision());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SemanticVersion)) {
                return false;
        }
        return this.compareTo((SemanticVersion) obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, revision);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(": [").append("major=").append(this.major).append(", minor=").append(this.minor)
                .append(", revision=").append(this.revision).append(']');
        return sb.toString();
    }
    
    public String asStringValue() {
        return new StringBuilder().append(this.major).append(".").append(this.minor)
                .append(".").append(this.revision).toString();
    }

    public static SemanticVersion parse(String version) {
        if (version == null) {
            version = "0.0.0";
        }
        int[] asArray = SemanticVersion.splitAndPad(version, 3);
        return new SemanticVersion(asArray[0], asArray[1], asArray[2]);
    }

    private static int[] splitAndPad(String version, int expectedElements) {
        int[] toReturn = new int[expectedElements];
        String[] tmp = version.split("[\\.-]");
        int i = 0;
        for (; i < tmp.length && i < expectedElements; i++) {
            toReturn[i] = parseElementAsInt(tmp[i]);
        }
        for (; i < expectedElements; i++) {
            toReturn[i] = 0;
        }
        return toReturn;
    }

    private static Integer parseElementAsInt(String element) {
        try {
            return Integer.valueOf(element);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }
}