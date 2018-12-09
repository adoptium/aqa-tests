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

import java.net.IDN;

public class IDNtoASCII {
	static void printUsage() {
		System.out.println("Usage : java IDNtoASCII [<string>|-e EnvValue]");
		System.exit(0);
	}
	public static void main(String[] args) {
		//String hostname = IDNData.hostname;
		if(args.length < 1) printUsage();
		String ace;
		if (args[0].equals("-e")) {
			if (args.length < 2) printUsage();
			ace = IDN.toASCII(System.getenv(args[1]));
		} else {
			ace = IDN.toASCII(args[0]);
		}
		System.out.println(ace);
	}
}
