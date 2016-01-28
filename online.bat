@echo off
set CP=lib/commons-cli-1.3.1.jar;lib/commons-codec-1.10.jar;lib/commons-collections-3.2.1.jar
set CP=%CP%;lib/commons-io-2.4.jar;lib/commons-lang3-3.4.jar;lib/commons-logging-1.2.jar
set CP=%CP%;lib/cssparser-0.9.18.jar;lib/htmlunit-2.19.jar;lib/htmlunit-core-js-2.17.jar;lib/httpclient-4.5.1.jar;lib/httpcore-4.4.3.jar;lib/httpmime-4.5.1.jar
set CP=%CP%;lib/jetty-io-9.2.13.v20150730.jar;lib/jetty-util-9.2.13.v20150730.jar
set CP=%CP%;lib/nekohtml-1.9.22.jar;lib/sac-1.3.jar;lib/serializer-2.7.2.jar
set CP=%CP%;lib/websocket-api-9.2.13.v20150730.jar;lib/websocket-client-9.2.13.v20150730.jar;lib/websocket-common-9.2.13.v20150730.jar
set CP=%CP%;lib/xalan-2.7.2.jar;lib/xercesImpl-2.11.0.jar;lib/xml-apis-1.4.01.jar
set Q=lib/sqlite-jdbc-3.8.11.2.jar
set E=lib/org.eclipse.jdt.annotation_2.0.0.v20140415-1436.jar
set CP=%CP%;%Q%
rem echo %*
java -Dorg.sqlite.lib.path=lib -Xms100m -Xmx150m -esa -ea -cp %CP%;bin/ com.pinternals.ir.NotesRetriever -us0000000000 -c C:\sap\notes-cache %*
