FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

# Give execute permission to Maven wrapper
RUN chmod +x mvnw

# Build Spring Boot jar
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java","-jar","target/*.jar"]
