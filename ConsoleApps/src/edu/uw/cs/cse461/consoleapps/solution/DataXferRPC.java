package edu.uw.cs.cse461.consoleapps.solution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.consoleapps.DataXferInterface.DataXferRPCInterface;
import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.net.rpc.RPCCall;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.service.DataXferServiceBase;
import edu.uw.cs.cse461.service.EchoRPCService;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRate;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

public class DataXferRPC extends NetLoadableConsoleApp implements DataXferRPCInterface {

	public DataXferRPC() {
		super("dataxferrpc");
	}

	@Override
	public void run() throws Exception {
		try {

			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

			ConfigManager config = NetBase.theNetBase().config();
			String server = config.getProperty("net.server.ip");
			if ( server == null ) {
				System.out.print("Enter a host ip, or exit to exit: ");
				server = console.readLine();
				if ( server == null ) return;
				if ( server.equals("exit")) return;
			}

			System.out.print("Enter the server's port number, or empty line to exit: ");
			String targetPortStr = console.readLine();
			if ( targetPortStr == null || targetPortStr.trim().isEmpty() ) return;
			int targetPort = Integer.parseInt( targetPortStr );
			
			int socketTimeout = config.getAsInt("net.timeout.socket", -1);
			if ( socketTimeout < 0 ) {
				System.out.print("Enter socket timeout (in msec.): ");
				String timeoutStr = console.readLine();
				socketTimeout = Integer.parseInt(timeoutStr);
				
			}

			System.out.print("Enter number of trials: ");
			String trialStr = console.readLine();
			int nTrials = Integer.parseInt(trialStr);
			
			System.out.print("Enter the ammount of data to transfer, or empty line to exit: ");
			String xferLengthStr = console.readLine();
			if ( xferLengthStr == null || xferLengthStr.trim().isEmpty() ) return;
			int xferLength = Integer.parseInt( xferLengthStr );
			
			TransferRate.clear();

			System.out.println("\n" + xferLength + " bytes");

			//-----------------------------------------------------
			// TCP transfer
			//-----------------------------------------------------
			JSONObject header = new JSONObject().put(EchoRPCService.HEADER_TAG_KEY, DataXferServiceBase.HEADER_STR)
												.put("xferLength", xferLength);
			TransferRateInterval xferStats = DataXferRate(header, server, targetPort, socketTimeout, nTrials);

			System.out.println("\nTCP: xfer rate = " + String.format("%9.0f", xferStats.mean() * 1000.0) + " bytes/sec.");
			System.out.println("TCP: failure rate = " + String.format("%5.1f", xferStats.failureRate()) +
					" [" + xferStats.nAborted()+ "/" + xferStats.nTrials() + "]");

			
		} catch (Exception e) {
			System.out.println("Unanticipated exception: " + e.getMessage());
		}
		
	}
	

	@Override
	public TransferRateInterval DataXferRate(JSONObject header, String hostIP, int port, int timeout, int nTrials) {
		
		for ( int trial=0; trial<nTrials; trial++) {
			try {
				TransferRate.start("xfer");
				DataXfer(header, hostIP, port, timeout);
				TransferRate.stop("xfer", header.optLong("xferLength"));
			} catch (ConnectException e) {
				TransferRate.abort("xfer", header.optLong("xferLength"));
				System.out.println("xfer trial failed: " + e.getMessage());
			} catch (Exception e) {
				TransferRate.abort("xfer", header.optLong("xferLength"));
				System.out.println("xfer trial failed: " + e.getMessage());
			}
		
		}
		return TransferRate.get("xfer");
	}
	
	
	@Override
	public byte[] DataXfer(JSONObject header, String hostIP, int port, int timeout) throws JSONException, IOException {
		byte[] resp = null;

		// send message
		
		JSONObject args = new JSONObject().put(EchoRPCService.HEADER_KEY, header);
		JSONObject response = RPCCall.invoke(hostIP, port, "dataxferrpc", "dataxfer", args, timeout );
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
		
		// read response
		resp = response.getString("data").getBytes();
		return resp;
	}
	

}

