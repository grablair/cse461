package edu.uw.cs.cse461.consoleapps.solution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.json.JSONObject;

import edu.uw.cs.cse461.consoleapps.PingInterface.PingRPCInterface;
import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.net.rpc.RPCCall;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.service.EchoRPCService;
import edu.uw.cs.cse461.service.EchoServiceBase;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

public class PingRPC extends NetLoadableConsoleApp implements PingRPCInterface{
	private static final String TAG="EchoRPC";
	
	// ConsoleApp's must have a constructor taking no arguments
	public PingRPC() {
		super("pingrpc");
	}
	
	@Override
	public void run() {
		try {
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			ConfigManager config = NetBase.theNetBase().config();

			String targetIP = config.getProperty("net.server.ip");
			if ( targetIP == null ) {
				System.out.print("Enter the server's ip, or empty line to exit: ");
				targetIP = console.readLine();
				if ( targetIP == null || targetIP.trim().isEmpty() ) return;
			}

			System.out.print("Enter the server's RPC port, or empty line to exit: ");
			String targetTCPPortStr = console.readLine();
			if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) return;
			int targetTCPPort = Integer.parseInt( targetTCPPortStr );

			System.out.print("Enter number of trials: ");
			String trialStr = console.readLine();
			int nTrials = Integer.parseInt(trialStr);

			int socketTimeout = config.getAsInt("net.timeout.socket", 2000);

			System.out.println("Host: " + targetIP);
			System.out.println("tcp port: " + targetTCPPort);
			System.out.println("trials: " + nTrials);

			ElapsedTimeInterval pingResult = null;
			
			if ( targetTCPPort != 0 ) {
				ElapsedTime.clear();
				JSONObject header = new JSONObject().put(EchoRPCService.HEADER_TAG_KEY, EchoRPCService.HEADER_STR);
				pingResult = ping(header, targetIP, targetTCPPort, socketTimeout, nTrials);
			}

			if ( pingResult != null ) System.out.println("PING: " + String.format("%.2f msec (%d failures)", pingResult.mean(), pingResult.nAborted()));


		} catch (Exception e) {
			System.out.println("EchoRPC.run() caught exception: " +e.getMessage());
		}
	}
	
	
	public ElapsedTimeInterval ping(JSONObject header, String hostIP, int port, int timeout, int nTrials) throws Exception {

		String msg = "";
		for(int i = 0 ; i < nTrials ; i++) {
			try {
				ElapsedTime.start("PingRPC_PingTotalDelay");

				// send message
				
				JSONObject args = new JSONObject().put(EchoRPCService.HEADER_KEY, header)
						.put(EchoRPCService.PAYLOAD_KEY, msg);
				JSONObject response = RPCCall.invoke(hostIP, port, "echorpc", "echo", args, timeout );
				if ( response == null ) throw new IOException("RPC failed; response is null");

				// examine response
				/*
				JSONObject rcvdHeader = response.optJSONObject(EchoRPCService.HEADER_KEY);
				if ( rcvdHeader == null || !rcvdHeader.has(EchoRPCService.HEADER_TAG_KEY)||
						!rcvdHeader.getString(EchoRPCService.HEADER_TAG_KEY).equalsIgnoreCase(EchoServiceBase.RESPONSE_OKAY_STR))
					throw new IOException("Bad response header: got '" + rcvdHeader.toString() +
							"' but wanted a JSONOBject with key '" + EchoRPCService.HEADER_TAG_KEY + "' and string value '" +
							EchoServiceBase.RESPONSE_OKAY_STR + "'");
							*/
				
				ElapsedTime.stop("PingRPC_PingTotalDelay");
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
				ElapsedTime.abort("PingRPC_PingTotalDelay");
			}
		}
		
		return ElapsedTime.get("PingRPC_PingTotalDelay");
	}

}

