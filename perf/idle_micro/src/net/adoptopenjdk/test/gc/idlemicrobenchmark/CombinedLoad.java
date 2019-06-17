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
package net.adoptopenjdk.test.gc.idlemicrobenchmark;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.Calendar;
import java.util.Date;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.imageio.ImageIO;
import java.lang.management.ManagementFactory;

import net.adoptopenjdk.test.gc.idlemicrobenchmark.LargeObj;


public class CombinedLoad implements Callable<Long>
{
	private int workloadImageNumber;
	private long threadId;
	private long busyTime = 0;
	private long memoryLimitInBytes = 0L;
	private final int numObjs = 10000;
	private long numtrans = 0;

	public CombinedLoad(int workloadTypeNum, long mSec, long memLimitInBytes) {
		workloadImageNumber = workloadTypeNum;
		busyTime = mSec * 1000000L ;
		memoryLimitInBytes = memLimitInBytes;
	}

	public long getTrans() {
		return numtrans;
	}

	public void resetTrans() {
		numtrans = 0;
	}

	public long getThreadId() {
		return threadId;
	}

	public Long call() {
		Date dateobj;
		Calendar calobj;
		long startTime = 0;
		long currTime = 0;
		float diff = 0;
		LargeObj[] memoryFill = new LargeObj[numObjs];
		DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		java.lang.management.MemoryMXBean membean = ManagementFactory.getMemoryMXBean();
		long heapInBytes = 0L;

		threadId = Thread.currentThread().getId();
		startTime = System.nanoTime();
		System.out.println("Test Application: CPU: Thread: " + threadId + " busy looping start");
		do {
			File iFile;
			BufferedImage image;

			if ( System.getProperty("isIdle").equals("true") ) {
				System.out.println("App is going IDLE.. Stop working.. zzzzzz");
				break;
			}
			
			// This loop tries to wake up JIT by running for 30 secs
			long busyTime = 30000 * 1000000L;
			while ((System.nanoTime() - startTime) < busyTime) {
				Random rand = new Random();
				double result = java.lang.Math.log(rand.nextDouble());
			}

			for (int i = 0; i < 10000 ; i++) {

				dateobj = new Date();
				calobj = Calendar.getInstance();
				currTime = System.nanoTime();
				diff = (float) ((float) (currTime - startTime) / (float) 1000000);
				for (int j = 0; j < numObjs; j++) {
					memoryFill[j] = new LargeObj(diff);
				}

				if (diff == 1.000000 || diff == 2.000000 || diff == 3.000000 || diff == 4.000000 || diff == 5.000000) {
					System.out.println("CurrTime: " + currTime + " startTime: " + startTime + " busyTime: " + busyTime + " diff: " + diff);
				}
				if (diff > 6) {
					break;
				}
			}

			try {
				if( System.getProperty("isIdle").equals("true") ) {
					System.out.println("App is going IDLE.. Stop working.. zzzzzz");
					break;
				}
				switch(workloadImageNumber) {
					case 0:
						iFile = new File("images/1KbImage.jpg");
						break;
					case 1:
						iFile = new File("images/500KbImage.jpg");
						break;
					case 2:
						iFile = new File("images/1MbImage.jpg");
						break;
					case 3:
						iFile = new File("images/1point5MbImage.jpg");
						break;
					case 4:
						iFile = new File("images/2MbImage.jpg");
						break;
					case 5:
						iFile = new File("images/5MbImage.jpg");
						break;
					case 6:
						iFile = new File("images/15point5MbImage.png");
						break;
					default :
						iFile = new File("images/1MbImage.jpg");
				}
				image = ImageIO.read(iFile);
			}catch (IOException e) {
				System.err.println("Error reading image file\n");
			}

			iFile = null;
			image = null;
			currTime = System.nanoTime();
			numtrans++;
			heapInBytes = membean.getHeapMemoryUsage().getUsed();

		} while((currTime - startTime) < busyTime && heapInBytes <= memoryLimitInBytes);

		System.out.println("Test Application: Thread: " + threadId + " busy looping done; Num-trans: " + numtrans);
		return threadId;
	}
}
