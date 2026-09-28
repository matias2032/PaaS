// Organization module models — mirror the paasbackend organization DTOs
// (OrganizationResponseDTO, OrganizationSuspendRequestDTO) plus the Spring
// Page envelope returned by GET /api/admin/organizations. One file per
// layer: every model of this module lives here.
//
// Members and roles are not modelled: their endpoints require membership
// of the organization, so the admin app never calls them (the response
// only exposes memberCount).

// ── Catalogs and client-side rules ──────────────────────────────────

class OrganizationStatuses {
  OrganizationStatuses._();

  static const String active = 'ACTIVE';
  static const String inactive = 'INACTIVE';
  static const String suspended = 'SUSPENDED';

  // Mirrors ck_organizations_status.
  static const List<String> all = [active, suspended, inactive];

  // The backend would also suspend an INACTIVE organization, and lifting
  // the suspension always goes back to ACTIVE — which would silently
  // reactivate something its owner deactivated. So the client only
  // offers suspension for ACTIVE organizations.
  static bool canSuspend(String status) => status == active;
  static bool canLiftSuspension(String status) => status == suspended;
}

class OrganizationValidators {
  OrganizationValidators._();

  // Mirrors @Size(max = 2000) in OrganizationSuspendRequestDTO.
  static const int maxSuspensionReasonLength = 2000;
}

DateTime _parseDateTime(dynamic value) => DateTime.parse(value as String);

// ── Organization ────────────────────────────────────────────────────

class OrganizationModel {
  final String publicUuid;
  final String name;
  final String slug;
  final String status;
  // Only set while status == SUSPENDED.
  final String? suspensionReason;
  final int memberCount;
  final DateTime createdAt;
  final DateTime updatedAt;

  const OrganizationModel({
    required this.publicUuid,
    required this.name,
    required this.slug,
    required this.status,
    this.suspensionReason,
    required this.memberCount,
    required this.createdAt,
    required this.updatedAt,
  });

  bool get isActive => status == OrganizationStatuses.active;
  bool get isSuspended => status == OrganizationStatuses.suspended;
  bool get isInactive => status == OrganizationStatuses.inactive;

  factory OrganizationModel.fromJson(Map<String, dynamic> json) {
    return OrganizationModel(
      publicUuid: json['publicUuid'] as String,
      name: json['name'] as String,
      slug: json['slug'] as String,
      status: json['status'] as String,
      suspensionReason: json['suspensionReason'] as String?,
      memberCount: (json['memberCount'] as num?)?.toInt() ?? 0,
      createdAt: _parseDateTime(json['createdAt']),
      updatedAt: _parseDateTime(json['updatedAt']),
    );
  }
}

// ── Page envelope (Spring Data Page) ────────────────────────────────

class OrganizationPage {
  final List<OrganizationModel> items;
  // Zero-based index of this page.
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool isLast;

  const OrganizationPage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.isLast,
  });

  factory OrganizationPage.fromJson(Map<String, dynamic> json) {
    final content = (json['content'] as List?) ?? const [];

    // Classic Spring Page is flat (number/size/totalElements/...); newer
    // versions can nest the metadata under "page". Accept both.
    final nested = json['page'];
    final meta = nested is Map<String, dynamic> ? nested : json;

    final number = (meta['number'] as num?)?.toInt() ?? 0;
    final totalPages = (meta['totalPages'] as num?)?.toInt() ?? 1;

    return OrganizationPage(
      items: content
          .map((e) => OrganizationModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      page: number,
      size: (meta['size'] as num?)?.toInt() ?? content.length,
      totalElements: (meta['totalElements'] as num?)?.toInt() ?? content.length,
      totalPages: totalPages,
      isLast: (json['last'] as bool?) ?? (number + 1 >= totalPages),
    );
  }
}

// ── Request DTOs ────────────────────────────────────────────────────

// Only the platform-side suspend action has a body; lift-suspension takes
// none.
class OrganizationSuspendRequest {
  final String reason;
  // Irreversible: the backend also revokes every ACTIVE API key.
  final bool revokeApiKeys;

  const OrganizationSuspendRequest({
    required this.reason,
    this.revokeApiKeys = false,
  });

  Map<String, dynamic> toJson() => {
        'reason': reason,
        'revokeApiKeys': revokeApiKeys,
      };
}