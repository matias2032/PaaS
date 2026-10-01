import '../model/billing_model.dart';
import '../service/api_exception.dart';
import '../service/billing_service.dart';

// Billing module repository — client-side business rules on top of
// BillingService. The backend validates DTO constraints but NOT the
// billing cycle, the currency, nor the status transitions, so those
// are enforced here.
class BillingRepository {
  final BillingService _service;

  BillingRepository({BillingService? service})
      : _service = service ?? BillingService();

  Future<List<PlanModel>> listAllPlans() => _service.listAllPlans();

  Future<PlanModel> createPlan({
    required String name,
    String? description,
  }) {
    final cleanName = _requireName(name);
    final slug = BillingValidators.slugify(cleanName);
    if (slug.isEmpty ||
        slug.length > 100 ||
        !BillingValidators.isValidSlug(slug)) {
      throw _invalid(
        'Could not generate a valid slug from this name. '
        'Use letters or numbers.',
      );
    }

    return _service.createPlan(
      PlanRequest(name: cleanName, slug: slug, description: description),
    );
  }

  Future<PlanModel> updatePlan(
    PlanModel plan, {
    required String name,
    String? description,
  }) {
    final cleanName = _requireName(name);
    // The slug is never changed, but the backend DTO requires it.
    return _service.updatePlan(
      plan.publicUuid,
      PlanRequest(name: cleanName, slug: plan.slug, description: description),
    );
  }

  Future<PlanModel> deactivatePlan(PlanModel plan) {
    if (!BillingStatuses.canDeactivate(plan.status)) {
      throw _invalid('Only ACTIVE plans can be deactivated.');
    }
    return _service.deactivatePlan(plan.publicUuid);
  }

  Future<PlanModel> reactivatePlan(PlanModel plan) {
    if (!BillingStatuses.canReactivate(plan.status)) {
      throw _invalid('Only INACTIVE plans can be reactivated.');
    }
    return _service.reactivatePlan(plan.publicUuid);
  }

  Future<PlanModel> archivePlan(PlanModel plan) {
    if (!BillingStatuses.canArchive(plan.status)) {
      throw _invalid('This plan is already archived.');
    }
    return _service.archivePlan(plan.publicUuid);
  }

  Future<PlanResourceLimitModel> setResourceLimits({
    required String planPublicUuid,
    required double cpuLimit,
    required int memoryLimitMb,
    required int storageLimitMb,
    int? maxProjects,
    int? maxServices,
    int? maxDomains,
    int? maxEnvironmentVariables,
    int? bandwidthLimitMb,
  }) {
    if (!cpuLimit.isFinite || cpuLimit <= 0) {
      throw _invalid('CPU limit must be greater than 0.');
    }
    final nonNegative = <int?>[
      memoryLimitMb,
      storageLimitMb,
      maxProjects,
      maxServices,
      maxDomains,
      maxEnvironmentVariables,
      bandwidthLimitMb,
    ];
    if (nonNegative.any((v) => v != null && v < 0)) {
      throw _invalid('Limits cannot be negative.');
    }

    return _service.setResourceLimits(
      planPublicUuid,
      PlanResourceLimitRequest(
        cpuLimit: cpuLimit,
        memoryLimitMb: memoryLimitMb,
        storageLimitMb: storageLimitMb,
        maxProjects: maxProjects,
        maxServices: maxServices,
        maxDomains: maxDomains,
        maxEnvironmentVariables: maxEnvironmentVariables,
        bandwidthLimitMb: bandwidthLimitMb,
      ),
    );
  }

  Future<PlanPriceModel> addPrice({
    required String planPublicUuid,
    required String billingCycle,
    required double amount,
    required String currency,
  }) {
    if (!BillingStatuses.billingCycles.contains(billingCycle)) {
      throw _invalid('Invalid billing cycle: $billingCycle');
    }
    if (!amount.isFinite || amount < 0) {
      throw _invalid('Amount must be 0 or greater.');
    }
    final cleanCurrency = currency.trim().toUpperCase();
    if (!BillingValidators.isValidCurrency(cleanCurrency)) {
      throw _invalid('Currency must be a 3-letter code (e.g. MZN).');
    }

    return _service.addPrice(
      planPublicUuid,
      PlanPriceRequest(
        billingCycle: billingCycle,
        amount: amount,
        currency: cleanCurrency,
      ),
    );
  }

  // ── Internal rules ────────────────────────────────────────────────

  String _requireName(String raw) {
    final name = raw.trim();
    if (name.isEmpty) throw _invalid('Name is required.');
    if (name.length > 100) throw _invalid('Name must be at most 100 characters.');
    return name;
  }

  ApiException _invalid(String message) =>
      ApiException(statusCode: 400, message: message);
}