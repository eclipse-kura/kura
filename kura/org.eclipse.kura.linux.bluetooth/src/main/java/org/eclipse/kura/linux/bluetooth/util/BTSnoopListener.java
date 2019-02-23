package org.eclipse.kura.linux.bluetooth.util;

/**
 * For listening to btsnoop streams
 */
public interface BTSnoopListener {

    /**
     * Process a BTSnoop Record
     *
     * @param record
     */
    public void processBTSnoopRecord(byte[] record);
    
    public void processErrorStream(String string);

}
