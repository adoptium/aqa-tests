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

public class JavaTestRunner {
	private static String testJdk;
	private static String tests;
	private static String testExecutionType;
	private static String withAgent;
	private static String interactive;
	private static String extraJvmOptions;
	private static String concurrencyString;
	private static String jckVersion;
	private static String config;
	private static String jckRoot;

	private static String testSuite;
	private static String jckBase;
	private static String jtiFile;
	private static String nativesLoc;
	private static String jckConfigLoc;
	private static String initialJtxFullPath;
	private static String jtxRelativePath;
	private static String jtxFullPath;
	private static String kflRelativePath;
	private static String kflFullPath;
	private static String krbConfFile;
	private static String fileUrl;

	private static String jckPolicyFileFullPath; 
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
	private static String newJtbLocation; 
	private static String reportDir; 
	private static String workDir;
	private static String newJtbFileRef; 
	private static String jckVersionNo;
	private static String testSuiteFolder;
	private static String fileContent;
	private static String testRoot; 
	private static String resultDir;
	private static String pathToJava;

	private static HashMap<String, String> testArgs = new HashMap<String, String>();
	private static String jvmOpts = ""; 

	// Variables to contain the JVM options required to suppress dumps being taken for OutOfMemory exceptions
	private static String suppressOutOfMemoryDumpOptions = "";

	private static int freePort;
	private static String archName = System.getProperty("os.arch");
	private static String platform = getOSNameShort();
	private static boolean isRiscv = archName.toLowerCase().contains("riscv");
	
	private static final String RESULTS_ROOT = "resultsRoot";
	private static final String TEST_ROOT = "testRoot";
	private static final String JCK_ROOT = "jckRoot";
	private static final String TESTS = "tests";
	private static final String JCK_VERSION = "jckversion";
	private static final String TEST_SUITE = "testsuite";	
	private static final String TEST_EXECUTION_TYPE = "testExecutionType";
	private static final String WITH_AGENT = "withAgent";
	private static final String INTERACTIVE = "interactive";
	private static final String CONFIG = "config";

	public static void main(String args[]) throws Exception {
		ArrayList<String> essentialParameters = new ArrayList<String>(); 
		essentialParameters.add(RESULTS_ROOT);
		essentialParameters.add(TEST_ROOT);
		essentialParameters.add(JCK_ROOT);
		essentialParameters.add(TESTS);
		essentialParameters.add(JCK_VERSION);
		essentialParameters.add(TEST_SUITE);	
		essentialParameters.add(TEST_EXECUTION_TYPE);
		essentialParameters.add(WITH_AGENT);
		essentialParameters.add(INTERACTIVE);
		essentialParameters.add(CONFIG);

		for (String arg : args) {
			if (arg.contains("=")) { 
				String [] aPair = arg.split("="); 
				String key = aPair[0]; 
				String value = aPair[1];  
				// We only load testArgs with key,value pairs that are needed by the JavaTestRunner 
				if (essentialParameters.contains(key)) { 
					// This is a special case for JCK where we may supply multiple sub-folders to run
					if(value.contains(";")) {
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
					System.out.println("Unrecognized input key ignored: " + key);
					System.exit(1);
				}
			} 
		}
		
		jvmOpts = System.getProperty("jvm.options").trim() + " " + System.getProperty("other.opts"); 
		
		try { 
			boolean jtbGenerated = false, testSuccedded = false, summaryGenerated = false;
			jtbGenerated = generateJTB(); 
			if (jtbGenerated) {
				testSuccedded = execute();
				summaryGenerated = generateSummary();
			}  
			
			if (jtbGenerated && testSuccedded && summaryGenerated) {
				System.exit(0);
			} else {
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace(); 
			System.exit(1);
		}
	}
	
	public static boolean generateJTB() throws Exception {
		testJdk = System.getenv("JAVA_HOME");
		tests = testArgs.get(TESTS).trim(); 
		jckVersion = testArgs.get(JCK_VERSION); 
		jckRoot = new File(testArgs.get(JCK_ROOT)).getCanonicalPath(); 
		testSuite = testArgs.get(TEST_SUITE); 
		testExecutionType = testArgs.get(TEST_EXECUTION_TYPE) == null ? "multijvm" : testArgs.get(TEST_EXECUTION_TYPE); 
		withAgent = testArgs.get(WITH_AGENT) == null ? "off" : testArgs.get(WITH_AGENT); 
		interactive = testArgs.get(INTERACTIVE) == null ? "no" : testArgs.get(INTERACTIVE); 
		concurrencyString = testArgs.get("concurrency") == null ? "NULL" : testArgs.get("concurrency"); 
		config = testArgs.get(CONFIG) == null ? "NULL" : testArgs.get(CONFIG); 
		testRoot = new File(testArgs.get(TEST_ROOT)).getCanonicalPath();
		resultDir = new File(testArgs.get(RESULTS_ROOT)).getCanonicalPath();	
		jckVersionNo = jckVersion.replace("jck", "");	

		File f = new File(jckRoot);
		File[] files = f.listFiles();

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
					}
				}
			}
		}
		
		if (!found) {
			System.out.println("Cannot locate the JCK artifacts under : " + jckRoot);
			return false; 
		}

		testSuiteFolder = "JCK-" + testSuite.toString().toLowerCase() + "-" + jckVersionNo;
		jckBase = jckRoot + File.separator + testSuiteFolder; 
		jckPolicyFileFullPath = jckRoot + File.separator + testSuiteFolder + File.separator + "lib" + File.separator + "jck.policy"; 
		javatestJarFullPath = jckRoot + File.separator + testSuiteFolder + File.separator + "lib" + File.separator + "javatest.jar"; 
		classesFullPath = jckRoot + File.separator + testSuiteFolder + File.separator + "classes"; 
		nativesLoc = jckRoot + File.separator + "natives" + File.separator + platform;
		jtiFile = testRoot + File.separator + "jck" + File.separator + "jtrunner" + File.separator + CONFIG + File.separator + jckVersion + File.separator + testSuite.toLowerCase() + ".jti"; 
		fileUrl = "file:///" + testSuiteFolder + "/testsuite.jtt";

		// The first release of a JCK will have an initial excludes (.jtx) file in test-suite/lib - e.g. JCK-runtime-8b/lib/jck8b.jtx.
		// Updates to the excludes list may subsequently be supplied as a separate file, which supersedes the initial file.
		// A known failures list (.kfl) file is optional.
		// The automation here adds any files found (initial or updates) as 'custom' files. 
		initialJtxFullPath = jckBase + "/lib/" + jckVersion + ".jtx";

		// Look for an update to the initial excludes file
		if (jckVersion.contains("jck6") || jckVersion.contains("jck7")) {
			jtxRelativePath = "excludes/jdk" + jckVersionNo + ".jtx";
			kflRelativePath = "excludes/jdk" + jckVersionNo + ".kfl";
		} else {
			jtxRelativePath = "excludes/jck" + jckVersionNo + ".jtx";
			kflRelativePath = "excludes/jck" + jckVersionNo + ".kfl";
		}
		
		jtxFullPath = jckRoot + File.separator + jtxRelativePath; 
		File jtxFile = new File(jtxFullPath); 
		
		if (jtxFile.exists()) { 
			System.out.println("Using excludes list file " + jtxFullPath);
		} else {
			System.out.println("Unable to find excludes list file " + jtxFullPath);
			jtxFullPath = "";
		}

		// Look for a known failures list file
		kflFullPath = jckRoot + File.separator + kflRelativePath;
		File kflFile = new File(kflFullPath); 
		
		if (kflFile.exists()) { 
			System.out.println("Using known failures list file " + kflFullPath);
		} else {
			System.out.println("Unable to find known failures list file " + kflFullPath);
			kflFullPath = "";
		}

		if (testSuite.equals("RUNTIME") && (tests.contains("api/java_net") || tests.contains("api/java_nio") || tests.contains("api/org_ietf") || tests.contains("api/javax_security") || tests.equals("api"))) {
			if (config.equals("NULL")) {
				config = "default";	
			}
			String subdir = "config/" + config;
			jckConfigLoc = jckRoot + File.separator + subdir;
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

		// Some tests provoke out of memory exceptions.
		// If we're testing a J9 VM that will result in dumps being taken and a non-zero return code
		// which stf will detect as a failure. So in this case add the -Xdump options required to suppress
		// taking dumps for OutOfMemory.
		if (isIbmJvm()) { 
			suppressOutOfMemoryDumpOptions = " -Xdump:system:none -Xdump:system:events=gpf+abort+traceassert+corruptcache -Xdump:snap:none -Xdump:snap:events=gpf+abort+traceassert+corruptcache -Xdump:java:none -Xdump:java:events=gpf+abort+traceassert+corruptcache -Xdump:heap:none -Xdump:heap:events=gpf+abort+traceassert+corruptcache"; 
		}

		newJtbLocation = resultDir + File.separator + "JTB"; 
		workDir = resultDir + File.separator + "workdir"; 
		reportDir = resultDir + File.separator + "report"; 
		newJtbFileRef = newJtbLocation + File.separator + "generated.jtb";

		File file = new File(newJtbLocation);
		file.mkdir();

		fileContent = "testsuite \"" + jckBase + "\";\n";
		fileContent += "workDirectory -create -overwrite " + workDir + ";\n";
		fileContent += "tests " + tests + ";\n";

		if (testExecutionType.equals("multijvm") && withAgent.equals("off")) {
			if (!jckConfigurationForMultijvmWithNoAgent()) {
				return false; 
			}
		} else {
			System.out.println(testExecutionType + "with Agent " + withAgent + "combination is not yet supported.");
			return false; 
		}

		fileContent += "set jck.excludeList.customFiles \"" + initialJtxFullPath + " " + jtxFullPath + " " + kflFullPath + "\"" + ";\n";
		fileContent += "runTests" + ";\n";
		fileContent += "writeReport -type xml " + reportDir + ";\n";

		// Make sure any backslashes are escaped, required by the test harness.
		fileContent = fileContent.replace("\\\\", "\\"); 		// Replaces \\ with \, leave \ alone.
		fileContent = fileContent.replace("\\", "\\\\");		// Replaces \ with \\

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(newJtbFileRef))); 
		bw.write(fileContent); 
		bw.flush();
		bw.close();

		if (platform.equals("zos")) {
			if(!doIconvFile()) {
				System.out.println("Failed to convert jtb file encoding for z/OS");
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
	
	public static boolean jckConfigurationForMultijvmWithNoAgent() throws Exception {
		pathToJava = testJdk + File.separator + "bin" + File.separator + "java";
		String pathToRmic = testJdk + File.separator + "bin" + File.separator + "rmic";
		String pathToLib = testJdk + File.separator + "jre" + File.separator + "lib";
		String pathToJavac = testJdk + File.separator + "bin" + File.separator + "javac";
		String pathToToolsJar = testJdk + File.separator + "lib" + File.separator + "tools.jar";
		// Use escaped backslashes for paths on Windows
		if (platform.contains("win")) {
			pathToJava = pathToJava.replace("/", "\\") + ".exe";
			pathToRmic = pathToRmic.replace("/", "\\") + ".exe";
			pathToLib = pathToLib.replace("/", "\\");
			pathToJavac = pathToJavac.replace("/", "\\") + ".exe";
			pathToToolsJar = pathToToolsJar.replace("/", "\\");
		}

		String jckRuntimeNativeLibValue = nativesLoc;
		String jckRuntimeJmxLibValue = nativesLoc;
		int concurrency;
		String keyword = "";
		String libPath = "";
		String robotAvailable = "";
		String hostname = "";
		String ipAddress = "";
		extraJvmOptions = jvmOpts;

		InetAddress addr = InetAddress.getLocalHost();
		ipAddress = addr.getHostAddress();
		hostname = addr.getHostName();

		freePort = getFreePort();

		if (freePort == -1) {
			System.out.println("Unable to get a free port");
			return false; 
		}

		// If concurrency was not specified as a test-arg it will have been assigned the value NULL.
		// Default to concurrency=1.
		if ( concurrencyString.equals("NULL") ) {
			concurrencyString = "1";
		}

		// If concurrency=cpus was specified, set concurrency to the number of processors + 1.
		if ( concurrencyString.equals("cpus") ) {
			concurrency = Runtime.getRuntime().availableProcessors() + 1;
			concurrencyString = String.valueOf(concurrency);
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

			if (platform.contains("win")) {
				libPath = "PATH";
				robotAvailable = "Yes";
			} else if (platform.contains("linux"))  {
				libPath = "LD_LIBRARY_PATH";
				robotAvailable = "Yes";
			} else if (platform.contains("aix")) {
				libPath = "LIBPATH";
				robotAvailable = "Yes";
			} else if (platform.contains("zos")) {
				pathToLib = testJdk + File.separator + "lib";
				libPath = "LIBPATH";
				robotAvailable = "No";
			} else if (platform.contains("osx")) {
				libPath = "DYLD_LIBRARY_PATH";
				robotAvailable = "Yes";
			} else {
				System.out.println("Unknown platform:: " + platform);
				return false; 
			}
			
			fileContent += "concurrency " + concurrencyString + ";\n";
			fileContent += "timeoutfactor 2" + ";\n";	// 2 base time limit equal 20 minutes
			fileContent += keyword + ";\n";

			if (platform.equals("win")) {
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
				if (platform.equals("zos")) {
					fileContent += "set jck.env.testPlatform.headless Yes" + ";\n";
					fileContent += "set jck.env.runtime.testExecute.otherEnvVars LIBPATH=/usr/lpp/tcpip/X11R66/lib" + ";\n";
				}
				else {
					if ( !platform.equals("win") ) {
						fileContent += "set jck.env.testPlatform.headless No" + ";\n";
						fileContent += "set jck.env.testPlatform.xWindows Yes" + ";\n";
						String display = System.getenv("DISPLAY");
						if ( display == null ) {
							System.out.println("Error: DISPLAY must be set to run tests " + tests + " on " + platform);
							return false; 
						}
						else {
							fileContent += "set jck.env.testPlatform.display " + display + ";\n";
						}
					}
				}
			}

			if ( tests.contains("api/java_awt") || tests.contains("api/javax_swing") || tests.equals("api") ) {
				keyword += "&!robot";
			}

			fileContent += "set jck.env.runtime.testExecute.cmdAsString \"" + pathToJava + "\"" + ";\n";

			if ( tests.equals("api/java_lang") || tests.contains("api/java_lang/instrument") ||
					tests.contains("api/javax_management") || tests.equals("api") || tests.startsWith("vm") ) {
				fileContent += "set jck.env.runtime.testExecute.libPathEnv " + libPath + ";\n";
				fileContent += "set jck.env.runtime.testExecute.nativeLibPathValue \"" + jckRuntimeNativeLibValue + "\"" + ";\n";
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

			if ( tests.contains("api/javax_management") || tests.equals("api") ) {
				fileContent += "set jck.env.runtime.testExecute.jmxResourcePathValue \"" + jckRuntimeJmxLibValue + "\"" + ";\n";
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

				extraJvmOptions += " -Djava.security.krb5.conf=" + krbConfFile + " -DKRB5CCNAME=" + resultDir + File.separator + "krb5.cache" + " -DKRB5_KTNAME=" + resultDir + File.separator + "krb5.keytab";
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
				fileContent += "set jck.env.runtime.awt.robotAvailable " + robotAvailable + ";\n";
			}
			if ( tests.equals("api/java_lang") || tests.contains("api/java_lang/instrument") || tests.equals("api") ) {
				fileContent += "set jck.env.runtime.jplis.jplisLivePhase Yes;\n";
			}

			// Get any additional jvm options for specific tests.
			extraJvmOptions += getTestSpecificJvmOptions(jckVersion, tests);

			extraJvmOptions += suppressOutOfMemoryDumpOptions;

			if (getJckVersionInt(jckVersionNo) > 11) {
				extraJvmOptions += " --enable-preview -Xfuture ";
			}

			// Add the JVM options supplied by the user plus those added in this method to the jtb file option.
			fileContent += "set jck.env.runtime.testExecute.otherOpts \" " + extraJvmOptions + " \"" + ";\n";
		}

		// Compiler settings
		if (testSuite.equals("COMPILER")) {

			keyword = "keywords compiler";

			// Overrides only required on zOS for compiler tests
			if (platform.equals("zos")) {
				pathToLib = testJdk + File.separator + "lib";
			} 

			fileContent += "concurrency " + concurrencyString + ";\n";
			fileContent += "timeoutfactor 1" + ";\n";							// lang.CLSS,CONV,STMT,INFR requires more than 1h to complete. lang.Annot,EXPR,LMBD require more than 2h to complete tests
			fileContent += keyword + ";\n";

			String cmdAsStringOrFile = "cmdAsString"; // Whether to reference cmd via cmdAsString or cmdAsFile
			if (platform.equals("win")) {
				// On Windows set the testplatform.os to Windows and set systemRoot, but do not
				// set the file and path separators (an error is thrown if they are set).
				fileContent += "set jck.env.testPlatform.os \"Windows\";\n";
				fileContent += "set jck.env.testPlatform.systemRoot " + System.getenv("WINDIR") + ";\n";
			} else if (platform.equals("zos") || platform.equals("aix")) {
				// On z/OS and AIX set the testplatform.os Current system
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
			fileContent += "set jck.env.compiler.testCompile.testCompileAPImultiJVM." + cmdAsStringOrFile + " \"" + pathToJava + "\"" + ";\n";

			if (jckVersion.contains("jck8")) {
				fileContent += "set jck.env.compiler.testCompile.otherOpts \"-source 1.8 \"" + ";\n";
				if (tests.contains("api/signaturetest") || tests.equals("api")) {
					fileContent += "set jck.env.compiler.testCompile.compilerstaticsigtest.compilerStaticSigTestClasspathRemote \"" + getSignatureTestJars(pathToLib) + "\"" + ";\n";
				}
			} else if (jckVersion.contains("jck11")) {
				fileContent += "set jck.env.compiler.testCompile.otherOpts \"-source 11 \"" + ";\n";
			} else { // This is the case where JCK Version > 11
				fileContent += "set jck.env.compiler.testCompile.otherOpts \"-source " + jckVersionNo + " --enable-preview\"" + ";\n";
			} 

			if (tests.contains("api/java_rmi") || tests.equals("api")) {
				fileContent += "set jck.env.compiler.testRmic." + cmdAsStringOrFile + " \"" + pathToRmic + "\"" + ";\n";
			}
			
			fileContent += "set jck.env.compiler.compRefExecute." + cmdAsStringOrFile + " \"" + pathToJava + "\"" + ";\n";

			if (platform.equals("zos") || platform.equals("aix")) {
				// On z/OS and AIX set the compRefExecute file and path separators
				// due to JCK class OsHelper bug with getFileSep() in Compiler JCK Interviewer
				fileContent += "set jck.env.compiler.compRefExecute.fileSep \"/\";\n";
				fileContent += "set jck.env.compiler.compRefExecute.pathSep \":\";\n";
			}

			extraJvmOptions += suppressOutOfMemoryDumpOptions;

			if (getJckVersionInt(jckVersionNo) > 11) {
				extraJvmOptions += " --enable-preview -Xfuture ";
			}

			// Add the JVM options supplied by the user plus those added in this method to the jtb file option.
			fileContent += "set jck.env.compiler.compRefExecute.otherOpts \" " + extraJvmOptions + " \"" + ";\n";	
		}
		// Devtools settings
		if (testSuite.equals("DEVTOOLS")) {
			String xjcCmd = "";				// Required for all devtools test, except "java2schema" & "jaxws"
			String jxcCmd = "";				// Required for "java2schema" test
			String genCmd,impCmd  = "";		// Required for "jaxws" test

			if (platform.equals("win")) {
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
			} else if (platform.equals("linux") || platform.equals("aix")) {
				xjcCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "xjc.sh";
				jxcCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "schemagen.sh";
				genCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "wsgen.sh";
				impCmd = jckBase + File.separator + "linux" + File.separator + "bin" + File.separator + "wsimport.sh";
			} else if (platform.equals("osx")) {
				xjcCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "xjc.sh";
				jxcCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "schemagen.sh";
				genCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "wsgen.sh";
				impCmd = jckBase + File.separator + "macos" + File.separator + "bin" + File.separator + "wsimport.sh";
			} else if (platform.equals("zos")) {
				pathToJavac = testJdk + File.separator + "bin" + File.separator + "javac";
				xjcCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "xjc.sh";
				jxcCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "schemagen.sh";
				genCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "wsgen.sh";
				impCmd = jckBase + File.separator + "solaris" + File.separator + "bin" + File.separator + "wsimport.sh";
			} else {
				System.out.println("Unknown platform:: " + platform);
				return false; 
			}

			fileContent += "concurrency " + concurrencyString + ";\n";
			fileContent += "timeoutfactor 1" + ";\n";							// All Devtools tests take less than 1h to finish.

			if (platform.equals("win")) {
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
			fileContent += "set jck.env.devtools.refExecute.otherOpts \" " + extraJvmOptions + " \"" + ";\n";	
		}
		return true;
	}

	public static boolean execute() throws Exception {
		if (testExecutionType.equals("multijvm") && withAgent.equals("off")) {
			Process javatestAgent = null;
			Process rmiRegistry = null;
			Process rmid = null;
			Process tnameserv = null;
			Process jck = null; 

			if ( (testSuite.equals("RUNTIME")) && (tests.contains("api/java_util") || tests.contains("api/java_net") || tests.contains("api/java_rmi")  || tests.contains("api/javax_management") 
					|| tests.contains("api/org_omg") || tests.contains("api/javax_xml") || tests.equals("api") || tests.contains("vm/jdwp") || tests.equals("vm")) ) {
				String addModules = "";
				// JCK 9/10 javatest agents need to be given access to non default modules.
				// JCK 8 doesn't support modules, JCK11 and beyond have removed these two modules: java.xml.ws,java.corba.
				if ( jckVersion.contains("jck9") || jckVersion.contains("jck10") ) {
					addModules = "--add-modules java.xml.ws,java.corba";
				}

				String classPath = javatestJarFullPath + File.pathSeparator + classesFullPath; 

				List<String> javatestAgentCmd = new ArrayList<>();
				javatestAgentCmd.add(pathToJava);
				javatestAgentCmd.add("-Djavatest.security.allowPropertiesAccess=true");
				javatestAgentCmd.add("-Djava.security.policy=" + jckPolicyFileFullPath);
				javatestAgentCmd.add(addModules);
				javatestAgentCmd.add("-cp");
				javatestAgentCmd.add(classPath);
				javatestAgentCmd.add("com.sun.javatest.agent.AgentMain");
				javatestAgentCmd.add(" -passive");
				javatestAgent = startSubProcess("javatestAgent",javatestAgentCmd);

				// We only need RMI registry and RMI activation daemon processes for tests under api/java_rmi
				if (tests.contains("api/java_rmi")) {
					String pathToRmiRegistry = testJdk + File.separator + "bin" + File.separator + "rmiregistry";
					List<String> rmiRegistryCmd = new ArrayList<>();
					rmiRegistryCmd.add(pathToRmiRegistry);
					rmiRegistry = startSubProcess("rmiregistry", rmiRegistryCmd); 

					String pathToRmid = testJdk + File.separator + "bin" + File.separator + "rmid";
					List<String> rmidCmd = new ArrayList<>();
					rmidCmd.add(pathToRmid);
					rmidCmd.add("-J-Dsun.rmi.activation.execPolicy=none"); 
					rmidCmd.add("-J-Djava.security.policy=" + jckPolicyFileFullPath); 
					rmid = startSubProcess("rmid", rmidCmd); 
				}

				// tnameserv has been removed from jdk11. We only should need it for jck8
				if (jckVersion.contains("jck8")) { 
					String pathToTnameserv = testJdk + File.separator + "bin" + File.separator + "tnameserv";
					List<String> tnameservCmd = new ArrayList<>();
					tnameservCmd.add(pathToTnameserv);
					tnameservCmd.add("-ORBInitialPort");
					tnameservCmd.add("9876");
					tnameserv = startSubProcess("tnameserve", tnameservCmd); 
				}
			}

			long timeout = 24;
			int chmodRC = 0; 
			int jckRC = 0; 
			
			// Use the presence of a '/' to signify that we are running a subset of tests.
			// If one of the highest level test nodes is being run it is likely to take a long time.
			if ( tests.contains("/") && !isRiscv ) {
				timeout = 8;
			}

			if (!platform.equals("win")) {
				List<String> chmodCmd = new ArrayList<>();
				chmodCmd.add("bash");
				chmodCmd.add("-c"); 
				chmodCmd.add("chmod 777 "+ javatestJarFullPath); 
				Process chmod = startSubProcess("chmod", chmodCmd);
				chmodRC = chmod.waitFor();
			}

			List<String> jckCmd = new ArrayList<>();
			jckCmd.add(pathToJava);
			jckCmd.add("-jar"); 
			jckCmd.add(javatestJarFullPath);
			jckCmd.add("-config");
			jckCmd.add(jtiFile);
			jckCmd.add(" @" + newJtbFileRef);
			System.out.println("Running JCK in " + testExecutionType + " way with Agent " + withAgent);
			jck = startSubProcess("jck", jckCmd);

			// Block parent for this process to finish
			boolean endedWithinTimeLimit = jck.waitFor(timeout, TimeUnit.HOURS); 
			
			jckRC = jck.exitValue(); 

			// The Compiler -ANNOT, EXPR and LMBD may take over 6 hours to run on some machines.
			if (javatestAgent != null) {
				System.out.println("Stopping javatest agent"); 
				javatestAgent.destroy();
			}
			if (rmiRegistry != null) {
				System.out.println("Stopping rmiregistry"); 
				rmiRegistry.destroy();
			}
			if (rmid != null) {
				System.out.println("Stopping rmid"); 
				rmid.destroy();
			}
			if (tnameserv != null) {
				System.out.println("Stopping tnameserver"); 
				tnameserv.destroy();
			}
			
			if ( chmodRC != 0 || jckRC != 0 || !endedWithinTimeLimit) {
				return false; 
			}
			return true; 
		}
		return false; 
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
		ProcessBuilder pb = new ProcessBuilder(command);
		return pb.inheritIO().start();
	}
	
	private static Process startSubProcessRedirectOut(String processName, List<String> command) throws IOException {
		File outputFile = new File(resultDir + File.separator + processName + ".out"); 
		File errFile = new File(resultDir + File.separator + processName + ".err");
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
		File f = new File(root);
		File[] files = f.listFiles();
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
			tests.contains("api/java_applet") || tests.contains("api/java_io") ||
			tests.contains("api/javax_swing") || tests.contains("api/javax_sound") ||
			tests.contains("api/java_awt")  || tests.contains("api/javax_print") ||
			tests.contains("api/java_beans") || tests.contains("api/javax_accessibility") ||
			tests.contains("api/javax_naming") || tests.contains("api/signaturetest")) {
			return true;
		}
		return false;
	}

	private static String getTestSpecificJvmOptions(String jckVersion, String tests) {
		String testSpecificJvmOptions = "";
		Matcher matcher = Pattern.compile("jck(\\d+)c?").matcher(jckVersion);
		if (matcher.matches()) {
			// first group is going to be 8, 9, 10, 11, etc.
			int jckVerNum = Integer.parseInt(matcher.group(1));
			// --add-modules options are required to make some modules visible for Java 9 onwards.
			if (jckVerNum >= 9) {
				// If the top level api node is being run, add all modules required by the api tests
				if (tests.equals("api")) {
					testSpecificJvmOptions = " --add-modules java.xml.crypto,java.sql";
					if (jckVerNum < 11) {
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
				if (jckVerNum < 11) {
					if (tests.contains("api/javax_activation")) {
						testSpecificJvmOptions = " --add-modules java.activation";
					}
					if (tests.contains("api/javax_activity")) {
						testSpecificJvmOptions = " --add-modules java.corba";
					}
					if (tests.contains("api/javax_rmi")) {
						testSpecificJvmOptions = " --add-modules java.corba";
					}
					if (tests.contains("api/org_omg")) {
						testSpecificJvmOptions = " --add-modules java.corba";
					}
					if (tests.contains("api/javax_annotation")) {
						testSpecificJvmOptions = " --add-modules java.xml.ws.annotation";
					}
					if (tests.contains("api/java_lang")) {
						testSpecificJvmOptions = " --add-modules java.xml.ws.annotation,java.xml.bind,java.xml.ws,java.activation,java.corba";
					}
					if (tests.contains("api/javax_transaction") ) {
						testSpecificJvmOptions = " --add-modules java.transaction";
					}
					if (tests.contains("api/javax_xml") ) {
						testSpecificJvmOptions = " --add-modules java.xml.bind,java.xml.ws";
					}
					if (tests.contains("api/modulegraph")) {
						testSpecificJvmOptions = " --add-modules java.activation,java.corba,java.transaction,java.se.ee,java.xml.bind,java.xml.ws,java.xml.ws.annotation";
					}
					if (tests.contains("api/signaturetest")) {
						testSpecificJvmOptions = " --add-modules java.activation,java.corba,java.transaction,java.xml.bind,java.xml.ws,java.xml.ws.annotation";
					}
					if (tests.contains("java2schema") ) {
						testSpecificJvmOptions = " --add-modules java.xml.bind";
					}
					if (tests.contains("xml_schema") ) {
						testSpecificJvmOptions = " --add-modules java.xml.bind";
					}
					if (tests.contains("jaxws") ) {
						testSpecificJvmOptions = " --add-modules java.xml.bind,java.xml.ws";
					}
					if (tests.contains("schema2java") ) {
						testSpecificJvmOptions = " --add-modules java.xml.bind";
					}
					if (tests.contains("schema_bind") ) {
						testSpecificJvmOptions = " --add-modules java.xml.bind";
					}
				}
				testSpecificJvmOptions += " -Djdk.attach.allowAttachSelf=true";
			}
		} else {
			throw new Error("Unexpected jck version : " + jckVersion);
		}
		return testSpecificJvmOptions;
	}  

	private static String getOSNameShort() {
		// get the osName and make it lowercase
		String osName = System.getProperty("os.name").toLowerCase();

		// set the shortname to the osName if the current system is Linux
		// or AIX this is all that is needed
		String osShortName = osName;

		// if we are on z/OS remove the slash
		if (osName.equals("z/os")) {
			osShortName = "zos";
		}

		// if we are on a Windows machine use win as the shortname
		if (osName.contains("win")) {
			osShortName = "win";
		}

		// if we are on a Mac use osx as the shortname
		if (osName.contains("mac")) {
			osShortName = "osx";
		}   

		// if we are on BSD use bsd as the shortname
		if (osName.contains("bsd")) {
			osShortName = "bsd";
		}

		// if we are on sunos use solaris as the shortname
		if (osName.contains("sunos")) {
			osShortName = "solaris";
		}

		return osShortName;
	}

	private static boolean doIconvFile() throws Exception {
		String tempFile = newJtbLocation + File.separator + "temp.jtb";
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
	
	private static int getJckVersionInt(String version) {
		if (version.equals("8c")) {
			return 8; 
		} else {
			return Integer.parseInt(version); 
		}
	}
}
