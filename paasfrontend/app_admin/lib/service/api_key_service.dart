import '../config/api_config.dart';
import '../model/api_key_model.dart';
import 'api_client.dart';

// API key module service — thin HTTP layer over ApiClient.
// Both endpoints are guarded by hasRole('PLATFORM_ADMIN') and need no
// membership. The client-facing create/list/revoke are OWNER-only and
// intentionally not wrapped.
class ApiKeyService {
  Future<List<ApiKeyModel>> listByOrganization(String orgPublicUuid) async {
    final json = await ApiClient.get(
      ApiConfig.adminOrganizationApiKeysUrl(orgPublicUuid),
    );
    return (json as List)
        .map((e) => ApiKeyModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ApiKeyModel> revoke(
    String keyPublicUuid,
    ApiKeyRevokeRequest request,
  ) async {
    final json = await ApiClient.patch(
      ApiConfig.adminApiKeyRevokeUrl(keyPublicUuid),
      body: request.toJson(),
    );
    return ApiKeyModel.fromJson(json as Map<String, dynamic>);
  }
}