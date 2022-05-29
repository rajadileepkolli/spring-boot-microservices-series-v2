/* (C)2022 */
package com.example.inventoryservice.web.controllers;

import com.example.inventoryservice.dtos.InventoryDto;
import com.example.inventoryservice.entities.Inventory;
import com.example.inventoryservice.services.InventoryService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<Inventory> getAllInventories() {
        return inventoryService.findAllInventories();
    }

    @GetMapping("/{productCode}")
    // @Retry(name = "inventory-api", fallbackMethod = "hardcodedResponse")
    // @CircuitBreaker(name = "default", fallbackMethod = "hardcodedResponse")
    @RateLimiter(name = "default")
    // @Bulkhead(name = "inventory-api")
    public ResponseEntity<Inventory> getInventoryByProductCode(@PathVariable String productCode) {
        return inventoryService
                .findInventoryByProductCode(productCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Inventory createInventory(@RequestBody @Validated InventoryDto inventoryDto) {
        return inventoryService.saveInventory(inventoryDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inventory> updateInventory(
            @PathVariable Long id, @RequestBody Inventory inventory) {
        return inventoryService
                .findInventoryById(id)
                .map(
                        inventoryObj -> {
                            inventory.setId(id);
                            return ResponseEntity.ok(inventoryService.updateInventory(inventory));
                        })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Inventory> deleteInventory(@PathVariable Long id) {
        return inventoryService
                .findInventoryById(id)
                .map(
                        inventory -> {
                            inventoryService.deleteInventoryById(id);
                            return ResponseEntity.ok(inventory);
                        })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
