call c:\progr\apache-maven-3.8.4\bin\mvn package
set JAVA_HOME=c:\progr\java\jdk-17.0.8
echo "COMPILE OK"
del ..\keycloak-23.0.1\providers\gr.hcg.papas.keycloak-ggps.jar
echo "DEL OK"
copy /Y target\gr.hcg.papas.keycloak-ggps.jar ..\keycloak-23.0.1\providers 

echo "COPY OK"