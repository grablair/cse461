package edu.uw.cs.cse461.net.rpc;


/**
 * The interface for the side of RPC that receives incoming calls.
 * @author zahorjan
 *
 */
public interface RPCServiceInterface {

	/**
	 * Sets up a callback when an incoming RPC mentions the service and method given by the arguments.
	 * @param serviceName  The name the service or appliation is known by for RPC.
	 * @param methodName  The name for the method, as used in RPC invocations.
	 * @param method  The descriptor for the Java method to connect to the serviceName/methodName
	 * @throws Exception 
	 */
	public void registerHandler(String serviceName, String methodName, RPCCallableMethod method) throws Exception;
	
	/**
	 * Some of the testing code needs to retrieve the current registration for a particular service and method,
	 * so this interface is required.  You probably won't find a use for it in your code, though.
	 * 
	 * @param serviceName  The service name
	 * @param methodName The method name
	 * @return The existing registration for that method of that service, or null if no registration exists.
	 */
	public RPCCallableMethod getRegistrationFor( String serviceName, String methodName);
	
	/**
	 * Returns the port number of the RPC server socket on this node.  (The IP is available from IPFinder.localIP().)
	 */
	public int localPort();
	
}
