FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]