FROM openjdk:8-jdk

# inotify-tools and net-tools are used to restart service on file change in development mode
RUN apt-get update && apt-get install -y inotify-tools net-tools

WORKDIR /opt/app
COPY ./ ./

RUN ./gradlew --no-daemon shadowJar

CMD /opt/app/bin/production.sh
