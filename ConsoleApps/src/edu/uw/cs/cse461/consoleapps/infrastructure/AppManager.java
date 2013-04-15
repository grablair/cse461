package edu.uw.cs.cse461.consoleapps.infrastructure;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;

/**
 * An AppManager acts someting like a shell on a traditional system - it lets
 * you run apps (OSConsoleApps).  Unlike traditional systems, but something
 * like Android, there is only one instance of an app created, no matter how
 * many times it's invoked.  In this system, all apps are loaded when the 
 * OS boots.  An app invocation is merely a call to its run() method.
 * The AppManager   
 * @author zahorjan
 *
 */
public class AppManager extends NetLoadableConsoleApp {
	
	/**
	 * Constructor required by OSConsoleApp.
	 */
	public AppManager() {
		super("appmanager");
	}

	/**
	 * This method implements a very primitive shell.  Apps are "run in the foreground."
	 * @throws Exception
	 */
	public void run() throws Exception {
		
		// Eclipse doesn't support System.console()
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		
		// convert list of loaded apps into a prompt string
		List<String> appList = NetBase.theNetBase().loadedAppNames();
		StringBuilder sb = new StringBuilder().append("\nLoaded applications:\n");
		for ( int i=0; i<appList.size(); i++) {
			sb.append( String.format("  * %s\n", appList.get(i)));
		}

		// sit in loop reading user input and executing apps
		while (true) {
			System.out.print("Select application (empty for list, exit to exit)> ");
			String appName = console.readLine();
			if ( appName == null ) break; // EOF reached (via ^d in Eclipse, at least)
			if ( appName.isEmpty() ) {
				System.out.println(sb.toString());
				continue;
			}
			if ( appName.equals("exit") ) break;
			try {
				NetBase.theNetBase().startApp(appName);
			} catch (Exception e) {
				System.out.println("App " + appName + " threw uncaught exception: " + e.getMessage());
			}
		}
	}
}
