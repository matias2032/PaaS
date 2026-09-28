// Billing module models — mirror the paasbackend billing DTOs (Plan,
// PlanResourceLimit, PlanPrice). One file per layer: every model of
// this module lives here.
//
// Subscriptions are not modelled: their endpoints are authorized for
// the organization's own OWNER, so the admin app never calls them.

// ── Catalogs and client-side rules ──────────────────────────────────

class BillingStatuses {
  BillingStatuses._();

  static const String active = 'ACTIVE';
  static const String inactive = 'INACTIVE';
  static const String archived = 'ARCHIVED';

  // Mirrors ck_plans_status.
  static const List<String> plan = [active, inactive, archived];

  // Mirrors ck_plan_prices_cycle. Order = display order.
  static const List<String> billingCycles = [
    'MONTHLY',
    'QUARTERLY',
    'SEMIANNUAL',
    'YEARLY',
  ];

  // The backend does not guard these transitions (reactivate even works
  // on an ARCHIVED plan), so the client enforces them.
  static bool canDeactivate(String status) => status == active;
  static bool canReactivate(String status) => status == inactive;
  static bool canArchive(String status) =>
      status == active || status == inactive;

  static String cycleLabel(String cycle) {
    switch (cycle) {
      case 'MONTHLY':
        return 'Monthly';
      case 'QUARTERLY':
        return 'Quarterly';
      case 'SEMIANNUAL':
        return 'Semiannual';
      case 'YEARLY':
        return 'Yearly';
      default:
        return cycle;
    }
  }
}

class BillingValidators {
  BillingValidators._();

  static final RegExp _slug = RegExp(r'^[a-z0-9]+(-[a-z0-9]+)*$');
  static final RegExp _currency = RegExp(r'^[A-Z]{3}$');

  static bool isValidSlug(String value) => _slug.hasMatch(value);

  static bool isValidCurrency(String value) => _currency.hasMatch(value);

  /// Slug generated from the plan name at creation time. It is never
  /// editable afterwards (same rule as organization/project slugs).
  static String slugify(String input) {
    return input
        .toLowerCase()
        .trim()
        .replaceAll(RegExp(r'[^a-z0-9]+'), '-')
        .replaceAll(RegExp(r'^-+|-+$'), '');
  }
}

DateTime _parseDateTime(dynamic value) => DateTime.parse(value as String);

// ── PlanResourceLimit ───────────────────────────────────────────────

class PlanResourceLimitModel {
  final double cpuLimit;
  final int memoryLimitMb;
  final int storageLimitMb;
  final int maxProjects;
  final int maxServices;
  final int maxDomains;
  final int? maxEnvironmentVariables;
  final int? bandwidthLimitMb;

  const PlanResourceLimitModel({
    required this.cpuLimit,
    required this.memoryLimitMb,
    required this.storageLimitMb,
    required this.maxProjects,
    required this.maxServices,
    required this.maxDomains,
    this.maxEnvironmentVariables,
    this.bandwidthLimitMb,
  });

  factory PlanResourceLimitModel.fromJson(Map<String, dynamic> json) {
    return PlanResourceLimitModel(
      // BigDecimal is serialized as a JSON number (int or double).
      cpuLimit: (json['cpuLimit'] as num).toDouble(),
      memoryLimitMb: (json['memoryLimitMb'] as num).toInt(),
      storageLimitMb: (json['storageLimitMb'] as num).toInt(),
      maxProjects: (json['maxProjects'] as num).toInt(),
      maxServices: (json['maxServices'] as num).toInt(),
      maxDomains: (json['maxDomains'] as num).toInt(),
      maxEnvironmentVariables:
          (json['maxEnvironmentVariables'] as num?)?.toInt(),
      bandwidthLimitMb: (json['bandwidthLimitMb'] as num?)?.toInt(),
    );
  }
}

// ── PlanPrice (always a CURRENT price: effectiveUntil == null) ──────

class PlanPriceModel {
  final String publicUuid;
  final String billingCycle;
  final double amount;
  final String currency;
  final DateTime effectiveFrom;

  const PlanPriceModel({
    required this.publicUuid,
    required this.billingCycle,
    required this.amount,
    required this.currency,
    required this.effectiveFrom,
  });

  String get display => '${amount.toStringAsFixed(2)} $currency';

  factory PlanPriceModel.fromJson(Map<String, dynamic> json) {
    return PlanPriceModel(
      publicUuid: json['publicUuid'] as String,
      billingCycle: json['billingCycle'] as String,
      amount: (json['amount'] as num).toDouble(),
      currency: json['currency'] as String,
      effectiveFrom: _parseDateTime(json['effectiveFrom']),
    );
  }

  /// Returns a copy sorted by [BillingStatuses.billingCycles] order.
  static List<PlanPriceModel> sortByCycle(List<PlanPriceModel> prices) {
    final sorted = [...prices];
    sorted.sort((a, b) => BillingStatuses.billingCycles
        .indexOf(a.billingCycle)
        .compareTo(BillingStatuses.billingCycles.indexOf(b.billingCycle)));
    return sorted;
  }
}

// ── Plan ────────────────────────────────────────────────────────────

class PlanModel {
  final String publicUuid;
  final String name;
  final String slug;
  final String? description;
  final String status;
  final PlanResourceLimitModel? resourceLimits;
  final List<PlanPriceModel> prices;
  final DateTime createdAt;
  final DateTime updatedAt;

  const PlanModel({
    required this.publicUuid,
    required this.name,
    required this.slug,
    this.description,
    required this.status,
    this.resourceLimits,
    required this.prices,
    required this.createdAt,
    required this.updatedAt,
  });

  // PUT resource-limits and POST prices return only the limits / the new
  // price, so the provider merges them into the cached plan with this.
  PlanModel copyWith({
    PlanResourceLimitModel? resourceLimits,
    List<PlanPriceModel>? prices,
  }) {
    return PlanModel(
      publicUuid: publicUuid,
      name: name,
      slug: slug,
      description: description,
      status: status,
      resourceLimits: resourceLimits ?? this.resourceLimits,
      prices: prices ?? this.prices,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }

  factory PlanModel.fromJson(Map<String, dynamic> json) {
    final limits = json['resourceLimits'];
    final prices = (json['prices'] as List?) ?? const [];

    return PlanModel(
      publicUuid: json['publicUuid'] as String,
      name: json['name'] as String,
      slug: json['slug'] as String,
      description: json['description'] as String?,
      status: json['status'] as String,
      resourceLimits: limits == null
          ? null
          : PlanResourceLimitModel.fromJson(limits as Map<String, dynamic>),
      prices: PlanPriceModel.sortByCycle(
        prices
            .map((p) => PlanPriceModel.fromJson(p as Map<String, dynamic>))
            .toList(),
      ),
      createdAt: _parseDateTime(json['createdAt']),
      updatedAt: _parseDateTime(json['updatedAt']),
    );
  }
}

// ── Request DTOs ────────────────────────────────────────────────────

// Used for both create and update. On update the backend ignores the
// slug but the DTO still marks it @NotBlank, so it must be sent.
class PlanRequest {
  final String name;
  final String slug;
  final String? description;

  const PlanRequest({
    required this.name,
    required this.slug,
    this.description,
  });

  Map<String, dynamic> toJson() => {
        'name': name,
        'slug': slug,
        if (description != null && description!.trim().isNotEmpty)
          'description': description!.trim(),
      };
}

// PUT replaces the whole row: optional fields left null are stored as NULL.
class PlanResourceLimitRequest {
  final double cpuLimit;
  final int memoryLimitMb;
  final int storageLimitMb;
  final int maxProjects;
  final int maxServices;
  final int maxDomains;
  final int? maxEnvironmentVariables;
  final int? bandwidthLimitMb;

  const PlanResourceLimitRequest({
    required this.cpuLimit,
    required this.memoryLimitMb,
    required this.storageLimitMb,
    required this.maxProjects,
    required this.maxServices,
    required this.maxDomains,
    this.maxEnvironmentVariables,
    this.bandwidthLimitMb,
  });

  Map<String, dynamic> toJson() => {
        'cpuLimit': cpuLimit,
        'memoryLimitMb': memoryLimitMb,
        'storageLimitMb': storageLimitMb,
        'maxProjects': maxProjects,
        'maxServices': maxServices,
        'maxDomains': maxDomains,
        if (maxEnvironmentVariables != null)
          'maxEnvironmentVariables': maxEnvironmentVariables,
        if (bandwidthLimitMb != null) 'bandwidthLimitMb': bandwidthLimitMb,
      };
}

// Adding a price closes the current one of the same cycle server-side.
class PlanPriceRequest {
  final String billingCycle;
  final double amount;
  final String currency;

  const PlanPriceRequest({
    required this.billingCycle,
    required this.amount,
    required this.currency,
  });

  Map<String, dynamic> toJson() => {
        'billingCycle': billingCycle,
        'amount': amount,
        'currency': currency,
      };
}