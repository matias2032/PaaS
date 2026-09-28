import 'package:flutter/foundation.dart';
import '../model/organization_model.dart';
import '../repository/organization_repository.dart';
import '../service/api_exception.dart';

// Organization module provider — state for the organization supervision
// list (admin view). The list is paginated ("load more"); mutations
// (suspend / lift suspension) return bool and expose the error message
// through organizationsErrorMessage. Results of mutations replace the
// cached organization, so the detail screen just reads it from here.
class OrganizationProvider extends ChangeNotifier {
  final OrganizationRepository _repository;

  OrganizationProvider({OrganizationRepository? repository})
      : _repository = repository ?? OrganizationRepository();

  // ── State ─────────────────────────────────────────────────────────

  List<OrganizationModel> _organizations = [];
  bool _isLoadingOrganizations = false;
  bool _isLoadingMore = false;
  // Distinguishes "not loaded yet" from "loaded and empty".
  bool _hasLoadedOrganizations = false;
  bool _hasMore = false;
  int _nextPage = 0;
  int _totalElements = 0;
  String? _organizationsErrorMessage;

  // publicUuids with a mutation in flight — lets the UI disable the
  // controls of that organization and avoid double submissions.
  final Set<String> _updating = {};

  // Bumped by every refresh and by reset(). A response that comes back
  // after that (e.g. a request started before logout) is discarded
  // instead of being written into the new state.
  int _generation = 0;

  // ── Getters ───────────────────────────────────────────────────────

  List<OrganizationModel> get organizations => _organizations;
  bool get isLoadingOrganizations => _isLoadingOrganizations;
  bool get isLoadingMore => _isLoadingMore;
  bool get hasLoadedOrganizations => _hasLoadedOrganizations;
  bool get hasMore => _hasMore;
  int get totalElements => _totalElements;
  String? get organizationsErrorMessage => _organizationsErrorMessage;

  bool isUpdating(String publicUuid) => _updating.contains(publicUuid);

  OrganizationModel? organizationByUuid(String publicUuid) {
    for (final organization in _organizations) {
      if (organization.publicUuid == publicUuid) return organization;
    }
    return null;
  }

  // ── Organizations ─────────────────────────────────────────────────

  /// Loads the first page and replaces the list. If a list is already
  /// loaded and the refresh fails, the list is kept and only the error
  /// message is set (the screen shows it as a dismissible banner).
  Future<void> loadOrganizations() async {
    if (_isLoadingOrganizations) return;

    final generation = ++_generation;
    _isLoadingOrganizations = true;
    _isLoadingMore = false;
    _organizationsErrorMessage = null;
    notifyListeners();

    try {
      final page = await _repository.listOrganizations(page: 0);
      if (generation != _generation) return;

      _organizations = page.items;
      _totalElements = page.totalElements;
      _hasMore = !page.isLast;
      _nextPage = page.page + 1;
    } catch (e) {
      if (generation != _generation) return;
      _organizationsErrorMessage = _messageFrom(e);
    }

    _isLoadingOrganizations = false;
    _hasLoadedOrganizations = true;
    notifyListeners();
  }

  /// Appends the next page. Does nothing while another load is running,
  /// before the first load, or when there are no more pages.
  Future<void> loadMore() async {
    if (_isLoadingOrganizations ||
        _isLoadingMore ||
        !_hasLoadedOrganizations ||
        !_hasMore) {
      return;
    }

    final generation = _generation;
    _isLoadingMore = true;
    _organizationsErrorMessage = null;
    notifyListeners();

    try {
      final page = await _repository.listOrganizations(page: _nextPage);
      if (generation != _generation) return;

      // Organizations created while paging shift the offsets (the list is
      // sorted by createdAt desc), so an item can show up twice. Skip
      // the ones already held.
      final known = _organizations.map((o) => o.publicUuid).toSet();
      _organizations = [
        ..._organizations,
        ...page.items.where((o) => !known.contains(o.publicUuid)),
      ];
      _totalElements = page.totalElements;
      _hasMore = !page.isLast;
      _nextPage = page.page + 1;
    } catch (e) {
      if (generation != _generation) return;
      _organizationsErrorMessage = _messageFrom(e);
    }

    _isLoadingMore = false;
    notifyListeners();
  }

  /// Re-reads one organization through the admin endpoint and merges it
  /// into the list (used by the detail screen).
  Future<bool> refreshOrganization(String publicUuid) {
    return _runOnOrganization(publicUuid, (organization) async {
      _replaceOrganization(await _repository.getOrganization(publicUuid));
    });
  }

  Future<bool> suspendOrganization(
    String publicUuid, {
    required String reason,
    bool revokeApiKeys = false,
  }) {
    return _runOnOrganization(publicUuid, (organization) async {
      _replaceOrganization(
        await _repository.suspendOrganization(
          organization,
          reason: reason,
          revokeApiKeys: revokeApiKeys,
        ),
      );
    });
  }

  Future<bool> liftSuspension(String publicUuid) {
    return _runOnOrganization(publicUuid, (organization) async {
      _replaceOrganization(await _repository.liftSuspension(organization));
    });
  }

  // ── Housekeeping ──────────────────────────────────────────────────

  void clearErrors() {
    _organizationsErrorMessage = null;
    notifyListeners();
  }

  /// Drops everything held in memory — call on logout so the next admin
  /// never sees the previous session's data. Also invalidates any
  /// request still in flight.
  void reset() {
    _generation++;
    _organizations = [];
    _isLoadingOrganizations = false;
    _isLoadingMore = false;
    _hasLoadedOrganizations = false;
    _hasMore = false;
    _nextPage = 0;
    _totalElements = 0;
    _organizationsErrorMessage = null;
    _updating.clear();
    notifyListeners();
  }

  // ── Internal ──────────────────────────────────────────────────────

  Future<bool> _runOnOrganization(
    String publicUuid,
    Future<void> Function(OrganizationModel organization) action,
  ) async {
    _organizationsErrorMessage = null;
    _updating.add(publicUuid);
    notifyListeners();

    try {
      final organization = organizationByUuid(publicUuid);
      if (organization == null) {
        throw ApiException(
          statusCode: 404,
          message: 'Organization not found. Reload the list and try again.',
        );
      }
      await action(organization);
      return true;
    } catch (e) {
      _organizationsErrorMessage = _messageFrom(e);
      return false;
    } finally {
      _updating.remove(publicUuid);
      notifyListeners();
    }
  }

  void _replaceOrganization(OrganizationModel updated) {
    _organizations = _organizations
        .map((o) => o.publicUuid == updated.publicUuid ? updated : o)
        .toList();
  }

  // ApiException carries a user-facing message; anything else (e.g. a
  // FormatException from fromJson) gets a generic one, so loading flags
  // never stay stuck on an unexpected error.
  String _messageFrom(Object error) {
    if (error is ApiException) return error.message;
    return 'Unexpected error. Please try again.';
  }
}