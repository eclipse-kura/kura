package org.eclipse.kura.protocol.can.test;

import java.io.IOException;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.kura.protocol.can.CanConnectionService;
import org.eclipse.kura.protocol.can.CanMessage;


public class CanSocketTest implements ConfigurableComponent {
	private static final Logger s_logger = LoggerFactory.getLogger(CanSocketTest.class);

	private CanConnectionService 	m_canConnection;
	private Map<String,Object>   	m_properties;
	private Thread 					m_pollThread;
	private boolean 				thread_done = false;
	private String					m_ifName;
	private int 					m_canId;
	private int						m_orig;
	private boolean					m_isMaster;
	private byte 					indice = 0;

	public void setCanConnectionService(CanConnectionService canConnection) {
		this.m_canConnection = canConnection;
	}

	public void unsetCanConnectionService(CanConnectionService canConnection) {
		this.m_canConnection = null;
	}

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
		m_properties = properties;
		s_logger.info("activating can test");
		m_ifName="can0";
		m_canId=0;
		m_orig =0;
		m_isMaster = false;
		
		if(m_properties!=null){
			if(m_properties.get("can.name") != null) 
				m_ifName = (String) m_properties.get("can.name");
			if(m_properties.get("can.identifier") != null) 
				m_canId = (Integer) m_properties.get("can.identifier");
			if(m_properties.get("master") != null) 
				m_isMaster = (Boolean) m_properties.get("master");
		}

		if(m_pollThread!=null){
			m_pollThread.interrupt();
			try {
				m_pollThread.join(100);
			} catch (InterruptedException e) {
				// Ignore
			}
			m_pollThread=null;
		}

		m_pollThread = new Thread(new Runnable() {		
			@Override
			public void run() {
				if(m_canConnection!=null){
					while(!thread_done){
						thread_done=doCanTest();
					}
				}
			}
		});
		m_pollThread.start();
	}


	protected void deactivate(ComponentContext componentContext) {
		if(m_pollThread!=null){
			m_pollThread.interrupt();
			try {
				m_pollThread.join(100);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		m_pollThread=null;
	}

	public void updated(Map<String,Object> properties)
	{
		s_logger.debug("updated...");		
		
		m_properties = properties;
		if(m_properties!=null){
			if(m_properties.get("can.name") != null) 
				m_ifName = (String) m_properties.get("can.name");
			if(m_properties.get("can.identifier") != null) 
				m_canId = (Integer) m_properties.get("can.identifier");
			if(m_properties.get("master") != null) 
				m_isMaster = (Boolean) m_properties.get("master");
		}
	}

	public boolean doCanTest() {
		byte[] b; 
		CanMessage cm = null;
		if(m_isMaster){
			if(m_orig>=0){
				try {
					testSendImpl(m_ifName,m_canId,m_orig);
				} catch (Exception e) {
					s_logger.warn("CanConnection Crash!");			
					e.printStackTrace();
					return false;
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		else{
			s_logger.info("Wait for a request");
			try {
				
				cm = m_canConnection.receiveCanMessage(-1,0x7FF);
				
			} catch (KuraException e) {
				s_logger.warn("CanConnection Crash! -> KuraException");			
				e.printStackTrace();
			} catch (IOException e) {
				s_logger.warn("CanConnection Crash! -> IOException");			
				e.printStackTrace();
			}
			b = cm.getData();
			if(b!=null){
				StringBuilder sb = new StringBuilder("received : ");
				for(int i=0; i<b.length; i++){
					sb.append(b[i]);
					sb.append(";");
				}
				sb.append(" on id = ");
				sb.append(cm.getCanId());			
				s_logger.info(sb.toString());
			}
			else{
				s_logger.warn("receive=null");
			}			
		}
		return false;
	}



	public void testSendImpl(String ifName, int orig, int dest) {
		try {
			if((m_canConnection==null)||(orig<0)) 
				return;
			int id = 0x500 + (orig << 4) + dest;
			StringBuilder sb = new StringBuilder("Try to send can frame with message = ");
			byte btest[] = new byte[8];
			for(int i=0; i<8; i++){
				btest[i]=(byte) (indice+i);
				sb.append(btest[i]);
				sb.append(" ");
			}			
			sb.append(" and id = ");
			sb.append(id);
			s_logger.info(sb.toString());
			
			m_canConnection.sendCanMessage(ifName, id, btest);
			
			indice++;
			if(indice>14) indice=0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
