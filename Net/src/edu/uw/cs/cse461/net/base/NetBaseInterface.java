package edu.uw.cs.cse461.net.base;

import java.util.List;

import edu.uw.cs.cse461.net.base.NetLoadableInterface.NetLoadableServiceInterface;
import edu.uw.cs.cse461.util.ConfigManager;

/**
 * This is the public interface to (just) the core NetBase component.
 * (Loaded services present their own interfaces.)  This interface is
 * defined primarily as documentation -- you can see all the publicly
 * accessible NetBase methods here.  It is NOT the intention that there ever be
 * more than one implementation of this interface.
 * @author zahorjan
 *
 */
public interface NetBaseInterface {
	
	// There is an important static method that returns the (sole) NetBase instance.
	// Because it's static, it can't be part of the interface spec, but if it could it would be...
	// public static NetBase theNetBase();  // returns the sole NetBase object
	
	// startup/shutdown related methods
	public boolean isUp();      // true if the network stack exists, has been initialized, and hasn't been shut down; false otherwise
	public void shutdown();     // Shuts down the network stack

	// config file related methods
	public ConfigManager config();   // Provides access to a parsed version of the config file read during boot
	public String hostname();        // Convenience method to get the host name (as specified in the boot config file)
	
	// general utility methods
	public String version();        // Version number of assignment software
	public long now();              // Returns current Unix time (seconds since 1/1/1970).
	
	// loadable service methods
	public List<String> loadedServiceNames();                  // Names of all currently loaded services
	public NetLoadableServiceInterface getService(String servicename);  // Get a handle to a loaded service 

	// loadable application methods, for both console and Android apps
	public String loadApp(String appclassname);              // Tries to load app whose Java class name is the argument.
	                                                         // Returns app's internal name if successful; null if unable to load.
	public List<String> loadedAppNames();                    // Names of all currently loaded applications
	public NetLoadableInterface getApp(String appname);      // Get a handle to a loaded application
	public void startApp(String appname) throws Exception;   // Invoke run() method of a loaded application

}
