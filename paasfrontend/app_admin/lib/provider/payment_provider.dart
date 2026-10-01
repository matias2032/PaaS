import 'package:flutter/foundation.dart';
import '../model/payment_model.dart';
import '../repository/payment_repository.dart';
import '../service/api_exception.dart';

// Payment module provider — invoices are always scoped to one
// organization at a time (mirrors how the admin never lists invoices
// across every organization at once, only per-org via
// listInvoicesByOrganizationAsAdmin). Payment methods are a small
// catalog, loaded once and cached for the lifetime of the session.
class PaymentProvider extends ChangeNotifier {
  final PaymentRepository _repository;

  PaymentProvider({PaymentRepository? repository})
      : _repository = repository ?? PaymentRepository();

  // ── State: invoices (scoped to the org currently being viewed) ────

  List<InvoiceModel> _invoices = [];
  String? _loadedOrgPublicUuid;
  bool _isLoadingInvoices = false;
  bool _hasLoadedInvoices = false;
  String? _invoicesErrorMessage;

  // publicUuids (invoice or payment) with a mutation in flight.
  final Set<String> _updating = {};

  // ── State: payment methods catalog ─────────────────────────────────

  List<PaymentMethodModel> _paymentMethods = [];
  bool _isLoadingMethods = false;
  String? _methodsErrorMessage;

  // ── Getters ──────────────────────────────────────────────────────

  List<InvoiceModel> get invoices => _invoices;
  String? get loadedOrgPublicUuid => _loadedOrgPublicUuid;
  bool get isLoadingInvoices => _isLoadingInvoices;
  bool get hasLoadedInvoices => _hasLoadedInvoices;
  String? get invoicesErrorMessage => _invoicesErrorMessage;

  List<PaymentMethodModel> get paymentMethods => _paymentMethods;
  bool get isLoadingMethods => _isLoadingMethods;
  String? get methodsErrorMessage => _methodsErrorMessage;

  bool isUpdating(String publicUuid) => _updating.contains(publicUuid);

  InvoiceModel? invoiceByUuid(String publicUuid) {
    for (final invoice in _invoices) {
      if (invoice.publicUuid == publicUuid) return invoice;
    }
    return null;
  }

  // The invoice that currently holds a given payment, if loaded.
  InvoiceModel? invoiceContainingPayment(String paymentPublicUuid) {
    for (final invoice in _invoices) {
      for (final payment in invoice.payments) {
        if (payment.publicUuid == paymentPublicUuid) return invoice;
      }
    }
    return null;
  }

  // ── Invoices ─────────────────────────────────────────────────────

  Future<void> loadInvoices(String orgPublicUuid) async {
    _isLoadingInvoices = true;
    _invoicesErrorMessage = null;
    notifyListeners();

    try {
      _invoices = await _repository.listInvoicesByOrganizationAsAdmin(orgPublicUuid);
      _loadedOrgPublicUuid = orgPublicUuid;
    } catch (e) {
      _invoicesErrorMessage = _messageFrom(e);
    }

    _isLoadingInvoices = false;
    _hasLoadedInvoices = true;
    notifyListeners();
  }

  Future<bool> markInvoicePaid(
    String invoicePublicUuid, {
    required String paymentMethodCode,
    String? transactionReference,
    String? reason,
  }) {
    return _runOnInvoice(invoicePublicUuid, (invoice) async {
      final updated = await _repository.markInvoicePaidAsAdmin(
        invoice,
        paymentMethodCode: paymentMethodCode,
        transactionReference: transactionReference,
        reason: reason,
      );
      _replaceInvoice(updated);
    });
  }

  Future<bool> refundPayment(
    String paymentPublicUuid, {
    required String reason,
  }) async {
    _invoicesErrorMessage = null;
    _updating.add(paymentPublicUuid);
    notifyListeners();

    try {
      final invoice = invoiceContainingPayment(paymentPublicUuid);
      final payment = invoice?.payments.firstWhere(
        (p) => p.publicUuid == paymentPublicUuid,
      );
      if (invoice == null || payment == null) {
        throw ApiException(
          statusCode: 404,
          message: 'Payment not found. Reload the list and try again.',
        );
      }

      final refunded = await _repository.refundPaymentAsAdmin(
        payment,
        reason: reason,
      );

      final updatedPayments = invoice.payments
          .map((p) => p.publicUuid == refunded.publicUuid ? refunded : p)
          .toList();
      _replaceInvoice(invoice.copyWith(payments: updatedPayments));

      return true;
    } catch (e) {
      _invoicesErrorMessage = _messageFrom(e);
      return false;
    } finally {
      _updating.remove(paymentPublicUuid);
      notifyListeners();
    }
  }

  // ── Payment methods catalog ─────────────────────────────────────

  Future<void> loadPaymentMethods() async {
    if (_paymentMethods.isNotEmpty) return; // cached for the session
    _isLoadingMethods = true;
    _methodsErrorMessage = null;
    notifyListeners();

    try {
      _paymentMethods = await _repository.listPaymentMethods();
    } catch (e) {
      _methodsErrorMessage = _messageFrom(e);
    }

    _isLoadingMethods = false;
    notifyListeners();
  }

  // ── Housekeeping ──────────────────────────────────────────────────

  void clearErrors() {
    _invoicesErrorMessage = null;
    _methodsErrorMessage = null;
    notifyListeners();
  }

  /// Drops everything held in memory — call on logout.
  void reset() {
    _invoices = [];
    _loadedOrgPublicUuid = null;
    _isLoadingInvoices = false;
    _hasLoadedInvoices = false;
    _invoicesErrorMessage = null;
    _paymentMethods = [];
    _isLoadingMethods = false;
    _methodsErrorMessage = null;
    _updating.clear();
    notifyListeners();
  }

  // ── Internal ──────────────────────────────────────────────────────

  Future<bool> _runOnInvoice(
    String publicUuid,
    Future<void> Function(InvoiceModel invoice) action,
  ) async {
    _invoicesErrorMessage = null;
    _updating.add(publicUuid);
    notifyListeners();

    try {
      final invoice = invoiceByUuid(publicUuid);
      if (invoice == null) {
        throw ApiException(
          statusCode: 404,
          message: 'Invoice not found. Reload the list and try again.',
        );
      }
      await action(invoice);
      return true;
    } catch (e) {
      _invoicesErrorMessage = _messageFrom(e);
      return false;
    } finally {
      _updating.remove(publicUuid);
      notifyListeners();
    }
  }

  void _replaceInvoice(InvoiceModel updated) {
    _invoices = _invoices
        .map((i) => i.publicUuid == updated.publicUuid ? updated : i)
        .toList();
  }

  String _messageFrom(Object error) {
    if (error is ApiException) return error.message;
    return 'Unexpected error. Please try again.';
  }
}