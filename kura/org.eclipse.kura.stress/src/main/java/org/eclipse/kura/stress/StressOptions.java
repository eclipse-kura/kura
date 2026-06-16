/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.stress;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.stress.Stress", name = "Stress", description = "Simple resource stress application.", icon = @Icon(resource = "http://s3.amazonaws.com/kura-resources/application/icon/applications-other.png", size = 32))
public @interface StressOptions {

    @AttributeDefinition(name = "heap.enable", description = "Enable heap stress.")
    boolean heap_enable() default false;

    @AttributeDefinition(name = "heap.threads", min = "1", description = "Number of heap allocation threads.")
    int heap_threads() default 1;

    @AttributeDefinition(name = "heap.size", min = "1", description = "Size in kilobytes of each memory allocation (as new byte[heap.size * 1024]).")
    int heap_size() default 51200;

    @AttributeDefinition(name = "heap.stride", min = "1", description = "For each memory allocation, touch (writing 'k') one byte every N kilobytes.")
    int heap_stride() default 1024;

    @AttributeDefinition(name = "heap.keep", min = "0", description = "The number of milliseconds to keep every allocation (0: forever).")
    int heap_keep() default 0;

    @AttributeDefinition(name = "heap.interval", min = "1", description = "The period between allocations in milliseconds.")
    int heap_interval() default 1000;

    @AttributeDefinition(name = "heap.delay", min = "0", description = "The start delay between threads in milliseconds.")
    int heap_delay() default 0;

    @AttributeDefinition(name = "heap.log", description = "Log each heap allocation iteration.")
    boolean heap_log() default true;

}


