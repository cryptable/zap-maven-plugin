zap-maven-plugin
================
Introduction
------------
This is a maven plugin to perform security scans with ZAProxy at integration tests.

Usage
-----
### plugin integration
Insert following basic code into your maven project as a plugin:

~~~~~
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>zap-maven-plugin</artifactId>
    <version>2.0-SNAPSHOT</version>
    <configuration>
        <apiKey>ghnesdk0tejjsd6n7dhs9gdhskd</apiKey>
        <zapProgram>C:\Program Files\ZAProxy\zap.bat</zapProgram>
        <zapProxyHost>localhost</zapProxyHost>
        <zapProxyPort>8080</zapProxyPort>
        <targetURL>http://localhost/bodgeit</targetURL>
        <format>html</format>
    </configuration>
    <executions>
        <execution>
            <id>startZAP</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>start-zap</goal>
            </goals>
        </execution>
        <execution>
            <id>porcessZAP</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>process-zap</goal>
            </goals>
        </execution>
    </executions>
</plugin>
~~~~~

Extra configuration parameters can be added (see Configuration Parameters)

### Configuration Parameters
* &lt;apiKey&gt; (String): API key of the ZAProxy web service
* &lt;zapProgram&gt; (String): location where ZAProxy application is installed
* &lt;zapProxyHost&gt; (String): host on which ZAProxy is installed
* &lt;zapProxyPort&gt; (Integer): port to which ZAProxy listens
* &lt;targetURL&gt; (String): URL to attack
* &lt;newSession&gt; (true/false): ZAProxy runs as a service (no start or stop or ZAProxy) and start everytime a new session
* &lt;zapSleep&gt; (Integer): milliseconds to wait to start ZAProxy
* &lt;daemon&gt; (true/false): start and stop ZAProxy as deamon
* &lt;property.file&gt; (String): name of property file to change
* &lt;property.file.proxy.host&gt; (String): parameter name in the properties file to write the ZAProxy host
* &lt;property.file.proxy.port&gt; (String): parameter name in the properties file to write the ZAProxy port 
* &lt;spiderURL&gt; (true/false): let ZAProxy execute a spider of the targetURL
* &lt;scanURL&gt; (true/false): let ZAProxy execute a scan of the targetURL
* &lt;saveSession&gt; (true/false): let ZAP save the session 
* &lt;shutdownZAP&gt; (true/false): stop ZAProxy after scan
* &lt;reportAlerts&gt; (true/false): report the alerts
* &lt;reportsDirectory&gt; (true/false): directory to store the report
* &lt;reportsFilenameNoExtension&gt; (true/false): filename of the report without extension, because extension is determined by the format
* &lt;format&gt; (none/html/xml/json): Output format of the report

Build
-----
1) Build ZAProxy to built the client API. It's name is zap-api-v2-5.jar (in build/zap directory)
2) Install the jar in the maven repository
~~~~~
mvn install:install-file -Dfile=zap-api-v2-5.jar -DgroupId=org.zaproxy -DartifactId=clientapi -Dversion=2.5.0 -Dpackaging=jar
~~~~~
3) perform a 'mvn clean install' in the zap-maven-plugin directory

Release Notes:
--------------

### Version 1.2
This version is a merge on the version 1.0 and 1.1, which supports ZAProxy 2.5
- Added extended report handling to create an index.html (based on html-file of ZAProxy)
- Modify a properties (used for serenity) file to setup the Proxy Host and Proxy Port to point to ZAProxy
- Support zaproxy-client-v2-5.jar
- added apiKey into the POM file

### Version 1.1
This is the version of Javabeanz, which supports ZAProxy 2.4

### Version 1.0
Initial version using ZAProxy 2.2

TODO:
-----
- Writing tests
- HTML formatting
    - Summary page of problems