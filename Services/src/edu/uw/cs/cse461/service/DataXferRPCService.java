package edu.uw.cs.cse461.service;

import java.io.IOException;

import org.json.JSONObject;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.rpc.RPCCallableMethod;
import edu.uw.cs.cse461.net.rpc.RPCService;
import edu.uw.cs.cse461.util.Base64;

/**
 * A simple service that simply echoes back whatever it is sent.
 * It exposes a single method via RPC: dataxfer.
 * <p>
 * To make a method available via RPC you must do two key things:
 * <ol>
 * <li> Create an <tt>RPCCallableMethod</tt> object that describes the method
 *      you want to expose.  In this class, that's done with two
 *      statements:
 *      <ol>
 *      <li><tt>private RPCCallableMethod<EchoService> xfer;</tt>
 *      <br>declares a variable that can hold a method description
 *      of the type the infrastructure requires to invoke a method.
 *      <li><tt>xfer = new RPCCallableMethod<EchoService>(this, "_xfer");</tt>
 *      <br>initializes that variable.  The arguments mean that the method to
 *      invoke is <tt>this->_xfer()</tt>.
 *      </ol>
 *  <p>
 *  <li> Register the method with the RPC service:
 *       <br><tt>((RPCService)OS.getService("rpc")).registerHandler(servicename(), "xfer", xfer );</tt>
 *       <br>This means that when an incoming RPC specifies service "xfer" (the 1st argument)
 *       and method "xfer" (the 2nd), that the method described by RPCCallableMethod variable
 *       <tt>xfer</tt> should be invoked.
 * </ol>
 * @author grahamb5
 * @author brymar
 */
public class DataXferRPCService extends DataXferServiceBase {
	/**
	 * Key used for DataXferRPC's header, in the args of an RPC call.
	 * The header element is a string (DataXferServiceBase.HEADER_STR).
	 */
	

	/**
	 * Key used for DataXferRPC's payload, in the args of an RPC call
	 */
	public static final String DATA_KEY = "data";

	// A variable capable of describing a method that can be invoked by RPC.
	private RPCCallableMethod xfer;
		
	/**
	 * The constructor registers RPC-callable methods with the RPCService.
	 * @throws IOException
	 * @throws NoSuchMethodException
	 */
	public DataXferRPCService() throws Exception {
		super("dataxferrpc");
		
		// Set up the method descriptor variable to refer to this->_echo()
		xfer = new RPCCallableMethod(this, "_xfer");
		// Register the method with the RPC service as externally invocable method "echo"
		((RPCService)NetBase.theNetBase().getService("rpc")).registerHandler(loadablename(), "dataxfer", xfer);
	}

	public JSONObject _xfer(JSONObject args) throws Exception {
		// check header
		JSONObject header = args.getJSONObject(HEADER_KEY);
		if ( header == null  || !header.has(HEADER_TAG_KEY) || !header.getString(HEADER_TAG_KEY).equalsIgnoreCase(HEADER_STR) || !header.has(HEADER_XFER_LEN_KEY))
			throw new Exception("Missing or incorrect header value: '" + header + "'");
		
		
		byte[] data = new byte[header.getInt(HEADER_XFER_LEN_KEY)];
		header.put(HEADER_TAG_KEY, RESPONSE_OKAY_STR);
		args.put("data", Base64.encodeBytes(data));
		return args;
	}
}
