FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY index.html .
COPY style.css .
COPY script.js .
COPY Student.java .

RUN javac Student.java

EXPOSE 8080

CMD ["java", "Student"]