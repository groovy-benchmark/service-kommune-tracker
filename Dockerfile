FROM openjdk:8 as build
COPY . /sk-tracking
WORKDIR /sk-tracking
RUN ./gradlew shadowJar

FROM openjdk:8
RUN mkdir -p /opt/apps/service-kommune-tracking-server
COPY --from=build /sk-tracking/build/libs/sk-tracker-with-dependencies.jar /opt/apps/service-kommune-tracking-server
EXPOSE 5050
ENTRYPOINT ["java", "-jar", "/opt/apps/service-kommune-tracking-server/sk-tracker-with-dependencies.jar"]
