import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/organization_model.dart';
import '../provider/auth_provider.dart';
import '../provider/organization_provider.dart';
import 'organization_api_keys_screen.dart';
import 'organization_dialogs.dart';
import '../widget/common_widgets.dart' show ErrorBanner, ErrorState, StatusChip, formatDateTime;

// Read-only detail of one organization plus the platform-side actions
// (suspend / lift suspension, PLATFORM_ADMIN and above). Reads the
// organization from the provider instead of keeping a copy.
class OrganizationDetailScreen extends StatefulWidget {
  final String publicUuid;

  const OrganizationDetailScreen({super.key, required this.publicUuid});

  @override
  State<OrganizationDetailScreen> createState() =>
      _OrganizationDetailScreenState();
}

class _OrganizationDetailScreenState extends State<OrganizationDetailScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = context.read<OrganizationProvider>();
      provider.clearErrors();
      // Re-read through the admin endpoint so status and reason are fresh.
      if (provider.organizationByUuid(widget.publicUuid) != null) {
        provider.refreshOrganization(widget.publicUuid);
      }
    });
  }

  Future<void> _suspend(OrganizationModel organization) async {
    context.read<OrganizationProvider>().clearErrors();
    final done = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (_) => SuspendOrganizationDialog(
        publicUuid: organization.publicUuid,
        organizationName: organization.name,
      ),
    );
    if (done == true && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Organization suspended.')),
      );
    }
  }

  Future<void> _liftSuspension(OrganizationModel organization) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Lift suspension of ${organization.name}?'),
        content: const Text(
          'The organization goes back to ACTIVE and the suspension reason '
          'is cleared.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Lift suspension'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    final provider = context.read<OrganizationProvider>();
    provider.clearErrors();
    final ok = await provider.liftSuspension(organization.publicUuid);
    if (ok && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Suspension lifted.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<OrganizationProvider>();
    final canManage = context.watch<AuthProvider>().isAtLeastPlatformAdmin;
    final organization = provider.organizationByUuid(widget.publicUuid);
    final updating = provider.isUpdating(widget.publicUuid);
    final error = provider.organizationsErrorMessage;

    return Scaffold(
      appBar: AppBar(
        title: Text(organization?.name ?? 'Organization'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: organization == null || updating
                ? null
                : () => provider.refreshOrganization(widget.publicUuid),
          ),
        ],
      ),
      body: organization == null
          ? const Center(
              child: Text('Organization not found. Go back and reload the list.'),
            )
          : Column(
              children: [
                if (updating) const LinearProgressIndicator(),
                Expanded(
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (error != null)
                        ErrorBanner(
                          message: error,
                          onDismiss: provider.clearErrors,
                        ),
                      _buildInfoCard(organization),
                      if (organization.isSuspended) ...[
                        const SizedBox(height: 12),
                        _buildSuspensionCard(organization),
                      ],
                      if (organization.isInactive) ...[
                        const SizedBox(height: 12),
                        const Text(
                          'This organization was deactivated by its owner. '
                          'It cannot be suspended until the owner '
                          'reactivates it.',
                        ),
                      ],
                      if (canManage) ...[
                        const SizedBox(height: 16),
                        _buildActions(organization, updating),
                        const SizedBox(height: 12),
                        // Both api_key admin endpoints are PLATFORM_ADMIN.
                        Align(
                          alignment: Alignment.centerLeft,
                          child: OutlinedButton.icon(
                            onPressed: () => Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (_) => OrganizationApiKeysScreen(
                                  organizationPublicUuid: organization.publicUuid,
                                  organizationName: organization.name,
                                ),
                              ),
                            ),
                            icon: const Icon(Icons.vpn_key_outlined),
                            label: const Text('API keys'),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
    );
  }

  Widget _buildInfoCard(OrganizationModel organization) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    organization.name,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                StatusChip(status: organization.status),
              ],
            ),
            const SizedBox(height: 12),
            _InfoRow(label: 'Slug', value: organization.slug),
            _InfoRow(label: 'Members', value: '${organization.memberCount}'),
            _InfoRow(
              label: 'Created',
              value: formatDateTime(organization.createdAt),
            ),
            _InfoRow(
              label: 'Updated',
              value: formatDateTime(organization.updatedAt),
            ),
            _InfoRow(label: 'ID', value: organization.publicUuid),
          ],
        ),
      ),
    );
  }

  Widget _buildSuspensionCard(OrganizationModel organization) {
    final scheme = Theme.of(context).colorScheme;
    final reason = organization.suspensionReason;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: scheme.errorContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Suspension reason',
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
                  color: scheme.onErrorContainer,
                ),
          ),
          const SizedBox(height: 4),
          Text(
            (reason == null || reason.trim().isEmpty)
                ? 'No reason recorded.'
                : reason,
            style: TextStyle(color: scheme.onErrorContainer),
          ),
        ],
      ),
    );
  }

  Widget _buildActions(OrganizationModel organization, bool updating) {
    final scheme = Theme.of(context).colorScheme;

    if (OrganizationStatuses.canSuspend(organization.status)) {
      return Align(
        alignment: Alignment.centerLeft,
        child: FilledButton.icon(
          onPressed: updating ? null : () => _suspend(organization),
          style: FilledButton.styleFrom(
            backgroundColor: scheme.error,
            foregroundColor: scheme.onError,
          ),
          icon: const Icon(Icons.block),
          label: const Text('Suspend'),
        ),
      );
    }

    if (OrganizationStatuses.canLiftSuspension(organization.status)) {
      return Align(
        alignment: Alignment.centerLeft,
        child: FilledButton.icon(
          onPressed: updating ? null : () => _liftSuspension(organization),
          icon: const Icon(Icons.lock_open),
          label: const Text('Lift suspension'),
        ),
      );
    }

    return const SizedBox.shrink();
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 90,
            child: Text(label, style: Theme.of(context).textTheme.bodySmall),
          ),
          Expanded(child: SelectableText(value)),
        ],
      ),
    );
  }
}