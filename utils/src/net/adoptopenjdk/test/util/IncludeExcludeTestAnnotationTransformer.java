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

package net.adoptopenjdk.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import org.testng.log4testng.Logger;

public class IncludeExcludeTestAnnotationTransformer implements IAnnotationTransformer {
	static private ArrayList<ExcludeData> excludeDatas = new ArrayList<ExcludeData>();
	private static final Logger logger = Logger.getLogger(IncludeExcludeTestAnnotationTransformer.class);
	static {
		String line = null;
		String excludeFile = System.getenv("EXCLUDE_FILE");
		if (null == excludeFile) {
			excludeFile = IncludeExcludeTestAnnotationTransformer.class.getClassLoader().getResource("excludes/default_exclude.txt").getFile();
		}
		logger.info("exclude file is " + excludeFile);
		try {
			FileReader fileReader = new FileReader(excludeFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while ((line = bufferedReader.readLine()) != null) {
				if (line.startsWith("#") || line.matches("\\s") || line.isEmpty()) {
					// comment to ignore - as the problems list from OpenJDK
				} else {
					// parse the line and populate the array lists
					String[] lineParts = line.split("\\s+");
					// expect to exclude all methods with the name that follows : at the start of a line
					if (-1 != line.indexOf("*")) {
						// TODO exclude all Test classes under the package?
					} else {
						String[] tests = lineParts[0].split(":");
						String fileName = tests[0];
						String methodsToExclude = "";
						if (tests.length > 1) {
							methodsToExclude = tests[1];
						} else { //exclude class level
							methodsToExclude = "ALL";
						}
						String defectNumber = lineParts[1];
						String[] excludeGroups = lineParts[2].split(";");
						for (int i = 0; i < excludeGroups.length; i++) {
							excludeGroups[i] = "disabled." + excludeGroups[i];
						}
						ArrayList<String> excludeGroupNames = new ArrayList<String> (Arrays.asList(excludeGroups));
						excludeDatas.add(new ExcludeData(methodsToExclude, fileName, defectNumber, excludeGroupNames));
					}
				}
			}
			bufferedReader.close();
		} catch(FileNotFoundException ex) {
			logger.info("Unable to open file " + excludeFile);
		} catch(IOException ex) {
			logger.info("Error reading file " + excludeFile);
		}
	}

	@Override
	public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
		// if testClass is not null, check if exclude file indicates disabling entire file, then change annotation of the test class
		if (null != testClass) {
			for (ExcludeData excludeData : excludeDatas) {
				if (excludeData.getMethodsToExclude().equals("ALL") &&
					testClass.getName().equals(excludeData.getClassName())) {
					logger.debug("Disabled test class name is " + testClass.getName());
					String[] intialGroup = annotation.getGroups();
					ArrayList<String> groups = new ArrayList<String> (Arrays.asList(annotation.getGroups()));
					groups.addAll(excludeData.getExcludeGroupNames());
					annotation.setGroups(groups.toArray(new String[0]));
				}
			}
		}

		/* if testMethod is not null, check:
		 * 1. if exclude file indicates disabling entire file ( as there in no rules if testClass or testMethod is not Null,
		 *                                                     have to check again)
		 * 2. if exclude file indicate disable specific method
		 */
		if (null != testMethod) {
			String testMethodClass = testMethod.getDeclaringClass().getName();
			for (ExcludeData excludeData : excludeDatas) {
				if (excludeData.getMethodsToExclude().equals("ALL") &&
						testMethodClass.equals(excludeData.getClassName())) {
						String[] intialGroup = annotation.getGroups();
						ArrayList<String> groups = new ArrayList<String> (Arrays.asList(annotation.getGroups()));
						groups.addAll(excludeData.getExcludeGroupNames());
						annotation.setGroups(groups.toArray(new String[0]));
						logger.debug("Disabled test method is " + testMethod.getName());
				} else if (testMethodClass.equals(excludeData.getClassName()) &&
					testMethod.getName().equals(excludeData.getMethodsToExclude())) {
					String[] intialGroup = annotation.getGroups();
					ArrayList<String> groups = new ArrayList<String> (Arrays.asList(annotation.getGroups()));
					groups.addAll(excludeData.getExcludeGroupNames());
					annotation.setGroups(groups.toArray(new String[0]));
					String[] testGroups = annotation.getGroups();
					logger.info("disable method is " + testMethod.getName() + "diabled annotation " + Arrays.toString(testGroups));
				}
			}
		}
	}
}
