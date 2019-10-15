FROM maven:3-jdk-11
WORKDIR /build

COPY . /build
RUN mvn -DskipTests install dependency:copy-dependencies

FROM openjdk:11

WORKDIR /app

COPY --from=0 /build/sccp-demos/target/demo-*.jar /app/
COPY --from=0 /build/sccp-demos/target/dependency/ /app/dependency/

COPY ./demo/src/main/script /script

CMD tail -f /dev/null

