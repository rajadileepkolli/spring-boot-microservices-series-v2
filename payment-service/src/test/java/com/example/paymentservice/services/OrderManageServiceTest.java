/* Licensed under Apache-2.0 2023 */
package com.example.paymentservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.common.dtos.OrderDto;
import com.example.common.dtos.OrderItemDto;
import com.example.paymentservice.data.TestData;
import com.example.paymentservice.entities.Customer;
import com.example.paymentservice.exception.CustomerNotFoundException;
import com.example.paymentservice.repositories.CustomerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class OrderManageServiceTest {

    @Mock private CustomerRepository customerRepository;

    @Mock private KafkaTemplate<String, OrderDto> kafkaTemplate;

    @InjectMocks private OrderManageService orderManageService;

    @Test
    void testConfirmWithValidOrder() {
        // Arrange
        OrderDto orderDto = new OrderDto();
        orderDto.setCustomerId(1L);
        orderDto.setStatus("CONFIRMED");
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setProductPrice(BigDecimal.TEN);
        orderItemDto.setQuantity(10);
        orderDto.setItems(List.of(orderItemDto));
        Customer customer = TestData.getCustomer();
        given(customerRepository.findById(orderDto.getCustomerId()))
                .willReturn(Optional.of(customer));
        given(customerRepository.save(any(Customer.class)))
                .willAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        // Act
        orderManageService.confirm(orderDto);

        // Assert
        assertThat(customer.getAmountReserved()).isEqualTo(0);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testConfirmWithRejectedOrder() {
        // Arrange
        OrderDto orderDto = new OrderDto();
        orderDto.setCustomerId(1L);
        orderDto.setStatus("ROLLBACK");
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setProductPrice(BigDecimal.TEN);
        orderItemDto.setQuantity(10);
        orderDto.setItems(List.of(orderItemDto));
        Customer customer = TestData.getCustomer();
        given(customerRepository.findById(orderDto.getCustomerId()))
                .willReturn(Optional.of(customer));
        given(customerRepository.save(any(Customer.class)))
                .willAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // Act
        orderManageService.confirm(orderDto);

        // Assert
        assertThat(customer.getAmountReserved()).isEqualTo(0);
        assertThat(customer.getAmountAvailable()).isEqualTo(1100);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testConfirmWithInvalidCustomer() {
        // Arrange
        OrderDto orderDto = new OrderDto();
        orderDto.setCustomerId(1L);
        orderDto.setStatus("CONFIRMED");
        given(customerRepository.findById(1L)).willReturn(Optional.empty());

        // Assert
        assertThrows(CustomerNotFoundException.class, () -> orderManageService.confirm(orderDto));
    }

    @Test
    void testReserveWithValidOrderAccepted() {
        // Arrange
        OrderDto orderDto = new OrderDto();
        orderDto.setCustomerId(1L);
        orderDto.setStatus("CONFIRMED");
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setProductPrice(BigDecimal.TEN);
        orderItemDto.setQuantity(10);
        orderDto.setItems(List.of(orderItemDto));
        Customer customer = TestData.getCustomer();
        given(customerRepository.findById(orderDto.getCustomerId()))
                .willReturn(Optional.of(customer));
        given(customerRepository.save(any(Customer.class)))
                .willAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        // Act
        orderManageService.reserve(orderDto);

        // Assert
        assertThat(customer.getAmountReserved()).isEqualTo(200);
        assertThat(customer.getAmountAvailable()).isEqualTo(900);
        assertThat(orderDto.getStatus()).isEqualTo("ACCEPT");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testReserveWithValidOrderRejected() {
        // Arrange
        OrderDto orderDto = new OrderDto();
        orderDto.setCustomerId(1L);
        orderDto.setStatus("CONFIRMED");
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setProductPrice(BigDecimal.TEN);
        orderItemDto.setQuantity(1000);
        orderDto.setItems(List.of(orderItemDto));
        Customer customer = TestData.getCustomer();
        given(customerRepository.findById(orderDto.getCustomerId()))
                .willReturn(Optional.of(customer));
        given(customerRepository.save(any(Customer.class)))
                .willAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        // Act
        orderManageService.reserve(orderDto);

        // Assert
        assertThat(customer.getAmountReserved()).isEqualTo(100);
        assertThat(customer.getAmountAvailable()).isEqualTo(1000);
        assertThat(orderDto.getStatus()).isEqualTo("REJECT");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }
}
