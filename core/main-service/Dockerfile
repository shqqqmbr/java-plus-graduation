FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/main-service-*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]