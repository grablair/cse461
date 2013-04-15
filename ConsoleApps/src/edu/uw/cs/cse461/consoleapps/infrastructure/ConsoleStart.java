package edu.uw.cs.cse461.consoleapps.infrastructure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetBaseConsole;
import edu.uw.cs.cse461.net.base.NetBaseInterface;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.Log;

/**
 * This class implements OS startup for the Console environment.
 * Execution in the Console environment begins in main() of this class.
 * @author zahorjan
 *
 */
public abstract class ConsoleStart implements NetBaseInterface {
	
	/**
	 * A simple driver that fires up the OS by calling its boot() method.
	 * We need to pass boot() a FileInputStream, connected to the config file to use,
	 * because of restrictions of the Android implementation.
	 * @param args
	 */
	public static void main(String[] args) {
		final String TAG="ConsoleStart";
		String configDir = ".";
		File configFile = null;
		
		try {
			// This code deals with command line options
			Options options = new Options();
			options.addOption("d", "configdir", true, "Config file directory (Default: " + configDir + ")");
			options.addOption("f", "configfile", true, "Path name of config file");
			options.addOption("h", "hostname", true, "Specify hostname.  (Overrides any value in config file.)");
			options.addOption("H", "help", false, "Print this message");

			CommandLineParser parser = new PosixParser();

			CommandLine line = parser.parse(options, args);
			if ( line.hasOption("help") ) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java ConsoleStart", options );
				return;
			}
			
			//------------------------------------------------------------------------
			// Choose configuration file
			//------------------------------------------------------------------------
			
			if ( line.hasOption("configfile") ) {
				configFile = new File(line.getOptionValue("configfile"));
			}
			else {
				if ( line.hasOption("configdir")) configDir = line.getOptionValue("configdir",configDir); 


				File dir = new File(configDir);
				if ( !dir.isDirectory()) {
					System.err.println(line.getOptionValue("configdir") + " isn't a directory");
					System.exit(-1);
				}

				// enumerate *.config.ini files in the directory and let the user choose one
				FilenameFilter configFilter = new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						return filename.endsWith("config.ini");
					}
				};
				File[] fileList = dir.listFiles(configFilter);
				if ( fileList.length == 1) {
					// if there's only one, just use it without asking
					configFile = fileList[0];
				} else if ( fileList.length > 0 ){
					Arrays.sort(fileList);
					BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
					// let user choose
					do {
						System.out.println("Choose a configuration file:");
						for ( int i=0; i<fileList.length; i++ ) {
							System.out.println( String.format("\t%2d  %s", i, fileList[i].getName()));
						}
						System.out.print("Enter choice (empty to exit)> ");
						try {
							String choice = console.readLine();
							if ( choice.isEmpty()) return;  // terminate if no choice made
							int selection = Integer.parseInt(choice);
							configFile = fileList[selection];
						} catch (NumberFormatException nfe) {
							System.err.println("Invalid selection");
							return;
						}
					} while ( configFile == null );
				}
			}
			
			if ( configFile == null ) {
				System.err.println("Can't identify a configuration file.  Try using a relevant command line option.");
				System.err.println("(Process working directory is " + new File(".").getCanonicalPath() + ")");
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java ConsoleStart", options, false );
				System.exit(-1);
			}
			
			if (!configFile.isFile() ) {
				System.err.println("Config file " + configFile.getCanonicalPath() + " doesn't exist");
				System.exit(-1);
			}
			
			if (!configFile.canRead() ) {
				System.err.println("Can't read config file " + configFile.getCanonicalPath() );
				System.exit(-1);
			}
			
			Log.i(TAG, "Config filename = " + configFile.getCanonicalPath());

			//------------------------------------------------------------------------
			// Create configuration manager
			//------------------------------------------------------------------------
			
			ConfigManager configMgr = new ConfigManager(new FileInputStream(configFile));
			
			// Override the config's net.host.name property with the hostname specified as a command line argument
			if ( line.hasOption("hostname") ) configMgr.setProperty("net.host.name", line.getOptionValue("hostname"));
			
			// Save the directory we found the config file in in the config itself.  (Used by DDNSService to locate ddns.nodefile.)
			configMgr.setProperty("config.directory", configFile.getParent() );
			
			//------------------------------------------------------------------------
			// Initialize Logging
			//------------------------------------------------------------------------
			
			int showDebug = configMgr.getAsInt("debug.enable", 1);
			Log.setShowLog(showDebug != 0);
			int debugLevel = configMgr.getAsInt("debug.level", Log.DebugLevel.DEBUG.toInt());
			Log.setLevel(debugLevel);

			//------------------------------------------------------------------------
			// Initialize IPFinder
			//------------------------------------------------------------------------

			IPFinder.setIP( configMgr.getProperty("net.host.ip"));
			if ( IPFinder.localIP() == null ) {
				// IPFinder can't figure out what to do.  Present options and ask user
				List<InetAddress> ipList = IPFinder.getPreferredAddressList();
				if ( ipList.isEmpty() ) {
					System.err.println("I can't find any plausible IP addresses for this machine.");
					System.err.println("You can set config file field 'net.host.ip' to 'localhost' or to some IP to force use of that IP (but it may fail).");
					return;
				}
				ipList.add(InetAddress.getByName("localhost"));
				
				System.out.println("Please choose an IP to use:");
				int index;
				for ( index=0; index<ipList.size(); index++ ) {
					System.out.println( String.format("%2d  %s", index, ipList.get(index).getHostAddress()) );
				}

				BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
				while ( true ) {
					System.out.println("Selection (empty to exit)> ");
					String choice = console.readLine();
					if ( choice.isEmpty()) return;
					int selection = Integer.parseInt(choice);
					if ( selection < 0 || selection >= ipList.size() ) return;
					IPFinder.setIP( ipList.get(selection).getHostAddress() );
					break;
				}
			}
			// this should never happen, but...
			if ( IPFinder.localIP() == null ) throw new RuntimeException("Failed to come up with any IP address for this machine.  You could try net.host.ip=localhost in the config file.");
			
			//------------------------------------------------------------------------
			// Set up NetBase
			//------------------------------------------------------------------------
			
			NetBaseConsole theNetBase = new NetBaseConsole(configMgr);
			
			//------------------------------------------------------------------------
			// if there is an console.initialapp app, start it. Otherwise, just run as a daemon
			//------------------------------------------------------------------------

			if ( theNetBase.startInitialApp("console.initialapp") )
				NetBase.theNetBase().shutdown(); // we're done when the initial app terminates
			
			// if there was no initial app, just run as a daemon
			
		} catch (Exception e) {
			Log.e(TAG, "Caught " + e.getClass().getName() + " exception: " + e.getMessage());
		}
	}
	
}
