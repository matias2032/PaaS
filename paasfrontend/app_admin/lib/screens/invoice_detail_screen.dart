import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/payment_model.dart';
import '../provider/auth_provider.dart';
import '../provider/payment_provider.dart';
import '../widget/common_widgets.dart'
    show ErrorBanner, StatusChip, formatDateTime;
import 'payment_dialogs.dart';

// Invoice detail: info, line items and payments. Reads the invoice from
// the provider so it refreshes after every mutation. Mark-paid and refund
// are PLATFORM_ADMIN only (PaymentController); SUPPORT gets a read-only view.
class InvoiceDetailScreen extends StatelessWidget {
  final String invoicePublicUuid;

  const InvoiceDetailScreen({super.key, required this.invoicePublicUuid});

  String _dt(DateTime? value) => value == null ? '—' : formatDateTime(value);

  Future<void> _openDialog(
    BuildContext context,
    Widget dialog,
    String successMessage,
  ) async {
    context.read<PaymentProvider>().clearErrors();
    final ok = await showDialog<bool>(context: context, builder: (_) => dialog);
    if (ok == true && context.mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(successMessage)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final payments = context.watch<PaymentProvider>();
    final canManage = context.watch<AuthProvider>().isAtLeastPlatformAdmin;
    final invoice = payments.invoiceByUuid(invoicePublicUuid);

    if (invoice == null) {
      return Scaffold(
        appBar: AppBar(),
        body: const Center(child: Text('This invoice is no longer available.')),
      );
    }

    final busy = payments.isUpdating(invoice.publicUuid);
    final canMarkPaid = canManage && InvoiceStatuses.isPayable(invoice.status);

    return Scaffold(
      appBar: AppBar(title: Text(invoice.invoiceNumber)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (busy) const LinearProgressIndicator(),
          if (payments.invoicesErrorMessage != null) ...[
            ErrorBanner(
              message: payments.invoicesErrorMessage!,
              onDismiss: payments.clearErrors,
            ),
            const SizedBox(height: 12),
          ],
          _Section(
            title: 'Details',
            action: canMarkPaid
                ? FilledButton.icon(
                    onPressed: busy
                        ? null
                        : () => _openDialog(
                              context,
                              MarkPaidDialog(invoice: invoice),
                              'Invoice ${invoice.invoiceNumber} marked as paid.',
                            ),
                    icon: const Icon(Icons.check_circle_outline),
                    label: const Text('Mark as paid'),
                  )
                : const SizedBox.shrink(),
            children: [
              _InfoRow('Status', child: StatusChip(status: invoice.status)),
              _InfoRow('Number', value: invoice.invoiceNumber),
              _InfoRow('Total', value: invoice.displayTotal),
              _InfoRow('Subscription', value: invoice.subscriptionPublicUuid),
              _InfoRow('Issued', value: _dt(invoice.issuedAt)),
              _InfoRow('Due', value: _dt(invoice.dueAt)),
              _InfoRow('Paid', value: _dt(invoice.paidAt)),
              _InfoRow('Created', value: formatDateTime(invoice.createdAt)),
            ],
          ),
          const SizedBox(height: 12),
          _Section(
            title: 'Items',
            children: invoice.items.isEmpty
                ? const [_InfoRow('Items', value: 'No items')]
                : invoice.items
                    .map((i) => _InfoRow(
                          i.description,
                          value: '${i.quantity} × ${i.unitPrice.toStringAsFixed(2)}'
                              ' = ${i.lineTotal.toStringAsFixed(2)} ${invoice.currency}',
                        ))
                    .toList(),
          ),
          const SizedBox(height: 12),
          _Section(
            title: 'Payments',
            children: invoice.payments.isEmpty
                ? const [_InfoRow('Payments', value: 'No payments yet')]
                : [
                    for (var i = 0; i < invoice.payments.length; i++) ...[
                      if (i > 0) const Divider(),
                      _PaymentBlock(
                        payment: invoice.payments[i],
                        canRefund: canManage &&
                            PaymentStatuses.isRefundable(
                                invoice.payments[i].status),
                        busy: payments.isUpdating(invoice.payments[i].publicUuid),
                        dt: _dt,
                        onRefund: () => _openDialog(
                          context,
                          RefundPaymentDialog(payment: invoice.payments[i]),
                          'Payment refunded.',
                        ),
                      ),
                    ],
                  ],
          ),
        ],
      ),
    );
  }
}

class _PaymentBlock extends StatelessWidget {
  final PaymentModel payment;
  final bool canRefund;
  final bool busy;
  final String Function(DateTime?) dt;
  final VoidCallback onRefund;

  const _PaymentBlock({
    required this.payment,
    required this.canRefund,
    required this.busy,
    required this.dt,
    required this.onRefund,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                '${payment.display} · ${payment.paymentMethodCode}',
                style: Theme.of(context).textTheme.titleSmall,
              ),
            ),
            StatusChip(status: payment.status),
          ],
        ),
        const SizedBox(height: 4),
        _InfoRow('Reference', value: payment.transactionReference ?? '—'),
        if (payment.externalPaymentId != null)
          _InfoRow('External ID', value: payment.externalPaymentId!),
        if (payment.failureReason != null)
          _InfoRow('Failure', value: payment.failureReason!),
        _InfoRow('Paid', value: dt(payment.paidAt)),
        _InfoRow('Created', value: formatDateTime(payment.createdAt)),
        if (canRefund)
          Align(
            alignment: Alignment.centerRight,
            child: busy
                ? const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : OutlinedButton.icon(
                    onPressed: onRefund,
                    icon: const Icon(Icons.undo),
                    label: const Text('Refund'),
                  ),
          ),
      ],
    );
  }
}

class _Section extends StatelessWidget {
  final String title;
  final Widget action;
  final List<Widget> children;

  const _Section({
    required this.title,
    this.action = const SizedBox.shrink(),
    required this.children,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(title, style: Theme.of(context).textTheme.titleMedium),
                const Spacer(),
                action,
              ],
            ),
            const Divider(),
            ...children,
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String? value;
  final Widget? child;

  const _InfoRow(this.label, {this.value, this.child});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 150,
            child: Text(label, style: Theme.of(context).textTheme.bodySmall),
          ),
          Expanded(child: child ?? Text(value ?? '')),
        ],
      ),
    );
  }
}