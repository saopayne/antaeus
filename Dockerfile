FROM adoptopenjdk/openjdk11-openj9:latest

# Setup app user.
RUN useradd --home /home/pleo --shell /bin/false pleo

# Switch to app user.
USER pleo
WORKDIR /home/pleo

# Copy over source code.
COPY --chown=pleo:pleo . /home/pleo

# When the container starts: build, test and run the app.
CMD ./gradlew build && ./gradlew test && ./gradlew run
