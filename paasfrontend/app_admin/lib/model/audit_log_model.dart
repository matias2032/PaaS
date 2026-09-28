import 'dart:convert';

// Audit log module models — mirror AuditLogResponseDTO plus the Spring
// Page envelope returned by GET /api/admin/audit-logs. One file per
// layer: every model of this module lives here.
//
// Read-only module: the backend has no create DTO (entries are written
// internally by other services through AuditLogService.record).

DateTime _parseDateTime(dynamic value) => DateTime.parse(value as String);

// ── Audit log entry ─────────────────────────────────────────────────

class AuditLogModel {
  // Internal id; only used as a stable key (dedupe between pages).
  final int id;
  // Null for platform-side actions without an owning organization.
  final String? organizationPublicUuid;
  // Null for system actions with no human behind them.
  final String? userPublicUuid;
  // Free text, e.g. "ORGANIZATION_SUSPENDED".
  final String action;
  final String? resourceType;
  final String? resourceIdentifier;
  final String? ipAddress;
  final String? userAgent;
  // jsonb as text, exactly as the backend sends it.
  final String? metadataRaw;
  final DateTime createdAt;

  const AuditLogModel({
    required this.id,
    this.organizationPublicUuid,
    this.userPublicUuid,
    required this.action,
    this.resourceType,
    this.resourceIdentifier,
    this.ipAddress,
    this.userAgent,
    this.metadataRaw,
    required this.createdAt,
  });

  /// Metadata ready to display: pretty-printed JSON when it parses,
  /// the raw text otherwise, null when empty. The entity maps a String
  /// to jsonb, which may come back double-encoded (a JSON string that
  /// itself contains JSON), so a decoded String is decoded once more.
  String? get metadataPretty {
    final raw = metadataRaw;
    if (raw == null || raw.trim().isEmpty) return null;

    try {
      var decoded = jsonDecode(raw);
      if (decoded is String) {
        try {
          decoded = jsonDecode(decoded);
        } catch (_) {
          return decoded as String;
        }
      }
      if (decoded == null) return null;
      return const JsonEncoder.withIndent('  ').convert(decoded);
    } catch (_) {
      return raw;
    }
  }

  bool get hasMetadata => metadataPretty != null;

  /// Text used by the client-side search.
  String get searchText => [
        action,
        resourceType,
        resourceIdentifier,
        organizationPublicUuid,
        userPublicUuid,
        ipAddress,
      ].whereType<String>().join(' ').toLowerCase();

  factory AuditLogModel.fromJson(Map<String, dynamic> json) {
    return AuditLogModel(
      id: (json['idAuditLog'] as num).toInt(),
      organizationPublicUuid: json['organizationPublicUuid'] as String?,
      userPublicUuid: json['userPublicUuid'] as String?,
      action: json['action'] as String,
      resourceType: json['resourceType'] as String?,
      resourceIdentifier: json['resourceIdentifier'] as String?,
      ipAddress: json['ipAddress'] as String?,
      userAgent: json['userAgent'] as String?,
      // If the backend ever sends metadata as a real JSON object instead
      // of text, keep it usable by re-encoding it.
      metadataRaw: switch (json['metadata']) {
        null => null,
        final String s => s,
        final other => jsonEncode(other),
      },
      createdAt: _parseDateTime(json['createdAt']),
    );
  }
}

// ── Page envelope (Spring Data Page) ────────────────────────────────

class AuditLogPage {
  final List<AuditLogModel> items;
  // Zero-based index of this page.
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool isLast;

  const AuditLogPage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.isLast,
  });

  factory AuditLogPage.fromJson(Map<String, dynamic> json) {
    final content = (json['content'] as List?) ?? const [];

    // Classic Spring Page is flat; newer versions can nest the metadata
    // under "page". Accept both (same as OrganizationPage).
    final nested = json['page'];
    final meta = nested is Map<String, dynamic> ? nested : json;

    final number = (meta['number'] as num?)?.toInt() ?? 0;
    final totalPages = (meta['totalPages'] as num?)?.toInt() ?? 1;

    return AuditLogPage(
      items: content
          .map((e) => AuditLogModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      page: number,
      size: (meta['size'] as num?)?.toInt() ?? content.length,
      totalElements: (meta['totalElements'] as num?)?.toInt() ?? content.length,
      totalPages: totalPages,
      isLast: (json['last'] as bool?) ?? (number + 1 >= totalPages),
    );
  }
}