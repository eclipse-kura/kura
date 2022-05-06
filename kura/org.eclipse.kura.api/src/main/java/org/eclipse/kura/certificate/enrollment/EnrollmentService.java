/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.certificate.enrollment;

import java.security.cert.Certificate;

public interface EnrollmentService {

    public void enroll();

    public void renew();

    public Certificate getCARoot();

    public void forceCARootRollover();

    public boolean isEnrolled();

}
