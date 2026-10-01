import '../config/api_config.dart';
import '../model/organization_model.dart';
import 'api_client.dart';

// Organization module service — thin HTTP layer over ApiClient.
//
// Reads go through AdminOrganizationController (hasRole('SUPPORT')).
// Suspend / lift-suspension live on OrganizationController and are
// guarded by hasRole('PLATFORM_ADMIN'); none of them requires membership
// of the organization. Self-service endpoints (update, deactivate,
// members, ...) are intentionally not wrapped: they are OWNER-only.
class OrganizationService {
  // Page size sent when the caller does not specify one.
  static const int defaultPageSize = 20;

  // Spring Data resolves ?page=&size=&sort=property,direction into a
  // Pageable. Without an explicit sort the order is not deterministic,
  // which would shuffle items between pages, so it is always sent.
  // `search` matches organization name OR slug (case-insensitive, contains).
  Future<OrganizationPage> listOrganizations({
    int page = 0,
    int size = defaultPageSize,
    String? search,
  }) async {
    final url = Uri.parse(ApiConfig.adminOrganizationsUrl).replace(
      queryParameters: {
        'page': '$page',
        'size': '$size',
        'sort': 'createdAt,desc',
        if (search != null && search.isNotEmpty) 'search': search,
      },
    ).toString();

    final json = await ApiClient.get(url);
    return OrganizationPage.fromJson(json as Map<String, dynamic>);
  }

  // Admin variant of getOrganization: works for organizations the caller
  // is not a member of.
  Future<OrganizationModel> getOrganization(String publicUuid) async {
    final json = await ApiClient.get(
      ApiConfig.adminOrganizationByPublicUuidUrl(publicUuid),
    );
    return OrganizationModel.fromJson(json as Map<String, dynamic>);
  }

  Future<OrganizationModel> suspendOrganization(
    String publicUuid,
    OrganizationSuspendRequest request,
  ) async {
    final json = await ApiClient.post(
      ApiConfig.organizationSuspendUrl(publicUuid),
      body: request.toJson(),
    );
    return OrganizationModel.fromJson(json as Map<String, dynamic>);
  }

  // No request body: the backend clears suspensionReason and sets ACTIVE.
  Future<OrganizationModel> liftSuspension(String publicUuid) async {
    final json = await ApiClient.post(
      ApiConfig.organizationLiftSuspensionUrl(publicUuid),
    );
    return OrganizationModel.fromJson(json as Map<String, dynamic>);
  }
}