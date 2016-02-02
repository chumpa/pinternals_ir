@echo off
%JAVA_HOME%\bin\xjc -mark-generated -extension -p com.sap xsd 
%JAVA_HOME%\bin\xjc -mark-generated -extension -p com.sap.lpad xsd_lpad
%JAVA_HOME%\bin\xjc -mark-generated -extension -p com.sap1 xsd_abap1 
rem "c:\Program Files\Java\jdk1.8.0_66\bin\xjc" -mark-generated -extension -p com.sap1a xsd_abap1a 
%JAVA_HOME%\bin\xjc -mark-generated -extension -p com.sap2 xsd_abap2
%JAVA_HOME%\bin\xjc -mark-generated -extension -p com.sap3 xsd_abap3
%JAVA_HOME%\bin\xjc -mark-generated -extension -p com.sap.err xsd_err
