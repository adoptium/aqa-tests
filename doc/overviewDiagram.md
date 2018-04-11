graph LR;

style elasticsearch fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style openliberty fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style derby fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style solr/lucene fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style acmeAir fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5

openjdk-tests--make _openjdk-->openjdk_regression;

openjdk-tests--make _system-->systemtest;

openjdk-tests--make _perf-->performance;
openjdk-tests--make _functional-->functional;
openjdk-tests--make _jck-->jck;
openjdk-tests--make _external-->thirdparty_containers;

performance-->idleMicroBenchmark;
performance-->acmeAir;

thirdparty_containers-->scala;
thirdparty_containers-->tomcat;
thirdparty_containers-->jenkins;
thirdparty_containers-->example;
thirdparty_containers-->openliberty;
thirdparty_containers-->elasticsearch;
thirdparty_containers-->derby;
thirdparty_containers-->solr/lucene;

