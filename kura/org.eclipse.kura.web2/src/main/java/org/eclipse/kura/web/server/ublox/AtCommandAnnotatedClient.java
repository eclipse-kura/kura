package org.eclipse.kura.web.server.ublox;



import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import asg.cliche.Command;
import asg.cliche.CommandTable;
import asg.cliche.DashJoinedNamer;
import asg.cliche.InputConversionEngine;
import asg.cliche.ShellCommand;
import asg.cliche.ShellFactory;
import asg.cliche.Token;

public class AtCommandAnnotatedClient  {
	private static Logger logger = Logger.getLogger("AtCommandAnnotatedClient");
	public CommandTable commandTable;
    private static InputConversionEngine inputConverter = new InputConversionEngine();

	public static void main(String[] args) throws IOException {
		logger.setLevel(Level.ALL);
		AtCommandAnnotatedClient atParser = new AtCommandAnnotatedClient();
		atParser.addDeclaredMethods(new UBloxCommand(), "");

		String response = atParser.invokeCommand("+UBTGDCD:test1,test2");
		logger.log(Level.INFO, "Response = " + response);
		
	}
	
	public AtCommandAnnotatedClient() {
		commandTable = new CommandTable(new DashJoinedNamer(true));
	}

    
    // Register the declared methods for a class
	
    public void addDeclaredMethods(Object handler, String prefix) throws SecurityException {
        for (Method m : handler.getClass().getMethods()) {
            Command annotation = m.getAnnotation(Command.class);
            if (annotation != null) {            	
                commandTable.addMethod(m, handler, prefix);
            }
        }
    }
    
    // Call a command. The path is /methodName/?param1=value1&...&param2=valuen
    
    public String invokeCommand(String path) {
    	String response = "Command not found";
		ATUtils req = new ATUtils(path);
		String service = req.getService();
		if (service.startsWith("+"))
			service = service.substring(1); 
			
		logger.info("Received " + req.getPath() + ". Calling " + service);
		List<Token> tokens = new ArrayList<Token>();
		Object[] values = req.getParameters().toArray();
		
		tokens.add(new Token(0, service));
		for (int i = 0;i < values.length; i++) {
			Token token = new Token(i+1, (String) values[i]);
			tokens.add(token);
		}
    	
		try {
	        ShellCommand commandToInvoke = commandTable.lookupCommand(service, tokens);
	        Class[] paramClasses = commandToInvoke.getMethod().getParameterTypes();
	        Object[] parameters = inputConverter.convertToParameters(tokens, paramClasses,
	                commandToInvoke.getMethod().isVarArgs());

			Object invocationResult = commandToInvoke.invoke(parameters);
			response = (String)invocationResult;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error executing the command", e);
		}
		return response;    	
    }

}
