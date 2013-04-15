package edu.uw.cs.cse461.net.tcpmessagehandler;

import java.io.IOException;
import java.net.SocketException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public interface TCPMessageHandlerInterface {
	
	//--------------------------------------------------------------------------------------
	// send routines
	//   What is actually sent is always byte[].  These routines must translate into
	//   byte[] for sending.
	//--------------------------------------------------------------------------------------
	
	public void sendMessage(byte[] buf) throws IOException;
	public void sendMessage(String str) throws IOException;
	public void sendMessage(int value) throws IOException;
	public void sendMessage(JSONArray jsArray) throws IOException;
	public void sendMessage(JSONObject jsObject) throws IOException;
	
	//--------------------------------------------------------------------------------------
	// read routines
	//   Data comes off the network as bytes.  The various read routines
	//   convert from bytes into the requested data type.  They're the
	//   inverses of the send routines.
	//   All read methods thrown an IOException when end-of-file is detected.
	//--------------------------------------------------------------------------------------
	
	public byte[] readMessageAsBytes() throws IOException;
	public String readMessageAsString() throws IOException;
	public int readMessageAsInt() throws IOException;
	public JSONArray readMessageAsJSONArray() throws IOException, JSONException;
	public JSONObject readMessageAsJSONObject() throws IOException, JSONException;
	
	public int setMaxReadLength(int maxLen);  // don't even try to read a message claiming to be longer than the arg value
	public int getMaxReadLength();            // returns current value of max read length

	/**
	 * Sets the amount of time between an attempt to read from the stream and timing out if no data becomes available.
	 * Note that because this can be a long time (e.g., tens of seconds, or more), the underlying socket may need to time out
	 * at the TCP level much more quickly, so that the thread executing the read routine can check for application termination.
	 * @param timeout Time out value, in msec.
	 * @return Previous timeout
	 */
	public int setTimeout(int timeout) throws SocketException;
	
	/**
	 * Sets the TCP no delay option to either true or false, as specified by the argument.
	 * (This affects whether ACKs are delayed or not, and may affect performance in some applications.)
	 * @param value true or false
	 * @return The prior value
	 */
	public boolean setNoDelay(boolean value) throws SocketException;
	
	/**
	 * Closes the TCPMessageHandler and the underlying socket it wraps.
	 */
	public void close();
}
