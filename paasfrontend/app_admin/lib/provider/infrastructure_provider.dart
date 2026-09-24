import 'package:flutter/foundation.dart';
import '../model/infrastructure_model.dart';
import '../repository/infrastructure_repository.dart';
import '../service/api_exception.dart';

// Infrastructure module provider — state for the three entities, each
// with its own loading/error pair (same shape as AuthProvider's staff
// state). One file per layer: ServerProvider, CoolifyInstance and Server
// all live here. Mutations return bool; the error message is exposed
// through the matching *ErrorMessage getter.
class InfrastructureProvider extends ChangeNotifier {
  final InfrastructureRepository _repository;

  InfrastructureProvider({InfrastructureRepository? repository})
      : _repository = repository ?? InfrastructureRepository();

  // ── State ─────────────────────────────────────────────────────────

  List<ServerProviderModel> _serverProviders = [];
  bool _isLoadingServerProviders = false;
  String? _serverProvidersErrorMessage;

  List<CoolifyInstanceModel> _coolifyInstances = [];
  bool _isLoadingCoolifyInstances = false;
  String? _coolifyInstancesErrorMessage;

  List<ServerModel> _servers = [];
  bool _isLoadingServers = false;
  String? _serversErrorMessage;
  // When non-null, _servers only holds the servers of this instance.
  String? _serversFilterInstanceUuid;

  // publicUuids with a status PATCH in flight — lets the UI disable the
  // control of that row and avoid double submissions.
  final Set<String> _updatingStatus = {};

  // ── Getters ───────────────────────────────────────────────────────

  List<ServerProviderModel> get serverProviders => _serverProviders;
  bool get isLoadingServerProviders => _isLoadingServerProviders;
  String? get serverProvidersErrorMessage => _serverProvidersErrorMessage;

  List<CoolifyInstanceModel> get coolifyInstances => _coolifyInstances;
  bool get isLoadingCoolifyInstances => _isLoadingCoolifyInstances;
  String? get coolifyInstancesErrorMessage => _coolifyInstancesErrorMessage;

  /// ACTIVE instances only — for the create-server dropdown.
  List<CoolifyInstanceModel> get activeCoolifyInstances =>
      _coolifyInstances.where((i) => i.status == 'ACTIVE').toList();

  List<ServerModel> get servers => _servers;
  bool get isLoadingServers => _isLoadingServers;
  String? get serversErrorMessage => _serversErrorMessage;
  String? get serversFilterInstanceUuid => _serversFilterInstanceUuid;

  bool isUpdatingStatus(String publicUuid) =>
      _updatingStatus.contains(publicUuid);

  // ── ServerProvider (read-only catalog) ────────────────────────────

  Future<void> loadServerProviders() async {
    _isLoadingServerProviders = true;
    _serverProvidersErrorMessage = null;
    notifyListeners();

    try {
      _serverProviders = await _repository.listServerProviders();
    } catch (e) {
      _serverProvidersErrorMessage = _messageFrom(e);
    }

    _isLoadingServerProviders = false;
    notifyListeners();
  }

  // ── CoolifyInstance ───────────────────────────────────────────────

  Future<void> loadCoolifyInstances() async {
    _isLoadingCoolifyInstances = true;
    _coolifyInstancesErrorMessage = null;
    notifyListeners();

    try {
      _coolifyInstances = await _repository.listCoolifyInstances();
    } catch (e) {
      _coolifyInstancesErrorMessage = _messageFrom(e);
    }

    _isLoadingCoolifyInstances = false;
    notifyListeners();
  }

  Future<bool> createCoolifyInstance({
    required String name,
    required String baseUrl,
    required String apiToken,
  }) async {
    _coolifyInstancesErrorMessage = null;
    try {
      final created = await _repository.createCoolifyInstance(
        name: name,
        baseUrl: baseUrl,
        apiToken: apiToken,
      );
      _coolifyInstances = [..._coolifyInstances, created];
      notifyListeners();
      return true;
    } catch (e) {
      _coolifyInstancesErrorMessage = _messageFrom(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateCoolifyInstanceStatus(
    String publicUuid,
    String status,
  ) async {
    _coolifyInstancesErrorMessage = null;
    _updatingStatus.add(publicUuid);
    notifyListeners();

    try {
      final updated =
          await _repository.updateCoolifyInstanceStatus(publicUuid, status);
      _coolifyInstances = _coolifyInstances
          .map((i) => i.publicUuid == publicUuid ? updated : i)
          .toList();
      return true;
    } catch (e) {
      _coolifyInstancesErrorMessage = _messageFrom(e);
      return false;
    } finally {
      _updatingStatus.remove(publicUuid);
      notifyListeners();
    }
  }

  // ── Server ────────────────────────────────────────────────────────

  /// Loads all servers, or only those of [coolifyInstancePublicUuid]
  /// when given (GET /coolify-instances/{uuid}/servers).
  Future<void> loadServers({String? coolifyInstancePublicUuid}) async {
    _isLoadingServers = true;
    _serversErrorMessage = null;
    _serversFilterInstanceUuid = coolifyInstancePublicUuid;
    notifyListeners();

    try {
      _servers = coolifyInstancePublicUuid == null
          ? await _repository.listServers()
          : await _repository
              .listServersByCoolifyInstance(coolifyInstancePublicUuid);
    } catch (e) {
      _serversErrorMessage = _messageFrom(e);
    }

    _isLoadingServers = false;
    notifyListeners();
  }

  Future<bool> createServer({
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
  }) async {
    _serversErrorMessage = null;
    try {
      final created = await _repository.createServer(
        coolifyInstancePublicUuid: coolifyInstancePublicUuid,
        idServerProvider: idServerProvider,
        name: name,
        coolifyServerUuid: coolifyServerUuid,
        hostname: hostname,
        publicIp: publicIp,
        region: region,
        totalCpu: totalCpu,
        totalMemoryMb: totalMemoryMb,
        totalStorageMb: totalStorageMb,
      );

      // Only show it in the current list if it matches the active filter.
      if (_serversFilterInstanceUuid == null ||
          _serversFilterInstanceUuid == created.coolifyInstancePublicUuid) {
        _servers = [..._servers, created];
      }
      notifyListeners();
      return true;
    } catch (e) {
      _serversErrorMessage = _messageFrom(e);
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateServerStatus(String publicUuid, String status) async {
    _serversErrorMessage = null;
    _updatingStatus.add(publicUuid);
    notifyListeners();

    try {
      final updated = await _repository.updateServerStatus(publicUuid, status);
      _servers = _servers
          .map((s) => s.publicUuid == publicUuid ? updated : s)
          .toList();
      return true;
    } catch (e) {
      _serversErrorMessage = _messageFrom(e);
      return false;
    } finally {
      _updatingStatus.remove(publicUuid);
      notifyListeners();
    }
  }

  // ── Housekeeping ──────────────────────────────────────────────────

  void clearErrors() {
    _serverProvidersErrorMessage = null;
    _coolifyInstancesErrorMessage = null;
    _serversErrorMessage = null;
    notifyListeners();
  }

  /// Drops everything held in memory — call on logout so the next admin
  /// never sees the previous session's infrastructure data.
  void reset() {
    _serverProviders = [];
    _coolifyInstances = [];
    _servers = [];
    _serversFilterInstanceUuid = null;
    _updatingStatus.clear();
    _isLoadingServerProviders = false;
    _isLoadingCoolifyInstances = false;
    _isLoadingServers = false;
    _serverProvidersErrorMessage = null;
    _coolifyInstancesErrorMessage = null;
    _serversErrorMessage = null;
    notifyListeners();
  }

  // ── Internal ──────────────────────────────────────────────────────

  // ApiException carries a user-facing message; anything else (e.g. a
  // FormatException from fromJson) gets a generic one, so loading flags
  // never stay stuck on an unexpected error.
  String _messageFrom(Object error) {
    if (error is ApiException) return error.message;
    return 'Unexpected error. Please try again.';
  }
}