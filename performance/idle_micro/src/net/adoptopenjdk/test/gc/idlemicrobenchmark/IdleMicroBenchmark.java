/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package net.adoptopenjdk.test.gc.idlemicrobenchmark;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.management.MalformedObjectNameException;


/**
 * Tests implement different kinds of load with different CPU/Memory Usages to 
 * observe the gc usage with and without Idle-detection enabled in the JVM.
 * 
 * Parameter to be passed to the command line as are follows :
 * 1} --time = (Default set to 300 seconds) The time for which the given workload
 *    must run.
 *
 * 2} --memoryLimit = (Default set to 512 MB). The heap memory limit passed
 *    to the test.
 *
 * 3} --workload = (Default set to 'image'). The kind of workload which
 * 	  needs to run. Option values can be transactional/image/spike.
 *
 * 4} --threadcount = (Default set to 10). The number of threads running the given
 *    workload.
 *
 * 5} --printstats = (Default set to true). Valid values are true/false. Used
 *    for printing thread related information after running the workload.
 *
 * 6} --iterations = (Default set to 3). Number of idle/active cycles to be run.
 *
 * 7} --warmup = (Default set to 120 seconds). Amount of time for which the
 *    warm up cycle should last before running the actual workload.
 *
 * 8} --idletime = (Default set to 600 sec). The duration for long idle
 *    between active cycles.
 *
 */

public class IdleMicroBenchmark {

	private static int busyInterval = 5000;
	private static int nIterations = 1;
	private static boolean doWarmUp = false;

	public static void main(String args[]) throws MalformedObjectNameException, IOException
	{
		int nThreads = 10;
		long runFor = 60L;		// This is the active cycle and is currently set to one minute
		long sleepTime = 300L;	// This is the idle cycle and is currently set to five minutes
		int memoryLimit = 512;
		String workloadType = "image";
		int workloadTypeNum = 3;
		long warmUpFor = 30L;
		int nWarmUpThreads = 2;
		ExecutorService executor = null;

		System.out.println(" ----------------------------------------------------------");
		System.out.println(" Starting the IdleMicroBenchmark test........");
		System.out.println(" ----------------------------------------------------------");
		System.out.println("");
		System.out.println("");

		for (int i = 0; i < args.length; i++) {
			// Parsing command line options 
			String option = args[i];
			String optionName = null;
			String optionValue = null;
			if (option.indexOf("=") == -1 ) {
				optionName = option;
			}
			else {
				String parts[] = option.split("=");
				optionName = parts[0];
				optionValue = parts[1];
			}

			if (optionName.equals("--help")) {
				help();
			}
			if (optionName.equals("--workload")) {
				if (optionValue.equals("transactional") || optionValue.equals("image") || optionValue.equals("spike")) {
					workloadType = optionValue;
					if (optionValue.equals("transactional")) {
						workloadTypeNum = 1;
					}
					if (optionValue.equals("image")) {
						workloadTypeNum = 3;
					}
					if (optionValue.equals("spike")) {
						nThreads = 10000;
						workloadTypeNum = 0;
					}
				} else {
					System.out.println("Correct usage is \n '--workload=transactional' for a transactional workload\n and\n '--workload=image' for a heavier memory intensive workload and ");
					System.out.println("workload=spike for a workload which mimics a sudden spike in the number of transactions in a short period of time.");
					System.exit(1);
				}
			}

			//Time is in seconds
			if (optionName.equals("--time")) {
				runFor = Long.parseLong(optionValue);
			}

			//Memory in MB
			if (optionName.equals("--memoryLimit")) {
				memoryLimit = Integer.parseInt(optionValue);
			}

			if (optionName.equals("--threadcount")) {
				nThreads = Integer.parseInt(optionValue);
			}

			if (optionName.equals("--iterations")) {
				nIterations = Integer.parseInt(optionValue);
			}

			if (optionName.equals("--idletime")) {
				sleepTime = Integer.parseInt(optionValue);
			}

			if (optionName.equals("--warmup")) {
				warmUpFor = Long.parseLong(optionValue);
				if (warmUpFor == 0) {
					doWarmUp = false;
				} else {
					doWarmUp = true;
				}
			}
		} //finish parsing command line

		try {
			IdleMicroBenchmark imbm1 = new IdleMicroBenchmark();
			long memoryLimitInBytes = memoryLimit * 1024 * 1024;
			sleepTime = sleepTime * 1000L;
			System.setProperty("isIdle", "false");

			executor = Executors.newFixedThreadPool(nThreads);
			System.out.println(" ========= TEST DETAILS =========\n\n");
			System.out.println(" TIME LIMIT = " + runFor + " SECONDS \n MEMORY LIMIT = " + memoryLimit + " MB" );
			System.out.println(" WORKLOAD TYPE = " + workloadType + "\n NUMBER OF THREADS = " + nThreads + "\n");
			System.out.println(" NUMBER OF ITERATIONS = " + nIterations + "\n IDLE TIME = " + sleepTime + " MSEC\n\n" );
			System.out.println(" =================================\n\n");

			if (doWarmUp == true) {
				System.out.println("\n\nMain : ======================== Application Warming up =========================\n\n");
				imbm1.induceCombinedLoad(nWarmUpThreads, busyInterval, workloadTypeNum, warmUpFor, memoryLimitInBytes, executor);
				System.out.println("\n\nMain : ======================== Application Warm Up Done ========================\n\n");
			}


			for (int i = 0; i < nIterations ; i++) {
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				System.out.println(dateFormat.format(new Date()) + " Main: =============== Starting: Application inducing load ===============");
				imbm1.induceCombinedLoad(nThreads, busyInterval, workloadTypeNum, runFor, memoryLimitInBytes, executor);
				System.out.println(dateFormat.format(new Date()) + " Main: =============== Complete: Application inducing load ===============");
				System.out.println(dateFormat.format(new Date()) + " Main: =============== Starting: Application Long Idle ===============");
				System.setProperty("isIdle", "true");	
				Thread.sleep(sleepTime);
				System.setProperty("isIdle", "false");
				System.out.println(dateFormat.format(new Date()) + " Main: =============== Complete: Application Long Idle ===============");
				// Letting the app sleep for one more minute to get the top usage for idle time properly
				Thread.sleep(1000);
			}

		} catch (Exception e) {
			System.out.println("Exception occurred .");
			e.printStackTrace();
			System.exit(1);
		} finally {
		}

		executor.shutdown();
		// Wait until all threads are terminated
		while (!executor.isTerminated()) {
		}

	} //main function ends here

	//*****************************************Implementing higher level function here *****************************************
		
	/**
	 *induceCombinedLoad() - Induces a workload consisting of both CPU as well as Memory Intensive operations.
	 *Implements the executor submit interface.
	 */
	void induceCombinedLoad(int threads, int interval, int workloadTypeNum, long runFor, long  memoryLimit, ExecutorService executor) {

		List<Future<Long>> combinedWorkerList = new ArrayList<Future<Long>>();
		long stopRunning = System.currentTimeMillis() + 1000L * runFor;
		java.lang.management.MemoryMXBean membean = ManagementFactory.getMemoryMXBean();
		long heapInBytes = 0;
		int iter = 1;
		long currTime = 0;
		long startTime = 0;
		long timeDiff = 0;
		CombinedLoad combinedList[] = new CombinedLoad[threads];

		for (int i = 0; i < threads; i++) {
			combinedList[i] = new CombinedLoad(workloadTypeNum, interval, memoryLimit);
		}

		do {
			if ( System.getProperty("isIdle").equals("true") ) {
                                System.out.println("App is going IDLE.. Stop creating tasks.. zzzzzz");
                                break;
                        }

			if (heapInBytes < memoryLimit) {
				System.out.println("CombinedLoad Load: Iteration: " + iter + ": Starting: ===== Application Active =====");
				startTime = System.currentTimeMillis();
				for (int i = 0; i < threads; i++) {
					Callable<Long> combinedWorker = combinedList[i];
					Future<Long> res = executor.submit(combinedWorker);
					combinedWorkerList.add(res);
				}

				for (Future<Long> future : combinedWorkerList) {
					try {
						System.out.println("Test Application:  Thread: <" + future.get() + "> finished");
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
				combinedWorkerList.clear();

				heapInBytes = membean.getHeapMemoryUsage().getUsed();
				currTime = System.currentTimeMillis();
				timeDiff = currTime - startTime;

				System.out.println("CombinedLoad Load: Iteration: " + iter + ": Complete. Time: " + timeDiff + " Heap: " + heapInBytes);

				try {
					//Thread.sleep(10000);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			iter++;

		} while ( System.currentTimeMillis() < stopRunning && heapInBytes < memoryLimit );
		
		for (int i = 0; i < threads ; i++) {
			System.out.println(" Index : "+ i +"ThreadId :" + combinedList[i].getThreadId()  + " Transactions : " + combinedList[i].getTrans() + "\n");
			combinedList[i].resetTrans();
		}

	}

	private static void help()
	{
		System.out.println(" Parameters to be passed to the command line as are follows :\n"
                                    + "1} --time = (Default set to 300 seconds) The time for which the given workload must run.\n"
                                    + "2} --memoryLimit = (Default set to 512 MB). The heap memory limit passed to the test.\n"
                                    + "3} --workload = (Default set to 'image'). The kind of workload which needs to run. Option values can be transactional/image/spike.\n"
                                    + "4} --threadcount = (Default set to 10). The number of threads running the given workload.\n"
                                    + "5} --printstats = (Default set to true). Valid values are true/false. Used for printing thread related information after running the workload.\n"
                                    + "6} --iterations = (Default set to 3). Number of idle/active cycles to be run.\n"
                                    + "7} --warmup = (Default set to 120 seconds). Amount of time for which the warm up cycle should last before running the actual workload.\n"
                                    + "8} --idletime = (Default set to 300 sec). The duration for long idle between active cycles.\n");
		System.exit(1);
	}
}
