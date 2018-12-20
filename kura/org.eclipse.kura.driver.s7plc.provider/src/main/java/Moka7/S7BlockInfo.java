/*=============================================================================|
|  PROJECT Moka7                                                         1.0.2 |
|==============================================================================|
|  Copyright (C) 2013, 2016 Davide Nardella                                    |
|  All rights reserved.                                                        |
|==============================================================================|
|  SNAP7 is free software: you can redistribute it and/or modify               |
|  it under the terms of the Lesser GNU General Public License as published by |
|  the Free Software Foundation, either version 3 of the License, or under     |
|  EPL Eclipse Public License 1.0.                                             |
|                                                                              |
|  This means that you have to chose in advance which take before you import   |
|  the library into your project.                                              |
|                                                                              |
|  SNAP7 is distributed in the hope that it will be useful,                    |
|  but WITHOUT ANY WARRANTY; without even the implied warranty of              |
|  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE whatever license you    |
|  decide to adopt.                                                            |
|                                                                              |
|=============================================================================*/
package Moka7;

import java.util.Date;

/**
 *
 * @author Davide
 */
public class S7BlockInfo {

    private final int BufSize = 96;
    // MilliSeconds between 1970/1/1 (Java time base) and 1984/1/1 (Siemens base)
    private final long DeltaMilliSecs = 441763200000L;
    protected byte[] Buffer = new byte[this.BufSize];

    protected void Update(byte[] Src, int Pos) {
        System.arraycopy(Src, Pos, this.Buffer, 0, this.BufSize);
    }

    public int BlkType() {
        return this.Buffer[2];
    }

    public int BlkNumber() {
        return S7.GetWordAt(this.Buffer, 3);
    }

    public int BlkLang() {
        return this.Buffer[1];
    }

    public int BlkFlags() {
        return this.Buffer[0];
    }

    public int MC7Size()  // The real size in bytes
    {
        return S7.GetWordAt(this.Buffer, 31);
    }

    public int LoadSize() {
        return S7.GetDIntAt(this.Buffer, 5);
    }

    public int LocalData() {
        return S7.GetWordAt(this.Buffer, 29);
    }

    public int SBBLength() {
        return S7.GetWordAt(this.Buffer, 25);
    }

    public int Checksum() {
        return S7.GetWordAt(this.Buffer, 59);
    }

    public int Version() {
        return this.Buffer[57];
    }

    public Date CodeDate() {
        long BlockDate = S7.GetWordAt(this.Buffer, 17) * 86400000L + this.DeltaMilliSecs;
        return new Date(BlockDate);
    }

    public Date IntfDate() {
        long BlockDate = S7.GetWordAt(this.Buffer, 23) * 86400000L + this.DeltaMilliSecs;
        return new Date(BlockDate);
    }

    public String Author() {
        return S7.GetStringAt(this.Buffer, 33, 8);
    }

    public String Family() {
        return S7.GetStringAt(this.Buffer, 41, 8);
    }

    public String Header() {
        return S7.GetStringAt(this.Buffer, 49, 8);
    }

}
