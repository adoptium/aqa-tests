# Dockerize AdoptOpenJDK Testing

### What is our motivation?

* We want to simplify test execution as much as possible.
* We want to minimize the OS requirement to ease users' work.
* We want to automate test process by just running one command. To run the test,
you can simply run `docker run -it -v <path to test jdk>:/java -v <path to test root folder>:/test <docker_image_name>`

### How to use this Dockerfile

We assume you have already cloned this repo to your local machine.

1. Execute get.sh in the test root folder to get all dependencies.
2. Build the docker image: `docker build -t openjdk-test .`
3. If you want to get into the Docker image to manually run tests, the command is: 
   `docker run -it -v <path_to_JDK_root>:/java -v <path_to_openjdk_test_root_dir>:/test openjdk-test /bin/bash` , then you could follow the README.md in test root to run tests manually.

   If you want to automatically execute tests, the command is:
   `docker run -it -v <path_to_JDK_root>:/java -v <path_to_openjdk_test_root_dir>:/test openjdk-test`