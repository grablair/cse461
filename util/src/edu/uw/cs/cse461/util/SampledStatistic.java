package edu.uw.cs.cse461.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is a static class implementing a simple sample-based measurement facility.  Subclasses provide
 * support for sample elapsed time and for sampling transfer rates.  The caller provides a key (a string name)
 * with each sample registered with this package.  Statistics are computed over samples having the same key.
 * <p>
 * Subclasses provide explicit support for sampling elapsed time (summarized by an arithmetic mean) and average
 * transfer rate (summarized by a harmonic mean).
 * 
 * @author zahorjan
 *
 */
public class SampledStatistic {

	//--------------------------------------------------------------------------------------------
	/**
	 * A helper class.  There is a SampleSet associated with each distinct timer key value.
	 * (Java makes it too clumsy to make this a generic, so we settle as double for the type of samples.)
	 * @author zahorjan
	 *
	 */
	static abstract class SampleSet {
		protected long   mNumSamples = 0;
		protected long   mNumAborted = 0;
		
		protected double mMaxSample = Double.MIN_VALUE;
		protected double mMinSample = Double.MAX_VALUE;

		protected void addSample(double sample) {
			mNumSamples++;
			if ( sample < mMinSample ) mMinSample = sample;
			if ( sample > mMaxSample ) mMaxSample = sample;
		}
		void abort() { mNumAborted++; }
		
		abstract public double mean();
		public double min() { return mMinSample; }
		public double max() { return mMaxSample; }
		
		public long nSamples() { return mNumSamples; }
		public long nAborted() { return mNumAborted; }
		public long nTrials() { return mNumAborted + mNumSamples; }
		
		public double failureRate() {
			if ( nTrials() > 0 ) return ((double)mNumAborted)/nTrials();
			return 0.0;
		}
		
		/**
		 * Format a string containing simple measures of the samples recorded for this timer.
		 */
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("[").append(String.format("%6.2f", mean())).
			append(", ").append(String.format("%6.2f", min() )).
			append(", ").append(String.format("%6.2f", max() )).
			append("] (").append(nSamples()).append(" samples, ").append(nAborted()).append(" aborted)");
			return sb.toString();
		}

	}		
	//--------------------------------------------------------------------------------------------

	//--------------------------------------------------------------------------------------------
	static class ArithmeticMeanSet extends SampleSet {

		double   	mSampleTotal = 0.0;
		double  	mSquaredSampleTotal = 0.0;

		public void addSample(double sample) {
			super.addSample(sample);
			mSampleTotal += sample;
			mSquaredSampleTotal += sample*sample;
		}

		/**
		 * Return the arithemtic mean.
		 * @return Arithmetic mean of samples.  Returns 0.0 if no samples yet registered.
		 */
		public double mean() { return mSampleTotal / (mNumSamples>0?mNumSamples:1); 	}
	}
	//--------------------------------------------------------------------------------------------
	
	//--------------------------------------------------------------------------------------------
	public static class ElapsedTimeInterval extends ArithmeticMeanSet {
		long   mStartTime = -1;
	}
	//--------------------------------------------------------------------------------------------
	
	
	//--------------------------------------------------------------------------------------------
	static class HarmonicMean extends SampleSet {

		double   	mSampleTimeTotal = 0.0;   // total time spent in transfers
		long		mSampleDataTotal = 0;     // total data transferred

		public void addSample(double timeSample, long dataSample) {
			if ( timeSample <= 0.0 ) throw new RuntimeException("Harmonic mean time sample must be greater than 0.0");
			super.addSample(dataSample/timeSample);
			mSampleTimeTotal += timeSample;
			mSampleDataTotal += dataSample;
		}

		/**
		 * Return the arithemtic mean.
		 * @return Arithmetic mean of samples.  Returns 0.0 if no samples yet registered.
		 */
		public double mean() { return mSampleTimeTotal > 0.0 ? mSampleDataTotal / mSampleTimeTotal : 0.0; 	}
		
		public String toString() {
			return super.toString() + " {" + mSampleTimeTotal + ", " + mSampleDataTotal + "}";
		}
	}
	//--------------------------------------------------------------------------------------------
	
	//--------------------------------------------------------------------------------------------
	public static class TransferRateInterval extends HarmonicMean {
		long   mStartTime = -1;
		
		public long getDataTotal() {
			return mSampleDataTotal;
		}
	}
	//--------------------------------------------------------------------------------------------
	
	
	//--------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------

	private static class SampleSetManager<T extends SampleSet> extends HashMap<String, T> {

		/**
		 * Format a string containing simple measures of samples taken for all timers.
		 * @return Simple measures (e.g., mean, min, and max) for all timers. 
		 */
		public String statString() {
			StringBuilder sb = new StringBuilder();
			List<String> timerList = new ArrayList<String>(keySet());
			java.util.Collections.sort(timerList);		
			for ( String k : timerList ) {
				sb.append(k).append(": ").append(get(k).toString()).append("\n");
			}
			return sb.toString();
		}

		/**
		 * A compact output string giving just the mean sample size for each timer.
		 * @return
		 */
		public String meanString() {
			StringBuilder sb = new StringBuilder();
			for ( String k : keySet() ) {
				sb.append(k).append(": ").append(String.format("%6.2f", get(k).mean())).append("\n");
			}
			return sb.toString();
		}

	}

	//--------------------------------------------------------------------------------------------

	
	//--------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------

	/**
	 * Class to simplify making elapsed time measurements.  Basically, call ElapsedTime.start("foo") at the beginning
	 * of a block of code and then ElapsedTime.stop("foo") at the end of the block to create an elapsed time sample
	 * associated with key "foo".  (If some unusual condition occurs after calling start(), you can/must call 
	 * ElapsedTime.abort("foo") to cancel the current measurement.)
	 * @author zahorjan
	 *
	 */
	public static class ElapsedTime {
		private static double MSEC_SCALE = 1.0/1000000.0;
		
		private static SampleSetManager<ElapsedTimeInterval> mSampleSetManager = new SampleSetManager<ElapsedTimeInterval>(); 
		
		/**
		 * Indicate the start of an elapsed time interval.  The interval should be terminated by calling stop()
		 * or abort().
		 * @param key The arbitary name of a timer (e.g., ElapsedTime.start("foo")).
		 */
		public static void start(String key) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			ElapsedTimeInterval entry = mSampleSetManager.get(key);
			if ( entry == null ) {
				entry = new ElapsedTimeInterval();
				mSampleSetManager.put(key,  entry);
			}
			
			if ( entry.mStartTime > 0 ) {
				throw new RuntimeException("start(" + key + ") called but am already had a start call with no matching stop");
			}
			entry.mStartTime = System.nanoTime();
		}

		/**
		 * Indicate that an elapsed time sample should be taken.  The sample is the time that has passed since the last start() call with the name timer name
		 * (the argument key).  It is an error to call stop if there has been no corresponding call to start.
		 * @param key The name of the timer with which to record the new elapsed time sample.
		 * @return The length of the measured interval, in msec.
		 */
		public static double stop(String key) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			ElapsedTimeInterval entry = mSampleSetManager.get(key);
			if ( entry == null || entry.mStartTime < 0 ) throw new RuntimeException("stop(" + key + ") called but there was no matching start");
			double sample = (System.nanoTime() - entry.mStartTime) * MSEC_SCALE;
			entry.mStartTime = -1;
			entry.addSample(sample);
			return sample;
		}
		
		/**
		 * Indicates that the elapsed time since start() was called is no longer of interest, and so no new sample should be generated by ending the
		 * measurement interval.  This might happen, for instance, if some exceptional condition occurs.
		 * Note that it is not an error to abort a timer that hasn't been started.  (This makes it easier to catch errors over a long code sequence,
		 * as you don't have to keep track of just which timers have and which haven't yet been started.)
		 * @param key The name of a timer.
		 * @return The length of the measured interval, in msec.
		 */
		public static double abort(String key) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			ElapsedTimeInterval entry = mSampleSetManager.get(key);
			if ( entry == null || entry.mStartTime < 0 ) return 0.0;
			double sample = (System.nanoTime() - entry.mStartTime) * MSEC_SCALE;
			entry.mStartTime = -1;

			entry.abort();
			return sample;
		}
		
		public static ElapsedTimeInterval get(String key) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			return mSampleSetManager.get(key);
		}

		/**
		 * Reset all sample sets.
		 */
		public static void clear() { mSampleSetManager.clear(); }
		
		/**
		 * Returns String with summary information on all keys
		 */
		public static String statString() { return mSampleSetManager.statString(); }

		/**
		 * Returns String with summary information on all keys.  In comparison
		 * with statString(), the returned string is shorter: it contains just mean
		 * information.
		 */
		public static String meanString() { return mSampleSetManager.statString(); }

	}

	//--------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------

	/**
	 * Class to simplify making transfer rate measurements.  Basically, call ElapsedTime.start("foo") at the beginning
	 * of a block of code and then ElapsedTime.stop("foo") at the end of the block to create an elapsed time sample
	 * associated with key "foo".  (If some unusual condition occurs after calling start(), you can/must call 
	 * ElapsedTime.abort("foo") to cancel the current measurement.)
	 * @author zahorjan
	 *
	 */
	public static class TransferRate {
		private static double MSEC_SCALE = 1.0/1000000.0;

		private static SampleSetManager<TransferRateInterval> mSampleSetManager = new SampleSetManager<TransferRateInterval>(); 

		/**
		 * Indicate the start of an elapsed time interval.  The interval should be terminated by calling stop()
		 * or abort().
		 * @param key The arbitary name of a timer (e.g., ElapsedTime.start("foo")).
		 */
		public static void start(String key) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			TransferRateInterval entry = mSampleSetManager.get(key);
			if ( entry == null ) {
				entry = new TransferRateInterval();
				mSampleSetManager.put(key,  entry);
			}

			if ( entry.mStartTime > 0 ) {
				throw new RuntimeException("start(" + key + ") called but am already had a start call with no matching stop");
			}
			entry.mStartTime = System.nanoTime();
		}

		/**
		 * Indicate that an elapsed time sample should be taken.  The sample is the time that has passed since the last start() call with the name timer name
		 * (the argument key).  It is an error to call stop if there has been no corresponding call to start.
		 * @param key The name of the timer with which to record the new elapsed time sample.
		 * @param dataAmount The amount of data transfered during this interval.
		 * @return The length of the measured interval, in msec.
		 */
		public static double stop(String key, long dataAmount) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			TransferRateInterval entry = mSampleSetManager.get(key);
			if ( entry == null || entry.mStartTime < 0 ) throw new RuntimeException("stop(" + key + ") called but there was no matching start");
			double timeSample = (System.nanoTime() - entry.mStartTime) * MSEC_SCALE;
			entry.mStartTime = -1;
			entry.addSample(timeSample, dataAmount);
			return timeSample > 0.0 ? dataAmount / timeSample : Double.MAX_VALUE;
		}

		/**
		 * Indicates that the elapsed time since start() was called is no longer of interest, and so no new sample should be generated by ending the
		 * measurement interval.  This might happen, for instance, if some exceptional condition occurs.
		 * Note that it is not an error to abort a timer that hasn't been started.  (This makes it easier to catch errors over a long code sequence,
		 * as you don't have to keep track of just which timers have and which haven't yet been started.)
		 * @param key The name of a timer.
		 * @return The length of the measured interval, in msec.
		 */
		public static double abort(String key, long dataAmount) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			TransferRateInterval entry = mSampleSetManager.get(key);
			if ( entry == null || entry.mStartTime < 0 ) return 0.0;
			double sample = (System.nanoTime() - entry.mStartTime) * MSEC_SCALE;
			sample = sample > 0.0 ? sample = dataAmount / sample : Double.MAX_VALUE;
			entry.mStartTime = -1;

			entry.abort();
			return sample;
		}

		public static TransferRateInterval get(String key) {
			if ( key == null ) throw new RuntimeException("ElapsedTime key can't be null");
			return mSampleSetManager.get(key);
		}

		/**
		 * Reset all sample sets.
		 */
		public static void clear() { mSampleSetManager.clear(); }

		/**
		 * Return summary information on all keys
		 */
		public static String statString() { return mSampleSetManager.statString(); }

		/**
		 * Returns String with summary information on all keys.  In comparison
		 * with statString(), the returned string is shorter: it contains just mean
		 * information.
		 */
		public static String meanString() { return mSampleSetManager.statString(); }

	}
}
