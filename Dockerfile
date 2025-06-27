FROM tomcat:9.0-jdk17

# sterg app default din Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

#copiez fisierul war in directorul webapps al Tomcat
COPY target/ama.war /usr/local/tomcat/webapps/ama.war

# Expune portul 8080
EXPOSE 8080

# Pornește Tomcat
CMD ["catalina.sh", "run"]