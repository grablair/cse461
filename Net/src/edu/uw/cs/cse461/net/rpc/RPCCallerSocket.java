package edu.uw.cs.cse461.net.rpc;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage.RPCControlMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage.RPCInvokeMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage.RPCNormalResponseMessage;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.Log;

/**
 * Implements a Socket to use in sending remote RPC invocations.  (It must engage
 * in the RPC handshake before sending the invocation request.)
 * @author zahorjan
 *
 */
 class RPCCallerSocket extends Socket {
	private static final String TAG = "RPCCallerSocket";	
	
	/**
	 * Create a socket for sending RPC invocations, connecting it to the specified remote ip and port.
	 * @param Remote host's name. In Project 3, it's not terribly meaningful - repeat the ip.
	 *  In Project 4, it's intended to be the string name of the remote system, allowing a degree of sanity checking.
	 * @param ip  Remote system IP address.
	 * @param port Remote RPC service's port.
	 * @param wantPersistent True if caller wants to try to establish a persistent connection, false otherwise
	 * @throws IOException
	 * @throws JSONException
	 */
	RPCCallerSocket(String ip, int port, boolean wantPersistent) throws IOException, JSONException {
		super(ip, port);	
	}

	/**
	 * Close this socket.
	 */
	synchronized public void discard() {
	}
}
