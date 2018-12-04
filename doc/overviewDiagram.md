graph LR;

style openliberty fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style geode fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style hbase fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style akka fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style logstash fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5

style bbench fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style acme_air fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5
style liberty_dt fill:#eee,stroke:#cca,stroke-width:2px,stroke-dasharray: 5, 5

openjdk-tests--make _openjdk-->openjdk_regression;
openjdk-tests--make _system-->systemtest;
openjdk-tests--make _perf-->performance;
openjdk-tests--make _functional-->functional;
openjdk-tests--make _jck-->jck;
openjdk-tests--make _external-->thirdparty_containers;

subgraph jck
jck
end

subgraph system
systemtest
end

subgraph openjdk
openjdk_regression
end

subgraph functional
functional
end

subgraph perf
performance
end

subgraph _extended.perf
performance-->idle_micro;
performance-->odm;
performance-->liberty_dt;
performance-->acme_air;
end

subgraph _sanity.perf
performance-->bbench;
end

subgraph 
thirdparty_containers
end

subgraph _sanity.external
thirdparty_containers-->derby;
thirdparty_containers-->elasticsearch;
thirdparty_containers-->example;
thirdparty_containers-->jenkins;
thirdparty_containers-->kafka;
thirdparty_containers-->lucene-solr;
thirdparty_containers-->scala;
thirdparty_containers-->tomcat;
thirdparty_containers-->wildfly;
thirdparty_containers-->openliberty;
thirdparty_containers-->geode;
thirdparty_containers-->hbase;
thirdparty_containers-->akka;
thirdparty_containers-->logstash;
end

subgraph _extended.external 
    thirdparty_containers-->openliberty-mp-tck;
    thirdparty_containers-->payara-mp-tck;
    thirdparty_containers-->thorntail-mp-tck;
end



