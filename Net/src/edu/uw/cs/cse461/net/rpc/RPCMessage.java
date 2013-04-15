package edu.uw.cs.cse461.net.rpc;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage.RPCControlMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCCallMessage.RPCInvokeMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage.RPCErrorResponseMessage;
import edu.uw.cs.cse461.net.rpc.RPCMessage.RPCResponseMessage.RPCNormalResponseMessage;
import edu.uw.cs.cse461.util.Log;

/**
 * The message class hierarchy facilitates marshalling (encoding into the on-the-wire format)
 * and unmarshalling (decoding from the on-the-wire back to a Java object).  (This is also
 * called serialization, and is related to Java's specific notion of serialization.)
 * @author zahorjan
 *
 */
class RPCMessage {
	private static final String TAG="RPCMessage";
	static int mNextRPCId = 0;
	
	protected JSONObject mObject;

	private static synchronized int _nextRequestId() {
		return ++mNextRPCId;
	}
	
	protected RPCMessage() throws JSONException {
		mObject = new JSONObject().put("host", NetBase.theNetBase().hostname())
                                  .put("id", _nextRequestId());
	}
	
	protected RPCMessage(JSONObject obj) throws JSONException {
		mObject = new JSONObject().put("host", obj.getString("host"))
                                  .put("id", obj.getInt("id"));
	}

	int id() throws JSONException {
		return mObject.getInt("id");
	}
	
	String type() throws JSONException {
		return mObject.getString("type");
	}
	
	String host() throws JSONException {
		return mObject.getString("host");
	}
	
	JSONObject marshall() {
		return mObject;
	}
	
	@Override
	public String toString() {
		return mObject.toString();
	}
	
	static RPCMessage unmarshall(String jsonFormatString) throws IOException {
		if ( jsonFormatString == null ) throw new IOException("RPCMessage.stringToMessage was passed null");

		try {
			JSONObject jsonObj = new JSONObject(jsonFormatString);
			
			String type = jsonObj.getString("type"); 
			if ( type.equalsIgnoreCase("control") ) return new RPCControlMessage(jsonObj);
			if ( type.equalsIgnoreCase("invoke") )  return new RPCInvokeMessage(jsonObj);
			if ( type.equalsIgnoreCase("OK") )      return new RPCNormalResponseMessage(jsonObj);
			if ( type.equalsIgnoreCase("ERROR") )   return new RPCErrorResponseMessage(jsonObj);
			String msg = "Got unrecognized type in message: " + type + " [" + jsonFormatString + "]"; 
			Log.e(TAG, msg );
			throw new IOException(msg);

		} catch (JSONException je) {
			throw new IOException("Unparsable message: '" + jsonFormatString + "'");
		}
		
	}
	
	//---------------------------------------------------------
	// Call message classes
	//---------------------------------------------------------

	static class RPCCallMessage extends RPCMessage {
		RPCCallMessage() throws JSONException {
		}
		
		RPCCallMessage(JSONObject jsonObj) throws JSONException {
			super(jsonObj);
		}

		public JSONObject args() throws JSONException {
			if ( mObject.has("args") ) return mObject.getJSONObject("args");
			return null;
		}
		/**
		 * For sending RPCService -> RPCService control messages.
		 * @author zahorjan
		 *
		 */
		static public class RPCControlMessage extends RPCCallMessage {
			/**
			 * Creates a valid control message.
			 * @param action
			 * @param options A JSON encoded JSONOBject of optional values
			 * @throws JSONException
			 */
			RPCControlMessage(String action, JSONObject options) throws JSONException {
				mObject.put("type", "control");
				mObject.put("action", action);
				if ( options != null ) mObject.put("options", options);
			}
			
			RPCControlMessage(JSONObject jsonObject) throws JSONException {
				super(jsonObject);
				mObject.put("type", "control");
				mObject.put("action", jsonObject.get("action"));
				if ( jsonObject.has("options") ) mObject.put("options", jsonObject.getJSONObject("options"));
			}
			
			public String action() throws JSONException {
				return mObject.getString("action");
			}
			
			public String getOption(String fieldName) { 
				JSONObject optionObj = mObject.optJSONObject("options");
				if ( optionObj == null ) return null;
				return optionObj.optString(fieldName);
			}
		}

		/**
		 * For sending normal RPC invocations.
		 * @author zahorjan
		 *
		 */
		static public class RPCInvokeMessage extends RPCCallMessage {
			RPCInvokeMessage(String service, String method, JSONObject args) throws JSONException {
				mObject.put("type", "invoke")
					   .put("app", service)
					   .put("method", method);
				if ( args != null ) mObject.put("args", args);
			}
			
			RPCInvokeMessage(JSONObject jsonObject) throws JSONException {
				super(jsonObject);
				mObject.put("type", "invoke")
				       .put("app", jsonObject.getString("app"))
				       .put("method", jsonObject.getString("method"));
				if ( jsonObject.has("args") ) mObject.put("args", jsonObject.getJSONObject("args"));
			}
			
			String app() throws JSONException {
				return mObject.getString("app");
			}
			
			String method() throws JSONException {
				return mObject.getString("method");
			}
		}
	}
	
	
	//----------------------------------------------------------------------------------
	// Response message classes
	//----------------------------------------------------------------------------------
	
	static public class RPCResponseMessage extends RPCMessage {
		public static final String fatalErrorResponseMessage = "FATAL RESPONSE"; 

		RPCResponseMessage(int id) throws JSONException {
			mObject.put("callid", id);
		}
		
		RPCResponseMessage(JSONObject jsonObj) throws JSONException {
			super(jsonObj);
			mObject.put("callid", jsonObj.getInt("callid") );
		}
		
		int callid() throws JSONException {
			return mObject.getInt("callid");
		}

		static public class RPCNormalResponseMessage extends RPCResponseMessage {
			RPCNormalResponseMessage(int callid, JSONObject retval) throws JSONException {
				super(callid);
				mObject.put("type", "OK");
				if ( retval != null ) mObject.put("value", retval);
			}
			
			RPCNormalResponseMessage(JSONObject jsonObj ) throws JSONException {
				super(jsonObj);
				mObject.put("type", "OK");
				// JSONObject throws an exception for almost every kind of read if the key exists but the value is null
				if ( jsonObj.has("value") && jsonObj.get("value") != null ) mObject.put("value", jsonObj.getJSONObject("value"));
			}
			
			public JSONObject value() throws JSONException {
				if ( mObject.has("value") ) return mObject.getJSONObject("value");
				return null;
			}
		}

		static public class RPCErrorResponseMessage extends RPCResponseMessage {

			public static final String FATAL_ERROR_RESPONSE = "{\"id\":-1, \"host\":\"" + NetBase.theNetBase().hostname() +
					"\", \"type\":\"ERROR\", \"message\":\"Fatal JSON errors prevented sending a sensible response\"}";
			
			RPCErrorResponseMessage(int callid, String msg, RPCCallMessage callMessage ) throws JSONException {
				super(callid);
				mObject.put("type", "ERROR");
				mObject.put("message", msg);
				if ( callMessage != null ) mObject.put("callargs", callMessage.marshall());
				else mObject.put("callargs", "unrecognizable");
			}
			
			RPCErrorResponseMessage(JSONObject jsonObj) throws JSONException {
				super(jsonObj);
				mObject.put("type", "ERROR");
				mObject.put("message", jsonObj.getString("message") );
				if ( jsonObj.has("callargs") ) mObject.put("callargs", jsonObj.getJSONObject("callargs"));
			}
		}
	}
}


