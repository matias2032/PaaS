// API key module models — mirror the paasbackend api_key DTOs
// (ApiKeyResponseDTO, AdminRevokeApiKeyRequestDTO). One file per layer:
// every model of this module lives here.
//
// ApiKeyCreatedResponseDTO (the only one carrying rawKey) and
// ApiKeyRequestDTO are not modelled: create is OWNER-only and the admin
// app never calls it, so rawKey and keyHash never reach this app.

// ── Catalogs and client-side rules ──────────────────────────────────

class ApiKeyStatuses {
  ApiKeyStatuses._();

  static const String active = 'ACTIVE';
  static const String revoked = 'REVOKED';

  // Revoking is the only transition.
  static bool canRevoke(String status) => status == active;
}

class ApiKeyValidators {
  ApiKeyValidators._();

  // The backend only enforces @NotBlank, but revocation_reason is mapped
  // without a length (VARCHAR(255) by default). Raise this if the
  // migration made the column TEXT.
  static const int maxRevocationReasonLength = 255;
}

DateTime _parseDateTime(dynamic value) => DateTime.parse(value as String);

DateTime? _parseOptionalDateTime(dynamic value) =>
    value == null ? null : _parseDateTime(value);

// ── API key ─────────────────────────────────────────────────────────

class ApiKeyModel {
  final String publicUuid;
  final String organizationPublicUuid;
  final String name;
  // Visible prefix only, e.g. "sk_live_AbCdEfGh".
  final String keyPrefix;
  final String status;
  final DateTime? lastUsedAt;
  // Null = never expires.
  final DateTime? expiresAt;
  final DateTime createdAt;
  // Only set once revoked, and only when revoked by a platform admin.
  final String? revocationReason;

  const ApiKeyModel({
    required this.publicUuid,
    required this.organizationPublicUuid,
    required this.name,
    required this.keyPrefix,
    required this.status,
    this.lastUsedAt,
    this.expiresAt,
    required this.createdAt,
    this.revocationReason,
  });

  bool get isActive => status == ApiKeyStatuses.active;
  bool get isRevoked => status == ApiKeyStatuses.revoked;

  // The backend never flips status on expiry, so it is derived here.
  bool get isExpired =>
      expiresAt != null && expiresAt!.isBefore(DateTime.now());

  factory ApiKeyModel.fromJson(Map<String, dynamic> json) {
    return ApiKeyModel(
      publicUuid: json['publicUuid'] as String,
      organizationPublicUuid: json['organizationPublicUuid'] as String,
      name: json['name'] as String,
      keyPrefix: json['keyPrefix'] as String,
      status: json['status'] as String,
      lastUsedAt: _parseOptionalDateTime(json['lastUsedAt']),
      expiresAt: _parseOptionalDateTime(json['expiresAt']),
      createdAt: _parseDateTime(json['createdAt']),
      revocationReason: json['revocationReason'] as String?,
    );
  }
}

// ── Request DTOs ────────────────────────────────────────────────────

class ApiKeyRevokeRequest {
  final String reason;

  const ApiKeyRevokeRequest({required this.reason});

  Map<String, dynamic> toJson() => {'reason': reason};
}