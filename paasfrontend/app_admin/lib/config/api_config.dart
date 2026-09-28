import 'package:flutter/foundation.dart';

class ApiConfig {
  // ── Configuração de ambiente ──────────────────────────────────────

  static const String _baseUrlFromEnv = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  static String? _baseUrlCache;

  // ── Resolução do baseUrl ──────────────────────────────────────────

  static Future<String> get baseUrlAsync async {
    if (_baseUrlCache != null) return _baseUrlCache!;

    _baseUrlCache = _baseUrlFromEnv;
    return _baseUrlCache!;
  }

  static String get baseUrl {
    if (_baseUrlCache != null) return _baseUrlCache!;
    return _baseUrlFromEnv;
  }

  // ── Caminhos relativos — AUTH ────────────────────────────────────

  static const String _auth = '/api/auth';
  static const String _authLogin = '/api/auth/login';
  static const String _authStaff = '/api/auth/staff';

  // ── Caminhos relativos — ADMIN (supervisão) ────────────────────────

  static const String _adminOrganizations = '/api/admin/organizations';

  // Suspend / lift-suspension live on the regular organization controller
  // (hasRole('PLATFORM_ADMIN')), not under /api/admin.
  static const String _organizations = '/api/organizations';

  // ── Relative paths — INFRASTRUCTURE ─────────────────────────────────

  static const String _infra = '/api/infrastructure';

  // ── Relative paths — BILLING (catalog, admin view) ────────────────

  static const String _plans = '/api/plans';

  // ── URLs completas — AUTH ───────────────────────────────────────────

  static String get authUrl => '$baseUrl$_auth';
  static String get authLoginUrl => '$baseUrl$_authLogin';
  static String get authStaffUrl => '$baseUrl$_authStaff';

  static String authByPublicUuidUrl(String publicUuid) =>
      '$baseUrl$_auth/$publicUuid';

    static String authStaffActiveUrl(String publicUuid) =>
      '$authStaffUrl/$publicUuid/active';    

  static String authPlatformRoleUrl(String publicUuid) =>
      '$baseUrl$_auth/$publicUuid/platform-role';

  static String authMeUrl() => '$baseUrl$_auth/me';

  static String authMePasswordUrl() => '$baseUrl$_auth/me/password';

  // ── URLs completas — ADMIN (supervisão) ─────────────────────────────

  static String get adminOrganizationsUrl => '$baseUrl$_adminOrganizations';

  static String adminOrganizationByPublicUuidUrl(String publicUuid) =>
      '$baseUrl$_adminOrganizations/$publicUuid';

  static String adminOrganizationApiKeysUrl(String orgPublicUuid) =>
      '$baseUrl$_adminOrganizations/$orgPublicUuid/api-keys';

  static String adminApiKeyRevokeUrl(String keyPublicUuid) =>
    '$baseUrl/api/admin/api-keys/$keyPublicUuid/revoke';    

  static String get adminAuditLogsUrl => '$baseUrl/api/admin/audit-logs';

      

  static String organizationSuspendUrl(String publicUuid) =>
      '$baseUrl$_organizations/$publicUuid/suspend';

  static String organizationLiftSuspensionUrl(String publicUuid) =>
      '$baseUrl$_organizations/$publicUuid/lift-suspension';

  // ── Full URLs — INFRASTRUCTURE ──────────────────────────────────────

  static String get coolifyInstancesUrl => '$baseUrl$_infra/coolify-instances';

  static String coolifyInstanceByPublicUuidUrl(String publicUuid) =>
      '$coolifyInstancesUrl/$publicUuid';

  static String coolifyInstanceStatusUrl(String publicUuid) =>
      '$coolifyInstancesUrl/$publicUuid/status';

  static String coolifyInstanceServersUrl(String publicUuid) =>
      '$coolifyInstancesUrl/$publicUuid/servers';

  static String get serversUrl => '$baseUrl$_infra/servers';

  static String serverByPublicUuidUrl(String publicUuid) =>
      '$serversUrl/$publicUuid';

  static String serverStatusUrl(String publicUuid) =>
      '$serversUrl/$publicUuid/status';

  static String get serverProvidersUrl => '$baseUrl$_infra/server-providers';

  // ── Full URLs — BILLING ───────────────────────────────────────────────

  static String get plansUrl => '$baseUrl$_plans';

  static String get plansAllUrl => '$baseUrl$_plans/all';

  // PUT (update) and DELETE (deactivate) share this URL.
  static String planByPublicUuidUrl(String publicUuid) =>
      '$plansUrl/$publicUuid';

  static String planReactivateUrl(String publicUuid) =>
      '$plansUrl/$publicUuid/reactivate';

  static String planArchiveUrl(String publicUuid) =>
      '$plansUrl/$publicUuid/archive';

  static String planResourceLimitsUrl(String publicUuid) =>
      '$plansUrl/$publicUuid/resource-limits';

  static String planPricesUrl(String publicUuid) =>
      '$plansUrl/$publicUuid/prices';

  // ── Configurações gerais ──────────────────────────────────────────

  static const Duration timeout = Duration(seconds: 30);

  static Map<String, String> get defaultHeaders => const {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };

  static Map<String, String> authHeaders(String token) => {
        ...defaultHeaders,
        'Authorization': 'Bearer $token',
      };

  static void printConfig() {
    debugPrint('🚀 API CONFIG — ${kIsWeb ? "Web" : "Desktop/Mobile"}');
    debugPrint('🔗 Base URL: $baseUrl');
    debugPrint(
      '🌍 API_BASE_URL env: ${const String.fromEnvironment('API_BASE_URL')}',
    );
  }
}