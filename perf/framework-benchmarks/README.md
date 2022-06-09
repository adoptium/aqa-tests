# FrameworkBenchmarks

[FrameworkBenchmarks](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Java)  provides representative performance measures across a wide field of web application frameworks. With much help from the community, coverage is quite broad and we are happy to broaden it further with contributions.

Here we focus on the wide range of Java frameworks for web applications scenarios.

FrameworkBenchmarks covers common workloads as plaintext processing, json content parsing, popular database manipulations 
for different frameworks. 

Notice the different from external tests:
```aidl
external tests runs framework inner unit tests to ensure frameworks behaving correctly.
perf framework tests stresses common worloads to ensure frameworks are performing well under test VMs.
```

We are enabling the following list of frameworks to avoid regression:

| Framework | Workloads                              |
|-----------|----------------------------------------|
| netty     | plaintext,json                         |
| quarkus   | plaintext,json,query,fortune,update,db |
| spark     | plaintext,json,query,fortune,update,db     |
| spring    | plaintext,json,query,fortune,update,db |
| spring-jpa  | plaintext,json,query,fortune,update,db |
| servlet   | plaintext,json                         |
| servlet3  | plaintext,json                         |
