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


public class IDNtoUNICODE {

	public static void main(String[] args) {
		
		if(args.length !=1){
			System.out.println("Usage : java IDNtoUNICODE <strings>");
			System.exit(0);
		}
				
		String unicode = IDN.toUnicode(args[0]);
		System.out.println(unicode);
	}

}
