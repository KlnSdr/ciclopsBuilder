FROM docker.klnsdr.com/nyx-cli:1.3 as builder

WORKDIR /app

COPY . .

RUN nyx build

FROM eclipse-temurin:21-jdk

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y git podman iptables

WORKDIR /app

COPY --from=builder /app/build/ciclpopsBuilder-1.0.jar /app/app.jar

CMD ["java", "-jar", "app.jar"]