/***
<p>
    Licensed under MIT License Copyright (c) 2021-2023 Raja Kolli.
</p>
***/

package com.example.catalogservice.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

import com.example.catalogservice.common.AbstractCircuitBreakerTest;
import com.example.catalogservice.entities.Product;
import com.example.catalogservice.model.response.InventoryDto;
import com.example.catalogservice.model.response.PagedResult;
import com.example.catalogservice.repositories.ProductRepository;
import com.example.common.dtos.ProductDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;

class ProductControllerIT extends AbstractCircuitBreakerTest {

    @Autowired private ProductRepository productRepository;

    public static MockWebServer mockWebServer;

    private List<Product> savedProductList = new ArrayList<>();

    @BeforeAll
    static void setUpServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void backendProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "application.inventory-service-url", () -> mockWebServer.url("/").url().toString());
    }

    @BeforeEach
    void setUp() {

        List<Product> productList =
                List.of(
                        new Product(null, "P001", "name 1", "description 1", 9.0, false),
                        new Product(null, "P002", "name 2", "description 2", 10.0, false),
                        new Product(null, "P003", "name 3", "description 3", 11.0, false));
        savedProductList =
                productRepository
                        .deleteAll()
                        .thenMany(productRepository.saveAll(productList))
                        .thenMany(productRepository.findAll())
                        .collectList()
                        .block();
    }

    @Test
    void shouldFetchAllProducts() throws JsonProcessingException {

        mockBackendEndpoint(
                200,
                objectMapper.writeValueAsString(
                        List.of(
                                new InventoryDto("P001", 0),
                                new InventoryDto("P002", 0),
                                new InventoryDto("P003", 0))));

        webTestClient
                .get()
                .uri("/api/catalog?pageSize=2&pageNo=1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(PagedResult.class)
                .consumeWith(
                        response -> {
                            PagedResult<Product> pagedResult = response.getResponseBody();
                            assertThat(pagedResult).isNotNull();
                            assertThat(pagedResult.isFirst()).isFalse();
                            assertThat(pagedResult.isLast()).isTrue();
                            assertThat(pagedResult.hasNext()).isFalse();
                            assertThat(pagedResult.hasPrevious()).isTrue();
                            assertThat(pagedResult.totalElements()).isEqualTo(3);
                            assertThat(pagedResult.pageNumber()).isEqualTo(2);
                            assertThat(pagedResult.totalPages()).isEqualTo(2);
                            assertThat(pagedResult.data().size()).isEqualTo(1);
                        });
    }

    @Test
    void shouldFetchAllProductsAsEmpty() {

        productRepository.deleteAll().block();
        webTestClient
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder.queryParam("sortDir", "desc");
                            uriBuilder.path("/api/catalog");
                            return uriBuilder.build();
                        })
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(PagedResult.class)
                .value(
                        response ->
                                assertAll(
                                        () -> assertTrue(response.isFirst()),
                                        () -> assertTrue(response.isLast()),
                                        () -> assertFalse(response.hasNext()),
                                        () -> assertFalse(response.hasPrevious()),
                                        () -> assertEquals(0, response.totalElements()),
                                        () -> assertEquals(1, response.pageNumber()),
                                        () -> assertEquals(0, response.totalPages()),
                                        () -> assertEquals(0, response.data().size())));
    }

    @Test
    void shouldFetchAllProductsWithCircuitBreaker() {

        transitionToOpenState("getInventoryByProductCodes");
        circuitBreakerRegistry
                .circuitBreaker("getInventoryByProductCodes")
                .transitionToHalfOpenState();

        mockBackendEndpoint(
                500, """
        {"message":"Product with id 100 not found"}
        """);

        webTestClient
                .get()
                .uri("/api/catalog")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(PagedResult.class)
                .value(
                        response ->
                                assertAll(
                                        () -> assertTrue(response.isFirst()),
                                        () -> assertTrue(response.isLast()),
                                        () -> assertFalse(response.hasNext()),
                                        () -> assertFalse(response.hasPrevious()),
                                        () -> assertEquals(3, response.totalElements()),
                                        () -> assertEquals(1, response.pageNumber()),
                                        () -> assertEquals(1, response.totalPages()),
                                        () -> assertEquals(3, response.data().size())));

        // Then, As it is still failing state should not change
        checkHealthStatus("getInventoryByProductCodes", CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    void shouldFindProductById() throws JsonProcessingException {

        Product product = savedProductList.get(0);
        Long productId = product.getId();
        mockBackendEndpoint(
                200, objectMapper.writeValueAsString(new InventoryDto(product.getCode(), 0)));
        webTestClient
                .get()
                .uri("/api/catalog/id/{id}", productId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(product.getId())
                .jsonPath("$.code")
                .isEqualTo(product.getCode())
                .jsonPath("$.productName")
                .isEqualTo(product.getProductName())
                .jsonPath("$.description")
                .isEqualTo(product.getDescription())
                .jsonPath("$.price")
                .isEqualTo(product.getPrice());
    }

    @Test
    @DisplayName("temporarily reduced to 1 retry")
    void shouldFindProductWhenInventoryIsDown() {

        Product product = savedProductList.get(0);
        Long productId = product.getId();
        mockBackendEndpoint(
                500,
                """
                                        {"message":"Product with id 100 not found"}
                                        """);
        webTestClient
                .get()
                .uri("/api/catalog/id/{id}", productId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(product.getId())
                .jsonPath("$.code")
                .isEqualTo(product.getCode())
                .jsonPath("$.productName")
                .isEqualTo(product.getProductName())
                .jsonPath("$.description")
                .isEqualTo(product.getDescription())
                .jsonPath("$.price")
                .isEqualTo(product.getPrice())
                .jsonPath("$.inStock")
                .isEqualTo(false);
    }

    @Test
    void shouldNotFindProductById() {
        Long productId = 100L;

        webTestClient
                .get()
                .uri("/api/catalog/id/{id}", productId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectHeader()
                .contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("https://api.microservices.com/errors/not-found")
                .jsonPath("$.title")
                .isEqualTo("Product Not Found")
                .jsonPath("$.status")
                .isEqualTo(404)
                .jsonPath("$.detail")
                .isEqualTo("Product with id 100 not found")
                .jsonPath("$.instance")
                .isEqualTo("/api/catalog/id/100")
                .jsonPath("$.timestamp")
                .isNotEmpty()
                .jsonPath("$.errorCategory")
                .isEqualTo("Generic");
    }

    @Test
    void shouldFindProductByProductCode() {
        Product product = savedProductList.get(0);

        webTestClient
                .get()
                .uri("/api/catalog/productCode/{productCode}", product.getCode())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(product.getId())
                .jsonPath("$.code")
                .isEqualTo(product.getCode())
                .jsonPath("$.productName")
                .isEqualTo(product.getProductName())
                .jsonPath("$.description")
                .isEqualTo(product.getDescription())
                .jsonPath("$.price")
                .isEqualTo(product.getPrice());
    }

    @Test
    void productsShouldExistsByProductCodes() {
        List<String> productCodeList = savedProductList.stream().map(Product::getCode).toList();

        webTestClient
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder.queryParam("productCodes", productCodeList);
                            uriBuilder.path("/api/catalog/exists");
                            return uriBuilder.build();
                        })
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(Boolean.class)
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void productsShouldNotExistsByProductCodes() {
        List<String> productCodeList = List.of("P1", "P2", "P3", "P4", "P5");

        webTestClient
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder.queryParam("productCodes", productCodeList);
                            uriBuilder.path("/api/catalog/exists");
                            return uriBuilder.build();
                        })
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(Boolean.class)
                .isEqualTo(Boolean.FALSE);
    }

    @Test
    void shouldCreateNewProduct() {
        ProductDto productDto = new ProductDto("code 4", "name 4", "description 4", 19.0);
        webTestClient
                .post()
                .uri("/api/catalog")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(productDto), ProductDto.class)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .exists("Location")
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id")
                .isNotEmpty()
                .jsonPath("$.code")
                .isEqualTo(productDto.code())
                .jsonPath("$.productName")
                .isEqualTo(productDto.productName())
                .jsonPath("$.description")
                .isEqualTo(productDto.description())
                .jsonPath("$.price")
                .isEqualTo(productDto.price());
    }

    @Test
    void shouldThrowConflictForCreateNewProduct() {
        ProductDto productDto = new ProductDto("P001", "name 4", "description 4", 19.0);

        webTestClient
                .post()
                .uri("/api/catalog")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(productDto), ProductDto.class)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("https://api.microservices.com/errors/already-exists")
                .jsonPath("$.title")
                .isEqualTo("Product Already Exists")
                .jsonPath("$.status")
                .isEqualTo(409)
                .jsonPath("$.detail")
                .isEqualTo("Product with id P001 already Exists")
                .jsonPath("$.instance")
                .isEqualTo("/api/catalog")
                .jsonPath("$.timestamp")
                .isNotEmpty()
                .jsonPath("$.errorCategory")
                .isEqualTo("Generic");
    }

    @Test
    void shouldReturn400WhenCreateNewProductWithoutCode() {
        ProductDto productDto = new ProductDto(null, null, null, null);

        webTestClient
                .post()
                .uri("/api/catalog")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("about:blank")
                .jsonPath("$.title")
                .isEqualTo("Bad Request")
                .jsonPath("$.status")
                .isEqualTo(400)
                .jsonPath("$.detail")
                .isEqualTo("Invalid request content.")
                .jsonPath("$.instance")
                .isEqualTo("/api/catalog");
    }

    @Test
    void shouldUpdateProduct() {
        Product product = savedProductList.get(0);
        product.setDescription("Updated Catalog");

        webTestClient
                .put()
                .uri("/api/catalog/{id}", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(product), Product.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .value(is(product.getId().intValue()))
                .jsonPath("$.code")
                .value(is(product.getCode()))
                .jsonPath("$.productName")
                .value(is(product.getProductName()))
                .jsonPath("$.description")
                .value(is(product.getDescription()))
                .jsonPath("$.price")
                .value(is(product.getPrice()));
    }

    @Test
    void shouldDeleteProduct() {
        Product product = savedProductList.get(0);

        webTestClient
                .delete()
                .uri("/api/catalog/{id}", product.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(product.getId())
                .jsonPath("$.code")
                .isEqualTo(product.getCode())
                .jsonPath("$.productName")
                .isEqualTo(product.getProductName())
                .jsonPath("$.description")
                .isEqualTo(product.getDescription())
                .jsonPath("$.price")
                .isEqualTo(product.getPrice());
    }

    private void mockBackendEndpoint(int responseCode, String body) {
        MockResponse mockResponse =
                new MockResponse()
                        .setResponseCode(responseCode)
                        .setBody(body)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        mockWebServer.enqueue(mockResponse);
    }
}
