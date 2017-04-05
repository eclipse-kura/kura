/*
 * Copyright (c) 1990, 2010, Oracle and/or its affiliates. All rights reserved.
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


#include "javautil_linked_list.h"
#include "javacall_memory.h"


typedef struct _javacall_node {
    void *data;
    struct _javacall_node *next;
} javacall_node;

typedef struct {
    javacall_node *head;
    javacall_node *tail;
    javacall_node *next;
    javacall_int32 count;
} javacall_list;

/**
 * Creates a new linked list.
 *
 * @param listHandle pointer to receive handle to the newly allocated list
 * @return JAVACALL_OK on success,
 *         JAVACALL_INVALID_ARGUMENT if listHandle is NULL,
 *         JAVACALL_OUT_OF_MEMORY if unable to allocate the list
 */
javacall_result javautil_list_create(javacall_handle *listHandle) {
    javacall_list *list;

    if (!listHandle) {
        return JAVACALL_INVALID_ARGUMENT;
    }
    list = (javacall_list *)javacall_malloc(sizeof(javacall_list));
    if (!list) {
        return JAVACALL_OUT_OF_MEMORY;
    }
    list->head = NULL;
    list->tail = NULL;
    list->next = NULL;
    list->count = 0;
    *listHandle = (javacall_handle)list;
    return JAVACALL_OK;
}

/**
 * Destroys the list, freeing all nodes but not freeing the contained data.
 *
 * @param listHandle handle to the list
 */
void javautil_list_destroy(javacall_handle listHandle) {
    javacall_list *list = (javacall_list *)listHandle;
    if (!list) {
        return;
    }

    while (list->head) {
        list->next = list->head;
        list->head = list->head->next;
        javacall_free(list->next);
    }
    javacall_free(list);
}

/**
 * Adds new element to the list.
 *
 * @param listHandle handle to the list
 * @param data pointer to contents of the new element
 * @return JAVACALL_OK on success,
 *         JAVACALL_INVALID_ARGUMENT if list handle is not valid,
 *         JAVACALL_OUT_OF_MEMORY if unable to allocate new element
 */
javacall_result javautil_list_add(javacall_handle listHandle, void *data) {
    javacall_list *list = (javacall_list *)listHandle;
    javacall_node *newNode;

    if (!list) {
        return JAVACALL_INVALID_ARGUMENT;
    }
    newNode = (javacall_node *)javacall_malloc(sizeof(javacall_node));
    if (!newNode) {
        return JAVACALL_OUT_OF_MEMORY;
    }

    newNode->next = NULL;
    newNode->data = data;
    if (!list->head) {
        list->head = newNode;
        list->tail = newNode;
        list->next = newNode;
    } else {
        list->tail->next = newNode;
        list->tail = newNode;
    }
    list->count++;
    return JAVACALL_OK;
}


/**
 * Removes the first occurrence of the element with specified content
 * from the list, if it is present. Resets list iterator.
 *
 * @param listHandle handle to the list
 * @param data pointer to contents of the element to remove
 * @return JAVACALL_OK on success,
 *         JAVACALL_INVALID_ARGUMENT if list handle is not valid,
 *         JAVACALL_FAIL if element with specified content is not present
           in the list
 */
javacall_result javautil_list_remove(javacall_handle listHandle, void *data) {
    javacall_list *list = (javacall_list *)listHandle;
    javacall_node *prevNode = NULL;
    javacall_result res = JAVACALL_FAIL;

    if (!list || !data) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    list->next = list->head;    /* resets iterator */

    while(list->next != NULL) {
        if (list->next->data == data) {
            if (prevNode == NULL) {
                list->head = list->head->next;
            } else {
                prevNode->next = list->next->next;
            }
            if (list->next->next == NULL) {
                list->tail = prevNode;
            }

            javacall_free(list->next);
            list->count--;
            res = JAVACALL_OK;
            break;
        } else {
            prevNode = list->next;
            list->next = list->next->next;
        }
    }

    list->next = list->head;    /* resets iterator */
    return res;
}

/**
 * Iterates to the beginning of the list. Subsequent call to
 * <code>javautil_list_get_next</code> will return the first element.
 *
 * @param listHandle handle to the list
 */
void javautil_list_reset_iterator(javacall_handle listHandle) {
    javacall_list *list = (javacall_list *)listHandle;

    if (list) {
        list->next = list->head;
    }
}

/**
 * Iterates to the next element of the list.
 *
 * @param listHandle handle to the list
 * @param data pointer to receive pointer to the next element contents
 * @return JAVACALL_OK on success,
 *         JAVACALL_INVALID_ARGUMENT if list handle is not valid or data
 *         pointer is NULL,
 *         JAVACALL_FAIL if no more elements found in the list
 */
javacall_result javautil_list_get_next(javacall_handle listHandle, void **data) {
    javacall_list *list = (javacall_list *)listHandle;

    if (!list || !data) {
        return JAVACALL_INVALID_ARGUMENT;
    }
    if (!list->next) {
        return JAVACALL_FAIL;
    }
    *data = list->next->data;
    list->next = list->next->next;
    return JAVACALL_OK;
}

/**
 * Returns number of elements in the list.
 *
 * @param listHandle handle to the list
 * @param size pointer to receive the list size
 * @return JAVACALL_OK on success,
 *         JAVACALL_INVALID_ARGUMENT if list handle is not valid or size
 *         pointer is NULL
 */
javacall_result javautil_list_get_size(javacall_handle listHandle,
        javacall_int32 *size) {
    javacall_list *list = (javacall_list *)listHandle;

    if (!list || !size) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    *size = list->count;
    return JAVACALL_OK;
}
