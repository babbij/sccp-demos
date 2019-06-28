FROM openjdk:11
WORKDIR /app

#COPY ./target/demo-0.1-SNAPSHOT.jar /app
COPY ./lib /app/lib
COPY ../lib/kpabe/lib/native/ /native

CMD java -jar *.jar
