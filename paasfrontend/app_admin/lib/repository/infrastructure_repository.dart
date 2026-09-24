import '../model/infrastructure_model.dart';
import '../service/api_exception.dart';
import '../service/infrastructure_service.dart';

// Infrastructure module repository — client-side business rules on top
// of InfrastructureService. Validations here mirror or complement the
// backend (which validates statuses and DTO constraints, but NOT the
// publicIp format nor the baseUrl shape). One file per layer:
// ServerProvider, CoolifyInstance and Server all live here.
class InfrastructureRepository {
  final InfrastructureService _service;

  InfrastructureRepository({InfrastructureService? service})
      : _service = service ?? InfrastructureService();

  // ── ServerProvider (read-only catalog) ────────────────────────────

  Future<List<ServerProviderModel>> listServerProviders() {
    return _service.listServerProviders();
  }

  // ── CoolifyInstance ───────────────────────────────────────────────

  Future<List<CoolifyInstanceModel>> listCoolifyInstances() {
    return _service.listCoolifyInstances();
  }

  /// Only ACTIVE instances — meant for the "Coolify instance" dropdown
  /// in the create-server form, so a server is not attached to an
  /// instance that is down or under maintenance.
  Future<List<CoolifyInstanceModel>> listActiveCoolifyInstances() async {
    final instances = await _service.listCoolifyInstances();
    return instances.where((i) => i.status == 'ACTIVE').toList();
  }

  Future<CoolifyInstanceModel> getCoolifyInstance(String publicUuid) {
    return _service.getCoolifyInstance(publicUuid);
  }

  Future<CoolifyInstanceModel> createCoolifyInstance({
    required String name,
    required String baseUrl,
    required String apiToken,
  }) {
    final normalizedUrl = _normalizeBaseUrl(baseUrl);

    return _service.createCoolifyInstance(
      CreateCoolifyInstanceRequest(
        name: name.trim(),
        baseUrl: normalizedUrl,
        apiToken: apiToken.trim(),
      ),
    );
  }

  Future<CoolifyInstanceModel> updateCoolifyInstanceStatus(
    String publicUuid,
    String status,
  ) {
    _requireValidStatus(
      status,
      InfrastructureStatuses.coolifyInstance,
      'Coolify instance',
    );
    return _service.updateCoolifyInstanceStatus(publicUuid, status);
  }

  Future<List<ServerModel>> listServersByCoolifyInstance(
    String coolifyInstancePublicUuid,
  ) {
    return _service.listServersByCoolifyInstance(coolifyInstancePublicUuid);
  }

  // ── Server ────────────────────────────────────────────────────────

  Future<List<ServerModel>> listServers() {
    return _service.listServers();
  }

  Future<ServerModel> getServer(String publicUuid) {
    return _service.getServer(publicUuid);
  }

  Future<ServerModel> createServer({
    required String coolifyInstancePublicUuid,
    int? idServerProvider,
    required String name,
    required String coolifyServerUuid,
    String? hostname,
    String? publicIp,
    String? region,
    required double totalCpu,
    required int totalMemoryMb,
    required int totalStorageMb,
  }) {
    // The backend casts publicIp with ?::inet without validating it
    // first, so a malformed value would fail at the DB level.
    final ip = publicIp?.trim();
    if (ip != null && ip.isNotEmpty && !InfrastructureValidators.isValidIp(ip)) {
      throw ApiException(
        statusCode: 400,
        message: 'Public IP is not a valid IPv4/IPv6 address.',
      );
    }

    return _service.createServer(
      CreateServerRequest(
        coolifyInstancePublicUuid: coolifyInstancePublicUuid,
        idServerProvider: idServerProvider,
        name: name.trim(),
        coolifyServerUuid: coolifyServerUuid.trim(),
        hostname: hostname,
        publicIp: ip,
        region: region,
        totalCpu: totalCpu,
        totalMemoryMb: totalMemoryMb,
        totalStorageMb: totalStorageMb,
      ),
    );
  }

  Future<ServerModel> updateServerStatus(String publicUuid, String status) {
    _requireValidStatus(status, InfrastructureStatuses.server, 'server');
    return _service.updateServerStatus(publicUuid, status);
  }

  // ── Internal rules ────────────────────────────────────────────────

  void _requireValidStatus(String status, List<String> valid, String label) {
    if (!valid.contains(status)) {
      throw ApiException(
        statusCode: 400,
        message: 'Invalid $label status: $status',
      );
    }
  }

  /// Must be an absolute http(s) URL. Trailing slashes are stripped so
  /// "https://x.com" and "https://x.com/" cannot coexist — the backend
  /// uniqueness check (existsByBaseUrl) compares the raw string.
  String _normalizeBaseUrl(String raw) {
    final trimmed = raw.trim();
    final uri = Uri.tryParse(trimmed);

    final isValid = uri != null &&
        (uri.scheme == 'http' || uri.scheme == 'https') &&
        uri.host.isNotEmpty;

    if (!isValid) {
      throw ApiException(
        statusCode: 400,
        message: 'Base URL must start with http:// or https://',
      );
    }

    return trimmed.replaceAll(RegExp(r'/+$'), '');
  }
}