import '../config/api_config.dart';
import '../model/audit_log_model.dart';
import 'api_client.dart';

// Audit log module service — thin HTTP layer over ApiClient.
//
// Single endpoint: GET /api/admin/audit-logs (hasRole('SUPPORT')). There
// is no detail endpoint, no filters and no public write endpoint.
class AuditLogService {
  // Page size sent when the caller does not specify one.
  static const int defaultPageSize = 20;

  // The backend defaults to createdAt,desc, but the sort is always sent
  // explicitly so paging stays deterministic if that default changes.
  Future<AuditLogPage> listAuditLogs({
    int page = 0,
    int size = defaultPageSize,
  }) async {
    final url = Uri.parse(ApiConfig.adminAuditLogsUrl).replace(
      queryParameters: {
        'page': '$page',
        'size': '$size',
        'sort': 'createdAt,desc',
      },
    ).toString();

    final json = await ApiClient.get(url);
    return AuditLogPage.fromJson(json as Map<String, dynamic>);
  }
}