import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/api_key_model.dart';
import '../model/organization_model.dart';
import '../provider/api_key_provider.dart';
import '../provider/organization_provider.dart';
import '../widget/common_widgets.dart'
    show ErrorBanner, ErrorState, StatusChip, formatDateTime;
import 'api_key_dialogs.dart';

// API keys of one organization (platform-side view). Reached only from
// the organization detail screen, which gates the entry point with
// isAtLeastPlatformAdmin (both backend endpoints are PLATFORM_ADMIN).
class OrganizationApiKeysScreen extends StatefulWidget {
  final String organizationPublicUuid;
  final String organizationName;

  const OrganizationApiKeysScreen({
    super.key,
    required this.organizationPublicUuid,
    required this.organizationName,
  });

  @override
  State<OrganizationApiKeysScreen> createState() =>
      _OrganizationApiKeysScreenState();
}

class _OrganizationApiKeysScreenState extends State<OrganizationApiKeysScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = context.read<ApiKeyProvider>();
      provider.clearErrors();
      provider.loadKeys(widget.organizationPublicUuid);
    });
  }

  Future<void> _revoke(ApiKeyModel key) async {
    context.read<ApiKeyProvider>().clearErrors();
    final done = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (_) => RevokeApiKeyDialog(
        organizationPublicUuid: widget.organizationPublicUuid,
        keyPublicUuid: key.publicUuid,
        keyName: key.name,
      ),
    );
    if (done == true && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('API key revoked.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ApiKeyProvider>();
    final orgUuid = widget.organizationPublicUuid;
    final keys = provider.keysOf(orgUuid);
    final loading = provider.isLoading(orgUuid);
    final loaded = provider.hasLoaded(orgUuid);
    final error = provider.errorMessage;

    return Scaffold(
      appBar: AppBar(
        title: Text('API keys · ${widget.organizationName}'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: loading ? null : () => provider.loadKeys(orgUuid),
          ),
        ],
      ),
      body: _buildBody(provider, keys, loading, loaded, error),
    );
  }

  Widget _buildBody(
    ApiKeyProvider provider,
    List<ApiKeyModel> keys,
    bool loading,
    bool loaded,
    String? error,
  ) {
    final orgUuid = widget.organizationPublicUuid;
    final organization =
        context.watch<OrganizationProvider>().organizationByUuid(orgUuid);

    if (!loaded) {
      return const Center(child: CircularProgressIndicator());
    }
    if (keys.isEmpty && error != null) {
      return ErrorState(
        message: error,
        onRetry: () => provider.loadKeys(orgUuid),
      );
    }

    return Column(
      children: [
        if (loading) const LinearProgressIndicator(),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              if (organization != null && !organization.isActive)
                _KeysNotAcceptedBanner(organization: organization),
              if (error != null)
                ErrorBanner(message: error, onDismiss: provider.clearErrors),
              if (keys.isEmpty)
                const Padding(
                  padding: EdgeInsets.only(top: 32),
                  child: Center(
                    child: Text('This organization has no API keys.'),
                  ),
                )
              else
                for (final key in keys)
                  _ApiKeyCard(
                    apiKey: key,
                    updating: provider.isUpdating(key.publicUuid),
                    onRevoke: () => _revoke(key),
                  ),
            ],
          ),
        ),
      ],
    );
  }
}

// Shown while the organization is SUSPENDED or INACTIVE: its keys stay
// listed (and revocable by the platform) but are not accepted at runtime.
class _KeysNotAcceptedBanner extends StatelessWidget {
  final OrganizationModel organization;

  const _KeysNotAcceptedBanner({required this.organization});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final suspended = organization.isSuspended;
    final background =
        suspended ? scheme.errorContainer : scheme.secondaryContainer;
    final foreground =
        suspended ? scheme.onErrorContainer : scheme.onSecondaryContainer;

    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        suspended
            ? 'This organization is suspended. Its API keys are not accepted '
                'until the suspension is lifted.'
            : 'This organization is inactive. Its API keys are not accepted '
                'until the owner reactivates it.',
        style: TextStyle(color: foreground),
      ),
    );
  }
}

class _ApiKeyCard extends StatelessWidget {
  final ApiKeyModel apiKey;
  final bool updating;
  final VoidCallback onRevoke;

  const _ApiKeyCard({
    required this.apiKey,
    required this.updating,
    required this.onRevoke,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final reason = apiKey.revocationReason;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    apiKey.name,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                // Expiry is derived on the client; the backend keeps ACTIVE.
                if (apiKey.isActive && apiKey.isExpired) ...[
                  const StatusChip(status: 'EXPIRED'),
                  const SizedBox(width: 8),
                ],
                StatusChip(status: apiKey.status),
              ],
            ),
            const SizedBox(height: 8),
            _InfoRow(label: 'Prefix', value: apiKey.keyPrefix),
            _InfoRow(
              label: 'Created',
              value: formatDateTime(apiKey.createdAt),
            ),
            _InfoRow(
              label: 'Last used',
              value: apiKey.lastUsedAt == null
                  ? 'Never'
                  : formatDateTime(apiKey.lastUsedAt!),
            ),
            _InfoRow(
              label: 'Expires',
              value: apiKey.expiresAt == null
                  ? 'Never'
                  : formatDateTime(apiKey.expiresAt!),
            ),
            _InfoRow(label: 'ID', value: apiKey.publicUuid),
            if (apiKey.isRevoked) ...[
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: scheme.errorContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  (reason == null || reason.trim().isEmpty)
                      ? 'Revoked by the organization owner (no reason).'
                      : 'Revocation reason: $reason',
                  style: TextStyle(color: scheme.onErrorContainer),
                ),
              ),
            ],
            if (ApiKeyStatuses.canRevoke(apiKey.status)) ...[
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerLeft,
                child: FilledButton.icon(
                  onPressed: updating ? null : onRevoke,
                  style: FilledButton.styleFrom(
                    backgroundColor: scheme.error,
                    foregroundColor: scheme.onError,
                  ),
                  icon: updating
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.key_off),
                  label: const Text('Revoke'),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
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