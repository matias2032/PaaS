import '../config/api_config.dart';
import '../model/infrastructure_model.dart';
import 'api_client.dart';

// Infrastructure module service — thin HTTP layer over ApiClient.
// Every endpoint requires PLATFORM_ADMIN (class-level @PreAuthorize on
// InfrastructureController), so all calls are authenticated (default).
// One file per layer: ServerProvider, CoolifyInstance and Server all
// live here, in dependency order.
class InfrastructureService {
  // ── ServerProvider (read-only catalog) ────────────────────────────

  Future<List<ServerProviderModel>> listServerProviders() async {
    final json = await ApiClient.get(ApiConfig.serverProvidersUrl);
    return (json as List)
        .map((item) => ServerProviderModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  // ── CoolifyInstance ───────────────────────────────────────────────

  Future<List<CoolifyInstanceModel>> listCoolifyInstances() async {
    final json = await ApiClient.get(ApiConfig.coolifyInstancesUrl);
    return (json as List)
        .map((item) => CoolifyInstanceModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<CoolifyInstanceModel> getCoolifyInstance(String publicUuid) async {
    final json = await ApiClient.get(
      ApiConfig.coolifyInstanceByPublicUuidUrl(publicUuid),
    );
    return CoolifyInstanceModel.fromJson(json as Map<String, dynamic>);
  }

  Future<CoolifyInstanceModel> createCoolifyInstance(
    CreateCoolifyInstanceRequest request,
  ) async {
    final json = await ApiClient.post(
      ApiConfig.coolifyInstancesUrl,
      body: request.toJson(),
    );
    return CoolifyInstanceModel.fromJson(json as Map<String, dynamic>);
  }

  Future<CoolifyInstanceModel> updateCoolifyInstanceStatus(
    String publicUuid,
    String status,
  ) async {
    final json = await ApiClient.patch(
      ApiConfig.coolifyInstanceStatusUrl(publicUuid),
      body: UpdateStatusRequest(status: status).toJson(),
    );
    return CoolifyInstanceModel.fromJson(json as Map<String, dynamic>);
  }

  Future<List<ServerModel>> listServersByCoolifyInstance(
    String coolifyInstancePublicUuid,
  ) async {
    final json = await ApiClient.get(
      ApiConfig.coolifyInstanceServersUrl(coolifyInstancePublicUuid),
    );
    return (json as List)
        .map((item) => ServerModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  // ── Server ────────────────────────────────────────────────────────

  Future<List<ServerModel>> listServers() async {
    final json = await ApiClient.get(ApiConfig.serversUrl);
    return (json as List)
        .map((item) => ServerModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<ServerModel> getServer(String publicUuid) async {
    final json = await ApiClient.get(ApiConfig.serverByPublicUuidUrl(publicUuid));
    return ServerModel.fromJson(json as Map<String, dynamic>);
  }

  Future<ServerModel> createServer(CreateServerRequest request) async {
    final json = await ApiClient.post(
      ApiConfig.serversUrl,
      body: request.toJson(),
    );
    return ServerModel.fromJson(json as Map<String, dynamic>);
  }

  Future<ServerModel> updateServerStatus(String publicUuid, String status) async {
    final json = await ApiClient.patch(
      ApiConfig.serverStatusUrl(publicUuid),
      body: UpdateStatusRequest(status: status).toJson(),
    );
    return ServerModel.fromJson(json as Map<String, dynamic>);
  }
}