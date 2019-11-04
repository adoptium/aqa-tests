/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package net.adoptopenjdk.casa.workload_caching;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Bucket sorts transaction time count into all buckets whose place value is 
 * greater than the time. 
 * 
 *  
 *
 */
public class TransactionTimeBuckets 
{
	/*
	 * The value of the next bucket: 1 * log base LOG_BASE of the 
	 * bucket id. In other words, each bucket represents a time period of the 
	 * previous bucket divided by the LOG_BASE
	 */
	private static final double LOG_BASE = 1.1;
	
	/*
	 *  An offset for the log value of the first bucket. The buckets store counts of 
	 *  transactions that take less than 
	 *    1/Math.pow(THRESHOLD_LOG_BASE, (i + FIRST_BUCKET_OFFSET))
	 *  where i is the bucket number.  
	 *  seconds 
	 */
	private final double firstBucketOffest; 
	
	/*
	 * The number of buckets to sort transaction times into. 
	 */
	private final int numBuckets; 
	
	// Stores counts of transactions of various lengths. Bucket 0 includes all transactions longer than  second. 
	private final AtomicLong[] logTransactionCounts;
		
	// The number of transactions greater than the largest bucket in logTransactionCounts
	private final AtomicLong longTransactions; 
	
	// Total number of transactions. 
	private final AtomicLong numTransactions; 

	/**
	 * Constructs new buckets and initializes all to 0	 
	 * 
	 * @param minValue - The value of the minimum bucket. Along with the log base, 
	 * this determines how many buckets will be used to store the counts to 
	 * achieve this minimum place value. 
	 * @param maxValue - The value of the maximum bucket. 
	 */
	public TransactionTimeBuckets(double minValue, double maxValue)
	{				
		firstBucketOffest = Math.log(1/maxValue) / Math.log(LOG_BASE);		
		numBuckets = (int)Math.ceil(Math.log(1/minValue) / Math.log(LOG_BASE)); 
		
		numTransactions = new AtomicLong(0); 
		longTransactions = new AtomicLong(0);
		
		logTransactionCounts = new AtomicLong[numBuckets];
		
		for (int i = 0; i < logTransactionCounts.length; i++)
			logTransactionCounts[i] = new AtomicLong(0);
	}
	
	/**
	 * Adds the transaction count to all buckets with a place 
	 * value greater than the given transaction time.  
	 * 
	 * @param averageTime
	 * @param transactionsCompleted
	 */
	public void countTransactions(double averageTime, int transactionsCompleted)
	{
		numTransactions.addAndGet(transactionsCompleted); 
		
		// Transactions longer than one second are counted separately.
		if (averageTime > getBucketPlaceValue(0))
		{
			longTransactions.addAndGet(transactionsCompleted);
		}
		else 
		{
			/*
			 * Add the number of transactions in this sampling run to all the buckets
			 * whose place value is greater than the average transaction time for the 
			 * sampling run.   
			 */
			for (long i = 0; i < numBuckets; i++)
			{													
				if (averageTime <= getBucketPlaceValue(i))
					logTransactionCounts[(int)i].addAndGet(transactionsCompleted);
			}
		}
	}
	
	/**
	 * Gets the number of transactions longer than the largest bucket. 
	 * 
	 * @return
	 */
	public long getNumLongTransactions()
	{
		return longTransactions.get(); 
	}
	
	/**
	 * Gets the proportion of all transactions that are longer 
	 * than the place value of the largest bucket.  
	 * 
	 * @param decimalPlaces
	 * @return
	 */
	public double getProportionLongTransactions()
	{
		return longTransactions.get() / (double)numTransactions.get();
	}
	
	/**
	 * Gets the time within which the supplied proportion of 
	 * transactions complete within.  
	 * 
	 * @param threshold
	 * @return
	 */
	public double getThresholdTime(double threshold)
	{		
		int bucketIndex = 0;		
		double timeWindow; 
		
		// Add thresholds 
		do 				
		{			
			/*
			 * The time window represented by bucket i
			 */
			timeWindow = getBucketPlaceValue(bucketIndex);
			
			/* 
			 * Convert counts to proportions of cycles which completed within the 
			 * time window to match them to the thresholds   
			 */
			double cyclesWithin = logTransactionCounts[bucketIndex].get() / (double) numTransactions.get();
			double cyclesWithinNext = logTransactionCounts[bucketIndex+1].get() / (double) numTransactions.get();
			
			//Main.err.println(threshold + " " + cyclesWithin + " " + cyclesWithinNext + " " + timeWindow);
			
			/*
			 *  Detects a falling edge between two buckets where the count 
			 *  in the next bucket is lower than the threshold. 
			 */
			if (cyclesWithin != cyclesWithinNext && cyclesWithinNext < threshold)  
			{
				/*
				 * We're searching for the bucket right before the count drops below our 
				 * threshold. If we don't find it, try the next bucket. If we do, try the next
				 * threshold. One bucket may uniquely satisfy multiple consecutive thresholds
				 * so we don't advance to the next bucket until this condition fails to be met.      
				 */
				return timeWindow;													
			}
			else 
			{
				/*
				 * If we haven't hit the threshold yet, check the next bucket.  
				 */
				bucketIndex++; 
			}
		}
		while (bucketIndex < (logTransactionCounts.length - 1));

		// If no time window fits well enough, just return the last one. 
		return getBucketPlaceValue(bucketIndex);
	}
	
	/**
	 * Gets the place value of bucket i in the logTransactionCounts 
	 * array. This means that whatever count is in bucket i completed
	 * within the returned number of seconds. 
	 * 
	 * @param i
	 * @return
	 */
	private double getBucketPlaceValue(double i)
	{
		return 1/Math.pow(LOG_BASE, i + firstBucketOffest);
	}
	
	/**
	 * Gets the time interval represented by the 0th bucket.  
	 * 
	 * @return
	 */
	public double getLargestPlaceValue()
	{
		return getBucketPlaceValue(0); 
	}
}
