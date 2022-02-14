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
package org.eclipse.kura.ai.inference;

import java.util.List;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The Tensor class represents the input or output of a model
 * for Artificial Intelligence and Machine Learning algorithms
 * 
 * @since 2.3
 */
@ProviderType
public class Tensor {

    /** the {@link TensorDescriptor} of this tensor */
    private final TensorDescriptor descriptor;
    /** the type of tensor data as Java class */
    private final Class<?> type;
    /** the list of data of this tensor */
    private final List<?> data;

    /**
     * Instantiates a Tensor
     * 
     * @param type
     *            the type of tensor data as Java class
     * @param descriptor
     *            the {@link TensorDescriptor} of this tensor
     * @param data
     *            the list of data of this tensor
     */
    public <T> Tensor(Class<T> type, TensorDescriptor descriptor, List<T> data) {
        this.type = type;
        this.descriptor = descriptor;
        this.data = data;
    }

    public TensorDescriptor getDescriptor() {
        return this.descriptor;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<List<T>> getData(Class<T> type) {
        if (this.type == type) {
            return Optional.of((List<T>) this.data);
        } else {
            return Optional.empty();
        }
    }

    public Class<?> getType() {
        return this.type;
    }
}
