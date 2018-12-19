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
import java.io.*;
import java.util.*;

public class showlocale {
	public static void main(java.lang.String[] args){
		Locale l = Locale.getDefault();
		System.out.println("Name : " + l.toString());
		System.out.println("Country : " + l.getCountry());
		System.out.println("Language : " + l.getLanguage());
		System.out.println("Variant : " + l.getVariant());
		System.out.println("DisplayName : " + l.getDisplayName());
		System.out.println("DisplayCountry : " + l.getDisplayCountry());
		System.out.println("DisplayLanguage : " + l.getDisplayLanguage());
		System.out.println("DisplayVariant : " + l.getDisplayVariant());
		System.out.println("ISO3Country : " + l.getISO3Country());
		System.out.println("ISO3Language : " + l.getISO3Language());
		System.out.println("Encoding : " + new OutputStreamWriter(System.out).getEncoding());
	}
}
