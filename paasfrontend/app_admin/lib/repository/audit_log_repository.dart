import '../model/audit_log_model.dart';
import '../service/api_exception.dart';
import '../service/audit_log_service.dart';

// Audit log module repository — client-side guards on top of
// AuditLogService. Read-only module, so the only rules are the paging
// bounds.
class AuditLogRepository {
  // Upper bound for the page size requested from the backend.
  static const int maxPageSize = 100;

  final AuditLogService _service;

  AuditLogRepository({AuditLogService? service})
      : _service = service ?? AuditLogService();

  Future<AuditLogPage> listAuditLogs({
    int page = 0,
    int size = AuditLogService.defaultPageSize,
  }) {
    if (page < 0) {
      throw _invalid('Page must be 0 or greater.');
    }
    if (size < 1 || size > maxPageSize) {
      throw _invalid('Page size must be between 1 and $maxPageSize.');
    }
    return _service.listAuditLogs(page: page, size: size);
  }

  ApiException _invalid(String message) =>
      ApiException(statusCode: 400, message: message);
}