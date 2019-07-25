FROM adoptopenjdk/openjdk11:latest

RUN useradd --home /home/pleo --shell /bin/false pleo &&\
    apt-get update && \
    apt-get install -y sqlite3

USER pleo

COPY . /anteus
WORKDIR /anteus

EXPOSE 7000
# When the container starts: build, test and run the app.
CMD ./gradlew build && ./gradlew test && ./gradlew run
