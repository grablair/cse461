package edu.uw.cs.cse461.net.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage.RPCControlMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage.RPCInvokeMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage.RPCErrorResponseMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage.RPCNormalResponseMessage;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.Log;

/**
 * Implements the side of RPC that receives remote invocation requests.
 * 
 * @author zahorjan
 * @author grahamb5
 * @author brymar
 */
public class RPCService extends NetLoadableService implements Runnable, RPCServiceInterface {
	private static final String TAG="RPCService";
	private static final boolean ALLOW_PERSISTENCE = true;
	
	private final Map<Pair<String, String>, RPCCallableMethod> rpcMethods;
	private ServerSocket mServerSocket = null;
	private ExecutorService threadPool = null;
	
	private static final int NUM_THREADS = 40;
		
	/**
	 * Constructor.  Creates the Java ServerSocket and binds it to a port.
	 * If the config file specifies an rpc.server.port value, it should be bound to that port.
	 * Otherwise, you should specify port 0, meaning the operating system should choose a currently unused port.
	 * <p>
	 * Once the port is created, a thread needs to be spun up to listen for connections on it.
	 * 
	 * @throws Exception
	 */
	public RPCService() throws Exception {
		super("rpc");
		rpcMethods = new HashMap<Pair<String, String>, RPCCallableMethod>();
		
		String serverIP = IPFinder.localIP();
		int tcpPort = NetBase.theNetBase().config().getAsInt("rpc.server.port", 0);
		mServerSocket = new ServerSocket();
		mServerSocket.bind(new InetSocketAddress(serverIP, tcpPort));
		mServerSocket.setSoTimeout( NetBase.theNetBase().config().getAsInt("net.timeout.granularity", 500));
		
		// Create a thread pool for this service.
		threadPool = Executors.newFixedThreadPool(NUM_THREADS);
		
		new Thread() {
			@Override
			public void run() {
				RPCService.this.run();
			}
		}.start();
	}
	
	/**
	 * Executed by an RPCService-created thread.  Sits in loop waiting for
	 * connections, then creates an RPCCalleeSocket to handle each one.
	 */
	@Override
	public void run() {
		while (!mAmShutdown) {
			try {
				TCPMessageHandler handler = new TCPMessageHandler(mServerSocket.accept());
				
				// Spawn a thread to process this connection.
				threadPool.execute(new RPCConnection(handler));
			} catch (SocketTimeoutException e) {
				// this is normal.  Just loop back and see if we're terminating.
			} catch (IOException e) {
				Log.w(TAG, "Unable to accept new connection.");
			}
		}
		
		if (mServerSocket != null && !mServerSocket.isClosed())
			try { mServerSocket.close(); } catch (IOException e) { }
	}
	
	/**
	 * Services and applications with RPC callable methods register them with the RPC service using this routine.
	 * Those methods are then invoked as callbacks when an remote RPC request for them arrives.
	 * @param serviceName  The name of the service.
	 * @param methodName  The external, well-known name of the service's method to call
	 * @param method The descriptor allowing invocation of the Java method implementing the call
	 * @throws Exception
	 */
	@Override
	public synchronized void registerHandler(String serviceName, String methodName, RPCCallableMethod method) throws Exception {
		if (serviceName == null) throw new IllegalArgumentException("serviceName must be non-null");
		if (methodName == null) throw new IllegalArgumentException("methodName must be non-null");
		if (method == null) throw new IllegalArgumentException("method must be non-null");
		
		rpcMethods.put(Pair.pair(serviceName, methodName), method);
	}
	
	/**
	 * Some of the testing code needs to retrieve the current registration for a particular service and method,
	 * so this interface is required.  You probably won't find a use for it in your code, though.
	 * 
	 * @param serviceName  The service name
	 * @param methodName The method name
	 * @return The existing registration for that method of that service, or null if no registration exists.
	 */
	public RPCCallableMethod getRegistrationFor(String serviceName, String methodName) {
		return rpcMethods.get(Pair.pair(serviceName, methodName));
	}
	
	/**
	 * Returns the port to which the RPC ServerSocket is bound.
	 * @return The RPC service's port number on this node
	 */
	@Override
	public int localPort() {
		return mServerSocket == null ? -1 : mServerSocket.getLocalPort();
	}
	
	@Override
	public String dumpState() {
		StringBuilder message = new StringBuilder();
		message.append("Listening at ");
		message.append(mServerSocket.getLocalSocketAddress() + "\n");
		
		message.append("Registered apps/methods:\n");
		
		for (Pair<String, String> app : rpcMethods.keySet()) {
			message.append(app.left + ": " + app.right + "\n");
		}
		
		return message.toString();
	}
	
	/**
	 * This class handles a RPCConnection. It performs an initial handshake,
	 * and then the procedure call, if the call is valid.
	 * 
	 * @author grahamb5
	 * @author brymar
	 */
	private class RPCConnection implements Runnable {
		private TCPMessageHandler handler;
		private boolean keepAlive;
		private int commandsExecuted = 0;
		
		public RPCConnection(TCPMessageHandler handler) throws SocketException {
			this.handler = handler;
			handler.setMaxReadLength(Integer.MAX_VALUE);
			handler.setTimeout(NetBase.theNetBase().config().getAsInt("net.timeout.socket", 10000));
		}
		
		@Override
		public void run() {
			// Perform handshake
			RPCMessage rawConnectMsg = null;
			try {
				// Get message.
				rawConnectMsg = RPCMessage.unmarshall(handler.readMessageAsString());
				
				// Convert to control message.
				RPCControlMessage connectMsg = (RPCControlMessage) rawConnectMsg;
				keepAlive = connectMsg.getOption("connection").equalsIgnoreCase("keep-alive");
				
				// Create response message.
				JSONObject retval = ALLOW_PERSISTENCE && keepAlive ? 
						new JSONObject().put("connection", "keep-alive") : null;
				RPCMessage successMsg = new RPCNormalResponseMessage(connectMsg.id(), retval);
				
				// Change timeout if there is persistence.
				if (retval != null)
					handler.setTimeout(NetBase.theNetBase().config().getAsInt("rpc.persistence.timeout", 25000));
				
				// Send message.
				handler.sendMessage(successMsg.marshall());
			} catch (IOException | JSONException | ClassCastException e) {
				try {
					// Try to send error, assuming enough information exists.
					RPCMessage errorMsg = new RPCErrorResponseMessage(rawConnectMsg.id(), e.getMessage(), null);
					handler.sendMessage(errorMsg.marshall());
				} catch (Exception e2) { }
				
				// Handshake failed. Close connection and return.
				handler.close();
				return;
			}
			
			// Wait for invocation message(s).
			RPCMessage invocationMsg = null;
			try {
				// Enter the loop only once if we are not
				// keeping it alive.
				while (commandsExecuted++ == 0 || keepAlive) {
					// Get message.
					invocationMsg = RPCMessage.unmarshall(handler.readMessageAsString());
					try {
						if (invocationMsg instanceof RPCInvokeMessage) {
							// Is a valid invocation message.
							RPCInvokeMessage invokeMsg = (RPCInvokeMessage) invocationMsg;
							RPCCallableMethod method = rpcMethods.get(Pair.pair(invokeMsg.app(), invokeMsg.method()));
							if (method != null) {
								// The requested method is registered.
								JSONObject result = method.handleCall(invokeMsg.args());
								
								RPCNormalResponseMessage responseMsg = new RPCNormalResponseMessage(invokeMsg.id(), result);
								handler.sendMessage(responseMsg.marshall());
							} else {
								// Send non-connection breaking error.
								throw new Exception("No (app, method) of the requested combination is registered: " + 
										Pair.pair(invokeMsg.app(), invokeMsg.method()));
							}
						} else {
							// Send non-connection breaking error.
							throw new Exception("Expected RPCInvokeMessage but got " + invocationMsg.getClass());
						}
					} catch (Exception e) {
						// Send non-connection-breaking error message.
						// Breaks connection on failure.
						RPCCallMessage sendErrorInfo = invocationMsg instanceof RPCCallMessage ?
								(RPCCallMessage) invocationMsg : null;
						RPCMessage errorMsg = new RPCErrorResponseMessage(invocationMsg.id(), e.getMessage(), sendErrorInfo);
						handler.sendMessage(errorMsg.marshall());
					}
				}
			} catch (SocketTimeoutException ste) {
				Log.w(TAG, "Socket timed out.");
			} catch (Exception e) {
				try {
					// Try to send connection-breaking error.
					RPCMessage errorMsg = new RPCErrorResponseMessage(invocationMsg.id(), e.getMessage(), (RPCCallMessage) invocationMsg);
					handler.sendMessage(errorMsg.marshall());
				} catch (Exception e2) { }
				Log.w(TAG, "Unable to process invocation due to " + e.getClass());
			} finally {
				handler.close();
			}
		}
	}
	
	// Simple pair-of-values class.
	private static class Pair<E, T> {
		private E left;
		private T right;
		
		public static <E, T> Pair<E, T> pair(E left, T right) {
			return new Pair<E, T>(left, right);
		}
		
		private Pair(E left, T right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public int hashCode() {
			return left.hashCode() * 11 + right.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof Pair<?, ?>) {
				return left.equals(((Pair<?, ?>) other).left) &&
						right.equals(((Pair<?, ?>) other).right);				
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "(" + left + ", " + right + ")";
		}
	}
}
