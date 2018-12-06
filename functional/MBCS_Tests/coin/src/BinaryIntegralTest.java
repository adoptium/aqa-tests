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

public class BinaryIntegralTest{
    public static void main(String[] args) {
	boolean flag = false;
	if (0b11111111 != 0xFF){
		System.err.println("Error: 0b11111111, 0xFF mismatch");
		flag = true;
	}
	if (0b1111_1111 != 0xFF){
		System.err.println("Error: 0b1111_1111, 0xFF mismatch");
		flag = true;
	}
	if (0 != 0b0){
		System.err.println("Error: 0, 0b0 mismatch");
		flag = true;
	}
	if (0b0001_0010_0011_0100 != 0x1234){
		System.err.println("Error: 0b0001_0010_0011_0100, 0x1234 mismatch");
		flag = true;
	}
	if (0x1.ffff_ffff_ffff_fP1_023 != Double.MAX_VALUE){
		System.err.println("Error: 0x1.ffff_ffff_ffff_fP1_023, Double.MAX_VALUE mismatch");
		flag = true;
	}
	if (0x1.0P-1022 != Double.MIN_NORMAL){
		System.err.println("Error: 0x1.0P-1022, Double.MIN_NORMAL mismatch");
		flag = true;
	}
	if (0x0.0000_0000_0000_1P-1022 != Double.MIN_VALUE){
		System.err.println("Error: 0x0.0000_0000_0000_1P-1022, Double.MIN_VALUE mismatch");
		flag = true;
	}
	if (0b0111_1111_1111_1111_1111_1111_1111_1111 != Integer.MAX_VALUE) {
		System.err.println("Error: 0b0111_1111_1111_1111_1111_1111_1111_1111, Integer.MAX_VALUE mismatch");
		flag = true;
	}
	if (-0b1000_0000_0000_0000_0000_0000_0000_0000 != Integer.MIN_VALUE) {
		System.err.println("Error: -0b1000_0000_0000_0000_0000_0000_0000_0000, Integer.MIN_VALUE mismatch");
		flag = true;
	}
	if (0b0111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111L != Long.MAX_VALUE) {
		System.err.println("Error: 0b0111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111_1111L, Long.MAX_VALUE mismatch");
		flag = true;
	}
	if (-0b1000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L != Long.MIN_VALUE) {
		System.err.println("Error: -0b10000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L, Long.MIN_VALUE mismatch");
		flag = true;
	}

	if (!flag){
		System.out.println("BinaryIntegral test passed");
	}else{
		System.out.println("BinaryIntegral test failed");
	}
    }
}
