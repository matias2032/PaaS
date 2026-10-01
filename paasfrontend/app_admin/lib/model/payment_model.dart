// Payment module models — mirror the paasbackend payment DTOs (Invoice,
// InvoiceItem, Payment, PaymentMethod). Admin-only: the client-facing
// endpoints (submitPayment) are authorized for the organization's own
// OWNER, so the admin app never calls them — only the …AsAdmin methods.

DateTime _parseDateTime(dynamic value) => DateTime.parse(value as String);
DateTime? _parseDateTimeOrNull(dynamic value) =>
    value == null ? null : DateTime.parse(value as String);

// ── Catalogs and client-side rules ──────────────────────────────────

class InvoiceStatuses {
  InvoiceStatuses._();

  static const String draft = 'DRAFT';
  static const String pending = 'PENDING';
  static const String paid = 'PAID';
  static const String overdue = 'OVERDUE';
  static const String cancelled = 'CANCELLED';
  static const String void_ = 'VOID';

  // Mirrors PaymentService.PAYABLE_INVOICE_STATUSES.
  static bool isPayable(String status) => status == pending || status == overdue;
}

class PaymentStatuses {
  PaymentStatuses._();

  static const String pending = 'PENDING';
  static const String processing = 'PROCESSING';
  static const String paid = 'PAID';
  static const String failed = 'FAILED';
  static const String cancelled = 'CANCELLED';
  static const String refunded = 'REFUNDED';

  // Mirrors PaymentService.IN_FLIGHT_PAYMENT_STATUSES.
  static bool isInFlight(String status) => status == pending || status == processing;

  static bool isRefundable(String status) => status == paid;
}

// ── InvoiceItem ──────────────────────────────────────────────────────

class InvoiceItemModel {
  final String description;
  final int quantity;
  final double unitPrice;
  final double lineTotal;

  const InvoiceItemModel({
    required this.description,
    required this.quantity,
    required this.unitPrice,
    required this.lineTotal,
  });

  factory InvoiceItemModel.fromJson(Map<String, dynamic> json) {
    return InvoiceItemModel(
      description: json['description'] as String,
      quantity: (json['quantity'] as num).toInt(),
      unitPrice: (json['unitPrice'] as num).toDouble(),
      lineTotal: (json['lineTotal'] as num).toDouble(),
    );
  }
}

// ── Payment ──────────────────────────────────────────────────────────

class PaymentModel {
  final String publicUuid;
  final String invoicePublicUuid;
  final String paymentMethodCode;
  final double amount;
  final String currency;
  final String? transactionReference;
  // Always null while no gateway is wired in (see PaymentService TODOs).
  final String? externalPaymentId;
  final String status;
  final String? failureReason;
  final DateTime? paidAt;
  final DateTime createdAt;

  const PaymentModel({
    required this.publicUuid,
    required this.invoicePublicUuid,
    required this.paymentMethodCode,
    required this.amount,
    required this.currency,
    this.transactionReference,
    this.externalPaymentId,
    required this.status,
    this.failureReason,
    this.paidAt,
    required this.createdAt,
  });

  String get display => '${amount.toStringAsFixed(2)} $currency';

  factory PaymentModel.fromJson(Map<String, dynamic> json) {
    return PaymentModel(
      publicUuid: json['publicUuid'] as String,
      invoicePublicUuid: json['invoicePublicUuid'] as String,
      paymentMethodCode: json['paymentMethodCode'] as String,
      amount: (json['amount'] as num).toDouble(),
      currency: json['currency'] as String,
      transactionReference: json['transactionReference'] as String?,
      externalPaymentId: json['externalPaymentId'] as String?,
      status: json['status'] as String,
      failureReason: json['failureReason'] as String?,
      paidAt: _parseDateTimeOrNull(json['paidAt']),
      createdAt: _parseDateTime(json['createdAt']),
    );
  }
}

// ── Invoice ──────────────────────────────────────────────────────────

class InvoiceModel {
  final String publicUuid;
  final String subscriptionPublicUuid;
  final String invoiceNumber;
  final String currency;
  final String status;
  final double totalAmount;
  final DateTime? issuedAt;
  final DateTime? dueAt;
  final DateTime? paidAt;
  final DateTime createdAt;
  final List<InvoiceItemModel> items;
  final List<PaymentModel> payments;

  const InvoiceModel({
    required this.publicUuid,
    required this.subscriptionPublicUuid,
    required this.invoiceNumber,
    required this.currency,
    required this.status,
    required this.totalAmount,
    this.issuedAt,
    this.dueAt,
    this.paidAt,
    required this.createdAt,
    required this.items,
    required this.payments,
  });

  String get displayTotal => '${totalAmount.toStringAsFixed(2)} $currency';

  // Any payment currently PENDING/PROCESSING for this invoice, if one
  // exists — mirrors the "in-flight" concept PaymentService checks
  // before creating a second payment or marking paid.
  PaymentModel? get inFlightPayment {
    for (final p in payments) {
      if (PaymentStatuses.isInFlight(p.status)) return p;
    }
    return null;
  }

  InvoiceModel copyWith({
    String? status,
    DateTime? paidAt,
    List<PaymentModel>? payments,
  }) {
    return InvoiceModel(
      publicUuid: publicUuid,
      subscriptionPublicUuid: subscriptionPublicUuid,
      invoiceNumber: invoiceNumber,
      currency: currency,
      status: status ?? this.status,
      totalAmount: totalAmount,
      issuedAt: issuedAt,
      dueAt: dueAt,
      paidAt: paidAt ?? this.paidAt,
      createdAt: createdAt,
      items: items,
      payments: payments ?? this.payments,
    );
  }

  factory InvoiceModel.fromJson(Map<String, dynamic> json) {
    final items = (json['items'] as List? ?? const [])
        .map((i) => InvoiceItemModel.fromJson(i as Map<String, dynamic>))
        .toList();
    final payments = (json['payments'] as List? ?? const [])
        .map((p) => PaymentModel.fromJson(p as Map<String, dynamic>))
        .toList();

    return InvoiceModel(
      publicUuid: json['publicUuid'] as String,
      subscriptionPublicUuid: json['subscriptionPublicUuid'] as String,
      invoiceNumber: json['invoiceNumber'] as String,
      currency: json['currency'] as String,
      status: json['status'] as String,
      totalAmount: (json['totalAmount'] as num).toDouble(),
      issuedAt: _parseDateTimeOrNull(json['issuedAt']),
      dueAt: _parseDateTimeOrNull(json['dueAt']),
      paidAt: _parseDateTimeOrNull(json['paidAt']),
      createdAt: _parseDateTime(json['createdAt']),
      items: items,
      payments: payments,
    );
  }
}

// ── PaymentMethod (catalog) ─────────────────────────────────────────

class PaymentMethodModel {
  final String code;
  final String name;
  final String status;

  const PaymentMethodModel({
    required this.code,
    required this.name,
    required this.status,
  });

  factory PaymentMethodModel.fromJson(Map<String, dynamic> json) {
    return PaymentMethodModel(
      code: json['code'] as String,
      name: json['name'] as String,
      status: json['status'] as String,
    );
  }
}

// ── Request DTOs ────────────────────────────────────────────────────

class AdminMarkPaidRequest {
  final String paymentMethodCode;
  final String? transactionReference;
  // Not persisted on the payment row: goes to audit_log server-side.
  final String? reason;

  const AdminMarkPaidRequest({
    required this.paymentMethodCode,
    this.transactionReference,
    this.reason,
  });

  Map<String, dynamic> toJson() => {
        'paymentMethodCode': paymentMethodCode,
        if (transactionReference != null)
          'transactionReference': transactionReference,
        if (reason != null) 'reason': reason,
      };
}

class AdminRefundRequest {
  final String reason;

  const AdminRefundRequest({required this.reason});

  Map<String, dynamic> toJson() => {'reason': reason};
}