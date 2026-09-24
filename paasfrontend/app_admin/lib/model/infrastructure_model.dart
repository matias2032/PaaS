import 'dart:io' show InternetAddress;

// Infrastructure module models — mirror the paasbackend DTOs
// (ServerProvider, CoolifyInstance, Server). One file per layer:
// every model of this module lives here.

// ── Status catalogs ─────────────────────────────────────────────────
// Mirror ck_coolify_instances_status / ck_servers_status in the schema
// and VALID_*_STATUSES in InfrastructureService. Keep in sync.

class InfrastructureStatuses {
  InfrastructureStatuses._();

  static const List<String> coolifyInstance = [
    'ACTIVE',
    'INACTIVE',
    'MAINTENANCE',
    'UNAVAILABLE',
  ];

  static const List<String> server = [
    'ACTIVE',
    'MAINTENANCE',
    'OFFLINE',
    'FULL',
    'INACTIVE',
  ];
}

// ── Validators ──────────────────────────────────────────────────────
// The backend does NOT validate publicIp before the ?::inet cast, so a
// malformed value fails at the DB level. Validate on the client first.

class InfrastructureValidators {
  InfrastructureValidators._();

  static bool isValidIp(String value) =>
      InternetAddress.tryParse(value.trim()) != null;
}

DateTime _parseDateTime(dynamic value) => DateTime.parse(value as String);

// ── ServerProvider (read-only catalog) ──────────────────────────────

class ServerProviderModel {
  final int idServerProvider;
  final String name;
  final String code;

  const ServerProviderModel({
    required this.idServerProvider,
    required this.name,
    required this.code,
  });

  factory ServerProviderModel.fromJson(Map<String, dynamic> json) {
    return ServerProviderModel(
      idServerProvider: json['idServerProvider'] as int,
      name: json['name'] as String,
      code: json['code'] as String,
    );
  }
}

// ── CoolifyInstance ─────────────────────────────────────────────────
// The API token is never returned by the backend, so it is not modelled
// here — it only exists in CreateCoolifyInstanceRequest.

class CoolifyInstanceModel {
  final String publicUuid;
  final String name;
  final String baseUrl;
  final String status;
  final DateTime createdAt;
  final DateTime updatedAt;

  const CoolifyInstanceModel({
    required this.publicUuid,
    required this.name,
    required this.baseUrl,
    required this.status,
    required this.createdAt,
    required this.updatedAt,
  });

  factory CoolifyInstanceModel.fromJson(Map<String, dynamic> json) {
    return CoolifyInstanceModel(
      publicUuid: json['publicUuid'] as String,
      name: json['name'] as String,
      baseUrl: json['baseUrl'] as String,
      status: json['status'] as String,
      createdAt: _parseDateTime(json['createdAt']),
      updatedAt: _parseDateTime(json['updatedAt']),
    );
  }
}

// ── Server ──────────────────────────────────────────────────────────

class ServerModel {
  final String publicUuid;
  final String coolifyInstancePublicUuid;
  final String coolifyInstanceName;
  final int? idServerProvider;
  final String? serverProviderName;
  final String name;
  final String coolifyServerUuid;
  final String? hostname;
  final String? publicIp;
  final String? region;
  final double totalCpu;
  final int totalMemoryMb;
  final int totalStorageMb;
  final String status;
  final DateTime createdAt;
  final DateTime updatedAt;

  const ServerModel({
    required this.publicUuid,
    required this.coolifyInstancePublicUuid,
    required this.coolifyInstanceName,
    this.idServerProvider,
    this.serverProviderName,
    required this.name,
    required this.coolifyServerUuid,
    this.hostname,
    this.publicIp,
    this.region,
    required this.totalCpu,
    required this.totalMemoryMb,
    required this.totalStorageMb,
    required this.status,
    required this.createdAt,
    required this.updatedAt,
  });

  // Display helpers (MB -> GB, base 1024).
  double get totalMemoryGb => totalMemoryMb / 1024;
  double get totalStorageGb => totalStorageMb / 1024;

  factory ServerModel.fromJson(Map<String, dynamic> json) {
    return ServerModel(
      publicUuid: json['publicUuid'] as String,
      coolifyInstancePublicUuid: json['coolifyInstancePublicUuid'] as String,
      coolifyInstanceName: json['coolifyInstanceName'] as String,
      idServerProvider: json['idServerProvider'] as int?,
      serverProviderName: json['serverProviderName'] as String?,
      name: json['name'] as String,
      coolifyServerUuid: json['coolifyServerUuid'] as String,
      hostname: json['hostname'] as String?,
      publicIp: json['publicIp'] as String?,
      region: json['region'] as String?,
      // BigDecimal is serialized as a JSON number (int or double).
      totalCpu: (json['totalCpu'] as num).toDouble(),
      totalMemoryMb: (json['totalMemoryMb'] as num).toInt(),
      totalStorageMb: (json['totalStorageMb'] as num).toInt(),
      status: json['status'] as String,
      createdAt: _parseDateTime(json['createdAt']),
      updatedAt: _parseDateTime(json['updatedAt']),
    );
  }
}

// ── Request DTOs ────────────────────────────────────────────────────

class CreateCoolifyInstanceRequest {
  final String name;
  final String baseUrl;
  final String apiToken; // plaintext, encrypted server-side

  const CreateCoolifyInstanceRequest({
    required this.name,
    required this.baseUrl,
    required this.apiToken,
  });

  Map<String, dynamic> toJson() => {
        'name': name,
        'baseUrl': baseUrl,
        'apiToken': apiToken,
      };
}

class CreateServerRequest {
  final String coolifyInstancePublicUuid;
  final int? idServerProvider;
  final String name;
  final String coolifyServerUuid;
  final String? hostname;
  final String? publicIp;
  final String? region;
  final double totalCpu;
  final int totalMemoryMb;
  final int totalStorageMb;

  const CreateServerRequest({
    required this.coolifyInstancePublicUuid,
    this.idServerProvider,
    required this.name,
    required this.coolifyServerUuid,
    this.hostname,
    this.publicIp,
    this.region,
    required this.totalCpu,
    required this.totalMemoryMb,
    required this.totalStorageMb,
  });

  // Optional fields are omitted when null/blank so the backend stores
  // NULL instead of an empty string (matters for the inet cast).
  Map<String, dynamic> toJson() => {
        'coolifyInstancePublicUuid': coolifyInstancePublicUuid,
        if (idServerProvider != null) 'idServerProvider': idServerProvider,
        'name': name,
        'coolifyServerUuid': coolifyServerUuid,
        if (hostname != null && hostname!.trim().isNotEmpty)
          'hostname': hostname!.trim(),
        if (publicIp != null && publicIp!.trim().isNotEmpty)
          'publicIp': publicIp!.trim(),
        if (region != null && region!.trim().isNotEmpty)
          'region': region!.trim(),
        'totalCpu': totalCpu,
        'totalMemoryMb': totalMemoryMb,
        'totalStorageMb': totalStorageMb,
      };
}

// Shared by PATCH /coolify-instances/{uuid}/status and
// PATCH /servers/{uuid}/status — both DTOs have the same shape.
class UpdateStatusRequest {
  final String status;

  const UpdateStatusRequest({required this.status});

  Map<String, dynamic> toJson() => {'status': status};
}