import 'package:flutter/foundation.dart';
import '../model/organization_model.dart';
import '../model/payment_model.dart';
import '../repository/organization_repository.dart';
import '../repository/payment_repository.dart';
import '../service/api_exception.dart';

// Payment module provider — invoices are always scoped to one
// organization at a time (mirrors how the admin never lists invoices
// across every organization at once, only per-org via
// listInvoicesByOrganizationAsAdmin). Payment methods are a small
// catalog, loaded once and cached for the lifetime of the session.
class PaymentProvider extends ChangeNotifier {
  final PaymentRepository _repository;
  final OrganizationRepository _organizationRepository;

  PaymentProvider({
    PaymentRepository? repository,
    OrganizationRepository? organizationRepository,
  })  : _repository = repository ?? PaymentRepository(),
        _organizationRepository =
            organizationRepository ?? OrganizationRepository();

  // ── State: invoices (scoped to the org currently being viewed) ────

  List<InvoiceModel> _invoices = [];
  String? _loadedOrgPublicUuid;
  bool _isLoadingInvoices = false;
  bool _hasLoadedInvoices = false;
  String? _invoicesErrorMessage;

  // Organization of the loaded invoices, when known (picked from the
  // search). Null when the invoices were loaded by UUID only.
  OrganizationModel? _loadedOrganization;

  // ── State: organization search (name or slug) ─────────────────────

  List<OrganizationModel> _organizationResults = [];
  String _organizationSearchTerm = '';
  bool _isSearchingOrganizations = false;
  bool _hasSearchedOrganizations = false;
  String? _organizationSearchError;

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

  OrganizationModel? get loadedOrganization => _loadedOrganization;
  List<OrganizationModel> get organizationResults => _organizationResults;
  bool get isSearchingOrganizations => _isSearchingOrganizations;
  bool get hasSearchedOrganizations => _hasSearchedOrganizations;
  String? get organizationSearchError => _organizationSearchError;

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

  Future<void> loadInvoices(
    String orgPublicUuid, {
    OrganizationModel? organization,
  }) async {
    final switching = orgPublicUuid != _loadedOrgPublicUuid;

    if (organization != null) {
      _loadedOrganization = organization;
    } else if (switching) {
      _loadedOrganization = null;
    }
    // Never show the previous organization's invoices under the new one.
    if (switching) _invoices = [];

    // Set before the request so that "Retry" targets the organization that
    // was attempted, even when the request fails.
    _loadedOrgPublicUuid = orgPublicUuid;
    _isLoadingInvoices = true;
    _invoicesErrorMessage = null;
    notifyListeners();

    try {
      final result =
          await _repository.listInvoicesByOrganizationAsAdmin(orgPublicUuid);
      if (_loadedOrgPublicUuid != orgPublicUuid) return; // a newer load took over
      _invoices = result;
    } catch (e) {
      if (_loadedOrgPublicUuid != orgPublicUuid) return;
      _invoicesErrorMessage = _messageFrom(e);
    }

    _isLoadingInvoices = false;
    _hasLoadedInvoices = true;
    notifyListeners();
  }

  // ── Organization search ──────────────────────────────────────────

  Future<void> searchOrganizations(String term) async {
    final clean = term.trim();
    _organizationSearchTerm = clean;

    if (clean.isEmpty) {
      clearOrganizationSearch();
      return;
    }

    _isSearchingOrganizations = true;
    _organizationSearchError = null;
    notifyListeners();

    try {
      final page = await _organizationRepository.listOrganizations(
        search: clean,
        size: 10,
      );
      if (_organizationSearchTerm != clean) return; // a newer search took over

      final items = [...page.items];
      // Slug is unique: an exact slug match goes first.
      final exact = items.indexWhere((o) => o.slug.toLowerCase() == clean.toLowerCase());
      if (exact > 0) items.insert(0, items.removeAt(exact));
      _organizationResults = items;
    } catch (e) {
      if (_organizationSearchTerm != clean) return;
      _organizationResults = [];
      _organizationSearchError = _messageFrom(e);
    }

    _isSearchingOrganizations = false;
    _hasSearchedOrganizations = true;
    notifyListeners();
  }

  void clearOrganizationSearch() {
    _organizationSearchTerm = '';
    _organizationResults = [];
    _isSearchingOrganizations = false;
    _hasSearchedOrganizations = false;
    _organizationSearchError = null;
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
    _loadedOrganization = null;
    _organizationResults = [];
    _organizationSearchTerm = '';
    _isSearchingOrganizations = false;
    _hasSearchedOrganizations = false;
    _organizationSearchError = null;
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