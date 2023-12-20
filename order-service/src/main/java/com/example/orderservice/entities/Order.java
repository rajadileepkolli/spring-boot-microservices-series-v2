/***
<p>
    Licensed under MIT License Copyright (c) 2021-2023 Raja Kolli.
</p>
***/

package com.example.orderservice.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends Auditable<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Long customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;

    private String source;

    @Version private Short version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order setId(Long id) {
        this.id = id;
        return this;
    }

    public Order setCustomerId(Long customerId) {
        this.customerId = customerId;
        return this;
    }

    public Order setStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public Order setSource(String source) {
        this.source = source;
        return this;
    }

    public Order setVersion(Short version) {
        this.version = version;
        return this;
    }

    public Order setItems(List<OrderItem> items) {
        this.items = items;
        return this;
    }

    public void addOrderItem(OrderItem orderItem) {
        items.add(orderItem);
        orderItem.setOrder(this);
    }

    public void removeOrderItem(OrderItem orderItem) {
        items.remove(orderItem);
        orderItem.setOrder(null);
    }
}
