mvn clean
rm -rfv /opt/tomcat/webapps/rpct-masking*
mvn package
cp -av target/rpct-masking*.war /opt/tomcat/webapps/
mvn install