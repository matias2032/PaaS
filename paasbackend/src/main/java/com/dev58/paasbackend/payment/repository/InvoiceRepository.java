package com.dev58.paasbackend.payment.repository;

import com.dev58.paasbackend.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByPublicUuid(UUID publicUuid);

    List<Invoice> findBySubscription_IdSubscription(Long idSubscription);

    // Admin listing: every invoice of an organization, across all of
    // its subscriptions (past and current).
    List<Invoice> findBySubscription_Organization_IdOrganization(Long idOrganization);

    boolean existsByInvoiceNumber(String invoiceNumber);

    // Backs invoice_number generation (INV-{year}-{sequence}). A DB
    // sequence is used instead of counting rows so concurrent creations
    // can never receive the same number.
    @Query(value = "SELECT nextval('paas_platform.invoice_number_seq')", nativeQuery = true)
    long nextInvoiceSequenceValue();
}