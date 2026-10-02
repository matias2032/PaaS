import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/organization_model.dart';
import '../model/payment_model.dart';
import '../provider/payment_provider.dart';
import '../widget/common_widgets.dart'
    show ErrorBanner, ErrorState, StatusChip, formatDateTime;
import 'invoice_detail_screen.dart';
import 'payment_dialogs.dart';

// Payments module entry point (admin view). Invoices are always scoped to
// one organization, so the screen searches organizations by name or slug
// (a pasted UUID also works) and lists the picked one's invoices.
// Tapping an invoice drills down to
// InvoiceDetailScreen. Nested Scaffold inside the HomeScreen body, same as
// PlansScreen.
//
// Reusable from the organization detail screen:
//   Navigator.push(context, MaterialPageRoute(builder: (_) => Scaffold(
//     appBar: AppBar(title: const Text('Invoices')),
//     body: PaymentsScreen(initialOrgPublicUuid: org.publicUuid),
//   )));
class PaymentsScreen extends StatefulWidget {
  final String? initialOrgPublicUuid;

  const PaymentsScreen({super.key, this.initialOrgPublicUuid});

  @override
  State<PaymentsScreen> createState() => _PaymentsScreenState();
}

class _PaymentsScreenState extends State<PaymentsScreen> {
  final _searchController = TextEditingController();
  Timer? _debounce;

  // Whether the organization results panel is open (user is searching).
  bool _showResults = false;

  // null = "All" (default view). Lives in the widget state on purpose, so a
  // new access to the screen always starts without a filter.
  String? _statusFilter;

  static final _uuidRegex = RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
  );

  @override
  void initState() {
    super.initState();

    // Only an explicit organization (opened from the organization detail
    // screen) pre-fills the field. Anything the provider remembers from a
    // previous visit is deliberately ignored.
    final explicit = widget.initialOrgPublicUuid;
    if (explicit != null) _searchController.text = explicit;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final p = context.read<PaymentProvider>();
      p.clearErrors();
      p.clearOrganizationSearch();

      // Provider state is app-wide, so every access starts from the
      // default view instead of whatever the last visit left behind.
      if (explicit == null) {
        p.clearInvoices();
      } else {
        p.loadInvoices(explicit); // always fresh, never a stale copy
      }
    });
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  // A pasted UUID loads straight away; anything else is a name/slug
  // search, debounced so the backend isn't hit on every keystroke.
  void _onSearchChanged(String value) {
    _debounce?.cancel();
    final term = value.trim();
    final provider = context.read<PaymentProvider>();

    if (term.isEmpty) {
      _clearSearch();
      return;
    }

    if (_uuidRegex.hasMatch(term)) {
      provider.clearOrganizationSearch();
      setState(() => _showResults = false);
      _loadByUuid(term);
      return;
    }

    setState(() => _showResults = true);
    _debounce = Timer(const Duration(milliseconds: 350), () {
      if (mounted) provider.searchOrganizations(term);
    });
  }

  // Enter searches right away instead of waiting for the debounce.
  void _onSearchSubmitted(String value) {
    _debounce?.cancel();
    final term = value.trim();
    if (term.isEmpty || _uuidRegex.hasMatch(term)) return;
    setState(() => _showResults = true);
    context.read<PaymentProvider>().searchOrganizations(term);
  }

  void _selectOrganization(OrganizationModel organization) {
    FocusScope.of(context).unfocus();
    _searchController.text = organization.name;
    setState(() {
      _showResults = false;
      _statusFilter = null; // a different organization starts in the default view
    });
    context.read<PaymentProvider>()
      ..clearOrganizationSearch()
      ..loadInvoices(organization.publicUuid, organization: organization);
  }

  void _loadByUuid(String uuid) {
    FocusScope.of(context).unfocus();
    setState(() => _statusFilter = null);
    context.read<PaymentProvider>().loadInvoices(uuid);
  }

  // Full reset: search text, results panel, loaded invoices and status
  // filter. Used by the clear button and when the field is emptied.
  void _clearSearch() {
    _debounce?.cancel();
    _searchController.clear();
    context.read<PaymentProvider>()
      ..clearOrganizationSearch()
      ..clearInvoices();
    setState(() {
      _showResults = false;
      _statusFilter = null;
    });
  }
  void _reload() {
    final uuid = context.read<PaymentProvider>().loadedOrgPublicUuid;
    if (uuid != null) context.read<PaymentProvider>().loadInvoices(uuid);
  }

    String _organizationLabel(PaymentProvider payments) {
    final org = payments.loadedOrganization;
    return org == null
        ? 'Organization ${payments.loadedOrgPublicUuid}'
        : '${org.name} (${org.slug})';
  }

    // Applies the status filter and sorts by priority (pending first), then
  // newest first. Sorted here because List.sort is not guaranteed stable.
  List<InvoiceModel> _visibleInvoices(List<InvoiceModel> invoices) {
    final list = _statusFilter == null
        ? [...invoices]
        : invoices.where((i) => i.status == _statusFilter).toList();

    list.sort((a, b) {
      final byStatus = InvoiceStatuses.priority(a.status)
          .compareTo(InvoiceStatuses.priority(b.status));
      if (byStatus != 0) return byStatus;
      return b.createdAt.compareTo(a.createdAt);
    });
    return list;
  }

  @override
  Widget build(BuildContext context) {
    final payments = context.watch<PaymentProvider>();

    return Scaffold(
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('Payments',
                    style: Theme.of(context).textTheme.headlineSmall),
                const Spacer(),
                IconButton(
                  tooltip: 'Reload',
                  icon: const Icon(Icons.refresh),
                  onPressed: payments.isLoadingInvoices ||
                          payments.loadedOrgPublicUuid == null
                      ? null
                      : _reload,
                ),
              ],
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _searchController,
              textInputAction: TextInputAction.search,
              onChanged: _onSearchChanged,
              onSubmitted: _onSearchSubmitted,
              decoration: InputDecoration(
                labelText: 'Search organization (name or slug)',
                helperText: 'You can also paste an organization UUID.',
                border: const OutlineInputBorder(),
                isDense: true,
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isEmpty
                    ? null
                    : IconButton(
                        tooltip: 'Clear',
                        icon: const Icon(Icons.clear),
                        onPressed: _clearSearch,
                      ),
              ),
            ),
            if (_showResults) ...[
              const SizedBox(height: 4),
              _OrganizationResults(
                results: payments.organizationResults,
                isSearching: payments.isSearchingOrganizations,
                hasSearched: payments.hasSearchedOrganizations,
                errorMessage: payments.organizationSearchError,
                onSelected: _selectOrganization,
              ),
            ],
            const SizedBox(height: 12),
            Expanded(child: _buildContent(payments)),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(PaymentProvider payments) {
    if (!payments.hasLoadedInvoices && !payments.isLoadingInvoices) {
      return const Center(
        child: Text('Search an organization by name or slug to see its invoices.'),
      );
    }

    if (payments.invoices.isEmpty) {
      if (payments.isLoadingInvoices) {
        return const Center(child: CircularProgressIndicator());
      }
      if (payments.invoicesErrorMessage != null) {
        return ErrorState(
          message: payments.invoicesErrorMessage!,
          onRetry: _reload,
        );
      }
      return const Center(child: Text('This organization has no invoices.'));
    }

    final visible = _visibleInvoices(payments.invoices);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (payments.isLoadingInvoices) const LinearProgressIndicator(),
        if (payments.invoicesErrorMessage != null)
          ErrorBanner(
            message: payments.invoicesErrorMessage!,
            onDismiss: payments.clearErrors,
          ),
        if (payments.loadedOrgPublicUuid != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text(
              '${_organizationLabel(payments)} · '
              '${visible.length} of ${payments.invoices.length} invoice(s)',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: _StatusFilterBar(
            invoices: payments.invoices,
            selected: _statusFilter,
            onSelected: (status) => setState(() => _statusFilter = status),
          ),
        ),
        Expanded(
          child: visible.isEmpty
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('No ${_statusFilter?.toLowerCase()} invoices.'),
                      TextButton(
                        onPressed: () => setState(() => _statusFilter = null),
                        child: const Text('Show all'),
                      ),
                    ],
                  ),
                )
              : ListView.separated(
                  itemCount: visible.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 8),
                  itemBuilder: (context, index) {
                    final invoice = visible[index];
                    return _InvoiceCard(
                      invoice: invoice,
                      onTap: () {
                        payments.clearErrors();
                        Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => InvoiceDetailScreen(
                              invoicePublicUuid: invoice.publicUuid,
                            ),
                          ),
                        );
                      },
                    );
                  },
                ),
        ),
      ],
    );
  }



}

class _InvoiceCard extends StatelessWidget {
  final InvoiceModel invoice;
  final VoidCallback onTap;

  const _InvoiceCard({required this.invoice, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final inFlight = invoice.inFlightPayment;

    return Card(
      margin: EdgeInsets.zero,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(invoice.invoiceNumber,
                        style: theme.textTheme.titleMedium),
                  ),
                  StatusChip(status: invoice.status),
                ],
              ),
              const SizedBox(height: 4),
              Text(invoice.displayTotal, style: theme.textTheme.titleSmall),
              const SizedBox(height: 4),
              Wrap(
                spacing: 16,
                runSpacing: 4,
                children: [
                  Text(
                    'Due: ${invoice.dueAt == null ? '—' : formatDateTime(invoice.dueAt!)}',
                    style: theme.textTheme.bodySmall,
                  ),
                  Text(
                    'Payments: ${invoice.payments.length}',
                    style: theme.textTheme.bodySmall,
                  ),
                  if (inFlight != null)
                    Text(
                      'Payment ${inFlight.status.toLowerCase()}',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.primary,
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}


// One chip per invoice status (plus "All"), each with its count, so the
// admin sees at a glance how many invoices are in each state.
class _StatusFilterBar extends StatelessWidget {
  final List<InvoiceModel> invoices;
  final String? selected;
  final ValueChanged<String?> onSelected;

  const _StatusFilterBar({
    required this.invoices,
    required this.selected,
    required this.onSelected,
  });

  String _label(String status) =>
      status[0] + status.substring(1).toLowerCase();

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 4,
      children: [
        ChoiceChip(
          label: Text('All (${invoices.length})'),
          selected: selected == null,
          onSelected: (_) => onSelected(null),
        ),
        for (final status in InvoiceStatuses.all)
          ChoiceChip(
            label: Text(
              '${_label(status)} (${invoices.where((i) => i.status == status).length})',
            ),
            selected: selected == status,
            onSelected: (_) => onSelected(status),
          ),
      ],
    );
  }
}

// Results of the organization search: name + slug, exact slug match first
// (ordered by the provider). Tapping one loads that organization's invoices.
class _OrganizationResults extends StatelessWidget {
  final List<OrganizationModel> results;
  final bool isSearching;
  final bool hasSearched;
  final String? errorMessage;
  final ValueChanged<OrganizationModel> onSelected;

  const _OrganizationResults({
    required this.results,
    required this.isSearching,
    required this.hasSearched,
    required this.errorMessage,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    Widget body;
    if (errorMessage != null) {
      body = Padding(
        padding: const EdgeInsets.all(12),
        child: Text(
          errorMessage!,
          style: TextStyle(color: theme.colorScheme.error),
        ),
      );
    } else if (results.isEmpty) {
      body = Padding(
        padding: const EdgeInsets.all(12),
        child: Text(
          isSearching || !hasSearched
              ? 'Searching...'
              : 'No organizations match your search.',
          style: theme.textTheme.bodySmall,
        ),
      );
    } else {
      body = ListView.separated(
        shrinkWrap: true,
        itemCount: results.length,
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, index) {
          final org = results[index];
          return ListTile(
            dense: true,
            title: Text(org.name),
            subtitle: Text(org.slug),
            trailing: Text(org.status, style: theme.textTheme.bodySmall),
            onTap: () => onSelected(org),
          );
        },
      );
    }

    return Card(
      margin: EdgeInsets.zero,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxHeight: 240),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isSearching) const LinearProgressIndicator(minHeight: 2),
            Flexible(child: body),
          ],
        ),
      ),
    );
  }
}