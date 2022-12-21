/* Licensed under Apache-2.0 2022 */
package com.example.paymentservice.config;

import com.example.common.dtos.OrderDto;
import com.example.paymentservice.services.OrderManageService;
import com.example.paymentservice.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableKafka
public class KafkaListenerConfig {

    private final OrderManageService orderManageService;

    @KafkaListener(id = "orders", topics = AppConstants.ORDERS_TOPIC, groupId = "payment")
    public void onEvent(OrderDto o) {
        log.info("Received: {}", o);
        if (o.getStatus().equals("NEW")) {
            orderManageService.reserve(o);
        } else {
            orderManageService.confirm(o);
        }
    }
}
