package edu.uw.cs.cse461.consoleapps.solution;

import java.io.IOException;

import org.json.JSONException;

import edu.uw.cs.cse461.consoleapps.DataXferInterface.DataXferTCPMessageHandlerInterface;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

public class DataXferTCPMessageHandler implements
		DataXferTCPMessageHandlerInterface {

	@Override
	public byte[] DataXfer(String header, String hostIP, int port, int timeout,
			int xferLength) throws JSONException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransferRateInterval DataXferRate(String header, String hostIP,
			int port, int timeout, int xferLength, int nTrials) {
		// TODO Auto-generated method stub
		return null;
	}

}
