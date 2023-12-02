/***
<p>
    Licensed under MIT License Copyright (c) 2021-2022 Raja Kolli.
</p>
***/

package com.example.inventoryservice.config;

import com.example.inventoryservice.entities.Inventory;
import com.example.inventoryservice.repositories.InventoryRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("local")
public class Initializer implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) {
        log.info("Running Initializer.....");
        if (this.inventoryRepository.count() == 0) {
            SecureRandom r = new SecureRandom();
            List<Inventory> inventoryList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int count = r.nextInt(1000);
                Inventory inventory =
                        new Inventory().setProductCode("Product" + i).setAvailableQuantity(count);
                inventoryList.add(inventory);
            }
            inventoryRepository.saveAll(inventoryList);
        }
    }
}
