package com.dev58.paasbackend.payment.repository;

import com.dev58.paasbackend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPublicUuid(UUID publicUuid);

    List<Payment> findByInvoice_IdInvoice(Long idInvoice);

    // Admin listing: every payment of an organization, across all of
    // its invoices.
    List<Payment> findByInvoice_Subscription_Organization_IdOrganization(Long idOrganization);
}