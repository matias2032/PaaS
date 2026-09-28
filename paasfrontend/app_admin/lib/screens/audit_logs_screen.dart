import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/audit_log_model.dart';
import '../provider/audit_log_provider.dart';
import '../widget/common_widgets.dart'
    show ErrorBanner, ErrorState, formatDateTime;

// Platform-wide audit log (read-only, SUPPORT and above). The backend has
// no filters, so the search box only filters the entries already loaded;
// "Load more" pulls further pages.
class AuditLogsScreen extends StatefulWidget {
  const AuditLogsScreen({super.key});

  @override
  State<AuditLogsScreen> createState() => _AuditLogsScreenState();
}

class _AuditLogsScreenState extends State<AuditLogsScreen> {
  final _searchController = TextEditingController();
  String _query = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = context.read<AuditLogProvider>();
      provider.clearErrors();
      provider.loadLogs();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<AuditLogProvider>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Audit log'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: provider.isLoading ? null : provider.loadLogs,
          ),
        ],
      ),
      body: _buildBody(provider),
    );
  }

  Widget _buildBody(AuditLogProvider provider) {
    final error = provider.errorMessage;

    if (!provider.hasLoaded) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.logs.isEmpty && error != null) {
      return ErrorState(message: error, onRetry: provider.loadLogs);
    }

    final query = _query.trim().toLowerCase();
    final visible = query.isEmpty
        ? provider.logs
        : provider.logs.where((l) => l.searchText.contains(query)).toList();

    return Column(
      children: [
        if (provider.isLoading) const LinearProgressIndicator(),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Search loaded entries (action, resource, IDs, IP)',
              prefixIcon: const Icon(Icons.search),
              border: const OutlineInputBorder(),
              isDense: true,
              suffixIcon: _query.isEmpty
                  ? null
                  : IconButton(
                      tooltip: 'Clear',
                      icon: const Icon(Icons.close),
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _query = '');
                      },
                    ),
            ),
            onChanged: (value) => setState(() => _query = value),
          ),
        ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: ErrorBanner(message: error, onDismiss: provider.clearErrors),
          ),
        Expanded(
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            // One extra row for the footer.
            itemCount: visible.length + 1,
            itemBuilder: (context, index) {
              if (index == visible.length) {
                return _buildFooter(provider, visible.length, query.isNotEmpty);
              }
              return _AuditLogCard(log: visible[index]);
            },
          ),
        ),
      ],
    );
  }

  Widget _buildFooter(
    AuditLogProvider provider,
    int visibleCount,
    bool filtering,
  ) {
    final theme = Theme.of(context);

    if (provider.logs.isEmpty) {
      return const Padding(
        padding: EdgeInsets.only(top: 32),
        child: Center(child: Text('No audit log entries yet.')),
      );
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        children: [
          if (filtering && visibleCount == 0)
            const Padding(
              padding: EdgeInsets.only(bottom: 8),
              child: Text('No loaded entry matches the search.'),
            ),
          Text(
            filtering
                ? '$visibleCount matching · ${provider.logs.length} of '
                    '${provider.totalElements} loaded'
                : '${provider.logs.length} of ${provider.totalElements} loaded',
            style: theme.textTheme.bodySmall,
          ),
          if (provider.hasMore) ...[
            const SizedBox(height: 8),
            provider.isLoadingMore
                ? const Padding(
                    padding: EdgeInsets.all(8),
                    child: SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  )
                : OutlinedButton.icon(
                    onPressed: provider.loadMore,
                    icon: const Icon(Icons.expand_more),
                    label: const Text('Load more'),
                  ),
          ],
        ],
      ),
    );
  }
}

class _AuditLogCard extends StatelessWidget {
  final AuditLogModel log;

  const _AuditLogCard({required this.log});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final metadata = log.metadataPretty;
    final resource = [log.resourceType, log.resourceIdentifier]
        .whereType<String>()
        .where((s) => s.isNotEmpty)
        .join(' · ');

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    log.action,
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  formatDateTime(log.createdAt),
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
            const SizedBox(height: 8),
            _InfoRow(label: 'Resource', value: resource),
            _InfoRow(label: 'Organization', value: log.organizationPublicUuid),
            _InfoRow(label: 'User', value: log.userPublicUuid),
            _InfoRow(label: 'IP', value: log.ipAddress),
            if (metadata != null || log.userAgent != null)
              Theme(
                // Removes the divider lines ExpansionTile draws by default.
                data: theme.copyWith(dividerColor: Colors.transparent),
                child: ExpansionTile(
                  tilePadding: EdgeInsets.zero,
                  childrenPadding: EdgeInsets.zero,
                  expandedCrossAxisAlignment: CrossAxisAlignment.start,
                  title: Text('Details', style: theme.textTheme.labelLarge),
                  children: [
                    if (log.userAgent != null)
                      _InfoRow(label: 'User agent', value: log.userAgent),
                    if (metadata != null) ...[
                      const SizedBox(height: 4),
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: theme.colorScheme.surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: SelectableText(
                          metadata,
                          style: const TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 12,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String? value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final text = (value == null || value!.isEmpty) ? '—' : value!;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(label, style: Theme.of(context).textTheme.bodySmall),
          ),
          Expanded(child: SelectableText(text)),
        ],
      ),
    );
  }
}