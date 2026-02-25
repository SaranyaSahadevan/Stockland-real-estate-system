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
```
   
3. Install the dependencies

 ```
 ./mvnw clean install
 ```

4. Run the application

```
./mvnw spring-boot:run
```


