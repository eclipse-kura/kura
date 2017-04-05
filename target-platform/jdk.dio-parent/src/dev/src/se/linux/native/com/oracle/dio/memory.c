/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>

#include "javacall_defs.h"
#include "javacall_memory.h"
#include "javacall_logging.h"

/**
 * Allocates large memory heap
 * VM will use this memory heap for internal memory allocation/deallocation
 * Will be called ONCE during VM startup!
 *
 * @param    size required heap size in bytes
 * @param    outSize actual size of memory allocated
 * @return        a pointer to the newly allocated memory, or <tt>0</tt> if not available
 */
void* javacall_memory_heap_allocate(long size, /*OUT*/ long* outSize) {
    void* mem = (void *)malloc(size);

    *outSize = mem ? size : 0;
    return mem;
}

/**
 * Free large memory heap
 * VM will call this function once when VM is shutdown to free large memory heap
 * Will be called ONCE during VM shutdown!
 *
 * @param    heap memory pointer to free
 */
void javacall_memory_heap_deallocate(void* heap) {
    free(heap);
}


#if !ENABLE_MEMORY_POOL
/**
 * Allocates memory of the given size from the private JAVACALL memory
 * pool.
 *
 * @param    size Number of byte to allocate
 * @return        a pointer to the newly allocated memory
 */
void* /*OPTIONAL*/ javacall_malloc(unsigned int size){
    if(0 == size) {
        JAVACALL_REPORT_WARN(JC_MEMORY, "javacall_malloc << try to allocate 0 bytes");
        return NULL;
    }
    return (void *)malloc(size);
}

/**
 * Frees memory at the given pointer in the private JAVACALL memory pool.
 *
 * @param    ptr        Pointer to allocated memory
 */
void  /*OPTIONAL*/ javacall_free(void* ptr) {
    free(ptr);
}

/**
 * Checks JAVACALL memory pool for consistency and correctness.
 *
 * @retval  JAVACALL_TRUE    JAVACALL memory pool is correct
 * @retval  JAVACALL_FALSE   JAVACALL memory pool structure is damaged
 */
javacall_bool /*OPTIONAL*/ javacall_memory_is_correct() {
    return JAVACALL_TRUE;
}

/**
 * Allocates and clears the given number of elements of the given size
 * from the private JAVACALL memory pool.
 *
 * @param    numberOfElements Number of elements to allocate
 * @param    elementSize Size of one element
 * @return        pointer to the newly allocated and cleared memory
 */
void* /*OPTIONAL*/ javacall_calloc(unsigned int numberOfElements, unsigned int elementSize ) {
        return calloc(numberOfElements, elementSize);
}

/**
 * Re-allocates memory at the given pointer location in the private
 * JAVACALL memory pool (or null for new memory) so that it is the given
 * size.
 *
 * @param  ptr          Original memory pointer
 * @param  size         New size
 * @return        pointer to the re-allocated memory
 */
void* /*OPTIONAL*/ javacall_realloc(void* ptr, unsigned int size) {
        return realloc(ptr,size);
}

/**
 * Duplicates the given string after allocating the memory for it.
 *
 * @param    str        String to duplicate
 * @return      pointer to the duplicate string
 */
char* /*OPTIONAL*/ javacall_strdup(const char* str) {
    return strdup(str);
}

#endif /* !ENABLE_MEMORY_POOL */
