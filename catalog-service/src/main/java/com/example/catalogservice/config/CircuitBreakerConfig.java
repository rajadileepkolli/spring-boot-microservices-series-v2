/***
<p>
    Licensed under MIT License Copyright (c) 2023 Raja Kolli.
</p>
***/

package com.example.catalogservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CircuitBreakerConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Bean
    RegistryEventConsumer<CircuitBreaker> myRegistryEventConsumer() {

        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                entryAddedEvent
                        .getAddedEntry()
                        .getEventPublisher()
                        .onEvent(event -> log.info("CircuitBreaker EntryAddedEvent : {}", event));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                entryRemoveEvent
                        .getRemovedEntry()
                        .getEventPublisher()
                        .onEvent(event -> log.info("CircuitBreaker EntryRemovedEvent : {}", event));
            }

            @Override
            public void onEntryReplacedEvent(
                    EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                entryReplacedEvent
                        .getOldEntry()
                        .getEventPublisher()
                        .onEvent(
                                event ->
                                        log.info(
                                                "CircuitBreaker EntryReplacedEvent Old Entry :{}",
                                                event));
                entryReplacedEvent
                        .getNewEntry()
                        .getEventPublisher()
                        .onEvent(
                                event ->
                                        log.info(
                                                "CircuitBreaker EntryReplacedEvent New Entry :{}",
                                                event));
            }
        };
    }

    @Bean
    RegistryEventConsumer<Retry> myRetryRegistryEventConsumer() {

        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                entryAddedEvent
                        .getAddedEntry()
                        .getEventPublisher()
                        .onEvent(event -> log.info("Retry EntryAddedEvent : {}", event));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
                entryRemoveEvent
                        .getRemovedEntry()
                        .getEventPublisher()
                        .onEvent(event -> log.info("Retry EntryRemovedEvent : {}", event));
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                entryReplacedEvent
                        .getOldEntry()
                        .getEventPublisher()
                        .onEvent(
                                event -> log.info("Retry EntryReplacedEvent Old Entry :{}", event));
                entryReplacedEvent
                        .getNewEntry()
                        .getEventPublisher()
                        .onEvent(
                                event -> log.info("Retry EntryReplacedEvent New Entry :{}", event));
            }
        };
    }
}
