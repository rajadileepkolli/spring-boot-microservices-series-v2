[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/rajadilipkolli/spring-boot-microservices-series-v2)

Initial Code generated using [springboot generator](https://github.com/sivaprasadreddy/generator-springboot)

### Local SetUp

To run in local first build all services using command 

```bash
mvnw clean spotless:apply spring-boot:build-image -DskipTests
```

To start docker using 

```bash
docker compose up
```

To start silently use `docker compose -d up` , which starts in detached mode

### Starting infrastructure 

```bash
docker compose up rabbitmq zipkin-server postgresql pgadmin4 mongodb kafka config-server naming-server
```

### URLs to access pieces of software
 - Zipkin : http://localhost:9411/zipkin/
 - RabbitMq : http://localhost:15672/
 - Service Registry : http://localhost:8761
 - PgAdmin : http://localhost:5050

### Swagger URLs for accessing Services

| **Service Name**  | **URL**                                                  |
|-------------------|----------------------------------------------------------|
| api gateway       | http://localhost:8765/api-gateway/swagger-ui.html        |
| catalog service   | http://localhost:18080/catalog-service/swagger-ui.html   |
| inventory service | http://localhost:18181/inventory-service/swagger-ui.html |
| order service     | http://localhost:18282/order-service/swagger-ui.html     |
| payment service   | http://localhost:18085/payment-service/swagger-ui.html   |


#### References
  - https://piotrminkowski.com/2022/01/24/distributed-transactions-in-microservices-with-kafka-streams-and-spring-boot/
  
#### Projects unable to convert to native Image OOTB
 - config-server (Due to presence of Netty)
 - api-gateway (Due to presence of Netty)

#### Breaking Changes in 3.0
 - Migration to jakarta namespace from javax
 - Spring Cloud Seluth is deprecated in favor of Micrometer
 - With New Observability we cant use Rabbit as sender type and use asynchronous communication
