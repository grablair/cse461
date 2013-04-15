package edu.uw.cs.cse461.net.rpc;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.Log;

/**
 * Class implementing the caller side of RPC -- the RPCCall.invoke() method.
 * The invoke() method itself is static, for the convenience of the callers,
 * but this class is a normal, loadable, service.
 * <p>
 * <p>
 * This class is responsible for implementing persistent connections. 
 * (What you might think of as the actual remote call code is in RCPCallerSocket.java.)
 * Implementing persistence requires keeping a cache that must be cleaned periodically.
 * We do that using a cleaner thread.
 * 
 * @author zahorjan
 *
 */
public class RPCCall extends NetLoadableService {
	private static final String TAG="RPCCall";

	//-------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------
	// The static versions of invoke() are just a convenience for caller's -- it
	// makes sure the RPCCall service is actually running, and then invokes the
	// the code that actually implements invoke.
	
	/**
	 * Invokes method() on serviceName located on remote host ip:port.
	 * @param ip Remote host's ip address
	 * @param port RPC service port on remote host
	 * @param serviceName Name of service to be invoked
	 * @param method Name of method of the service to invoke
	 * @param userRequest Arguments to call
	 * @param socketTimeout Maximum time to wait for a response, in msec.
	 * @return Returns whatever the remote method returns.
	 * @throws JSONException
	 * @throws IOException
	 */
	public static JSONObject invoke(
			String ip,				  // ip or dns name of remote host
			int port,                 // port that RPC is listening on on the remote host
			String serviceName,       // name of the remote service
			String method,            // name of that service's method to invoke
			JSONObject userRequest,   // arguments to send to remote method,
			int socketTimeout         // timeout for this call, in msec.
			) throws JSONException, IOException {
		RPCCall rpcCallObj =  (RPCCall)NetBase.theNetBase().getService( "rpccall" );
		if ( rpcCallObj == null ) throw new IOException("RPCCall.invoke() called but the RPCCall service isn't loaded");
		return rpcCallObj._invoke(ip, port, serviceName, method, userRequest, socketTimeout, true);
	}
	
	/**
	 * A convenience implementation of invoke() that doesn't require caller to set a timeout.
	 * The timeout is set to the net.timeout.socket entry from the config file, or 2 seconds if that
	 * doesn't exist.
	 */
	public static JSONObject invoke(
			String ip,				  // ip or dns name of remote host
			int port,                 // port that RPC is listening on on the remote host
			String serviceName,       // name of the remote service
			String method,            // name of that service's method to invoke
			JSONObject userRequest    // arguments to send to remote method,
			) throws JSONException, IOException {
		int socketTimeout  = NetBase.theNetBase().config().getAsInt("net.timeout.socket", 2000);
		return invoke(ip, port, serviceName, method, userRequest, socketTimeout);
	}

	//-------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------
	
	/**
	 * The infrastructure requires a public constructor taking no arguments.  Plus, we need a constructor.
	 */
	public RPCCall() {
		super("rpccall");
	}

	/**
	 * This private method performs the actual invocation, including the management of persistent connections.
	 * Note that because we may issue the call twice, we  may (a) cause it to be executed twice at the server(!),
	 * and (b) may end up blocking the caller for around twice the timeout specified in the call. (!)
	 * 
	 * @param ip
	 * @param port
	 * @param serviceName
	 * @param method
	 * @param userRequest
	 * @param socketTimeout Max time to wait for this call
	 * @param tryAgain Set to true if you want to repeat call if a socket error occurs; e.g., persistent socket is no good when you use it
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	private JSONObject _invoke(
			String ip,				  // ip or dns name of remote host
			int port,                 // port that RPC is listening on on the remote host
			String serviceName,       // name of the remote service
			String method,            // name of that service's method to invoke
			JSONObject userRequest,   // arguments to send to remote method
			int socketTimeout,        // max time to wait for reply
			boolean tryAgain          // true if an invocation failure on a persistent connection should cause a re-try of the call, false to give up
			) throws JSONException, IOException {
		return null;
	}
	
	@Override
	public void shutdown() {
	}
	
	@Override
	public String dumpState() {
		return "Current persistent connections are ...";
	}
}
