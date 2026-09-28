import 'package:flutter/foundation.dart';
import '../model/audit_log_model.dart';
import '../repository/audit_log_repository.dart';
import '../service/api_exception.dart';

// Audit log module provider — state for the audit log list (admin view).
// The list is paginated ("load more") and read-only. A failed refresh
// keeps the list already loaded and only sets the error message (the
// screen shows it as a dismissible banner).
class AuditLogProvider extends ChangeNotifier {
  final AuditLogRepository _repository;

  AuditLogProvider({AuditLogRepository? repository})
      : _repository = repository ?? AuditLogRepository();

  // ── State ─────────────────────────────────────────────────────────

  List<AuditLogModel> _logs = [];
  bool _isLoading = false;
  bool _isLoadingMore = false;
  // Distinguishes "not loaded yet" from "loaded and empty".
  bool _hasLoaded = false;
  bool _hasMore = false;
  int _nextPage = 0;
  int _totalElements = 0;
  String? _errorMessage;

  // Bumped by every refresh and by reset(). A response that comes back
  // after that (e.g. a request started before logout) is discarded
  // instead of being written into the new state.
  int _generation = 0;

  // ── Getters ───────────────────────────────────────────────────────

  List<AuditLogModel> get logs => _logs;
  bool get isLoading => _isLoading;
  bool get isLoadingMore => _isLoadingMore;
  bool get hasLoaded => _hasLoaded;
  bool get hasMore => _hasMore;
  int get totalElements => _totalElements;
  String? get errorMessage => _errorMessage;

  // ── Logs ──────────────────────────────────────────────────────────

  /// Loads the first page and replaces the list.
  Future<void> loadLogs() async {
    if (_isLoading) return;

    final generation = ++_generation;
    _isLoading = true;
    _isLoadingMore = false;
    _errorMessage = null;
    notifyListeners();

    try {
      final page = await _repository.listAuditLogs(page: 0);
      if (generation != _generation) return;

      _logs = page.items;
      _totalElements = page.totalElements;
      _hasMore = !page.isLast;
      _nextPage = page.page + 1;
    } catch (e) {
      if (generation != _generation) return;
      _errorMessage = _messageFrom(e);
    }

    _isLoading = false;
    _hasLoaded = true;
    notifyListeners();
  }

  /// Appends the next page. Does nothing while another load is running,
  /// before the first load, or when there are no more pages.
  Future<void> loadMore() async {
    if (_isLoading || _isLoadingMore || !_hasLoaded || !_hasMore) return;

    final generation = _generation;
    _isLoadingMore = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final page = await _repository.listAuditLogs(page: _nextPage);
      if (generation != _generation) return;

      // New entries written while paging shift the offsets (the list is
      // sorted by createdAt desc), so an entry can show up twice. Skip
      // the ones already held.
      final known = _logs.map((l) => l.id).toSet();
      _logs = [
        ..._logs,
        ...page.items.where((l) => !known.contains(l.id)),
      ];
      _totalElements = page.totalElements;
      _hasMore = !page.isLast;
      _nextPage = page.page + 1;
    } catch (e) {
      if (generation != _generation) return;
      _errorMessage = _messageFrom(e);
    }

    _isLoadingMore = false;
    notifyListeners();
  }

  // ── Housekeeping ──────────────────────────────────────────────────

  void clearErrors() {
    _errorMessage = null;
    notifyListeners();
  }

  /// Drops everything held in memory — call on logout so the next admin
  /// never sees the previous session's data. Also invalidates any
  /// request still in flight.
  void reset() {
    _generation++;
    _logs = [];
    _isLoading = false;
    _isLoadingMore = false;
    _hasLoaded = false;
    _hasMore = false;
    _nextPage = 0;
    _totalElements = 0;
    _errorMessage = null;
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