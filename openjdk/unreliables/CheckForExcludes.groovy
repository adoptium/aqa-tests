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

/**
 * This program takes a comma-seperated list of JDK versions (no spaces)
 * And outputs any instances of the same test ocurring in both:
 * - an "unreliables" list of intermittently-failing tests
 * - and a ProblemList of excluded tests on the same jdk version
 * This is helpful to check if any tests identified as intermittent
 * failures have been excluded, and can be safely omitted from the 
 * intermittent failures file.
 */

def jdkListString = "8,11,17,21"
if (this.args.length > 0){
	jdkListString = this.args[0]
}
def jdkList = jdkListString.split(",")

jdkList.each { oneJDK ->
	findDups(oneJDK)
}


def findDups(String jdkVersion) {
	def excludesList = grabTestList("../excludes/ProblemList_openjdk${jdkVersion}.txt")
	excludesList.addAll(grabTestList("../excludes/alpine/ProblemList_openjdk${jdkVersion}_alpine.txt"))
	def unreliablesList = grabTestList("ProblemList_openjdk${jdkVersion}.txt")
	
	unreliablesList.each { u ->
        excludesList.each { e ->
            if (u.tokenize(" ")[0] != null && !u.tokenize(" ")[0].startsWith("#") && u.tokenize(" ")[0].equals(e.tokenize(" ")[0])) {
			    println("-------------- JDK${jdkVersion} match: --------------")
				println("Exclude line: \"${e}\"")
				println("Unrlble line: \"${u}\"")
				println("-------------- End of match --------------")
			}
        }
    }
}

def grabTestList(String fileName) {
	ArrayList manyTests = new ArrayList<String>()
	new File(fileName).withReader('UTF-8') { reader ->
        String line = ""
        while ((line = reader.readLine()) != null) { 
		    manyTests.add(line)
        }
	}
	return manyTests
}

