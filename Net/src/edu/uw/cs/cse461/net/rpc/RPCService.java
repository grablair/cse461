package edu.uw.cs.cse461.net.rpc;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage.RPCErrorResponseMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage.RPCNormalResponseMessage;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.Log;

/**
 * Implements the side of RPC that receives remote invocation requests.
 * 
 * @author zahorjan
 *
 */
public class RPCService extends NetLoadableService implements Runnable, RPCServiceInterface {
	private static final String TAG="RPCService";
	
	private final Map<Pair<String, String>, RPCCallableMethod> rpcMethods;
	private ServerSocket mServerSocket = null;
	private ExecutorService threadPool = null;
	
	private static final int NUM_THREADS = 40;
	
	private int currentId = 1;
	private final String HOST;
	
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
		HOST = "";
	}
	
	/**
	 * Executed by an RPCService-created thread.  Sits in loop waiting for
	 * connections, then creates an RPCCalleeSocket to handle each one.
	 */
	@Override
	public void run() {
		try {
			String serverIP = IPFinder.localIP();
			int tcpPort = 0;
			mServerSocket = new ServerSocket();
			mServerSocket.bind(new InetSocketAddress(serverIP, tcpPort));
			mServerSocket.setSoTimeout( NetBase.theNetBase().config().getAsInt("net.timeout.granularity", 500));
			
			threadPool = Executors.newFixedThreadPool(NUM_THREADS);
			
			try {
				TCPMessageHandler handler = new TCPMessageHandler(mServerSocket.accept());
				handler.setMaxReadLength(Integer.MAX_VALUE);

				threadPool.execute(new RCPConnection(handler));
			} catch (SocketTimeoutException e) {
				// this is normal.  Just loop back and see if we're terminating.
			}
		} catch (IOException ioe) {
			Log.w(TAG, "Server thread exiting due to exception: " + ioe.getMessage());
		} finally {
			if (mServerSocket != null && !mServerSocket.isClosed())
				try { mServerSocket.close(); } catch (IOException e) { }
		}
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
		return mServerSocket == null ? 0 : mServerSocket.getLocalPort();
	}
	
	@Override
	public String dumpState() {
		return "";
	}
	
	public synchronized int getIdAndInc() {
		return currentId++;
	}
	
	
	private class RCPConnection implements Runnable {
		private TCPMessageHandler handler;
		
		public RCPConnection(TCPMessageHandler handler) {
			this.handler = handler;
		}
		
		@Override
		public void run() {
			// Handshake
			RPCMessage connectMsg = null;
			try {
				connectMsg = RPCMessage.unmarshall(handler.readMessageAsString());
				
				RPCMessage successMsg = new RPCNormalResponseMessage(connectMsg.id(), null);
				handler.sendMessage(successMsg.marshall());
			} catch (IOException | JSONException e) {
				try {
					RPCMessage errorMsg = new RPCErrorResponseMessage(connectMsg.id(), e.getMessage(), null);
					handler.sendMessage(errorMsg.marshall());
				} catch (Exception e2) { }
				return;
			}
			
			//Success. Wait for invocation.
			RPCMessage invocationMsg = null;
			try {
				invocationMsg = RPCMessage.unmarshall(handler.readMessageAsString());
				
				if (invocationMsg instanceof RPCInvokeMessage) {
					RPCInvokeMessage invokeMsg = (RPCInvokeMessage) invocationMsg;
					RPCCallableMethod method = rpcMethods.get(Pair.pair(invokeMsg.app(), invokeMsg.method()));
					if (method != null) {
						JSONObject result = method.handleCall(invokeMsg.args());
						
						RPCNormalResponseMessage responseMsg = new RPCNormalResponseMessage(invokeMsg.id(), result);
						handler.sendMessage(responseMsg.marshall());
					} else {
						throw new Exception("No app/method of that combination found.");
					}
				} else {
					throw new Exception("Expected RPCInvokeMessage but got " + invocationMsg.getClass());
				}
			} catch (Exception e) {
				try {
					RPCMessage errorMsg = new RPCErrorResponseMessage(connectMsg.id(), e.getMessage(), (RPCCallMessage) invocationMsg);
					handler.sendMessage(errorMsg.marshall());
				} catch (Exception e2) { }
				return;
			}
		}
	}
	
	
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
	}
}
