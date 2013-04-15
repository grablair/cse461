package edu.uw.cs.cse461.consoleapps;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

public interface DataXferInterface {
	
	public interface DataXferRawInterface {
		// These interfaces provide the data that was received.  (The returned array is of length xferLength if
		// there are no transfer errors.)  Parameter xferLength is the number of data bytes that should be sent back
		// in each response packet.
		public byte[] udpDataXfer(byte[] header, String hostIP, int udpPort, int timeout, int xferLength) throws IOException;
		public byte[] tcpDataXfer(byte[] header, String hostIP, int tcpPort, int timeout, int xferLength) throws IOException;

		// These interfaces provide transfer rate information, but not the data transferred.  They simply
		// instrument calls to the methods above.
		public TransferRateInterval udpDataXferRate(byte[] header, String hostIP, int udpPort, int timeout, int xferLength, int nTrials);
		public TransferRateInterval tcpDataXferRate(byte[] header, String hostIP, int tcpPort, int timeout, int xferLength, int nTrials);
	}
	
	public interface DataXferTCPMessageHandlerInterface {
		public byte[] DataXfer(String header, String hostIP, int port, int timeout, int xferLength) throws JSONException, IOException;
		public TransferRateInterval DataXferRate(String header, String hostIP, int port, int timeout, int xferLength, int nTrials);
	}

	public interface DataXferRPCInterface {
		public byte[] DataXfer(JSONObject header, String hostIP, int port, int timeout) throws JSONException, IOException;
		public TransferRateInterval DataXferRate(JSONObject header, String hostIP, int port, int timeout, int nTrials);
	}
	
	public interface DataXferDDNSInterface {
		public byte[] DataXfer(JSONObject header, String hostName, int xferLength) throws JSONException, IOException;
		public TransferRateInterval DataXferRate(JSONObject header, String hostName, int xferLength, int nTrials);
	}

}