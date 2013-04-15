package edu.uw.cs.cse461.consoleapps;

import org.json.JSONObject;

import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

public interface PingInterface {
	
	public interface PingRawInterface {
		public ElapsedTimeInterval udpPing(byte[] header, String hostIP, int udpPort, int timeout, int nTrials);
		public ElapsedTimeInterval tcpPing(byte[] header, String hostIP, int tcpPort, int timeout, int nTrials);
	}
	
	public interface PingTCPMessageHandlerInterface {
		public ElapsedTimeInterval ping(String header, String hostIP, int port, int timeout, int nTrials) throws Exception;
	}

	public interface PingRPCInterface {
		public ElapsedTimeInterval ping(JSONObject header, String hostIP, int port, int timeout, int nTrials) throws Exception;
	}
	
	public interface PingDDNSInterface {
		public ElapsedTimeInterval ping(JSONObject header, String hostName, int timeout, int nTrials)  throws Exception;
	}

}