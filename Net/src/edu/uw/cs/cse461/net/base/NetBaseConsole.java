package edu.uw.cs.cse461.net.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.Log;


public class NetBaseConsole extends NetBase {
	private static final String TAG = "NetBaseConsole";
	
	/**
	 * Used to keep start of loaded apps.
	 */
	private static HashMap<String, NetLoadableConsoleApp> mAppMap = new HashMap<String, NetLoadableConsoleApp>();

	public NetBaseConsole(ConfigManager configMgr) {
		super(configMgr);
	}
	
    /**
     *  Tries to load app whose Java class name is the argument. Returns true for success; false otherwise.	
     */
	public String loadApp(String appClassname) {
		String startingApp = null; // for debugging output in catch block
		try {
			Log.d(TAG, "Loading app " + appClassname);
			startingApp = appClassname;
			Class<? extends Object> appClass = (Class<? extends Object>) Class.forName(appClassname);
			NetLoadableConsoleApp app = (NetLoadableConsoleApp)appClass.newInstance();
			
			if ( mAppMap.containsKey(app.loadablename())) throw new Exception("Class " + appClassname + " is using app name " + 
                    app.loadablename() + " but that name is already in use by class "
                    + mAppMap.get(app.loadablename()).getClass().getName());
			String appLoadableName = app.loadablename(); 
			mAppMap.put(appLoadableName, app);
			Log.i(TAG, appClassname + " Loaded as " + appLoadableName);
			return appLoadableName;
		} catch (ClassNotFoundException nfe) {
			Log.e(TAG, "Can't load " + startingApp + ": ClassNotFoundException (typo in config file?)");
		} catch (Exception e) {
			Log.e(TAG, "Can't load " + startingApp + ": " + e.getClass().getName() + " exception: " + e.getMessage());
		}
		return null;
	}
	
	
	/**
	 * Instantiate and remember instances of the loadable services and apps that are (a) named in the config
	 * file, and (b) have set their isImplemented flag to true (meaning the code is in a state that
	 * loading it is a reasonable thing to try).
	 */
	@Override
	protected void _loadApps() {
		String[] appClassList = config().getAsStringVec("console.apps");
		if ( appClassList == null ) return;
		
		for (String appClassname : appClassList) {
			loadApp(appClassname);
		}
	}
	

	/**
	 * Starts an execution of the app, which means calling the run() method
	 * of the single instance of the app.
	 * 
	 * @param appname
	 *            The name returned by the app's appname() method
	 */
	@Override
	public void startApp(String appname) throws Exception {
		NetLoadableConsoleApp app = mAppMap.get(appname);
		if (app == null)
			throw new RuntimeException("App doesn't exist: '" + appname + "'");
		app.run();
	}
	
	/**
	 * Returns the names of all loaded apps.
	 */
	@Override
	public List<String> loadedAppNames() {
		List<String> appList = new ArrayList<String>(mAppMap.keySet());
		java.util.Collections.sort(appList);		
		return appList;
	}

	/**
	 * Returns the single, instantiated instance.
	 */
	@Override
	public NetLoadableConsoleApp getApp(String appname) {
		check("getApp(" + appname + ")");
		return mAppMap.get(appname);
	}
	
	/**
	 * Shutdown associated applications. The main point is to terminate
	 * threads, so that app can terminate.
	 */
	@Override
	public synchronized void shutdown() {
		if (!isUp()) return;
		Log.d(TAG, "NetBaseConsole shutting down...");
		try {
			for (String appName : loadedAppNames()) {
				NetLoadableConsoleApp app = getApp(appName);
				app.shutdown();
			}
			// We can't remove items from the HashMap while iterating
			mAppMap.clear();
		} catch (Exception e) {
			Log.e(TAG, "Error shutting down console apps: " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		super.shutdown();
	}	
}
