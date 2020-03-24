package com.example.catalogservice.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.catalogservice.entity.Product;
import com.example.catalogservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ProductController.class)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<Product> productList;

    @BeforeEach
    void setUp() {
        this.productList = new ArrayList<>();
        this.productList.add(new Product(1L, "text 1"));
        this.productList.add(new Product(2L, "text 2"));
        this.productList.add(new Product(3L, "text 3"));

        objectMapper.registerModule(new ProblemModule());
        objectMapper.registerModule(new ConstraintViolationProblemModule());
    }

    @Test
    void shouldFetchAllProducts() throws Exception {
        given(productService.findAllProducts()).willReturn(this.productList);

        this.mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(productList.size())));
    }

    @Test
    void shouldFindProductById() throws Exception {
        Long productId = 1L;
        Product product = new Product(productId, "text 1");
        given(productService.findProductById(productId)).willReturn(Optional.of(product));

        this.mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", is(product.getText())))
        ;
    }

    @Test
    void shouldReturn404WhenFetchingNonExistingProduct() throws Exception {
        Long productId = 1L;
        given(productService.findProductById(productId)).willReturn(Optional.empty());

        this.mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());

    }

    @Test
    void shouldCreateNewProduct() throws Exception {
        given(productService.saveProduct(any(Product.class))).willAnswer((invocation) -> invocation.getArgument(0));

        Product product = new Product(1L, "some text");
        this.mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.text", is(product.getText())))
        ;

    }

    @Test
    void shouldReturn400WhenCreateNewProductWithoutText() throws Exception {
        Product product = new Product(null, null);

        this.mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", is("application/problem+json")))
                .andExpect(jsonPath("$.type", is("https://zalando.github.io/problem/constraint-violation")))
                .andExpect(jsonPath("$.title", is("Constraint Violation")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.violations", hasSize(1)))
                .andExpect(jsonPath("$.violations[0].field", is("text")))
                .andExpect(jsonPath("$.violations[0].message", is("Text cannot be empty")))
                .andReturn()
        ;
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        Long productId = 1L;
        Product product = new Product(productId, "Updated text");
        given(productService.findProductById(productId)).willReturn(Optional.of(product));
        given(productService.saveProduct(any(Product.class))).willAnswer((invocation) -> invocation.getArgument(0));

        this.mockMvc.perform(put("/api/products/{id}", product.getId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", is(product.getText())))
                ;

    }

    @Test
    void shouldReturn404WhenUpdatingNonExistingProduct() throws Exception {
        Long productId = 1L;
        given(productService.findProductById(productId)).willReturn(Optional.empty());
        Product product = new Product(productId, "Updated text");

        this.mockMvc.perform(put("/api/products/{id}", productId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isNotFound());

    }

    @Test
    void shouldDeleteProduct() throws Exception {
        Long productId = 1L;
        Product product = new Product(productId, "Some text");
        given(productService.findProductById(productId)).willReturn(Optional.of(product));
        doNothing().when(productService).deleteProductById(product.getId());

        this.mockMvc.perform(delete("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", is(product.getText())))
                ;

    }

    @Test
    void shouldReturn404WhenDeletingNonExistingProduct() throws Exception {
        Long productId = 1L;
        given(productService.findProductById(productId)).willReturn(Optional.empty());

        this.mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNotFound());

    }

}