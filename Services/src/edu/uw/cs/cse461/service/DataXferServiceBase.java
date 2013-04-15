package edu.uw.cs.cse461.service;

import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableService;


public abstract class DataXferServiceBase  extends NetLoadableService {

	public static final String HEADER_STR = "xfer";
	public static final byte[] HEADER_BYTES = HEADER_STR.getBytes();
	public static final int HEADER_LEN = HEADER_BYTES.length;

	public static final String RESPONSE_OKAY_STR = "okay";
	public static final byte[] RESPONSE_OKAY_BYTES = RESPONSE_OKAY_STR.getBytes();
	public static final int RESPONSE_OKAY_LEN = RESPONSE_OKAY_BYTES.length;
		
	/**
	 * Pass subclass's loadable name up to base class.
	 */
	protected DataXferServiceBase(String loadablename) {
		super(loadablename);
	}

	@Override
	public String dumpState() {
		return loadablename() + (mAmShutdown ? " is down" : " is up");
	}
	
}
