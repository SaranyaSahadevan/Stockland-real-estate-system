# STOCKLAND - Real Estate System

A Spring Boot application for managing real estate properties.

## Features
- User authentication and authorization 
- Property management (CRUD)
- RESTful API

## Technologies
- Java 18
- Spring Boot 4.0.2
- Maven
- JPA/Hibernate
- PostgreSQL (Neon service)
- Cloudinary (Cloud service for managing media)


## Project installation
1. Clone the repository

```
git clone https://github.com/SaranyaSahadevan/Stockland-real-estate-system.git
```

2. Configure the database (application.properties)

```
# PostgreSQL Database Connection
spring.datasource.url=your-postgresql-connection-url
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=your-postgresql-username
spring.datasource.password=your-postgresql-password

# Hibernate Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# H2 Web Console (disabled for PostgreSQL)
spring.h2.console.enabled=false

#Cloudinary connection
cloudinary.cloud_name=your-cloud-name
cloudinary.api_key=your-api-key
cloudinary.api_secret=your-api-secret

#Image config
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
server.tomcat.max-swallow-size=-1
spring.servlet.multipart.resolve-lazily=true
server.tomcat.max-parameter-count=1000
server.tomcat.max-file-count=1000
server.tomcat.max-part-count=50
```
   
3. Install the dependencies

 ```
 ./mvnw clean install
 ```

4. Run the application

```
./mvnw spring-boot:run
```


