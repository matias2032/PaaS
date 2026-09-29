package com.dev58.paasbackend.payment.repository;

import com.dev58.paasbackend.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByCode(String code);

    boolean existsByCode(String code);
}