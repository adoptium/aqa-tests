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

public class LargeObj {
	private float alpha;
	private float beta;
	private float gamma;
	private float delta;
	private float epsilon;
	private float zeta;
	private float eta;
	private float theta;
	private float iota;
	private float kappa;

	public LargeObj (float val) {
		alpha = beta = gamma = delta = epsilon = zeta = eta = theta = iota = kappa = val;
	}

}
