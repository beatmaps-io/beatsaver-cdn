FROM gradle:6.7-jdk15
WORKDIR /build
COPY . .
RUN gradle installDist

FROM openjdk:15.0.1-jdk-slim
EXPOSE 8080

RUN apt-get update
RUN apt-get install -y curl

WORKDIR /app
COPY --from=0 /build/build/install/BeatMaps-CDN ./

WORKDIR /app/bin
ENTRYPOINT ["./BeatMaps-CDN"]
