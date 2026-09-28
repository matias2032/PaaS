import 'package:flutter/foundation.dart';
import '../model/api_key_model.dart';
import '../repository/api_key_repository.dart';
import '../service/api_exception.dart';

// State for the API key supervision view. Keys are cached per
// organization (there is no global listing endpoint).
class ApiKeyProvider extends ChangeNotifier {
  final ApiKeyRepository _repository;

  ApiKeyProvider({ApiKeyRepository? repository})
      : _repository = repository ?? ApiKeyRepository();

  final Map<String, List<ApiKeyModel>> _byOrganization = {};
  final Set<String> _loading = {}; // org uuids
  final Set<String> _loaded = {}; // org uuids
  final Set<String> _updating = {}; // key uuids
  String? _errorMessage;
  int _generation = 0;

  String? get errorMessage => _errorMessage;

  List<ApiKeyModel> keysOf(String orgPublicUuid) =>
      _byOrganization[orgPublicUuid] ?? const [];
  bool isLoading(String orgPublicUuid) => _loading.contains(orgPublicUuid);
  bool hasLoaded(String orgPublicUuid) => _loaded.contains(orgPublicUuid);
  bool isUpdating(String keyPublicUuid) => _updating.contains(keyPublicUuid);

  Future<void> loadKeys(String orgPublicUuid) async {
    if (_loading.contains(orgPublicUuid)) return;

    final generation = _generation;
    _loading.add(orgPublicUuid);
    _errorMessage = null;
    notifyListeners();

    try {
      final keys = await _repository.listByOrganization(orgPublicUuid);
      if (generation != _generation) return;
      _byOrganization[orgPublicUuid] = keys;
    } catch (e) {
      if (generation != _generation) return;
      _errorMessage = _messageFrom(e);
    }

    _loading.remove(orgPublicUuid);
    _loaded.add(orgPublicUuid);
    notifyListeners();
  }

  Future<bool> revokeKey(
    String orgPublicUuid,
    String keyPublicUuid, {
    required String reason,
  }) async {
    _errorMessage = null;
    _updating.add(keyPublicUuid);
    notifyListeners();

    try {
      final key = keysOf(orgPublicUuid)
          .where((k) => k.publicUuid == keyPublicUuid)
          .firstOrNull;
      if (key == null) {
        throw ApiException(
          statusCode: 404,
          message: 'API key not found. Reload the list and try again.',
        );
      }
      final updated = await _repository.revoke(key, reason: reason);
      _byOrganization[orgPublicUuid] = keysOf(orgPublicUuid)
          .map((k) => k.publicUuid == updated.publicUuid ? updated : k)
          .toList();
      return true;
    } catch (e) {
      _errorMessage = _messageFrom(e);
      return false;
    } finally {
      _updating.remove(keyPublicUuid);
      notifyListeners();
    }
  }

  void clearErrors() {
    _errorMessage = null;
    notifyListeners();
  }

  /// Call on logout.
  void reset() {
    _generation++;
    _byOrganization.clear();
    _loading.clear();
    _loaded.clear();
    _updating.clear();
    _errorMessage = null;
    notifyListeners();
  }

  String _messageFrom(Object error) {
    if (error is ApiException) return error.message;
    return 'Unexpected error. Please try again.';
  }
}