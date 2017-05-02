/*
 *
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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


#ifndef __JAVACALL_MEMORY_H_
#define __JAVACALL_MEMORY_H_

/**
 * @file javacall_memory.h
 * @ingroup Memory
 * @brief Javacall interfaces for memory
 */

#ifdef __cplusplus
extern "C" {
#endif

#include "javacall_defs.h"

#if ENABLE_MEMORY_POOL
#include "javacall_mempool.h"
#else
void *javacall_malloc_internal(unsigned int size, const char* filename, int lineno);
void *javacall_calloc_internal(unsigned int nelem, unsigned int elsize, const char* filename, int lineno);
void *javacall_realloc_internal(void* ptr, unsigned int size, const char* filename, int lineno);
char *javacall_strdup_internal(const char *s1, const char* filename, int lineno);
void  javacall_free_internal(void *ptr, const char *filename, int lineno);
#endif

/**
 * @defgroup Memory Memory API
 * @ingroup IMPNG
 *
 * The Java VM handles memory allocation internally.
 * As a result, upon VM startup, Java asks for a big continous memory range and frees this memory only upon VM shutdown.
 * Memory API specification allows the platform to allocate the big memory range using a specialized function called
 * javacall_memory_heap_allocate.
 *
 * @{
 */

/** @defgroup MandatoryMemory Mandatory Memory API
 *  @ingroup Memory
 *
 * The Java VM handles memory allocation internally. As a result, upon VM startup, Java asks for a big
 * continous memory range and frees this memory only upon VM shutdown.\n
 * Memory API specification allows the platform to allocate the big memory range using a specialized
 * function called javacall_memory_heap_allocate.
 *
 *  @{
 */


/**
 * Allocates large memory heap
 * VM will use this memory heap for internal memory allocation/deallocation
 * Will be called ONCE during VM startup!
 *
 * @param    size required heap size in bytes
 * @param    outSize actual size of memory allocated
 * @return    a pointer to the newly allocated memory, or <tt>0</tt> if not available
 */
#ifndef javacall_memory_heap_allocate
void* javacall_memory_heap_allocate(long size, /*OUT*/ long* outSize);
#endif

/**
 * Free large memory heap
 * VM will call this function once when VM is shutdown to free large memory heap
 * Will be called ONCE during VM shutdown!
 *
 * @param    heap memory pointer to free
 */
#ifndef javacall_memory_heap_deallocate
void javacall_memory_heap_deallocate(void* heap);
#endif

/**
 * Allocates memory of the given size from the private JAVACALL memory
 * pool.
 *
 * @param    size Number of byte to allocate
 * @return    a pointer to the newly allocated memory.
 *            If the space cannot be allocated, a NULL pointer is returned.
 *            The zero value of the requested space is considered as an invalid argument. NULL pointer is returned.
 */
#ifndef javacall_malloc
void* javacall_malloc(unsigned int size);
#endif


/**
 * Reallocates memory of the given size from the private JAVACALL memory
 * pool. If memory could not be reallocated function returns null,
 * in this case old pointer is not released.
 *
 * @param    ptr  Pointer to previously allocated memory
 * @param    size Number of byte to allocate
 * @return    a pointer to the reallocated memory or null if memory could not be reallocated
 */
#ifndef javacall_realloc
void* javacall_realloc(void* ptr, unsigned int size);
#endif


/**
 * Frees memory at the given pointer in the private JAVACALL memory pool.
 *
 * @param    ptr    Pointer to allocated memory
 */
#ifndef javacall_free
void  javacall_free(void* ptr);
#endif

/** @} */

/******************************************************************************
 ******************************************************************************
 ******************************************************************************
    OPTIONAL FUNCTIONS
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************/

/** @defgroup OptionalMemory Optional Memory API
 *  @ingroup Memory
 *
 * The following functions \n
 * - malloc \n
 * - free \n
 * - calloc \n
 * - realloc \n
 * - strdup \n
 *
 * \n
 * can be implemented using basic malloc functionality, but using platform's optimized implementation is
 * preferred as these function are commonly-used functions.\n
 * The following definitions declare the standard memory allocation functions malloc and free
 *
 *  @{
 */



/**
 * Allocates and clears the given number of elements of the given size
 * from the private JAVACALL memory pool.
 *
 * @param    numberOfElements Number of elements to allocate
 * @param    elementSize Size of one element
 * @return    pointer to the newly allocated and cleared memory
 */

#ifndef javacall_calloc
void* /*OPTIONAL*/ javacall_calloc (unsigned int numberOfElements, unsigned int elementSize );
#endif

/**
 * Duplicates the given string after allocating the memory for it.
 *
 * @param    str    String to duplicate
 * @return  pointer to the duplicate string
 */
#ifndef javacall_strdup
char* /*OPTIONAL*/ javacall_strdup(const char* str);
#endif

/**
 * Initialize JavaCall memory management system, must be called once only
 *
 * @param startAddr starting address of memory pool; if NULL, it will
 *                  be either dynamically or statically allocated.
 * @param size      size of memory pool to use; if size is <= 0,
 *                  the default memory pool size will be used
 * @return          0 on success, not 0 on failure
 */
#ifndef javacall_memory_initialize
int /*OPTIONAL*/ javacall_memory_initialize(void *startAddr, int size);
#endif

/**
 * Finalizes JavaCall memory management subsystem
 */
#ifndef javacall_memory_finalize
void /*OPTIONAL*/ javacall_memory_finalize(void);
#endif

/**
 * Gets total amount of native heap memory available for JavaCall
 * @return total amount of native heap memory
 */
#ifndef javacall_get_total_heap
int /*OPTIONAL*/ javacall_get_total_heap(void);
#endif

/**
 * Gets the current amount of unused native heap memory
 * @return the current amount of unused native heap memory
 */
#ifndef javacall_get_free_heap
int /*OPTIONAL*/ javacall_get_free_heap(void);
#endif

/**
 * Dumps information on memory managed by JavaCall
 * @param dumpMemoryLeaksOnly set to 0 to dump more verbose information
 * @return number of allocated blocks
 */
#ifndef javacall_malloc_dump
int /*OPTIONAL*/ javacall_malloc_dump(int dumpMemoryLeaksOnly);
#endif

/** @} */

/** @} */

#ifdef __cplusplus
}
#endif

#endif
