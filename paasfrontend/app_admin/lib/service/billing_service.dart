import '../config/api_config.dart';
import '../model/billing_model.dart';
import 'api_client.dart';

// Billing module service — thin HTTP layer over ApiClient. Every call
// here is a catalog endpoint guarded by @PreAuthorize("hasRole('PLATFORM_ADMIN')")
// in BillingController, so all calls are authenticated (default).
// Subscription endpoints are intentionally not wrapped (OWNER-only).
class BillingService {
  Future<List<PlanModel>> listAllPlans() async {
    final json = await ApiClient.get(ApiConfig.plansAllUrl);
    return (json as List)
        .map((item) => PlanModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<PlanModel> createPlan(PlanRequest request) async {
    final json = await ApiClient.post(
      ApiConfig.plansUrl,
      body: request.toJson(),
    );
    return PlanModel.fromJson(json as Map<String, dynamic>);
  }

  Future<PlanModel> updatePlan(String publicUuid, PlanRequest request) async {
    final json = await ApiClient.put(
      ApiConfig.planByPublicUuidUrl(publicUuid),
      body: request.toJson(),
    );
    return PlanModel.fromJson(json as Map<String, dynamic>);
  }

  // DELETE /api/plans/{uuid} does not delete: it sets status INACTIVE.
  Future<PlanModel> deactivatePlan(String publicUuid) async {
    final json = await ApiClient.delete(
      ApiConfig.planByPublicUuidUrl(publicUuid),
    );
    return PlanModel.fromJson(json as Map<String, dynamic>);
  }

  Future<PlanModel> reactivatePlan(String publicUuid) async {
    final json = await ApiClient.post(ApiConfig.planReactivateUrl(publicUuid));
    return PlanModel.fromJson(json as Map<String, dynamic>);
  }

  Future<PlanModel> archivePlan(String publicUuid) async {
    final json = await ApiClient.post(ApiConfig.planArchiveUrl(publicUuid));
    return PlanModel.fromJson(json as Map<String, dynamic>);
  }

  Future<PlanResourceLimitModel> setResourceLimits(
    String publicUuid,
    PlanResourceLimitRequest request,
  ) async {
    final json = await ApiClient.put(
      ApiConfig.planResourceLimitsUrl(publicUuid),
      body: request.toJson(),
    );
    return PlanResourceLimitModel.fromJson(json as Map<String, dynamic>);
  }

  Future<PlanPriceModel> addPrice(
    String publicUuid,
    PlanPriceRequest request,
  ) async {
    final json = await ApiClient.post(
      ApiConfig.planPricesUrl(publicUuid),
      body: request.toJson(),
    );
    return PlanPriceModel.fromJson(json as Map<String, dynamic>);
  }
}