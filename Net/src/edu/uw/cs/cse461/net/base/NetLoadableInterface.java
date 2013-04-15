package edu.uw.cs.cse461.net.base;

/**
 * Because of some difficulties supporting both console and Android implementations via
 * a common base class, we end up with both a NetLoadable class and an interface.
 *
 * @author zahorjan
 *
 */
public interface NetLoadableInterface {
	
	public String loadablename();
	public void shutdown();
	public boolean isShutdown();
	
	public interface NetLoadableServiceInterface extends NetLoadableInterface {
		public String dumpState();
	}

	public interface NetLoadableConsoleAppInterface extends NetLoadableInterface {
		public void run() throws Exception;
	}
	
	public interface NetLoadableAndroidAppInterface extends NetLoadableInterface {
	}
}
