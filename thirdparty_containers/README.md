# Third Party Container Tests
### Basic Steps:
- Learn how to run the application tests that you intend to automate in the build manually first, and find out any special dependencies the application testing may have.
- Clone https://github.com/AdoptOpenJDK/openjdk-tests.git and look at thirdparty_containers directory.
- Copy the 'example-test' subdirectory and rename it after the application you are adding. 
- Modify the files in your new sub-directory according to your needs. 
- Check in the changes into https://github.com/[YOUR-BRANCH]/openjdk-tests and test it using a <a href="https://github.com/AdoptOpenJDK/openjdk-tests/wiki/How-to-Run-a-Personal-Test-Build-on-Jenkins">personal build</a>. 

### Which files do I need to modify after making a copy of example-test?

**Dockerfile**
- The example Dockerfile contains a default list of dependent executable files. Please read the documentation of the third party application you are enabling to find out if you need any executable files other than the default set, if yes, add them to the list.   
- Update the clone command based on your third party application's source repository.
 
 **Shell script**
- Replace the example command line at the bottom of this script with the initial command lines that trigger execution of your test.

**build.xml** 
- Update the distribution folder paths, docker image name etc according to the name of your application. 

**playlist.xml** 
- Update the name of the example test case to the actual test case of the third party application that you intend to run. 

