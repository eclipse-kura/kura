
package java.util;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 1998, 2002  All Rights Reserved
 */

/**
 * This exception is thrown when an attempt is made to add
 * more than one listener to an event source which only
 * supports a single listener. It is also thrown when the
 * same listener is added more than once.
 *
 * @author		OTI
 * @version		initial
 *
 * @see		java.lang.Exception
 */
public class TooManyListenersException extends Exception {

	static final long serialVersionUID = 5074640544770687831L;

/**
 * Constructs a new instance of this class with its
 * walkback filled in.
 *
 * @author		OTI
 * @version		initial
 */
public TooManyListenersException () {
	super();
}

/**
 * Constructs a new instance of this class with its
 * walkback and message filled in.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		detailMessage String
 *				The detail message for the exception.
 */
public TooManyListenersException (String detailMessage) {
	super(detailMessage);
}

}
