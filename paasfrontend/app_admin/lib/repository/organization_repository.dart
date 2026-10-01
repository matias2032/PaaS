import '../model/organization_model.dart';
import '../service/api_exception.dart';
import '../service/organization_service.dart';

// Organization module repository — client-side business rules on top of
// OrganizationService. The backend validates the suspension reason
// (@NotBlank, max 2000) but NOT the state transitions beyond "already
// suspended" / "not suspended", and it would suspend an INACTIVE
// organization (lifting the suspension would then reactivate it behind
// the owner's back), so the guards live here.
class OrganizationRepository {
  // Upper bound for the page size requested from the backend.
  static const int maxPageSize = 100;

  final OrganizationService _service;

  OrganizationRepository({OrganizationService? service})
      : _service = service ?? OrganizationService();

  // Upper bound for the search term sent to the backend.
  static const int maxSearchLength = 100;

  Future<OrganizationPage> listOrganizations({
    int page = 0,
    int size = OrganizationService.defaultPageSize,
    String? search,
  }) {
    if (page < 0) {
      throw _invalid('Page must be 0 or greater.');
    }
    if (size < 1 || size > maxPageSize) {
      throw _invalid('Page size must be between 1 and $maxPageSize.');
    }

    final term = search?.trim();
    if (term != null && term.length > maxSearchLength) {
      throw _invalid('Search term must be at most $maxSearchLength characters.');
    }

    return _service.listOrganizations(
      page: page,
      size: size,
      search: (term == null || term.isEmpty) ? null : term,
    );
  }
  Future<OrganizationModel> getOrganization(String publicUuid) =>
      _service.getOrganization(publicUuid);

  Future<OrganizationModel> suspendOrganization(
    OrganizationModel organization, {
    required String reason,
    bool revokeApiKeys = false,
  }) {
    if (!OrganizationStatuses.canSuspend(organization.status)) {
      throw _invalid(
        organization.isSuspended
            ? 'This organization is already suspended.'
            : 'Only ACTIVE organizations can be suspended.',
      );
    }

    final cleanReason = reason.trim();
    if (cleanReason.isEmpty) {
      throw _invalid('A suspension reason is required.');
    }
    if (cleanReason.length >
        OrganizationValidators.maxSuspensionReasonLength) {
      throw _invalid(
        'Reason must be at most '
        '${OrganizationValidators.maxSuspensionReasonLength} characters.',
      );
    }

    return _service.suspendOrganization(
      organization.publicUuid,
      OrganizationSuspendRequest(
        reason: cleanReason,
        revokeApiKeys: revokeApiKeys,
      ),
    );
  }

  Future<OrganizationModel> liftSuspension(OrganizationModel organization) {
    if (!OrganizationStatuses.canLiftSuspension(organization.status)) {
      throw _invalid('Only SUSPENDED organizations can be reactivated.');
    }
    return _service.liftSuspension(organization.publicUuid);
  }

  // ── Internal rules ────────────────────────────────────────────────

  ApiException _invalid(String message) =>
      ApiException(statusCode: 400, message: message);
}