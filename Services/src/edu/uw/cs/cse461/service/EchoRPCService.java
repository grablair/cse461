package edu.uw.cs.cse461.service;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.rpc.RPCCallableMethod;
import edu.uw.cs.cse461.net.rpc.RPCService;

/**
 * A simple service that simply echoes back whatever it is sent.
 * It exposes a single method via RPC: echo.
 * <p>
 * To make a method available via RPC you must do two key things:
 * <ol>
 * <li> Create an <tt>RPCCallableMethod</tt> object that describes the method
 *      you want to expose.  In this class, that's done with two
 *      statements:
 *      <ol>
 *      <li><tt>private RPCCallableMethod<EchoService> echo;</tt>
 *      <br>declares a variable that can hold a method description
 *      of the type the infrastructure requires to invoke a method.
 *      <li><tt>echo = new RPCCallableMethod<EchoService>(this, "_echo");</tt>
 *      <br>initializes that variable.  The arguments mean that the method to
 *      invoke is <tt>this->_echo()</tt>.
 *      </ol>
 *  <p>
 *  <li> Register the method with the RPC service:
 *       <br><tt>((RPCService)OS.getService("rpc")).registerHandler(servicename(), "echo", echo );</tt>
 *       <br>This means that when an incoming RPC specifies service "echo" (the 1st argument)
 *       and method "echo" (the 2nd), that the method described by RPCCallableMethod variable
 *       <tt>echo</tt> should be invoked.
 * </ol>
 * @author zahorjan
 *
 */
public class EchoRPCService extends EchoServiceBase  {
	
	/**
	 * Key used for EchoRPC's header, in the args of an RPC call.
	 * The header element is a string (EchoServiceBase.HEADER_STR).
	 */
	public static final String HEADER_KEY = "header";
	public static final String HEADER_TAG_KEY = "tag";

	/**
	 * Key used for EchoRPC's payload, in the args of an RPC call
	 */
	public static final String PAYLOAD_KEY = "payload";
	
	// A variable capable of describing a method that can be invoked by RPC.
	private RPCCallableMethod echo;
	
	/**
	 * The constructor registers RPC-callable methods with the RPCService.
	 * @throws IOException
	 * @throws NoSuchMethodException
	 */
	public EchoRPCService() throws Exception {
		super("echorpc");
		
		// Set up the method descriptor variable to refer to this->_echo()
		echo = new RPCCallableMethod(this, "_echo");
		// Register the method with the RPC service as externally invocable method "echo"
		((RPCService)NetBase.theNetBase().getService("rpc")).registerHandler(loadablename(), "echo", echo );
	}
	
	/**
	 * This method is callable by RPC (because of the actions taken by the constructor).
	 * <p>
	 * All RPC-callable methods take a JSONObject as their single parameter, and return
	 * a JSONObject.  (The return value can be null.)  This particular method simply
	 * echos its arguments back to the caller. 
	 * @param args
	 * @return
	 * @throws JSONException
	 */
	public JSONObject _echo(JSONObject args) throws Exception {
		
		// check header
		JSONObject header = args.getJSONObject(EchoRPCService.HEADER_KEY);
		if ( header == null  || !header.has(HEADER_TAG_KEY) || !header.getString(HEADER_TAG_KEY).equalsIgnoreCase(HEADER_STR) )
			throw new Exception("Missing or incorrect header value: '" + header + "'");
		
		header.put(HEADER_TAG_KEY, RESPONSE_OKAY_STR);
		return args;
	}
}
