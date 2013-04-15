package edu.uw.cs.cse461.net.rpc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;

import org.json.JSONObject;

import edu.uw.cs.cse461.util.Log;

/**
 * An object of this type represents an RPC callable method.  You'll have one
 * object of this type for each method you expose by RPC.
 * <p>
 * The parameterized type, T, is the class exposing the method.
 * <p>
 * See the source in EchoRPCService.java for an example of its use.
 * @author zahorjan
 *
 * @param <T>
 */
public class RPCCallableMethod {
	private static final String TAG="RPCCallableMethod";
	
	Object service;
	Method method;
	/**
	 * Constructor.
	 * @param serviceObject The Java instance of the object that will field the RPC
	 * @param methodName The name of the Java method to invoke on that object, as a String
	 * @throws NoSuchMethodException
	 */
	public RPCCallableMethod(Object serviceObject, String methodName) throws NoSuchMethodException { 
		service = serviceObject; 
		Class<? extends Object> serviceClass = (Class<? extends Object>)service.getClass();
		method = serviceClass.getMethod(methodName, JSONObject.class);
	}
	/**
	 * This method is called to actually invoke the method that handles the RPC.
	 * @param args  The arguments to pass on this call
	 * @return The JSONObject returned by the RPC handling method of the service
	 * @throws Exception
	 */
	public JSONObject handleCall(JSONObject args) throws Exception {
		try {
			return (JSONObject)method.invoke(service, args);
		} catch (Exception e) {
			final Writer trace = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(trace);
			e.printStackTrace(printWriter);
			Log.e(TAG, "Caught Exception: " + e.getMessage() + "\n" + trace.toString());
			printWriter.close();
			trace.close();
			throw e;
		}
	}
}
