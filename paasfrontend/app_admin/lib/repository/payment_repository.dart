import '../model/payment_model.dart';
import '../service/api_exception.dart';
import '../service/payment_service.dart';

// Payment module repository — client-side business rules on top of
// PaymentService, same role BillingRepository plays for billing.
class PaymentRepository {
  final PaymentService _service;

  PaymentRepository({PaymentService? service})
      : _service = service ?? PaymentService();

  Future<List<InvoiceModel>> listInvoicesByOrganizationAsAdmin(
    String orgPublicUuid,
  ) =>
      _service.listInvoicesByOrganizationAsAdmin(orgPublicUuid);

  Future<List<PaymentMethodModel>> listPaymentMethods() =>
      _service.listPaymentMethods();

  Future<InvoiceModel> markInvoicePaidAsAdmin(
    InvoiceModel invoice, {
    required String paymentMethodCode,
    String? transactionReference,
    String? reason,
  }) {
    if (!InvoiceStatuses.isPayable(invoice.status)) {
      throw _invalid(
        'Only PENDING or OVERDUE invoices can be marked as paid.',
      );
    }

    final code = paymentMethodCode.trim();
    if (code.isEmpty) {
      throw _invalid('A payment method is required.');
    }

    final cleanRef = _cleanOrNull(transactionReference);
    if (cleanRef != null && cleanRef.length > 255) {
      throw _invalid('Transaction reference must be at most 255 characters.');
    }

    final cleanReason = _cleanOrNull(reason);
    if (cleanReason != null && cleanReason.length > 500) {
      throw _invalid('Reason must be at most 500 characters.');
    }

    return _service.markInvoicePaidAsAdmin(
      invoice.publicUuid,
      AdminMarkPaidRequest(
        paymentMethodCode: code,
        transactionReference: cleanRef,
        reason: cleanReason,
      ),
    );
  }

  Future<PaymentModel> refundPaymentAsAdmin(
    PaymentModel payment, {
    required String reason,
  }) {
    if (payment.status == PaymentStatuses.refunded) {
      throw _invalid('This payment was already refunded.');
    }
    if (!PaymentStatuses.isRefundable(payment.status)) {
      throw _invalid('Only PAID payments can be refunded.');
    }

    final cleanReason = reason.trim();
    if (cleanReason.isEmpty) {
      throw _invalid('A reason is required to refund a payment.');
    }
    if (cleanReason.length > 500) {
      throw _invalid('Reason must be at most 500 characters.');
    }

    return _service.refundPaymentAsAdmin(
      payment.publicUuid,
      AdminRefundRequest(reason: cleanReason),
    );
  }

  // ── Internal ────────────────────────────────────────────────────

  String? _cleanOrNull(String? raw) {
    final t = raw?.trim();
    return (t == null || t.isEmpty) ? null : t;
  }

  ApiException _invalid(String message) =>
      ApiException(statusCode: 400, message: message);
}