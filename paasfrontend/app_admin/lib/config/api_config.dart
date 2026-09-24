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

  // ── Caminhos relativos — BILLING (catálogo, vista admin) ──────────

  static const String _plansAll = '/api/plans/all';

  // ── URLs completas — AUTH ───────────────────────────────────────────

  static String get authUrl => '$baseUrl$_auth';
  static String get authLoginUrl => '$baseUrl$_authLogin';
  static String get authStaffUrl => '$baseUrl$_authStaff';

  static String authByPublicUuidUrl(String publicUuid) =>
      '$baseUrl$_auth/$publicUuid';

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

  // ── URLs completas — BILLING ─────────────────────────────────────────

  static String get plansAllUrl => '$baseUrl$_plansAll';

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