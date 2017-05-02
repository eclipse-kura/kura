/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package dio.shared;

import com.sun.javatest.Status;

/**
 * Class to store all default methods for test base classes
 * @author stanislav.smirnov@oracle.com
 */
public interface TestBase {

    String STATUS_OK = "OK";

    default void stop(){
        System.out.println("Test completed");
    }

    default void start(String text){
        System.out.println("Test started");
        System.out.println(text);
    }

    /**
     * Method to lookup key in the input arguments
     * @param args input arguments
     * @param lookup key to lookup
     * @return
     */
    default int getDataIndex(String[] args, String lookup){
        int result = -1;
        for(int i = 0; i < args.length; i++){
            if(lookup.equals(args[i])){
                result = i;
            }
        }
        return result;
    }

    default Status printFailedStatus(String statusText) {
        System.out.println(statusText);
        return Status.failed(statusText);
    }
}
