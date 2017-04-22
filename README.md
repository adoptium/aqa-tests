# User Stories for AdoptOpenJDK Tests
0. Configure environment: 
    * required environment variables and default values
            
            cd /test/TestConfig
            export JAVA_BIN=/location_of_JVM_under_test
            export SPEC=platform_on_which_to_test
            export JAVA_VERSION=[SE80|SE90] (SE90 default value)
            make -f run_configure.mk
            
1. Add tests:
    * for Java8/Java9 functionality
       
            Check out /test/TestExample for the format to use.  
            We prefer to write Java unit and FV tests with TestNG.  We 
            leverage TestNG groups to create test make targets.

            This means that minimally your test source code should 
            belong to either level.sanity or level.extended group.
  * for a Java9 only features
    
2. Compile tests:
  * compile and run all tests

            make test
  * only compile but do not run tests

            make compile
            
3. Run tests:
  * all tests

            make test (to compile & run)
            make runtest (to run all tests without recompiling them)
  * sanity tests

            make sanity
  * openjdk regression tests

            make openjdk  
            This target will run all or a subset of the OpenJDK regression tests,
            you can add or subtract directories of tests by changing the contents of the 
            OpenJDK_Playlist/playlist.xml file
  * extended tests

            make extended
  * a specific individual test

            make _testExampleExtended_SE80_0
  * a directory of tests (WIP)
  * against a Java8 SDK

            Same general instructions for Configure environment, and make test, but export JAVA_VERSION=SE80 
            explicitly before run_configuration.mk step.
  * against a Java9 SDK

            No special steps to accomplish this, as JAVA_VERSION=SE90 by default, so simply need to Configure environment 
            and run make test.

  * rerun the failed tests from the last run

            make failed
  * with a different set of JVM options

            There are 3 ways to add options to your test run.  
            1) One-time override: If you simply want to add an option for a one-time run, 
            you can override the original options by using JVM_OPTIONS="your options".  
            2) One-time append: If you want to append options to the set that are already there, 
            use EXTRA_OPTIONS="your extra options".  For example,
            make _testExampleExtended_SE80_0 EXTRA_OPTIONS=-Xint 
            will append to those options already in the make target.
            3) New options for future test runs:  If you wish to add a particular set
            of options for a tests to be included in future builds, you can add a variant in
            the playlist.xml file for that test.  
4. Exclude tests:
  * temporarily on all platforms

            Add a line in the /test/TestConfig/default_exclude.txt file.  
            The format of the exclude file includes 3 pieces of information, name of test, defect number, platforms to exclude.
            To exclude on all platforms, use generic-all.  For example:
            
            net.adoptopenjdk.test.example.MyTest:aTestExample		141         generic-all
            
            Note that we additionally added support to exclude individual methods of a test class, by using 
            :methodName behind the class name.  In the example, only the aTestExample method from that class will 
            be excluded (on all platforms/specs).
  * temporarily on specific platforms or architectures

            Same as excluding on all platforms, you add a line to the default_exclude.txt file, but with specific 
            specs to exclude, for example:
            
            net.adoptopenjdk.test.example.MyTest:		141         linux_x86-64
            
            This example would exclude all test methods of the TestOperatingSystemMXBean from running 
            on the linux_x86-64 platform.  Note: the defect numbers should be valid git issue numbers, 
            so that when issue is resolved this exclusion can be removed.
  * permanently on all or specific platforms/archs

            For tests that should NEVER run on particular platforms or architectures, we should not use 
            the default_exclude.txt file.  To disable those tests, we annotate the test class to be disabled.  
            To exclude MyTest from running on the aix platform, for example:
        
            @Test(groups={ "level.sanity", "component.jit", "disabled.os.aix" })
            public class MyTest {
            ...
        
            We currently support the following exclusion groups:
            disabled.os.<os> (i.e. disabled.os.aix)
            disabled.arch.<arch> (i.e. disabled.arch.ppc)
            disabled.bits.<bits> (i.e. disabled.bits.64)
            disabled.spec.<spec> (i.e. disabled.spec.linux_x86-64)
5. View results:
  * in the console

            Java tests take advantage of the testNG logger.  If you want your test to print output, 
            you are required to use the testng logger (and not System.out.print statements).  
            In this way, we can not only direct that output to console, but also to various other output clients.  
            At the end of a test run, the results are summarized to show which tests passed / failed / skipped.  
            This gives you a quick view of the test names and numbers in each category (passed/failed/skipped).  
            If you've piped the output to a file, or if you like scrolling up, you can search for and find 
            the specific output of the tests that failed (exceptions or any other logging that the test produces).
  * in html files

            Html (and xml) output from the tests are created and stored in a test_output_xxxtimestamp 
            folder in the TestConfig directory (or from where you ran "make test").  The output is 
            organized by tests, each test having its own set of output.  If you open the index.html 
            file in a web browser, you will be able to see which tests passed, failed or were skipped, 
            along with other information like execution time and error messages, exceptions 
            and logs from the individual test methods.
  * Jenkins CI tool
            
            
            The summarized results are also captured in *.tap files so that they can be viewed in Jenkins
            using the TAP (Test Anything Protocol) plugin.  
6. Attach a debugger:
  * to a particular test

            The command line that is run for each particular test is echo-ed to the console, so you can easily 
            copy the command that is run.  You can then run the command directly (which is a direct call to the 
            java executable, adding any additional options, including those to attach a debugger.
7. Move test into different make targets (layers):
  * from extended to sanity (or vice versa)

            Change the group annotated at the top of the test class from "level.extended" to 
            "level.sanity" and the test will be automatically switched from the extended target 
            to the sanity target.
            
