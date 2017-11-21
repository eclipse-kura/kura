package org.eclipse.kura.web.server.ublox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;

import org.apache.log4j.Logger;

public class UBloxSerial implements SerialPortEventListener {

    private final Logger logger = Logger.getLogger(UBloxSerial.class);
    private String[] tempPortList, portList; // list of ports for combobox
    // dropdown
    private String portName;
    private CommPort commPort;
    private SerialPort serialPort;
    private CommPortIdentifier portIdentifier = null;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int baudRate = 9600;
    private boolean open = false;
    private AtCommandAnnotatedClient atParser = new AtCommandAnnotatedClient();
    private UBloxCommand ubloxCommand = new UBloxCommand();

    public enum RxFormat {
        ASCII,
        INT16;
    }

    private RxFormat displayFormat;

    // constants
    static final int MAX_PORTS = 20; // maximum number of ports to look for
    static final int MAX_DATA = 90;// maximum length of serial data received
    static final int MAX_BUFFER = 60;// maximun number
    String[] bufferScan = new String[MAX_BUFFER];// buffer to use for store scan data
    ArrayList<String> listScan = new ArrayList<String>();

    public UBloxSerial() {
        displayFormat = RxFormat.ASCII;
        atParser.addDeclaredMethods(ubloxCommand, "");
    }

    public void listPorts() {
        // display available ports to the terminal
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            portIdentifier = (CommPortIdentifier) portEnum.nextElement();
            System.out.println(portIdentifier.getName() + "\n");
        }
    }

    // run before initializing GUI
    // creates a string array of all the ports
    // to be displayed in dropdown box upon opening program
    public void getPorts() {
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        tempPortList = new String[MAX_PORTS]; // create array of 20 ports
        int numports = 0;
        int i;
        // tempPortList[0] = "Select Port";
        // fill up a temporary list of length MAX_PORTS with the portnames
        while (portEnum.hasMoreElements()) {
            portIdentifier = (CommPortIdentifier) portEnum.nextElement();
            tempPortList[numports++] = portIdentifier.getName();
        }

        // make the actual port list only as long as necessary
        portList = new String[numports];
        for (i = 0; i < numports; i++) {
            portList[i] = tempPortList[i];
        }
    }

    // serial event: when data is received from serial port
    // display the data on the terminal
    public void serialEvent(SerialPortEvent event) {
        String lastCommand = "";

        switch (event.getEventType()) {
        case SerialPortEvent.DATA_AVAILABLE:
            byte[] buffer = new byte[MAX_DATA]; // create a buffer (enlarge if
            // buffer overflow occurs)
            int numBytes = 0; // how many bytes read (smaller than buffer)
            int int16value;

            switch (displayFormat) {
            case ASCII: {
                try { // read the input stream and store to buffer, count number
                      // of bytes read
                    while ((numBytes = inputStream.read(buffer)) > 0) {
                        // convert to string of size numBytes
                        String str = new String(buffer).substring(0, numBytes);
                        str = str.replace('\r', '\n'); // replace CR with Newline
                        // listScan.add(str);
                        for (int i = 0; i < str.length(); i++) {
                            if (str.charAt(i) != '\n') {
                                lastCommand += str.charAt(i);
                            } else {
                                // System.out.println("==> " + lastCommand); //write to terminal
                                if (lastCommand.length() > 0) {
                                    if (lastCommand.startsWith("+") || lastCommand.equalsIgnoreCase("OK")
                                            || lastCommand.equalsIgnoreCase("ERROR")) {
                                        String result = atParser.invokeCommand(lastCommand);
                                        System.out.println("Result = " + result);
                                    }
                                    lastCommand = "";
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                break;
            }
            case INT16: {
                readall(inputStream, buffer, 2); // put two bytes in buffer
                int16value = 256 * (int) buffer[1] + (int) buffer[0];
                System.out.println(int16value + "\n"); // write to terminal
                break;
            }
            }
            // scroll terminal to bottom
            // textWin.setCaretPosition(textWin.getText().length());
            break;
        }
    }

    //
    public void printScan() {
        for (int i = 0; i < listScan.size(); i++) {
            System.out.print(listScan);
        }
    }

    // fill buffer with numBytes bytes from is
    public void readall(InputStream is, byte[] buffer, int numBytes) {
        int tempRead = 0;
        while (tempRead < numBytes) {
            try {
                tempRead = tempRead + is.read(buffer, tempRead, numBytes - tempRead);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return;
    }

    public String sendText(String text) {
        // if serial port open, write to serial port
        if (open == true) {
            if (!text.equals("+++") && displayFormat == RxFormat.ASCII) {
                text = text + "\r"; // append carriage return to text (except for +++ for XBee)
            }
            try {
                outputStream.write(text.getBytes()); // write to serial port
                logger.info(">> Sent");
                return ">> Sent";

            } catch (IOException ex) {
                ex.printStackTrace();
                logger.info(">> Error sending the tex");
                return "Error sending the text";

            }
        } else
            logger.info("Connection closed");
        return "Connection closed";
    }

    // open serial port
    public String connect(String portName, int baudRate) throws Exception {
        // make sure port is not currently in use
        portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        this.portName = portName;
        this.baudRate = baudRate;
        if (portIdentifier.isCurrentlyOwned()) {
            return "Error: Port is currently in use";
        } else {
            // create CommPort and identify available serial/parallel ports
            commPort = portIdentifier.open(this.getClass().getName(), 2000);
            serialPort = (SerialPort) commPort;// cast all to serial
            // set baudrate, 8N1 stopbits, no parity
            serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            // start I/O streams
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            open = true;
            try {
                serialPort.addEventListener(this);
            } catch (TooManyListenersException ex) {
                System.out.println(ex.getMessage());
            }
            serialPort.notifyOnDataAvailable(true);
            return "Connected";
        }
    }

    public String disconnect() {
        if ((portIdentifier != null) && portIdentifier.isCurrentlyOwned()) {
            System.out.println("Close Port");
            // close input stream
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            // close output stream
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            // close serial port
            System.out.println("closing serial port.");
            serialPort.removeEventListener();
            if (serialPort != null) {
                serialPort.close();
            }
            System.out.println("closed serial port.");

            open = false;
            return ">>Port " + portName + " is now closed.\n";
        } else
            return "Unable to close the port " + portName;
    }

    public String[] getPortList() {
        return portList;
    }

    public void setPortList(String[] portList) {
        this.portList = portList;
    }

    public UBloxCommand getUBloxCommand() {
        return ubloxCommand;
    }

}
