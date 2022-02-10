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
 ******************************************************************************/
package org.eclipse.kura.ai.inference.engine;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The TensorDescriptor class describes the {@link Tensor} used as input or output
 * of a model for Artificial Intelligence and Machine Learning algorithms
 *
 * @since 2.3
 */
@ProviderType
public class TensorDescriptor {

    private final String name;
    private final String type;
    private final List<Long> shape;

    public TensorDescriptor(String name, String type, List<Long> shape) {
        this.name = name;
        this.type = type;
        this.shape = shape;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public List<Long> getShape() {
        return this.shape;
    }

}
