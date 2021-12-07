/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.request.handler.jaxrs.consumer;

import java.util.Optional;

import org.eclipse.kura.KuraException;

@FunctionalInterface
public interface ResponseBodyHandler {

    Optional<byte[]> buildBody(final Object o) throws KuraException;
}