import 'package:flutter/foundation.dart';
import '../model/billing_model.dart';
import '../repository/billing_repository.dart';
import '../service/api_exception.dart';

// Billing module provider — state for the plan catalog (admin view).
// Mutations return bool; the error message is exposed through
// plansErrorMessage. Limits and prices come back from the API without
// the rest of the plan, so they are merged into the cached plan here.
class BillingProvider extends ChangeNotifier {
  final BillingRepository _repository;

  BillingProvider({BillingRepository? repository})
      : _repository = repository ?? BillingRepository();

  // ── State ─────────────────────────────────────────────────────────

  List<PlanModel> _plans = [];
  bool _isLoadingPlans = false;
  // Distinguishes "not loaded yet" from "loaded and empty".
  bool _hasLoadedPlans = false;
  String? _plansErrorMessage;

  // publicUuids with a mutation in flight — lets the UI disable the
  // controls of that plan and avoid double submissions.
  final Set<String> _updating = {};

  // ── Getters ───────────────────────────────────────────────────────

  List<PlanModel> get plans => _plans;
  bool get isLoadingPlans => _isLoadingPlans;
  bool get hasLoadedPlans => _hasLoadedPlans;
  String? get plansErrorMessage => _plansErrorMessage;

  bool isUpdating(String publicUuid) => _updating.contains(publicUuid);

  PlanModel? planByUuid(String publicUuid) {
    for (final plan in _plans) {
      if (plan.publicUuid == publicUuid) return plan;
    }
    return null;
  }

  // ── Plans ─────────────────────────────────────────────────────────

  Future<void> loadPlans() async {
    _isLoadingPlans = true;
    _plansErrorMessage = null;
    notifyListeners();

    try {
      _plans = await _repository.listAllPlans();
    } catch (e) {
      _plansErrorMessage = _messageFrom(e);
    }

    _isLoadingPlans = false;
    _hasLoadedPlans = true;
    notifyListeners();
  }

  Future<bool> createPlan({
    required String name,
    String? description,
  }) async {
    _plansErrorMessage = null;
    try {
      final created = await _repository.createPlan(
        name: name,
        description: description,
      );
      _plans = [..._plans, created];
      notifyListeners();
      return true;
    } catch (e) {
      _plansErrorMessage = _messageFrom(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> updatePlan(
    String publicUuid, {
    required String name,
    String? description,
  }) {
    return _runOnPlan(publicUuid, (plan) async {
      _requireEditable(plan);
      final updated = await _repository.updatePlan(
        plan,
        name: name,
        description: description,
      );
      _replacePlan(updated);
    });
  }

  Future<bool> deactivatePlan(String publicUuid) {
    return _runOnPlan(publicUuid, (plan) async {
      _replacePlan(await _repository.deactivatePlan(plan));
    });
  }

  Future<bool> reactivatePlan(String publicUuid) {
    return _runOnPlan(publicUuid, (plan) async {
      _replacePlan(await _repository.reactivatePlan(plan));
    });
  }

  Future<bool> archivePlan(String publicUuid) {
    return _runOnPlan(publicUuid, (plan) async {
      _replacePlan(await _repository.archivePlan(plan));
    });
  }

  Future<bool> setResourceLimits({
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
    return _runOnPlan(planPublicUuid, (plan) async {
      _requireEditable(plan);
      final limits = await _repository.setResourceLimits(
        planPublicUuid: planPublicUuid,
        cpuLimit: cpuLimit,
        memoryLimitMb: memoryLimitMb,
        storageLimitMb: storageLimitMb,
        maxProjects: maxProjects,
        maxServices: maxServices,
        maxDomains: maxDomains,
        maxEnvironmentVariables: maxEnvironmentVariables,
        bandwidthLimitMb: bandwidthLimitMb,
      );
      _replacePlan(plan.copyWith(resourceLimits: limits));
    });
  }

  Future<bool> addPrice({
    required String planPublicUuid,
    required String billingCycle,
    required double amount,
    required String currency,
  }) {
    return _runOnPlan(planPublicUuid, (plan) async {
      _requireEditable(plan);
      final price = await _repository.addPrice(
        planPublicUuid: planPublicUuid,
        billingCycle: billingCycle,
        amount: amount,
        currency: currency,
      );
      // The backend closed the previous price of this cycle; only current
      // prices are listed, so swap it for the new one.
      final prices = PlanPriceModel.sortByCycle([
        ...plan.prices.where((p) => p.billingCycle != price.billingCycle),
        price,
      ]);
      _replacePlan(plan.copyWith(prices: prices));
    });
  }

  // ── Housekeeping ──────────────────────────────────────────────────

  void clearErrors() {
    _plansErrorMessage = null;
    notifyListeners();
  }

  /// Drops everything held in memory — call on logout so the next admin
  /// never sees the previous session's data.
  void reset() {
    _plans = [];
    _isLoadingPlans = false;
    _hasLoadedPlans = false;
    _plansErrorMessage = null;
    _updating.clear();
    notifyListeners();
  }

  // ── Internal ──────────────────────────────────────────────────────

  Future<bool> _runOnPlan(
    String publicUuid,
    Future<void> Function(PlanModel plan) action,
  ) async {
    _plansErrorMessage = null;
    _updating.add(publicUuid);
    notifyListeners();

    try {
      final plan = planByUuid(publicUuid);
      if (plan == null) {
        throw ApiException(
          statusCode: 404,
          message: 'Plan not found. Reload the list and try again.',
        );
      }
      await action(plan);
      return true;
    } catch (e) {
      _plansErrorMessage = _messageFrom(e);
      return false;
    } finally {
      _updating.remove(publicUuid);
      notifyListeners();
    }
  }

  void _replacePlan(PlanModel updated) {
    _plans = _plans
        .map((p) => p.publicUuid == updated.publicUuid ? updated : p)
        .toList();
  }

    // ARCHIVED plans are terminal server-side (PlanArchivedException,
  // 409). Checked here too so the UI fails fast with a clear message
  // instead of round-tripping to the server first.
  void _requireEditable(PlanModel plan) {
    if (plan.status == BillingStatuses.archived) {
      throw ApiException(
        statusCode: 409,
        message: 'This plan is archived and can no longer be edited.',
      );
    }
  }

  // ApiException carries a user-facing message; anything else (e.g. a
  // FormatException from fromJson) gets a generic one, so loading flags
  // never stay stuck on an unexpected error.
  String _messageFrom(Object error) {
    if (error is ApiException) return error.message;
    return 'Unexpected error. Please try again.';
  }
}