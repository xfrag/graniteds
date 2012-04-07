package org.granite.gravity.jetty8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedList;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnBinaryMessage;
import org.granite.context.GraniteContext;
import org.granite.context.SimpleGraniteContext;
import org.granite.gravity.AbstractChannel;
import org.granite.gravity.AsyncHttpContext;
import org.granite.gravity.Gravity;
import org.granite.gravity.GravityConfig;
import org.granite.logging.Logger;

import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.Message;


public class JettyWebSocketChannel extends AbstractChannel implements WebSocket, OnBinaryMessage {
	
	private static final Logger log = Logger.getLogger(JettyWebSocketChannel.class);
	
	private Connection connection;

	
	public JettyWebSocketChannel(Gravity gravity, String id, JettyWebSocketChannelFactory factory) {
    	super(gravity, id, factory);
    }

	public void onOpen(Connection connection) {
		this.connection = connection;
		
		try {
			initializeRequest(getGravity());
			
			// Return an acknowledge message with the server-generated clientId
			AcknowledgeMessage message = new AcknowledgeMessage();
			message.setCorrelationId("OPEN_CONNECTION");
			message.setClientId(id);
	        byte[] resultData = serialize(getGravity(), new Message[] { message });
	        
			connection.sendMessage(resultData, 0, resultData.length);
		}
		catch (IOException e) {
			log.error(e, "Could not send connect acknowledge");
		}
		finally {
			cleanupRequest();
		}
	}

	public void onClose(int closeCode, String message) {
	}

	public void onMessage(byte[] data, int offset, int length) {
		try {
			initializeRequest(getGravity());
			
			Message[] messages = deserialize(getGravity(), data, offset, length);

            log.debug(">> [AMF3 REQUESTS] %s", (Object)messages);

            Message[] responses = null;
            
            boolean accessed = false;
            for (int i = 0; i < messages.length; i++) {
                Message message = messages[i];
                
                // Ask gravity to create a specific response (will be null with a connect request from tunnel).
                Message response = getGravity().handleMessage(getFactory(), message);
                String channelId = (String)message.getClientId();
                
                // Mark current channel (if any) as accessed.
                if (!accessed)
                	accessed = getGravity().access(channelId);                

                if (responses == null)
                	responses = new Message[messages.length];
                responses[i] = response;
            }

            log.debug("<< [AMF3 RESPONSES] %s", (Object)responses);

            byte[] resultData = serialize(getGravity(), responses);
            
            connection.sendMessage(resultData, 0, resultData.length);
		}
		catch (ClassNotFoundException e) {
			log.error(e, "Could not handle incoming message data");
		}
		catch (IOException e) {
			log.error(e, "Could not handle incoming message data");
		}
		finally {
			cleanupRequest();
		}
	}
	
	private static Gravity initializeRequest(Gravity gravity) {
		SimpleGraniteContext.createThreadIntance(gravity.getGraniteConfig(), gravity.getServicesConfig(), new HashMap<String, Object>());
		return gravity;
	}

	private static Message[] deserialize(Gravity gravity, byte[] data, int offset, int length) throws ClassNotFoundException, IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(data, offset, length);
		try {
			ObjectInput amf3Deserializer = gravity.getGraniteConfig().newAMF3Deserializer(is);
	        Object[] objects = (Object[])amf3Deserializer.readObject();
	        Message[] messages = new Message[objects.length];
	        System.arraycopy(objects, 0, messages, 0, objects.length);
	        
	        return messages;
		}
		finally {
			is.close();
		}
	}
	
	private static byte[] serialize(Gravity gravity, Message[] messages) throws IOException {
		ByteArrayOutputStream os = null;
		try {
	        os = new ByteArrayOutputStream(200*messages.length);
	        ObjectOutput amf3Serializer = gravity.getGraniteConfig().newAMF3Serializer(os);
	        amf3Serializer.writeObject(messages);	        
	        os.flush();
	        return os.toByteArray();
		}
		finally {
			if (os != null)
				os.close();
		}		
	}
	
	private static void cleanupRequest() {
		GraniteContext.release();
	}
	
	@Override
	public boolean runReceived(AsyncHttpContext asyncHttpContext) {
		
		LinkedList<AsyncMessage> messages = null;
		ByteArrayOutputStream os = null;

		try {
			receivedQueueLock.lock();
			try {
				// Do we have any pending messages? 
				if (receivedQueue.isEmpty())
					return false;
				
				// Both conditions are ok, get all pending messages.
				messages = receivedQueue;
				receivedQueue = new LinkedList<AsyncMessage>();
			}
			finally {
				receivedQueueLock.unlock();
			}
			
			AsyncMessage[] messagesArray = new AsyncMessage[messages.size()];
			int i = 0;
			for (AsyncMessage message : messages)
				messagesArray[i++] = message;
			
			// Setup serialization context (thread local)
			Gravity gravity = getGravity();
	        GraniteContext context = SimpleGraniteContext.createThreadIntance(
	            gravity.getGraniteConfig(), gravity.getServicesConfig(), new HashMap<String, Object>()
	        );
	        
	        os = new ByteArrayOutputStream(500);
	        ObjectOutput amf3Serializer = context.getGraniteConfig().newAMF3Serializer(os);
	        
	        log.debug("<< [MESSAGES for channel=%s] %s", this, messagesArray);
	        
	        amf3Serializer.writeObject(messagesArray);
	        
	        connection.sendMessage(os.toByteArray(), 0, os.size());
	        
	        return true; // Messages were delivered, http context isn't valid anymore.
		}
		catch (IOException e) {
			log.warn(e, "Could not send messages to channel: %s (retrying later)", this);
			
			GravityConfig gravityConfig = getGravity().getGravityConfig();
			if (gravityConfig.isRetryOnError()) {
				receivedQueueLock.lock();
				try {
					if (receivedQueue.size() + messages.size() > gravityConfig.getMaxMessagesQueuedPerChannel()) {
						log.warn(
							"Channel %s has reached its maximum queue capacity %s (throwing %s messages)",
							this,
							gravityConfig.getMaxMessagesQueuedPerChannel(),
							messages.size()
						);
					}
					else
						receivedQueue.addAll(0, messages);
				}
				finally {
					receivedQueueLock.unlock();
				}
			}
			
			return true; // Messages weren't delivered, but http context isn't valid anymore.
		}
		finally {
			if (os != null) {
				try {
					os.close();
				}
				catch (Exception e) {
					// Could not close bytearray ???
				}
			}
			
			// Cleanup serialization context (thread local)
			try {
				GraniteContext.release();
			}
			catch (Exception e) {
				// should never happen...
			}
		}
	}
	
	@Override
	protected boolean hasAsyncHttpContext() {
		return true;
	}

	@Override
	protected void releaseAsyncHttpContext(AsyncHttpContext context) {
	}

	@Override
	protected AsyncHttpContext acquireAsyncHttpContext() {
    	return null;
    }		
}