/* Licensed under Apache-2.0 2022-2023 */
package com.example.paymentservice.repositories;

import com.example.paymentservice.entities.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByName(String name);
}
