package edu.uw.cs.cse461.net.base;

/**
 * Base class for OS loadable classes.  In addition to the methods shown
 * here, each class implementing this interface must provide
 * a constructor that takes no arguments.
 *
 * @author zahorjan
 *
 */
public abstract class NetLoadable implements NetLoadableInterface {
	
	private String mLoadableName;    // set via argument provided to NetLoadable constructor
	protected boolean mAmShutdown;   // if true, indicates the loadable should shut down
	
	/**
	 * Every NetLoadable subclass must have a public constructor taking no arguments.  It must
	 * invoke its immediate superclass's constructor, providing the loadable's name and whether or not
	 * it is implemented (both of which are known statically).  If isImplemented is false, the loadable
	 * will NOT be loaded.
	 */
	protected NetLoadable(String loadableName) {
		mLoadableName = loadableName;
		mAmShutdown = false;
	}
	
	/**
	 * Returns the name used to identify the loadable.  Names must be unique,
	 * and may have global (cross-system) meaning.
	 * (For example, the instance for the RPC service should return "rpc".)
	 * @return
	 */
	@Override
	public String loadablename() {
		return mLoadableName;
	}

	/**
	 * Called by the infrastructure when this loadable should shut down.
	 * Subclasses are likely to define more specific shutdown() implementations.
	 * For example, if they have started any threads, its important that those
	 * threads be terminated - otherwise, the application as a whole may not terminate.
	 * Subclass implementations should always invoke this implementation as
	 * the first thing they do.
	 */
	@Override
	public void shutdown() {
		mAmShutdown = true;
	}
	
	/**
	 * Returns true if this component is shut (or is shutting) down, false otherwise.
	 */
	@Override
	public boolean isShutdown() {
		return mAmShutdown;
	}
	
	//---------------------------------------------------------------------------------------------------
	/**
	 * Interface for applications that run in console mode.
	 * Loadable console apps live in the ConsoleApps package.
	 * 
	 * @author zahorjan
	 *
	 */
	public static abstract class NetLoadableConsoleApp extends NetLoadable implements NetLoadableConsoleAppInterface {
		
		protected NetLoadableConsoleApp(String name) {
			super(name);
		}
		
		/**
		 * This method is called each time the app is invoked via the AppManager. 
		 * @throws Exception
		 */
		public abstract void run() throws Exception;
	
	}
	//---------------------------------------------------------------------------------------------------

	
	//---------------------------------------------------------------------------------------------------
	//
	// There is another class that is logically a subclass of NetLoadable -- the NetLoadableAnroidApp class.
	// Because of some issues involved with supporting both console and Android by a single code base,
	// the NetLoadableAndroidApp class is defined separated, in an Android project.
	//---------------------------------------------------------------------------------------------------

	
	//---------------------------------------------------------------------------------------------------
	/**
	 * Interface name defined to make it easy to identify loadable services.
	 * Loadable services have no generic "run" interface - logically, they start
	 * when they are loaded.  In practice, they may start threads when they are loaded
	 * (e.g., in their constructors), or loading them may simply make a set of methods available
	 * to other code (much like loading a library).
	 */
	public static abstract class NetLoadableService extends NetLoadable implements NetLoadableServiceInterface {
		protected NetLoadableService(String name) {
			super(name);
		}
		
		/**
		 * Produces a string describing the current state of the service.  Used for debugging.
		 * @return A string describing the current state of the service.
		 */
		public abstract String dumpState();

	}
	//---------------------------------------------------------------------------------------------------
}
