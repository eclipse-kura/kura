package org.eclipse.kura.core.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.cloud.CloudCallService;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.message.KuraApplicationTopic;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;

public class CloudEndpointPublisher implements DataServiceListener {
	private static RequestIdGenerator generator = RequestIdGenerator.getInstance();
	
	private static final int DFLT_PUB_QOS = 0;
    private static final boolean DFLT_RETAIN = false;
    private static final int DFLT_PRIORITY = 1;

    private static final String ACCOUNT_NAME_VAR_NAME = "#account-name";
    private static final String CLIENT_ID_VAR_NAME = "#client-id";
	
	private CloudEndpoint cloudEndpoint;
	private CloudPayloadProtoBufDecoder cloudPayloadProtoBufDecoder;
	private DataService dataService;
	
    
    private Map<String, CompletableFuture<KuraResponsePayload>> callFutures= new ConcurrentHashMap<>();
	
	
	public CloudEndpointPublisher(CloudEndpoint cloudEndpoint, DataService dataService) {
		this.cloudEndpoint = cloudEndpoint;
		this.cloudPayloadProtoBufDecoder = (CloudPayloadProtoBufDecoder) cloudEndpoint;
		this.dataService = dataService;
		dataService.addDataServiceListener(this);
	}
	
    public synchronized KuraResponsePayload call(String appId, String appTopic, KuraPayload appPayload, int timeout)
            throws KuraException {
        return call(CLIENT_ID_VAR_NAME, appId, appTopic, appPayload, timeout);
    }
	
	public KuraResponsePayload call(String deviceId, String appId, String appTopic, KuraPayload appPayload,
            int timeout) throws KuraException {
		
		String requestId = generator.next();

        String sbReqTopic = new StringBuilder("EDC").append("/").append(ACCOUNT_NAME_VAR_NAME).append("/")
                .append(deviceId).append("/").append(appId).append("/").append(appTopic).toString();

        String sbRespTopic = new StringBuilder("EDC").append("/").append(ACCOUNT_NAME_VAR_NAME).append("/")
                .append(CLIENT_ID_VAR_NAME).append("/").append(appId).append("/").append("REPLY").append("/")
                .append(requestId).toString();

        KuraRequestPayload req = null;
        if (appPayload != null) {
            // Construct a request payload
            req = new KuraRequestPayload(appPayload);
        } else {
            req = new KuraRequestPayload();
        }
        
        req.setRequestId(requestId);
        req.setRequesterClientId(CLIENT_ID_VAR_NAME);

        Map<String, Object> publishMessageProps = new HashMap<>();
        publishMessageProps.put("FULL_TOPIC", sbReqTopic);
        publishMessageProps.put("QOS", DFLT_PUB_QOS);
        publishMessageProps.put("RETAIN", DFLT_RETAIN);
        publishMessageProps.put("PRIORITY", DFLT_PRIORITY);
        
        KuraMessage kuraMessage = new KuraMessage(req, publishMessageProps);

        
        CompletableFuture<KuraResponsePayload> cf = new CompletableFuture<>();
        callFutures.put(requestId, cf);
        
        this.dataService.subscribe(sbRespTopic, 0);

        try {
            this.cloudEndpoint.publish(kuraMessage);
            return cf.get(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.BAD_REQUEST);
		} finally {
            try {
                dataService.unsubscribe(sbRespTopic);
            } catch (KuraException e) {
                //s_logger.error("Cannot unsubscribe");
            }
            
            callFutures.remove(requestId);
        }
	}
	
	@Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		
		CompletableFuture<KuraResponsePayload> future = callFutures.remove(getLastPartAfterSlash(topic));
		

        if (future != null) {
        	
                try {
                	KuraPayload kuraPayload = cloudPayloadProtoBufDecoder.buildFromByteArray(payload);
                    future.complete(new KuraResponsePayload(kuraPayload));
                } catch (Exception e) {
					future.completeExceptionally(e);
				}

            }
        }

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDisconnecting() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionLost(Throwable cause) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessagePublished(int messageId, String topic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		// TODO Auto-generated method stub
		
	}
	
	public static String getLastPartAfterSlash(String input) {
	    if (input == null || input.isEmpty()) {
	        return "";
	    }
	    
	    int lastSlashIndex = input.lastIndexOf('/');
	    if (lastSlashIndex == -1) {
	        // No slash found in the string
	        return input;
	    } else if (lastSlashIndex == input.length() - 1) {
	        // Input ends with a slash, return empty string
	        return "";
	    } else {
	        // Return everything after the last slash
	        return input.substring(lastSlashIndex + 1);
	    }
	}

}
