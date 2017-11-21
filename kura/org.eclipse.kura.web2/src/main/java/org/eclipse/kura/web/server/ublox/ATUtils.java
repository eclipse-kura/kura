package org.eclipse.kura.web.server.ublox;

import java.net.URISyntaxException;
import java.util.Vector;
import java.util.logging.Logger;

public class ATUtils {

    private static Logger logger = Logger.getLogger("ATUtils");

    String path = null;
    String service = null;
    Vector<String> parameters = null;

    public ATUtils(String path) {
        try {
            this.path = path;
            parseURI(path);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void parseURI(String path) throws URISyntaxException {
        parameters = new Vector<String>();
        String[] header = path.split(":");
        if (header.length > 0) {
            service = header[0];
            if (header.length > 1) {
                String[] values = header[1].split(",");
                if (values.length > 0) {
                    for (int i = 0; i < values.length; i++) {
                        parameters.add(values[i]);
                    }
                }
            }
        } else {
            service = path;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Vector<String> getParameters() {
        return parameters;
    }

    public void setParameters(Vector<String> parameters) {
        this.parameters = parameters;
    }
}
