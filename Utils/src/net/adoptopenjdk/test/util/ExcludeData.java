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

import java.util.ArrayList;

public class ExcludeData {
	private String methodsToExclude;
	private String className;
	private String defectNumber;
	private ArrayList<String> excludeGroupNames;
	
	public ExcludeData(String methodsToExclude, String className, String defectNumber, ArrayList<String> excludeGroupNames) {
		this.methodsToExclude = methodsToExclude;
		this.className = className;
		this.defectNumber = defectNumber;
		this.excludeGroupNames = new ArrayList<String> (excludeGroupNames);
	}
	
	public String getMethodsToExclude() { return methodsToExclude;}
	public String getClassName() { return className;}
	public String getDefectNumber() { return defectNumber;}
	public ArrayList<String> getExcludeGroupNames() { return excludeGroupNames;}
	
	public void setMethodsToExclude(String methodsToExclude) { this.methodsToExclude = methodsToExclude; }
	public void setClassName(String className) { this.className = className; }
	public void setDefectNumber(String defectNumber) { this.defectNumber = defectNumber; }
	public void setMethodsToExclude(ArrayList<String> excludeGroupNames) {this.excludeGroupNames = excludeGroupNames; }
	
}