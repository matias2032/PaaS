import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/payment_model.dart';
import '../provider/payment_provider.dart';
import '../widget/common_widgets.dart'
    show ErrorBanner, ErrorState, StatusChip, formatDateTime;
import 'invoice_detail_screen.dart';

// Payments module entry point (admin view). Invoices are always scoped to
// one organization, so the screen asks for the organization's public UUID
// and lists its invoices. Tapping an invoice drills down to
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
  final _formKey = GlobalKey<FormState>();
  final _orgController = TextEditingController();

  static final _uuidRegex = RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
  );

  @override
  void initState() {
    super.initState();
    final provider = context.read<PaymentProvider>();
    final initial = widget.initialOrgPublicUuid ?? provider.loadedOrgPublicUuid;
    if (initial != null) _orgController.text = initial;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final p = context.read<PaymentProvider>();
      p.clearErrors();
      final explicit = widget.initialOrgPublicUuid;
      if (explicit != null && explicit != p.loadedOrgPublicUuid) {
        p.loadInvoices(explicit);
      }
    });
  }

  @override
  void dispose() {
    _orgController.dispose();
    super.dispose();
  }

  void _load() {
    if (!_formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    context.read<PaymentProvider>().loadInvoices(_orgController.text.trim());
  }

  void _reload() {
    final uuid = context.read<PaymentProvider>().loadedOrgPublicUuid;
    if (uuid != null) context.read<PaymentProvider>().loadInvoices(uuid);
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
            Form(
              key: _formKey,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _orgController,
                      textInputAction: TextInputAction.search,
                      onFieldSubmitted: (_) => _load(),
                      validator: (v) {
                        final t = (v ?? '').trim();
                        if (t.isEmpty) return 'Required';
                        if (!_uuidRegex.hasMatch(t)) return 'Not a valid UUID';
                        return null;
                      },
                      decoration: const InputDecoration(
                        labelText: 'Organization public UUID',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  FilledButton.icon(
                    onPressed: payments.isLoadingInvoices ? null : _load,
                    icon: const Icon(Icons.search),
                    label: const Text('Load invoices'),
                  ),
                ],
              ),
            ),
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
        child: Text('Enter an organization UUID to see its invoices.'),
      );
    }

    if (payments.invoices.isEmpty) {
      if (payments.isLoadingInvoices) {
        return const Center(child: CircularProgressIndicator());
      }
      if (payments.invoicesErrorMessage != null) {
        return ErrorState(
          message: payments.invoicesErrorMessage!,
          onRetry: _load,
        );
      }
      return const Center(child: Text('This organization has no invoices.'));
    }

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
              'Organization ${payments.loadedOrgPublicUuid} · '
              '${payments.invoices.length} invoice(s)',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
        Expanded(
          child: ListView.separated(
            itemCount: payments.invoices.length,
            separatorBuilder: (_, __) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final invoice = payments.invoices[index];
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