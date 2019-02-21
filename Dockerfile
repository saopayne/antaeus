FROM adoptopenjdk/openjdk11:latest

# Install dependencies
RUN apt-get update && apt-get install -y sqlite3

# Expose the app port.
EXPOSE 7000

# Setup app user.
RUN useradd --home /home/pleo --shell /bin/false pleo

# Switch to app workdir.
WORKDIR /home/pleo

# Copy over source code.
COPY . /home/pleo

# When the container starts: build, test and run the app.
CMD ./gradlew build && ./gradlew test && ./gradlew run
