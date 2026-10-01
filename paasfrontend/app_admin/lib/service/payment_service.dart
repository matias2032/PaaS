import '../config/api_config.dart';
import '../model/payment_model.dart';
import 'api_client.dart';

// Payment module service — thin HTTP layer over ApiClient. Admin-only:
// every call here is guarded by @PreAuthorize on PaymentController
// (SUPPORT for reads, PLATFORM_ADMIN for mark-paid/refund). The
// client-facing endpoints (submitPayment, listInvoices for one's own
// org) are OWNER-only backend-side and are intentionally not wrapped
// here — the admin app never calls them.
//
// NOTE: mark-paid and refund are PATCH server-side. If ApiClient has
// no patch() method yet, add one mirroring put() before wiring the
// screens that call these.
class PaymentService {
  Future<List<InvoiceModel>> listInvoicesByOrganizationAsAdmin(
    String orgPublicUuid,
  ) async {
    final json = await ApiClient.get(
      ApiConfig.adminOrganizationInvoicesUrl(orgPublicUuid),
    );
    return (json as List)
        .map((item) => InvoiceModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<InvoiceModel> markInvoicePaidAsAdmin(
    String invoicePublicUuid,
    AdminMarkPaidRequest request,
  ) async {
    final json = await ApiClient.patch(
      ApiConfig.adminInvoiceMarkPaidUrl(invoicePublicUuid),
      body: request.toJson(),
    );
    return InvoiceModel.fromJson(json as Map<String, dynamic>);
  }

  Future<PaymentModel> refundPaymentAsAdmin(
    String paymentPublicUuid,
    AdminRefundRequest request,
  ) async {
    final json = await ApiClient.patch(
      ApiConfig.adminPaymentRefundUrl(paymentPublicUuid),
      body: request.toJson(),
    );
    return PaymentModel.fromJson(json as Map<String, dynamic>);
  }

  // Catalog, read-only.
  Future<List<PaymentMethodModel>> listPaymentMethods() async {
    final json = await ApiClient.get(ApiConfig.paymentMethodsUrl);
    return (json as List)
        .map((item) => PaymentMethodModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }
}