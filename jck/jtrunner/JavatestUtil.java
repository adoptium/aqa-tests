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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.io.FileNotFoundException;

public class JavatestUtil {
	private static String testJdk;
	private static String tests;
	private static String testExecutionType;
	private static String withAgent;
	private static String interactive;
	private static String extraJvmOptions = "";
	private static String concurrencyString;
        private static String timeoutFactorString;
	private static String jckVersion;
	private static String config;
	private static String configAltPath;
	private static String jckRoot;
	private static String testSuite;
	private static String jckBase;
	private static String jtiFile;
	private static String nativesLoc;
	private static String jckConfigLoc;
	private static String initialJtxFullPath = "";
	private static String defaultJtxFullPath = "";
	private static String kflFullPath = "";
	private static String customJtx = "";
	private static String krbConfFile = "";
	private static String fileUrl;
	private static String jckPolicyFileFullPath;
	private static String jtliteJarFullPath; 
	private static String javatestJarFullPath;
	private static String classesFullPath; 
	private static String testProps;
	private static String testHost1Name;
	private static String testHost1Ip;
	private static String testHost2Name;
	private static String testHost2Ip;
	private static String httpUrl;
	private static String krb5ClientPassword;
	private static String krb5ClientUsername;
	private static String krb5ServerPassword;
	private static String krb5ServerUsername;
	private static String workDir;
	private static String reportDir;
	private static String newJtbFileRef; 
	private static int    jckVersionInt;
	private static String jckVersionLetters;
	private static String testSuiteFolder;
	private static String fileContent;
	private static String testRoot;
	private static String pathToJava;
	private static String secPropsFile;
	private static String testFlag;
	private static String task;
	private static String isCustomTarget = "notCustomTarget";
	private static String agentHost;
	private static String testJavaForMultiJVMCompTest;
	private static String riJavaForMultiJVMCompTest;
	private static String spec;
	private static String osShortName;
	private static HashMap<String, String> testArgs = new HashMap<String, String>();
	private static String jvmOpts = ""; 
	private static int freePort;
	private static String suppressOutOfMemoryDumpOptions = ""; // Contains JVM options to suppress dumps being taken for OutOfMemory exceptions
	private static final String TEST_ROOT = "testRoot";
	private static final String JCK_ROOT = "jckRoot";
	private static final String TESTS = "tests";
	private static final String JCK_VERSION = "jckversion";
	private static final String TEST_SUITE = "testsuite";	
	private static final String TEST_EXECUTION_TYPE = "testExecutionType";
	private static final String WITH_AGENT = "withAgent";
	private static final String INTERACTIVE = "interactive";
	private static final String CONFIG = "config";
	private static final String CONCURRENCY = "concurrency";
        private static final String TIMEOUT_FACTOR = "timeoutFactor";
	private static final String CONFIG_ALT_PATH = "configAltPath";
	private static final String TASK = "task";
	private static final String TASK_CMD_FILE_GENERATION = "cmdfilegen";
	private static final String TASK_GENERATE_SUMMARY_REPORT = "summarygen";
	private static final String AGENT_HOST = "agentHost";
	private static final String TEST_JAVA_FOR_MULTIJVM_COMP_TEST = "testJava";
	private static final String RI_JAVA_FOR_MULTIJVM_COMP_TEST = "riJava";
	private static final String WORK_DIR = "workdir";
	private static final String SPEC = "spec";
	private static final String CUSTOM_JTX = "customJtx";
	private static final String IS_CUSTOM_TARGET = "isCustomTarget";

	public static void main(String args[]) throws Exception {
		ArrayList<String> essentialParameters = new ArrayList<String>(); 
		essentialParameters.add(TEST_ROOT);
		essentialParameters.add(JCK_ROOT);
		essentialParameters.add(TESTS);
		essentialParameters.add(JCK_VERSION);
		essentialParameters.add(TEST_SUITE);	
		essentialParameters.add(TEST_EXECUTION_TYPE);
		essentialParameters.add(WITH_AGENT);
		essentialParameters.add(INTERACTIVE);
		essentialParameters.add(CONFIG);
		essentialParameters.add(CONCURRENCY);
                essentialParameters.add(TIMEOUT_FACTOR);
		essentialParameters.add(CONFIG_ALT_PATH);
		essentialParameters.add(TASK);
		essentialParameters.add(AGENT_HOST);
		essentialParameters.add(TEST_JAVA_FOR_MULTIJVM_COMP_TEST);
		essentialParameters.add(RI_JAVA_FOR_MULTIJVM_COMP_TEST);
		essentialParameters.add(WORK_DIR);
		essentialParameters.add(SPEC);
		essentialParameters.add(CUSTOM_JTX);
		essentialParameters.add(IS_CUSTOM_TARGET);

		for (String arg : args) {
			if (arg.contains("=")) {
				String [] aPair = arg.split("=");
				String key = aPair[0];
				String value = aPair[1];
				
				// We only load testArgs with key,value pairs that are needed by the JavatestUtil 
				if (essentialParameters.contains(key)) {
					// This is a special case to supply multiple sub-folders separating by semicolon( another option is directively supply with double quoted multiple sub-folders separating by space.
					if(value.contains(";")) {
						value = value.trim().replace("\n", "").replace("\r", "");
						String [] tests = value.split(";");
						String finalTarget = "";
						for (int i = 0; i < tests.length; i++) {
							finalTarget = finalTarget + tests[i]; 
							if (i+1 < tests.length) {
								// JCK harness expects tests to be listed with a single space
								finalTarget = finalTarget + " "; 
							}
						}
						testArgs.put(key, finalTarget); 
					} else {
						testArgs.put(key, value); 
					}
				} else {
					System.out.println("In JavatestUtil");
					System.out.println("Unrecognized input key ignored: " + key);
					System.exit(1);
				}
			} 
		}
		
		String otherOptions = System.getProperty("other.options");
		String jvmOptions = System.getProperty("jvm.options");
		if (otherOptions != null) {
			jvmOpts += otherOptions.trim() + " ";
		}
		if (jvmOptions != null) {
			jvmOpts += jvmOptions.trim() + " ";
		} 
		testFlag = System.getenv("TEST_FLAG");
		task = testArgs.get(TASK).trim();
		customJtx = testArgs.get(CUSTOM_JTX) == null ? "" : testArgs.get(CUSTOM_JTX);
		spec = testArgs.get(SPEC);
		osShortName = getOSNameShort();
		
		if (osShortName == null) {
			System.out.println("Unknown spec value supplied : " + spec);
			System.exit(1);
		}
		
		testJdk = System.getenv("JAVA_HOME");
		pathToJava = testJdk + File.separator + "bin" + File.separator + "java";
		tests = testArgs.get(TESTS).trim();
		jckVersion = testArgs.get(JCK_VERSION);
		jckRoot = new File(testArgs.get(JCK_ROOT)).getCanonicalPath();
		testSuite = testArgs.get(TEST_SUITE);
		testExecutionType = testArgs.get(TEST_EXECUTION_TYPE) == null ? "default" : testArgs.get(TEST_EXECUTION_TYPE);
		withAgent = testArgs.get(WITH_AGENT) == null ? "off" : testArgs.get(WITH_AGENT);
		interactive = testArgs.get(INTERACTIVE) == null ? "no" : testArgs.get(INTERACTIVE);
		concurrencyString = testArgs.get("concurrency") == null ? "NULL" : testArgs.get("concurrency");
                timeoutFactorString = testArgs.get(TIMEOUT_FACTOR) == null ? "NULL" : testArgs.get(TIMEOUT_FACTOR);
		config = testArgs.get(CONFIG) == null ? "NULL" : testArgs.get(CONFIG);
		configAltPath = testArgs.get(CONFIG_ALT_PATH) == null ? "NULL" : testArgs.get(CONFIG_ALT_PATH);
		agentHost = testArgs.get(AGENT_HOST) == null ? "localhost" : testArgs.get(AGENT_HOST).trim();
		testRoot = new File(testArgs.get(TEST_ROOT)).getCanonicalPath();
		testJavaForMultiJVMCompTest = testArgs.get(TEST_JAVA_FOR_MULTIJVM_COMP_TEST) == null ? pathToJava : testArgs.get(TEST_JAVA_FOR_MULTIJVM_COMP_TEST);
		riJavaForMultiJVMCompTest = testArgs.get(RI_JAVA_FOR_MULTIJVM_COMP_TEST) == null ? pathToJava : testArgs.get(RI_JAVA_FOR_MULTIJVM_COMP_TEST);  
		workDir = testArgs.get(WORK_DIR);
		jckVersionInt = getJckVersionInt(jckVersion);
		jckVersionLetters = getJckVersionLetters(jckVersion);
		testSuiteFolder = "JCK-" + testSuite.toString().toLowerCase() + "-" + jckVersionInt + jckVersionLetters;
		jckBase = jckRoot + File.separator + testSuiteFolder;
		jckPolicyFileFullPath = jckBase + File.separator + "lib" + File.separator + "jck.policy";
		javatestJarFullPath = jckBase + File.separator + "lib" + File.separator + "javatest.jar";
		jtliteJarFullPath = jckBase + File.separator + "lib" + File.separator + "jtlite.jar"; 
		classesFullPath = jckBase + File.separator + "classes";
		nativesLoc = jckRoot + File.separator + "natives" + File.separator + osShortName;
		reportDir = workDir + File.separator + "report";
		newJtbFileRef = workDir + File.separator + "generated.jtb";
		secPropsFile = workDir + File.separator + "security.properties";
		
		// Solaris natives are in /natives/sunos
		if (spec.contains("sunos")) {
			nativesLoc = jckRoot + File.separator + "natives" + File.separator + "sunos";
		}
		
		File[] files = new File(jckRoot).listFiles();
		boolean found = false;
		for (File file : files) {
			if (file.isDirectory() && (file.getName().contains("JCK-runtime")) ) {
				found = true;
				String actualJckVersion = "jck" + file.getName().split("-")[2];
				System.out.println("jckversion determined to be " + actualJckVersion);
				if (!jckVersion.equals("NULL")) {
					if (!jckVersion.equals(actualJckVersion)) {
						System.out.println("test-args jckversion " + jckVersion + " does not match actual jckversion " + actualJckVersion + ". Using actual jckversion " + actualJckVersion);
						jckVersion = actualJckVersion;
						jckVersionInt = getJckVersionInt(jckVersion);
						jckVersionLetters = getJckVersionLetters(jckVersion);
					}
				}
			}
		}
		
		if (!found) {
			System.out.println("Cannot locate the `.*JCK-runtime.*` artifacts under : " + jckRoot);
			System.exit(1);
		}
		
		try { 
			if (task.equals(TASK_GENERATE_SUMMARY_REPORT)) { 
				if (!generateSummary()) {
					System.exit(1);
				}
				System.exit(0);
			}	
			else if (task.equals(TASK_CMD_FILE_GENERATION)) { 
				if (!generateJTB()) {
					System.exit(1);
				}
				System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); 
			System.exit(1);
		} 
	}
	
	private static boolean generateJTB() throws Exception {
		jtiFile = configAltPath + File.separator + jckVersion + File.separator + testSuite.toLowerCase() + ".jti";
		System.out.println("Using jti file "+ jtiFile);
		
		if (spec.contains("win")) {
			// Jck fileURL validator validates using java.net.URI, so must use forward slashes "/" 
			fileUrl = "file:///" + jckBase.replace("\\","/") + "/testsuite.jtt";
		} else {
			fileUrl = "file:///" + jckBase + "/testsuite.jtt";
		}
		
		// The first release of a JCK will have an initial excludes (.jtx) file in test-suite/lib - e.g. JCK-runtime-8b/lib/jck8b.jtx.
		// Updates to the excludes list may subsequently be supplied as a separate file, which supersedes the initial file.
		// A known failures list (.kfl) file is optional.
		// The automation here adds any files found (initial or updates) as 'custom' files. 
		initialJtxFullPath = jckBase + "/lib/" + jckVersion + ".jtx";
		if (new File(initialJtxFullPath).exists()) {
			System.out.println("Using initial jtx file:" + initialJtxFullPath);
		} else {
			initialJtxFullPath = "";
		}
		
		// Include any default jtx file that came as part of tck repo 
		defaultJtxFullPath = jckRoot + File.separator + "excludes" + File.separator + jckVersion + ".jtx";
		if (new File(defaultJtxFullPath).exists()) {
			System.out.println("Using default jtx file:" + defaultJtxFullPath);
		} else {
			defaultJtxFullPath = "";
		}
		
		// Include any known failures list(kfl) file if it came with the tck repo
		kflFullPath = jckRoot + File.separator + "excludes" + File.separator + jckVersion + ".kfl";
		if (new File(kflFullPath).exists()) { 
			System.out.println("Using kfl file: " + kflFullPath);
		} else {
			kflFullPath = "";
		}
		
		if (customJtx != "") { 
			System.out.println("Using custom exclude file(s): " + customJtx);
		} else {
			customJtx = "";
		}
		
		if (testSuite.equals("RUNTIME") && (tests.contains("api/java_net") || tests.contains("api/java_nio") || tests.contains("api/org_ietf") || tests.contains("api/javax_security") || tests.equals("api"))) {
			if (!configAltPath.equals("NULL")) {
				jckConfigLoc = configAltPath + File.separator + "default";
			} else {
				if (config.equals("NULL")) {
					config = "default";	
				}
				String subdir = "config" + File.separator + config;
				jckConfigLoc = jckRoot + File.separator + subdir; 
			}
			
			System.out.println("Reading config files from "+ jckConfigLoc);
			File configFolder = new File(jckConfigLoc); 
			if (!configFolder.exists()) {
				System.out.println(testExecutionType + "Cannot locate the configuration directory containing the Kerberos and Http server settings here: " + jckConfigLoc + ". The requested tests include at least one of the tests which require these files.");
				return false; 
			}
			krbConfFile = jckConfigLoc+ File.separator + "krb5.conf";
			testProps = jckConfigLoc + File.separator + "jcktest.properties";
			Properties prop = new Properties();
			InputStream ins = new FileInputStream(testProps);
			prop.load(ins);
			testHost1Name = prop.getProperty("testhost1name");
			testHost1Ip = prop.getProperty("testhost1ip");
			testHost2Name = prop.getProperty("testhost2name");
			testHost2Ip = prop.getProperty("testhost2ip");
			httpUrl = prop.getProperty("httpurl");
			// Make sure username properties do not have trailing whitespace before adding server location data.
			krb5ClientPassword = prop.getProperty("krb5ClientPassword");
			krb5ClientUsername = prop.getProperty("krb5ClientUsername");
			if ( krb5ClientUsername != null ) {
				krb5ClientUsername = krb5ClientUsername.trim();
			}
			krb5ServerPassword = prop.getProperty("krb5ServerPassword");
			krb5ServerUsername = prop.getProperty("krb5ServerUsername");
			if ( krb5ServerUsername != null ) {
				krb5ServerUsername = krb5ServerUsername.trim();
			}
		}
		
		if (tests.contains("api/javax_net") ) {
			// Requires TLS 1.0/1.1 enabling
			System.out.println("Custom security properties to be stored in: " + secPropsFile);
			String secPropsContents = "jdk.tls.disabledAlgorithms=SSLv3, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224, anon, NULL, include jdk.disabled.namedCurves";
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(secPropsFile))); 
			bw.write(secPropsContents); 
			bw.flush();
			bw.close();
		}

		if ( jckVersionInt >= 8 && (tests.contains("api/java_net") || tests.contains("api/java_util")) ) {
			// Requires SHA1 enabling for jar signers in jdk-8+
			System.out.println("Custom security properties to be stored in: " + secPropsFile);
			String secPropsContents = "jdk.jar.disabledAlgorithms=MD2, MD5, RSA keySize < 1024, DSA keySize < 1024, include jdk.disabled.namedCurves\n";
			secPropsContents += "jdk.certpath.disabledAlgorithms=MD2, MD5, SHA1 jdkCA & usage TLSServer, \\" + "\n";
			secPropsContents += "RSA keySize < 1024, DSA keySize < 1024, EC keySize < 224, include jdk.disabled.namedCurves" + "\n";
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(secPropsFile)));
			bw.write(secPropsContents);
			bw.flush();
			bw.close();
		}
		
		if ( tests.contains("api/javax_xml") ) {
			// Requires SHA1 enabling
			System.out.println("Custom security properties to be stored in: " + secPropsFile);
			String secPropsContents = "jdk.xml.dsig.secureValidationPolicy=\\" + "\n";
			secPropsContents += "disallowAlg http://www.w3.org/TR/1999/REC-xslt-19991116,\\" + "\n";
			secPropsContents += "disallowAlg http://www.w3.org/TR/1999/REC-xslt-19991116,\\" + "\n";
			secPropsContents += "disallowAlg http://www.w3.org/2001/04/xmldsig-more#rsa-md5,\\" + "\n";
			secPropsContents += "disallowAlg http://www.w3.org/2001/04/xmldsig-more#hmac-md5,\\" + "\n";
			secPropsContents += "disallowAlg http://www.w3.org/2001/04/xmldsig-more#md5,\\" + "\n";
			secPropsContents += "disallowAlg http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1,\\" + "\n";
			secPropsContents += "disallowAlg http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1,\\" + "\n";
			secPropsContents += "maxTransforms 5,\\" + "\n";
			secPropsContents += "maxReferences 30,\\" + "\n";
			secPropsContents += "disallowReferenceUriSchemes file http https,\\" + "\n";
			secPropsContents += "minKeySize RSA 1024,\\" + "\n";
			secPropsContents += "minKeySize DSA 1024,\\" + "\n";
			secPropsContents += "minKeySize EC 224,\\" + "\n";
			secPropsContents += "noDuplicateIds,\\" + "\n";
			secPropsContents += "noRetrievalMethodLoops";
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(secPropsFile))); 
			bw.write(secPropsContents); 
			bw.flush();
			bw.close();
		}

		// Some tests provoke out of memory exceptions.
		// If we're testing a J9 VM that will result in dumps being taken and a non-zero return code
		// which stf will detect as a failure. So in this case add the -Xdump options required to suppress
		// taking dumps for OutOfMemory.
		if (isIbmJvm()) { 
			suppressOutOfMemoryDumpOptions = " -Xdump:system:none -Xdump:system:events=gpf+abort+traceassert+corruptcache -Xdump:snap:none -Xdump:snap:events=gpf+abort+traceassert+corruptcache -Xdump:java:none -Xdump:java:events=gpf+abort+traceassert+corruptcache -Xdump:heap:none -Xdump:heap:events=gpf+abort+traceassert+corruptcache"; 
		}

		fileContent = "testsuite \"" + jckBase + "\";\n";
		fileContent += "workDirectory -create " + workDir + File.separator +  "work" + ";\n";
		fileContent += "tests " + tests + ";\n";

		String pathToRmic = testJdk + File.separator + "bin" + File.separator + "rmic";
		String pathToLib = testJdk + File.separator + "jre" + File.separator + "lib";
		String pathToJavac = testJdk + File.separator + "bin" + File.separator + "javac";
		String pathToToolsJar = testJdk + File.separator + "lib" + File.separator + "tools.jar";
		int concurrency;
		String keyword = "";
		String libPath = "";
		String robotAvailable = "";
		String hostname = "";
		String ipAddress = "";
		
		// Use escaped backslashes for paths on Windows
		if (spec.contains("win")) {
			pathToJava = pathToJava.replace("/", "\\") + ".exe";
			pathToRmic = pathToRmic.replace("/", "\\") + ".exe";
			pathToLib = pathToLib.replace("/", "\\");
			pathToJavac = pathToJavac.replace("/", "\\") + ".exe";
			pathToToolsJar = pathToToolsJar.replace("/", "\\");
		}

		InetAddress addr = InetAddress.getLocalHost();
		ipAddress = addr.getHostAddress();
		hostname = addr.getHostName();
		freePort = getFreePort();
		
		if (freePort == -1) {
			System.out.println("Unable to get a free port");
			return false; 
		}

		// If concurrency was not specified as a test-arg it will have been assigned the value NULL.
		// Default to concurrency=cpu.
		if ( concurrencyString.equals("NULL") ) {
			concurrencyString = "cpus";
		}

		// If concurrency=cpus was specified, set concurrency to the number of processors + 1.
		if ( concurrencyString.equals("cpus") ) {
			concurrency = Runtime.getRuntime().availableProcessors() + 1;
			concurrencyString = String.valueOf(concurrency);
		}
		
		if (spec.contains("zos")) {
			extraJvmOptions += " -Dfile.encoding=US-ASCII";
		}
		
		// testExecutionType of multiJVM_group on Windows and AIX causes memory exhaustion, so limit to non-group multiJVM
		if (jckVersionInt >= 17 && (spec.contains("win") || spec.contains("aix"))) {
			fileContent += "set jck.env.testPlatform.multiJVM \"Yes\";\n";
		}

		// Set the operating system as 'Windows' for Windows and 'other' for all other operating systems.
		// If 'other' is specified when executing on Windows, then Windows specific settings such
		// as systemRoot are rejected, and the JCK harness assumes that DISPLAY is required for GUI tests.
		// Otherwise 'other' is required because the JCK harness has no inherent knowledge of AIX and zOS.
		// Runtime settings
		if (testSuite.equals("RUNTIME")) {
			if ( interactive.equals("yes")) {
				keyword = "keywords interactive";
			}
			else { 
				keyword = "keywords !interactive";
			}

			if (spec.contains("win")) {
				libPath = "PATH";
				robotAvailable = "Yes";
			} else if (spec.contains("alpine-linux") || spec.contains("riscv64")) {
				libPath = "LD_LIBRARY_PATH";
				robotAvailable = "No";
			} else if (spec.contains("linux")) {
				libPath = "LD_LIBRARY_PATH";
				robotAvailable = "Yes";
			} else if (spec.contains("aix")) {
				libPath = "LIBPATH";
				robotAvailable = "Yes";
			} else if (spec.contains("zos")) {
				pathToLib = testJdk + File.separator + "lib";
				libPath = "LIBPATH";
				robotAvailable = "No";
			} else if (spec.contains("osx")) {
				libPath = "DYLD_LIBRARY_PATH";
				robotAvailable = "Yes";
			} else if (spec.contains("sunos")) {
				libPath = "LD_LIBRARY_PATH";
				robotAvailable = "Yes";
			} else {
				System.out.println("Unknown spec: " + spec);
				return false; 
			}
			
			if ( tests.contains("api/java_awt") || tests.contains("api/javax_swing") || tests.equals("api") ) {
				keyword += "&!robot";
			}
			
			fileContent += "concurrency " + concurrencyString + ";\n";
                       
                        if (!timeoutFactorString.equals("NULL")) {
				fileContent += "timeoutfactor " + timeoutFactorString + ";\n";
                        } else {
				fileContent += "timeoutfactor 4" + ";\n";	// 4 base time limit equal 40 minutes
			}
			fileContent += keyword + ";\n";

			if (spec.contains("win")) {
				// On Windows set the testplatform.os to Windows and set systemRoot, but do not
				// set the file and path separators (an error is thrown if they are set).
				fileContent += "set jck.env.testPlatform.os \"Windows\";\n";
				fileContent += "set jck.env.testPlatform.systemRoot " + System.getenv("WINDIR") + ";\n";
			}
			else {
				// On other platforms set the testplatform.os to other and set the file and path separators.
				fileContent += "set jck.env.testPlatform.os \"other\";\n";
				fileContent += "set jck.env.testPlatform.fileSep \"/\";\n";
				fileContent += "set jck.env.testPlatform.pathSep \":\";\n";
			}

			if ( testsRequireDisplay(tests) ) {
				if (spec.contains("zos") || spec.contains("alpine-linux") || spec.contains("riscv")) {
					fileContent += "set jck.env.testPlatform.headless Yes" + ";\n";
				}
				else {
					if ( !spec.contains("win") ) {
						fileContent += "set jck.env.testPlatform.headless No" + ";\n";
						fileContent += "set jck.env.testPlatform.xWindows Yes" + ";\n";
						if ( !spec.contains("osx") ) { 
							String display = System.getenv("DISPLAY");
							if ( display == null ) {
								System.out.println("Error: DISPLAY must be set to run tests " + tests + " on " + spec);
								return false; 
							}
							else {
								fileContent += "set jck.env.testPlatform.display " + display + ";\n";
							}
						}
					}
				}
			}

			if ( !spec.contains("win") && (tests.contains("api/signaturetest") || tests.contains("api/java_io")) ) {
				fileContent += "set jck.env.testPlatform.xWindows \"No\"" + ";\n";
			}

			fileContent += "set jck.env.runtime.testExecute.cmdAsString \"" + pathToJava + "\"" + ";\n";

			if ( tests.equals("api/java_lang") || tests.contains("api/java_lang/instrument") ||
					tests.contains("api/javax_management") || tests.equals("api") || tests.startsWith("vm") ) {
				fileContent += "set jck.env.runtime.testExecute.libPathEnv " + libPath + ";\n";
				fileContent += "set jck.env.runtime.testExecute.nativeLibPathValue \"" + nativesLoc + "\"" + ";\n";
			}
			
			// tools.jar was incorporated into modules from Java 9
			if ( jckVersion.contains("jck8") ) {
				if ( tests.startsWith("vm/jvmti") || tests.equals("vm") || tests.equals("api") || tests.equals("api/java_lang") || tests.contains("api/java_lang/instrument") ) {
					fileContent += "set jck.env.runtime.testExecute.additionalClasspathRemote \"" + pathToToolsJar + "\"" + ";\n";
				}
			}
			
			if ( tests.startsWith("vm/jvmti") || tests.equals("vm") ) {
				fileContent += "set jck.env.runtime.testExecute.jvmtiLivePhase Yes;\n";
			}
		
			if ( jckVersionInt < 23 && (tests.contains("api/javax_management") || tests.equals("api")) ) {
				fileContent += "set jck.env.runtime.testExecute.jmxResourcePathValue \"" + nativesLoc + "\"" + ";\n";
			}
			
			if ( tests.contains("api/javax_sound") || tests.equals("api") ) {
				fileContent += "set jck.env.runtime.audio.canPlaySound No" + ";\n";
				fileContent += "set jck.env.runtime.audio.canPlayMidi No" + ";\n";
				fileContent += "set jck.env.runtime.audio.canRecordSound No" + ";\n";
			}
			if ( tests.contains("api/org_ietf") || tests.contains("api/javax_security") || tests.equals("api") ) {
				readKrbConfFile();
				if (KerberosConfig.kdcHostName == null || KerberosConfig.kdcRealmName == null){
					System.out.println(tests + "expects kdcHostname and kdcRealmname. Recheck if the values are proper in the supplied kdc conf file.");
					return false; 
				}

				fileContent += "set jck.env.runtime.jgss.krb5ClientPassword " + krb5ClientPassword + ";\n";
				fileContent += "set jck.env.runtime.jgss.krb5ClientUsername " +  krb5ClientUsername + "/" + KerberosConfig.kdcHostName+'@'+KerberosConfig.kdcRealmName + ";\n";
				fileContent += "set jck.env.runtime.jgss.krb5ServerPassword " + krb5ServerPassword + ";\n";
				fileContent += "set jck.env.runtime.jgss.krb5ServerUsername " +  krb5ServerUsername + "/" + KerberosConfig.kdcHostName+'@'+KerberosConfig.kdcRealmName + ";\n";
				fileContent += "set jck.env.runtime.jgss.kdcHostName " + KerberosConfig.kdcHostName + ";\n";
				fileContent += "set jck.env.runtime.jgss.kdcRealmName " + KerberosConfig.kdcRealmName + ";\n";

				extraJvmOptions += " -Djava.security.krb5.conf=" + krbConfFile + " -DKRB5CCNAME=" + workDir + File.separator + "krb5.cache" + " -DKRB5_KTNAME=" + workDir + File.separator + "krb5.keytab";
			}	
			if ( tests.contains("api/java_net") || tests.contains("api/java_nio") || tests.equals("api") ) {
				fileContent += "set jck.env.runtime.net.localHostName " + hostname + ";\n";
				fileContent += "set jck.env.runtime.net.localHostIPAddr " + ipAddress + ";\n";
				fileContent += "set jck.env.runtime.net.testHost1Name " + testHost1Name + ";\n";
				fileContent += "set jck.env.runtime.net.testHost1IPAddr " + testHost1Ip + ";\n";
				fileContent += "set jck.env.runtime.net.testHost2Name " + testHost2Name + ";\n";
				fileContent += "set jck.env.runtime.net.testHost2IPAddr " + testHost2Ip + ";\n";
			}
			
			if ( tests.contains("api/java_net") || tests.equals("api") ) {
				fileContent += "set jck.env.runtime.url.httpURL " + httpUrl + ";\n";
				fileContent += "set jck.env.runtime.url.fileURL " + fileUrl + ";\n";
			}
			
			if ( tests.contains("api/java_net") || tests.contains("api/org_omg") || tests.contains("api/javax_management") || tests.contains("api/javax_xml") || tests.contains("vm/jdwp") || tests.equals("api")) {
				if ( !tests.contains("api/javax_xml/bind") &&
						!tests.contains("api/javax_xml/soap") &&
						!tests.contains("api/org_omg/PortableInterceptor") &&
						!tests.contains("api/org_omg/PortableServer") ) {
					fileContent += "set jck.env.runtime.remoteAgent.passiveHost localhost" + ";\n";
					fileContent += "set jck.env.runtime.remoteAgent.passivePortDefault Yes" + ";\n";
				}
			}
			
			// Without the following override the following failures occur:
			// Fatal Error: file:/jck/jck8b/JCK-runtime-8b/tests/api/javax_xml/xmlCore/w3c/ibm/valid/P85/ibm85v01.xml(6,3384): JAXP00010005: The length of entity "[xml]" is "3,381" that exceeds the "1,000" limit set by "FEATURE_SECURE_PROCESSING".
			// Fatal Error: file:/jck/jck8b/JCK-runtime-8b/tests/api/javax_xml/xmlCore/w3c/ibm/valid/P85/ibm85v01.xml(6,3384): JAXP00010005: The length of entity "[xml]" is "3,381" that exceeds the "1,000" limit set by "default".
			if ( tests.contains("api/javax_xml")  || tests.equals("api")) {
				extraJvmOptions += " -Djdk.xml.maxXMLNameLimit=4000";
			}
			
			// Needed to successfully pass api/xsl/conf/math/math85.html#math85. 
			// Ensures that the number of groups an XPath expression can contain is set to 14 or above.
			if ( tests.contains("api/xsl")) {
				extraJvmOptions += " -Djdk.xml.xpathExprGrpLimit=14";
			}

			//CORBA related files (e.g. tnameserver) were removed post Java 9
			if (jckVersion.contains("jck8")) {
				if ( tests.contains("api/org_omg") || tests.contains("api/javax_management") || tests.equals("api") ) {
					fileContent += "set jck.env.runtime.idl.orbHost " + hostname + ";\n";
				}
			}
			// ext/lib was removed at Java 9
			if ( jckVersion.contains("jck8") ) {
				if ( tests.contains("api/java_text") || tests.contains("api/java_util") || tests.equals("api")) {
					extraJvmOptions += " -Djava.ext.dirs=" + jckBase + File.separator + "lib" + File.separator + "extensions" + File.pathSeparator + 
							testJdk + File.separator + "jre" + File.separator + "lib" + File.separator + "ext";
				}
			}
			if (jckVersion.contains("jck8")) {
				if (tests.contains("api/signaturetest") || tests.equals("api")) {
					fileContent += "set jck.env.runtime.staticsigtest.staticSigTestClasspathRemote \"" + getSignatureTestJars(pathToLib) + "\"" + ";\n";
				}
			}
			if (extraJvmOptions.contains("nofallback") && tests.startsWith("vm") ) {
				fileContent += "set jck.env.testPlatform.typecheckerSpecific No" + ";\n";		
			}

			// The jplisLivePhase and Robot available settings are rejected if placed higher up in the .jtb file
			if ( tests.contains("api/java_awt") || tests.contains("api/javax_swing") || tests.equals("api") ) {
				if ( robotAvailable == "Yes" ) {
					fileContent += "set jck.env.runtime.awt.robotAvailable " + robotAvailable + ";\n";
				}
			}
			if ( tests.equals("api/java_lang") || tests.contains("api/java_lang/instrument") || tests.equals("api") ) {
				fileContent += "set jck.env.runtime.jplis.jplisLivePhase Yes;\n";
			}

			// Get any additional jvm options for specific tests.
			extraJvmOptions += getTestSpecificJvmOptions(jckVersion, tests);
			extraJvmOptions += suppressOutOfMemoryDumpOptions;
			

			if (jckVersionInt > 11) {
				extraJvmOptions += " --enable-preview -Xfuture ";
			}

			// Add the JVM options supplied by the user plus those added in this method to the jtb file option.
			fileContent += "set jck.env.runtime.testExecute.otherOpts \" " + extraJvmOptions + " " + jvmOpts + " \"" + ";\n";

			// Tests that need Display on OSX also require AWT_FORCE_HEADFUL=true 
			if (spec.contains("osx")) {
				fileContent += "set jck.env.runtime.testExecute.otherEnvVars \" AWT_FORCE_HEADFUL=true \"" + ";\n";
			}
		}

		// Compiler settings
		if (testSuite.equals("COMPILER")) {
			keyword = "keywords compiler";

			// Overrides only required on zOS for compiler tests
			if (spec.contains("zos")) {
				pathToLib = testJdk + File.separator + "lib";
			} 

			fileContent += "concurrency " + concurrencyString + ";\n";
                        if (!timeoutFactorString.equals("NULL")) {
                                fileContent += "timeoutfactor " + timeoutFactorString + ";\n";
                        } else {
				fileContent += "timeoutfactor 100" + ";\n";							// lang.CLSS,CONV,STMT,INFR requires more than 1h to complete. lang.Annot,EXPR,LMBD require more than 2h to complete tests
			}
			fileContent += keyword + ";\n";
			
			if (testExecutionType.equals("multijvm")) { 
				fileContent += "set jck.env.testPlatform.useAgent \"Yes\";\n";
				fileContent += "set jck.env.compiler.agent.agentType \"passive\";\n";
				fileContent += "set jck.env.compiler.agent.passiveHost \"" + agentHost + "\"" + ";\n";
				fileContent += "set jck.env.compiler.agent.passivePortDefault \"Yes\";\n";
			}
			
			String cmdAsStringOrFile = "cmdAsString"; // Whether to reference cmd via cmdAsString or cmdAsFile
			if (spec.contains("win")) {
				// On Windows set the testplatform.os to Windows and set systemRoot, but do not
				// set the file and path separators (an error is thrown if they are set).
				fileContent += "set jck.env.testPlatform.os \"Windows\";\n";
				fileContent += "set jck.env.testPlatform.systemRoot " + System.getenv("WINDIR") + ";\n";
			} else if (!jckVersion.contains("jck8") && (spec.contains("zos") || spec.contains("aix"))) {
				// On jck11+ z/OS and AIX set the testplatform.os Current system
				// due to JCK class OsHelper bug with getFileSep() in Compiler JCK Interviewer
				fileContent += "set jck.env.testPlatform.os \"Current system\";\n";
				cmdAsStringOrFile = "cmdAsFile";
			} else {
				// On other platforms set the testplatform.os to other and set the file and path separators.
				fileContent += "set jck.env.testPlatform.os \"other\";\n";
				fileContent += "set jck.env.testPlatform.fileSep \"/\";\n";
				fileContent += "set jck.env.testPlatform.pathSep \":\";\n";
			}
			
			// If the Select Compiler question in the JCK interview was answered as "Java Compiler API (JSR199)",
			// set jck.env.compiler.testCompile.testCompileAPImultiJVM.cmdAsString.
			fileContent += "set jck.env.compiler.testCompile.testCompileAPImultiJVM." + cmdAsStringOrFile + " \"" + testJavaForMultiJVMCompTest + "\"" + ";\n";

			if (jckVersion.contains("jck8")) {
				fileContent += "set jck.env.compiler.testCompile.otherOpts \"-source 1.8 \"" + ";\n";
				if (tests.contains("api/signaturetest") || tests.equals("api")) {
					fileContent += "set jck.env.compiler.testCompile.compilerstaticsigtest.compilerStaticSigTestClasspathRemote \"" + getSignatureTestJars(pathToLib) + "\"" + ";\n";
				}
			} else if (jckVersion.contains("jck11")) {
				fileContent += "set jck.env.compiler.testCompile.otherOpts \"-source 11 \"" + ";\n";
			} else { // This is the case where JCK Version > 11
				fileContent += "set jck.env.compiler.testCompile.otherOpts \"-source " + jckVersionInt + " --enable-preview\"" + ";\n";
			}

			if (tests.contains("api/java_rmi") || tests.equals("api")) {
				fileContent += "set jck.env.compiler.testRmic." + cmdAsStringOrFile + " \"" + pathToRmic + "\"" + ";\n";
			}
			
			System.out.println("RI JDK Used: " + riJavaForMultiJVMCompTest);
			fileContent += "set jck.env.compiler.compRefExecute." + cmdAsStringOrFile + " \"" + riJavaForMultiJVMCompTest + "\"" + ";\n";

			if (!jckVersion.contains("jck8") && (spec.contains("zos") || spec.contains("aix"))) {
				// On jck11+ z/OS and AIX set the compRefExecute file and path separators
				// due to JCK class OsHelper bug with getFileSep() in Compiler JCK Interviewer
				fileContent += "set jck.env.compiler.compRefExecute.fileSep \"/\";\n";
				fileContent += "set jck.env.compiler.compRefExecute.pathSep \":\";\n";
			}

			extraJvmOptions += suppressOutOfMemoryDumpOptions;
			

			if (jckVersionInt > 11) {
				extraJvmOptions += " --enable-preview -Xfuture ";
			}
			
			// Add the JVM options supplied by the user plus those added in this method to the jtb file option.
			if (!testExecutionType.equals("multijvm")) { 
				fileContent += "set jck.env.compiler.compRefExecute.otherOpts \" " + extraJvmOptions + " " + jvmOpts + " \"" + ";\n";
			}
		}
		// Devtools settings
		if (testSuite.equals("DEVTOOLS")) {
			String xjcCmd = "";				// Required for all devtools test, except "java2schema" & "jaxws"
			String jxcCmd = "";				// Required for "java2schema" test
			String genCmd,impCmd  = "";		// Required for "jaxws" test

			if (spec.contains("win")) {
				String winscriptdir;
				if ( jckVersion.contains("jck6") || jckVersion.contains("jck7") || jckVersion.contains("jck8") || jckVersion.contains("jck9") ) {
					winscriptdir="win32";
				} else {
					winscriptdir="windows";
				}
				xjcCmd = jckBase + File.separator + winscriptdir + File.separator + "bin" + File.separator + "xjc.bat"; 
				jxcCmd = jckBase + File.separator + winscriptdir + File.separator + "bin" + File.separator + "schemagen.bat"; 
				genCmd = jckBase + File.separator + winscriptdir + File.separator + "bin" + File.separator + "wsgen.bat"; 
				impCmd = jckBase + File.separator + winscriptdir + File.separator + "bin" + File.separator + "wsimport.bat"; 
				xjcCmd = xjcCmd.replace("/", "\\");
				jxcCmd = jxcCmd.replace("/", "\\");
				genCmd = genCmd.replace("/", "\\");
				impCmd = impCmd.replace("/", "\\");
			} else if (spec.contains("linux") || spec.contains("aix")) {
				xjcCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "xjc.sh";
				jxcCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "schemagen.sh";
				genCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "wsgen.sh";
				impCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "wsimport.sh";
			} else if (spec.contains("osx")) {
				xjcCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "xjc.sh";
				jxcCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "schemagen.sh";
				genCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "wsgen.sh";
				impCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "wsimport.sh";
			} else if (spec.contains("zos") || spec.contains("sunos")) {
				pathToJavac = testJdk + File.separator + "bin" + File.separator + "javac";
				xjcCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "xjc.sh";
				jxcCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "schemagen.sh";
				genCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "wsgen.sh";
				impCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "wsimport.sh";
			} else {
				System.out.println("Unknown spec: " + spec);
				return false; 
			}
			
			// bash/ksh required to run schema scripts (cannot be standard sh)
			if (spec.contains("linux")) {
				xjcCmd = "bash "+xjcCmd;
				jxcCmd = "bash "+jxcCmd;
			} else if (spec.contains("sunos")) {
				xjcCmd = "ksh "+xjcCmd;
				jxcCmd = "ksh "+jxcCmd;
			}

			fileContent += "concurrency " + concurrencyString + ";\n";
                        if (!timeoutFactorString.equals("NULL")) {
                                fileContent += "timeoutfactor " + timeoutFactorString + ";\n";
                        } else {
				fileContent += "timeoutfactor 40" + ";\n";							// All Devtools tests take less than 1h to finish.
			}

			if (spec.contains("win")) {
				// On Windows set the testplatform.os to Windows and set systemRoot, but do not
				// set the file and path separators (an error is thrown if they are set).
				fileContent += "set jck.env.testPlatform.os \"Windows\";\n";
			}
			else {
				// On other platforms set the testplatform.os to other and set the file and path separators.
				fileContent += "set jck.env.testPlatform.os \"other\";\n";
				fileContent += "set jck.env.testPlatform.fileSep \"/\";\n";
				fileContent += "set jck.env.testPlatform.pathSep \":\";\n";
			}

			fileContent += "set jck.env.devtools.testExecute.cmdAsString \"" + pathToJava + "\"" + ";\n";
			fileContent += "set jck.env.devtools.refExecute.cmdAsFile \"" + pathToJava + "\"" + ";\n";
			fileContent += "set jck.env.devtools.scriptEnvVars \"" + "JAVA_HOME=\"" + testJdk + "\" TOOLS_HOME=\"" + testJdk + "\"" + "\"" + ";\n";

			if (tests.contains("java2schema")) {
				fileContent += "set jck.env.devtools.jaxb.jxcCmd \"" + jxcCmd + "\"" + ";\n";
			} else if (tests.contains("jaxws")) {
				fileContent += "set jck.env.devtools.jaxws.cmdJavac \"" + pathToJavac + "\"" + ";\n";
				fileContent += "set jck.env.devtools.jaxws.genCmd \"" + genCmd + "\"" + ";\n";
				fileContent += "set jck.env.devtools.jaxws.impCmd \"" + impCmd + "\"" + ";\n";
			} else {
				fileContent += "set jck.env.devtools.jaxb.xjcCmd \"" + xjcCmd + "\"" + ";\n";
			}

			// Get any additional jvm options for specific tests.
			extraJvmOptions += getTestSpecificJvmOptions(jckVersion, tests);
			extraJvmOptions += suppressOutOfMemoryDumpOptions;
			

			// Add the JVM options supplied by the user plus those added in this method to the jtb file option.
			fileContent += "set jck.env.devtools.refExecute.otherOpts \" " + extraJvmOptions + " " + jvmOpts + " \"" + ";\n";	
		}

		// Only use default initial jtx exclude and disregard the rest of jck exclude lists 
		// when running a test via jck***_custom.
		
		if (testArgs.get(IS_CUSTOM_TARGET) == null) {
			fileContent += "set jck.excludeList.customFiles \"" + initialJtxFullPath + " " + defaultJtxFullPath + " " + kflFullPath + " " + customJtx + "\";\n";
		} else {
			fileContent += "set jck.excludeList.customFiles \"" + initialJtxFullPath + " " + defaultJtxFullPath + " " + kflFullPath + "\";\n";
		}
				
		fileContent += "runTests" + ";\n";
		fileContent += "writeReport -type xml " + reportDir + ";\n";

		// Make sure any backslashes are escaped, required by the test harness.
		fileContent = fileContent.replace("\\\\", "\\"); 		// Replaces \\ with \, leave \ alone.
		fileContent = fileContent.replace("\\", "\\\\");		// Replaces \ with \\

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(newJtbFileRef))); 
		bw.write(fileContent); 
		bw.flush();
		bw.close();

		if (spec.contains("zos")) {
			if(!doIconvFile()) {
				System.out.println("Failed to convert jtb file encoding for z/OS");
				return false; 
			}
		}

		System.out.println("Echoing contents of generated jtb file : " + newJtbFileRef); 
		System.out.println(">>>>>>>>>>");
		BufferedReader br = new BufferedReader (new FileReader(newJtbFileRef)); 
		while(true) {
			String s = br.readLine(); 
			if ( s == null) {
				break; 
			} else {
				System.out.println(s); 
			}
		}
		System.out.println("<<<<<<<<");
		return true;
	}

	private static boolean generateSummary() {
		try {
			String reportXML = reportDir + File.separator + "xml" + File.separator + "report.xml"; 
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse (new File(reportXML));
			doc.getDocumentElement ().normalize ();
			NodeList listOfTestResults = doc.getElementsByTagName("TestResult");
			String testPath = ""; 
			String message = ""; 
			StringBuffer resultSummary = new StringBuffer(); 

			for(int s=0; s<listOfTestResults.getLength() ; s++) {
				Node aResult = listOfTestResults.item(s);
				if(aResult.getNodeType() == Node.ELEMENT_NODE) {
					Element aResultElement = (Element) aResult; 
					testPath = aResultElement.getAttribute("url"); 
					NodeList ns = aResultElement.getElementsByTagName("ResultProperties"); 
					if ( ns != null) {
						Node resultPropertiesNode = ns.item(0);
						if (resultPropertiesNode != null) {
							if(resultPropertiesNode.getNodeType() == Node.ELEMENT_NODE) {
								Element aResultPropertiesElement = (Element) resultPropertiesNode; 
								NodeList properties = aResultPropertiesElement.getElementsByTagName("Property"); 
								for (int  i = 0 ; i < properties.getLength(); i++) {
									Node aProperty = properties.item(i); 
									if(aProperty.getNodeType() == Node.ELEMENT_NODE) {
										Element aPropertyElement = (Element) aProperty; 
										String name = aPropertyElement.getAttribute("name"); 
										if ( name != null && name.equals("execStatus")) {
											message = aPropertyElement.getAttribute("value"); 
											resultSummary.append(testPath + "   " + message + "\n");
											break; 
										}
									}
								}
							}
						}
					}
				}
			}
			FileWriter fw = new FileWriter(new File("summary.txt")); 
			fw.write(resultSummary.toString());
			fw.close();
			System.out.println(resultSummary.toString());
			return true; 
		} catch (FileNotFoundException e) {
			System.out.println("No javatest XML result file found. Please check if the "
					+ "test failed before generating the XML report");
			e.printStackTrace(); 
			return false; 
		} catch (SAXParseException err) {
			System.out.println ("Error processing XML JCK output report" + err.getMessage ());
			err.printStackTrace();
			return false; 
		}catch (SAXException e) {
			System.out.println ("Error processing XML JCK output report" + e.getMessage ());
			e.printStackTrace();
			return false; 
		}catch (Throwable t) {
			t.printStackTrace ();
			return false; 
		}
	}

	private static Process startSubProcess(String processName, List<String> command) throws IOException {
		System.out.println("Starting sub-process: "+ processName);
		System.out.println("Full command: " + command);
		ProcessBuilder pb = new ProcessBuilder(command);
		pb = pb.redirectErrorStream(true);
		return pb.inheritIO().start();
	}
	
	private static Process startSubProcessRedirectOut(String processName, List<String> command) throws IOException {
		File outputFile = new File(workDir + File.separator + processName + ".out"); 
		File errFile = new File(workDir + File.separator + processName + ".err");
		outputFile.createNewFile(); 
		errFile.createNewFile();
		System.out.println("Starting sub-process: "+ processName);
		ProcessBuilder pb = new ProcessBuilder(command);
		return pb.redirectError(errFile).redirectOutput(outputFile).start();
	}

	private static class KerberosConfig {
		static String kdcRealmName;
		static String kdcHostName;
		KerberosConfig(String kdcRealmName,String kdcHostName) {
			KerberosConfig.kdcRealmName = kdcRealmName;
			KerberosConfig.kdcHostName = kdcHostName;
		}
	}

	private static KerberosConfig readKrbConfFile() throws Exception {
		String kdcRealmName = null;
		String kdcHostName = null;
		BufferedReader br = new BufferedReader(new FileReader(krbConfFile.toString()));
		String thisLine = null;
		while ((thisLine = br.readLine()) != null ) {
			if (thisLine.contains("default_realm")) {
				String[] parts = thisLine.split("=");
				kdcRealmName = parts[1].trim();
			}
			if (thisLine.contains("admin_server")) {
				String[] parts = thisLine.split("=");
				kdcHostName = parts[1].trim();
			}
		}
		br.close();
		return new KerberosConfig(kdcRealmName, kdcHostName);
	}

	private static int getFreePort() throws Exception {
		int freePort = -1;
		ServerSocket s = new ServerSocket(0);
		freePort = s.getLocalPort();
		s.close();
		return freePort;
	}

	private static String locateFileOrFolder(String root, String pattern) throws Exception {
		String matches = "";
		StringBuffer sb = new StringBuffer();
		File[] files = new File(root).listFiles();
		for (File file : files) {
			if (file.getName().contains(pattern)) {
				sb.append(file.toString()).append(File.pathSeparator);
			}
			if (file.isDirectory() && !(file.getName().contains("ext")) ) {
				String s = locateFileOrFolder(file.toString(), pattern);
				if (!s.equals("")) {
					sb.append(s);
				}
			}
		}
		matches = sb.toString();
		return matches;
	}

	private static String getSignatureTestJars(String root) throws Exception {
		String jarsList = "";
		StringBuffer sb = new StringBuffer();
		System.out.println("Looking for .jar files in " + root + " for signaturetest.");
		sb.append(locateFileOrFolder(root,".jar"));
		String newRoot = new File(root).getParent() + File.separator + "bin";
		System.out.println("Looking for vm.jar in " + newRoot + " for signaturetest.");
		sb.append(locateFileOrFolder(newRoot,"vm.jar"));
		jarsList = sb.toString();
		System.out.println("Using " + jarsList + " for signaturetest.");
		return jarsList;
	}

	private static boolean testsRequireDisplay (String tests) {
		if (tests.equals("api") ||
			tests.contains("api/java_applet") || 
			tests.contains("api/javax_swing") || tests.contains("api/javax_sound") ||
			tests.contains("api/java_awt")  || tests.contains("api/javax_print") ||
			tests.contains("api/java_beans") || tests.contains("api/javax_accessibility") ||
			tests.contains("api/javax_naming")) {
			return true;
		}
		return false;
	}

	private static String getTestSpecificJvmOptions(String jckVersion, String tests) {
		String testSpecificJvmOptions = "";
		
		if ((new File(secPropsFile)).exists()) {
			// Needs extra security.properties
			testSpecificJvmOptions += " -Djava.security.properties=" + secPropsFile;
		}

		if (jckVersionInt < 9) {
			return testSpecificJvmOptions;
		}

		// --add-modules options are required to make some modules visible for Java 9 onwards.

		// If the top level api node is being run, add all modules required by the api tests
		if (tests.equals("api")) {
			testSpecificJvmOptions = " --add-modules java.xml.crypto,java.sql";
			if (jckVersionInt < 11) {
				// following modules have been removed from Java 11 and onwards
				testSpecificJvmOptions += ",java.activation,java.corba,java.xml.ws.annotation,java.se.ee,java.transaction,java.xml.bind,java.xml.ws";
			}
		}
		if (tests.contains("api/javax_crypto") ) {
			testSpecificJvmOptions = " --add-modules java.xml.crypto";
		}
		if (tests.contains("api/javax_sql") ) {
			testSpecificJvmOptions = " --add-modules java.sql";
		}

		testSpecificJvmOptions += " -Djdk.attach.allowAttachSelf=true";

		return testSpecificJvmOptions;
	}  

	private static String getOSNameShort() {
		// We need to determine if the spec is Alpine Linux or not
		if (spec.contains("linux")) {
			Path alpine = Paths.get("/etc/alpine-release");
			if (Files.exists(alpine)) {
				return "alpine-linux";
			}
			return "linux";
		}
		if (spec.contains("zos")) {
			return "zos";
		}
		if (spec.contains("win")) {
			return "win";
		}
		if (spec.contains("osx")) {
			return "osx";
		}
		if (spec.contains("aix")) {
			return "aix";
		}
		if (spec.contains("sunos")) {
			return "solaris";
		}
		return null;
	}

	private static boolean doIconvFile() throws Exception {
		String tempFile = workDir + File.separator + "jtb.tmp";
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile)); 
		bw.write(fileContent); 
		bw.flush();
		bw.close();
		new File(newJtbFileRef).delete(); 
		new File(newJtbFileRef).createNewFile(); 
		List<String> iconvCmd = new ArrayList<>();
		iconvCmd.add("iconv");
		iconvCmd.add("-f");
		iconvCmd.add("IBM-1047");
		iconvCmd.add("-t");
		iconvCmd.add("ISO8859-1");
		iconvCmd.add(tempFile);
		ProcessBuilder rmidProcessBuilder = new ProcessBuilder(iconvCmd);
		rmidProcessBuilder.redirectOutput(new File(newJtbFileRef)); 
		Process pp = rmidProcessBuilder.start();
		return pp.waitFor(15, TimeUnit.MINUTES);
	}

	private static boolean isIbmJvm() {
		String javaVMName = System.getProperty("java.vm.name").toLowerCase();
		return javaVMName.contains("ibm") || javaVMName.contains("openj9"); 
	}
	
	/**
	 * Return the jckVersion minus any letters.
	 */
	private static int getJckVersionInt(String version) {
		if (version.matches("^(jck)?[0-9]+[a-z]*$")){
			return Integer.parseInt(version.replaceAll("[a-z]", ""));
		}

		throw new Error("Invalid JCK Version found: " + version);
	}
	
	/**
	 * Return the letters on the end of jckVersion, if any.
	 */
	private static String getJckVersionLetters(String version) {
		if (version.matches("^(jck)?[0-9]+[a-z]*$")){
			return version.replaceFirst("^jck", "").replaceAll("[0-9]", "");
		}

		throw new Error("Invalid JCK Version found: " + version);
	}
}
