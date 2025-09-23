package com.example.pis.repository;

import com.example.pis.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for CRUD operations on {@link PaymentTransaction} entities.
 * 
 * <p>Exposes a custom finder to look up transactions by their unique reference.
 * Spring Data JPA automatically implements this interface at runtime.</p>
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find a payment transaction by its unique reference code.
     *
     * @param reference Unique transaction reference
     * @return Optional containing the transaction if found, empty otherwise
     */
    Optional<PaymentTransaction> findByReference(String reference);
}
