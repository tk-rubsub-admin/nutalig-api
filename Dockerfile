# Use official base image of Java Runtime
FROM amazoncorretto:21.0.1-alpine

# Set volume point to /tmp
VOLUME /tmp

COPY src/main/resources/report /app/report

# Install dependencies for handling fonts
# Install dependencies for font configuration
RUN apk update && apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    ttf-freefont \
    && apk add --no-cache --virtual .build-deps \
    curl

# Copy the font to the appropriate location
COPY src/main/resources/report/THSarabun.ttf /usr/share/fonts/truetype/
COPY src/main/resources/report/Sarabun.ttf /usr/share/fonts/truetype/

# Update font cache
RUN fc-cache -fv

# Set environment variables for headless operation (useful for servers)
ENV JAVA_OPTS="-Djava.awt.headless=true"

# Install other necessary utilities, if any
RUN apk del .build-deps

# Set application's JAR file
ARG JAR_FILE=target/api.jar

# Add the application's JAR file to the container
ADD ${JAR_FILE} app.jar

# Expose port
EXPOSE 8001

# Run the JAR file
ENTRYPOINT java -jar /app.jar
