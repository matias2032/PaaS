import '../model/api_key_model.dart';
import '../service/api_exception.dart';
import '../service/api_key_service.dart';

// Client-side guards on top of ApiKeyService: the backend validates
// @NotBlank and "already revoked", but not the reason length.
class ApiKeyRepository {
  final ApiKeyService _service;

  ApiKeyRepository({ApiKeyService? service})
      : _service = service ?? ApiKeyService();

  Future<List<ApiKeyModel>> listByOrganization(String orgPublicUuid) =>
      _service.listByOrganization(orgPublicUuid);

  Future<ApiKeyModel> revoke(ApiKeyModel key, {required String reason}) {
    if (!ApiKeyStatuses.canRevoke(key.status)) {
      throw _invalid('This API key is already revoked.');
    }
    final cleanReason = reason.trim();
    if (cleanReason.isEmpty) {
      throw _invalid('A revocation reason is required.');
    }
    if (cleanReason.length > ApiKeyValidators.maxRevocationReasonLength) {
      throw _invalid(
        'Reason must be at most '
        '${ApiKeyValidators.maxRevocationReasonLength} characters.',
      );
    }
    return _service.revoke(
      key.publicUuid,
      ApiKeyRevokeRequest(reason: cleanReason),
    );
  }

  ApiException _invalid(String message) =>
      ApiException(statusCode: 400, message: message);
}