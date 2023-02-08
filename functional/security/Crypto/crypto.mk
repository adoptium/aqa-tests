##############################################################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##############################################################################
ifneq ($(JDK_VERSION),8)
	JAVA_MOD_ARGS=--add-reads java.base=ALL-UNNAMED --add-exports java.base/com.sun.crypto.provider=ALL-UNNAMED --add-exports java.base/sun.security.internal.spec=ALL-UNNAMED --add-exports  java.base/sun.security.ssl=ALL-UNNAMED  --add-exports  java.base/sun.security.x509=ALL-UNNAMED --add-reads java.security.jgss=ALL-UNNAMED --add-exports java.security.jgss/sun.security.jgss=ALL-UNNAMED --add-exports java.security.jgss/sun.security.jgss.krb5=ALL-UNNAMED --add-exports java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-reads java.xml.crypto=ALL-UNNAMED --add-exports java.xml.crypto/org.jcp.xml.dsig.internal.dom=ALL-UNNAMED --add-modules=jdk.crypto.ec --add-reads jdk.crypto.ec=ALL-UNNAMED --add-exports jdk.crypto.ec/sun.security.ec=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED 
else
	JAVA_MOD_ARGS=
endif