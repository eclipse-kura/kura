/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.util;

/**
 * Utilities to quickly sort an array of values
 */
public class QuickSort {
	
	public static void sort(int[] array) {
		if(array.length > 0) {
			sort(array, 0, array.length-1);
		}
	}
	
	public static void sort(long[] array) {
		if(array.length > 0) {
			sort(array, 0, array.length-1);
		}
	}
	
	private static void sort(int[] array, int lo, int hi) {
	    int i=lo, j=hi, h;
	    int x=array[(lo+hi)/2];

	    //  partition
	    do {    
	        while (array[i]<x) i++; 
	        while (array[j]>x) j--;
	        if (i<=j) {
	            h=array[i]; array[i]=array[j]; array[j]=h;
	            i++; j--;
	        }
	    } while (i<=j);

	    //  recursion
	    if (lo<j) sort(array, lo, j);
	    if (i<hi) sort(array, i, hi);
	}
	
	private static void sort(long[] array, int lo, int hi) {
	    int i=lo, j=hi;
		long h;
	    long x=array[(lo+hi)/2];

	    //  partition
	    do {    
	        while (array[i]<x) i++; 
	        while (array[j]>x) j--;
	        if (i<=j) {
	            h=array[i]; array[i]=array[j]; array[j]=h;
	            i++; j--;
	        }
	    } while (i<=j);

	    //  recursion
	    if (lo<j) sort(array, lo, j);
	    if (i<hi) sort(array, i, hi);
	}

	
	/*
	public static void sort(int[] array) {
		quicksort(array, 0, array.length - 1);
	}

	private static void quicksort(int[] array, int left, int right) {
		if (right <= left) return;
		int i = partition(array, left, right);
		quicksort(array, left, i-1);
		quicksort(array, i+1, right);
	}

	private static int partition(int[] array, int left, int right) {
		int i = left, j = right;
		int tmp;
		int pivot = array[(left + right) / 2];

		while (i <= j) {
			while (array[i] < pivot) {
				i++;
			}

			while (array[j] > pivot) {
				j--;
			}

			if (i <= j) {
				tmp = array[i];
				array[i] = array[j];
				array[j] = tmp;
				i++;
				j--;
			}
		}

		return i;
	}
	
	public static void sort(long[] array) {
		quicksort(array, 0, array.length - 1);
	}

	private static void quicksort(long[] array, int left, int right) {
		if (right <= left) return;
		int i = partition(array, left, right);
		quicksort(array, left, i-1);
		quicksort(array, i+1, right);
	}

	private static int partition(long[] array, int left, int right) {
		int i = left, j = right;
		long tmp;
		long pivot = array[(left + right) / 2];

		while (i <= j) {
			while (array[i] < pivot) {
				i++;
			}

			while (array[j] > pivot) {
				j--;
			}

			if (i <= j) {
				tmp = array[i];
				array[i] = array[j];
				array[j] = tmp;
				i++;
				j--;
			}
		}

		return i;
	}
	*/
}
