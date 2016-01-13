This README describes how to use RecommendationServlet.

Dependencies
------------

    * HBaase Libraries:
        * commons-codec-1.7
        * commons-configuration-1.6
        * commons-lang-2.6
        * commons-logging-1.1.1
        * guava-12.0.1
        * hadoop-annotations-2.2.0
        * hadoop-auth-2.2.0
        * hadoop-common-2.2.0
        * hbase-client-0.98.0-hadoop2
        * hbase-common-0.98.0-hadoop2
        * hbase-protocol-0.98.0-hadoop2
        * htrace-core-2.04
        * jackson-core-asl-1.8.8
        * jackson-mapper-asl-1.8.8
        * log4j-1.2.17
        * netty-3.6.6.Final
        * phoenix-4.6.0-HBase-0.98-client - should be removed if server is well configured
        * protofub-java-2.5.0
        * slf4j-api-1.6.4
        * zookeeper-3.4.5

    * MyMediaLiteJava Library:
        * mymedialite

    * Custom Library:
        * CommonTools


Description
-----------

    * Hbase Libraries can be found on server, HBase installation lib folder.
    * Phoenix just should be recognized by itself, when Phoenix is properly installed on server.
    * For recommendations is used MyMediaLite Java port version 2.03
    * CommonTools contains packages to work with Algorithms, Database and Books.

Servlet deployment
------------------

    * Servlet .war file must be deployed on tomcat server.
        For example, from tomcat manager (http://tomcatUrl/manager) in "WAR file to deploy" section, click Browse,
        choose Servlet .war file from computer and press Deploy.
        Connect to Servlet using link: http://tomcatUrl:port/RecommendationServlet/RecommendationServlet

Build
-----

    Project build on:
    	* IntelliJ IDEA 14.1.5
    	* JDK 1.7

    How to build .war file is described here: https://www.jetbrains.com/idea/help/configuring-web-application-deployment.html

    * Build a WAR file from a module
        1. On the main menu, choose Build | Build Artifact.
        2. From the drop-down list, select the desired artifact of the type WAR.

        ** Build | Build Artifact

        READ: https://www.jetbrains.com/idea/help/packaging-a-module-into-a-jar-file.html

Contacts
--------

    * Author: Edgars Fjodorovs