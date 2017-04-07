/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.kura.raspberrypi.sensehat.ledmatrix;

public class Images {

    public static final short[] B = new short[] { 0, 0, 248 };
    public static final short[] W = new short[] { 0, 0, 0 };

    //@formatter:off
    public static final short TRIANGLE[][][] = new short[][][] { 
        { B, W, W, W, W, W, W, W }, 
        { B, B, W, W, W, W, W, W },
        { B, B, B, W, W, W, W, W }, 
        { B, B, B, B, W, W, W, W }, 
        { B, B, B, B, B, W, W, W },
        { B, B, B, B, B, B, W, W }, 
        { B, B, B, B, B, B, B, W }, 
        { B, B, B, B, B, B, B, B } 
    };

    public static final short ARROW_UP[][][] = new short[][][] { 
        { W, W, W, B, W, W, W, W },
        { W, W, B, B, B, W, W, W },
        { W, B, W, B, W, B, W, W },
        { B, W, W, B, W, W, B, W },
        { W, W, W, B, W, W, W, W },
        { W, W, W, B, W, W, W, W },
        { W, W, W, B, W, W, W, W },
        { W, W, W, B, W, W, W, W }

    };

    public static final short ARROW_DOWN[][][] = new short[][][] { 
        { W, W, W, B, W, W, W, W },
        { W, W, W, B, W, W, W, W },
        { W, W, W, B, W, W, W, W },
        { W, W, W, B, W, W, W, W },
        { B, W, W, B, W, W, B, W },
        { W, B, W, B, W, B, W, W },
        { W, W, B, B, B, W, W, W },
        { W, W, W, B, W, W, W, W }

    };
    
    public static final short ARROW_RIGHT[][][] = new short[][][] { 
        { W, W, W, W, B, W, W, W },
        { W, W, W, W, W, B, W, W },
        { W, W, W, W, W, W, B, W },
        { B, B, B, B, B, B, B, B },
        { W, W, W, W, W, W, B, W },
        { W, W, W, W, W, B, W, W },
        { W, W, W, W, B, W, W, W },
        { W, W, W, W, W, W, W, W }

    };    

    public static final short ARROW_LEFT[][][] = new short[][][] { 
        { W, W, W, B, W, W, W, W },
        { W, W, B, W, W, W, W, W },
        { W, B, W, W, W, W, W, W },
        { B, B, B, B, B, B, B, B },
        { W, B, W, W, W, W, W, W },
        { W, W, B, W, W, W, W, W },
        { W, W, W, B, W, W, W, W },
        { W, W, W, W, W, W, W, W }

    };      
    
    public static final short LETTER_T[][][] = new short[][][] { 
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, B, W },
        { W, W, W, W, W, W, B, W },
        { B, B, B, B, B, B, B, W },
        { W, W, W, W, W, W, B, W },
        { W, W, W, W, W, W, B, W },
        { W, W, W, W, W, W, W, W }

    };    
    
    public static final short BLANK[][][] = new short[][][] { 
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W },
        { W, W, W, W, W, W, W, W }

    };    
    
    //@formatter:on      

}