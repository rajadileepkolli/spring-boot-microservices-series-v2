

Code generated using [springboot generator](https://github.com/sivaprasadreddy/generator-springboot)

To run in local first build all services using command 

mvnw clean spring-boot:build-image -DskipTests

To start docker using 

docker-compose up

To start silently use docker-compose -d up , which starts in detached mode

### Starting infrastructure 

docker compose up rabbitmq zipkin-server postgresqldb mongo

### URLS to access services
 - Zipkin : http://localhost:9411/zipkin/
 - RabbitMq : http://localhost:15672/
 - Service Registry : http://localhost:8761/