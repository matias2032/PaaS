import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/organization_model.dart';
import '../provider/organization_provider.dart';
import '../widget/common_widgets.dart' show ErrorBanner, ErrorState, StatusChip;
import 'organization_detail_screen.dart';


// Supervision list of every organization on the platform (SUPPORT and
// above). Paginated with "Load more"; the search box filters only what is
// already loaded because the backend has no filter parameters.
class OrganizationsScreen extends StatefulWidget {
  const OrganizationsScreen({super.key});

  @override
  State<OrganizationsScreen> createState() => _OrganizationsScreenState();
}

class _OrganizationsScreenState extends State<OrganizationsScreen> {
  final _searchController = TextEditingController();
  String _query = '';

  @override
  void initState() {
    super.initState();
    // Reload on every visit: this is a supervision view, stale data is
    // worse than a brief progress bar (the old list stays visible).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = context.read<OrganizationProvider>();
      provider.clearErrors();
      provider.loadOrganizations();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<OrganizationModel> _filter(List<OrganizationModel> all) {
    final q = _query.trim().toLowerCase();
    if (q.isEmpty) return all;
    return all
        .where((o) =>
            o.name.toLowerCase().contains(q) || o.slug.toLowerCase().contains(q))
        .toList();
  }

  void _open(OrganizationModel organization) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) =>
            OrganizationDetailScreen(publicUuid: organization.publicUuid),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<OrganizationProvider>();
    final all = provider.organizations;
    final visible = _filter(all);

    return Scaffold(
      body: Column(
        children: [
          if (provider.isLoadingOrganizations) const LinearProgressIndicator(),
          Expanded(child: _buildBody(provider, all, visible)),
        ],
      ),
    );
  }

  Widget _buildBody(
    OrganizationProvider provider,
    List<OrganizationModel> all,
    List<OrganizationModel> visible,
  ) {
    final error = provider.organizationsErrorMessage;

    if (all.isEmpty) {
      if (!provider.hasLoadedOrganizations) {
        return const Center(child: CircularProgressIndicator());
      }
      if (error != null) {
        return ErrorState(
          message: error,
          onRetry: provider.loadOrganizations,
        );
      }
      return const Center(child: Text('No organizations yet.'));
    }

    final searching = _query.trim().isNotEmpty;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (error != null)
            ErrorBanner(
              message: error,
              onDismiss: provider.clearErrors,
            ),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _searchController,
                  decoration: InputDecoration(
                    prefixIcon: const Icon(Icons.search),
                    hintText: 'Search loaded organizations by name or slug',
                    border: const OutlineInputBorder(),
                    isDense: true,
                    suffixIcon: searching
                        ? IconButton(
                            icon: const Icon(Icons.close),
                            onPressed: () {
                              _searchController.clear();
                              setState(() => _query = '');
                            },
                          )
                        : null,
                  ),
                  onChanged: (value) => setState(() => _query = value),
                ),
              ),
              const SizedBox(width: 8),
              IconButton(
                tooltip: 'Refresh',
                icon: const Icon(Icons.refresh),
                onPressed: provider.isLoadingOrganizations
                    ? null
                    : provider.loadOrganizations,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            searching
                ? '${visible.length} of ${all.length} loaded match '
                    '(${provider.totalElements} in total)'
                : 'Showing ${all.length} of ${provider.totalElements}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.builder(
              itemCount: visible.length + 1,
              itemBuilder: (context, index) {
                if (index == visible.length) {
                  return _buildFooter(provider, visible.isEmpty && searching);
                }
                return _OrganizationCard(
                  organization: visible[index],
                  onTap: () => _open(visible[index]),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFooter(OrganizationProvider provider, bool noMatches) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        children: [
          if (noMatches)
            const Padding(
              padding: EdgeInsets.only(bottom: 8),
              child: Text('No loaded organization matches your search.'),
            ),
          if (provider.hasMore)
            provider.isLoadingMore
                ? const CircularProgressIndicator()
                : OutlinedButton.icon(
                    onPressed: provider.loadMore,
                    icon: const Icon(Icons.expand_more),
                    label: const Text('Load more'),
                  ),
        ],
      ),
    );
  }
}

class _OrganizationCard extends StatelessWidget {
  final OrganizationModel organization;
  final VoidCallback onTap;

  const _OrganizationCard({required this.organization, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final count = organization.memberCount;
    return Card(
      child: ListTile(
        title: Text(organization.name),
        subtitle: Text(
          '${organization.slug} · $count member${count == 1 ? '' : 's'}',
        ),
        trailing: StatusChip(status: organization.status),
        onTap: onTap,
      ),
    );
  }
}