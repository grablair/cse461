package edu.uw.cs.cse461.util;

public class Log {
	static private int mLevel = 0;
	static private boolean mShowLog = true;
	
	/**
	 * This is a simple debug message class that implements
	 * filtering based on log level.  It's modeled after android.util.Log.
	 * <p>
	 * You choose a level at which to produce a message by calling, say,
	 * Log.d("some tag", "my message").  You can set the class to filter
	 * all messages below a client-specified level.
	 * 
	 * @author zahorjan
	 *
	 */
	public static enum DebugLevel {
		VERBOSE(2, "VERBOSE"), 
		DEBUG(3, "DEBUG"), 
		INFO(4, "INFO"), 
		WARN(5, "WARN"), 
		ERROR(6, "ERROR"),
		ASSERT(7, "ASSERT");
		private final int mInt;
		private final String mString;
		private DebugLevel(int level, String s) { mInt = level; mString = s;}
		public int toInt() { return mInt; }
		@Override
		public String toString() { return mString; }  
	};
	
	// Note that this implementation always returns 0, unlike the android version
	// (which returns the number of characters printed) -- Java printf doesn't
	// provide any useful information about number of characters in formatted string.
	static private int _show(DebugLevel level, String tag, String msg) {
		if (  (mShowLog && level.toInt() >= mLevel) || level==DebugLevel.ASSERT) {
			System.out.printf("%010d %7s  %s  %s\n", System.currentTimeMillis(), level, tag, msg);
		}
		return 0;
	}
	
	static public int setLevel(int level) {
		int old = mLevel;
		mLevel = level;
		return old;
	}
	
	static public boolean setShowLog(boolean b) {
		boolean old = mShowLog;
		mShowLog = b;
		return old;
	}
	
	static public int v(String tag, String msg) { return _show(DebugLevel.VERBOSE, tag, msg); }
	static public int d(String tag, String msg) { return _show(DebugLevel.DEBUG, tag, msg); }
	static public int i(String tag, String msg) { return _show(DebugLevel.INFO, tag, msg); }
	static public int w(String tag, String msg) { return _show(DebugLevel.WARN, tag, msg); }
	static public int e(String tag, String msg) { return _show(DebugLevel.ERROR, tag, msg); }
	static public int wtf(String tag, String msg) { return _show(DebugLevel.ASSERT, tag, msg); }
}
