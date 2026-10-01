import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/payment_model.dart';
import '../provider/payment_provider.dart';

// Dialogs of the payment screens: mark invoice as paid and refund payment.
// Each one shows the provider error inside the dialog and pops with `true`
// on success. Same shell pattern as plan_dialogs.dart.

class _DialogShell extends StatefulWidget {
  final String title;
  final String submitLabel;
  final GlobalKey<FormState> formKey;
  final List<Widget> children;
  final Future<bool> Function() onSubmit;

  const _DialogShell({
    required this.title,
    required this.submitLabel,
    required this.formKey,
    required this.children,
    required this.onSubmit,
  });

  @override
  State<_DialogShell> createState() => _DialogShellState();
}

class _DialogShellState extends State<_DialogShell> {
  bool _submitting = false;

  Future<void> _submit() async {
    if (!widget.formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    final ok = await widget.onSubmit();
    if (!mounted) return;
    if (ok) {
      Navigator.of(context).pop(true);
    } else {
      setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final error = context.watch<PaymentProvider>().invoicesErrorMessage;

    return AlertDialog(
      title: Text(widget.title),
      content: SizedBox(
        width: 420,
        child: Form(
          key: widget.formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ...widget.children,
                if (error != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 12),
                    child: Text(
                      error,
                      style: TextStyle(color: Theme.of(context).colorScheme.error),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _submitting ? null : () => Navigator.of(context).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(widget.submitLabel),
        ),
      ],
    );
  }
}

Widget _field(
  TextEditingController controller,
  String label, {
  String? Function(String?)? validator,
  String? helper,
  int maxLines = 1,
  bool autofocus = false,
}) {
  return Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: TextFormField(
      controller: controller,
      autofocus: autofocus,
      maxLines: maxLines,
      validator: validator,
      decoration: InputDecoration(
        labelText: label,
        helperText: helper,
        border: const OutlineInputBorder(),
      ),
    ),
  );
}

String? _maxLength(String? v, int max) {
  final t = (v ?? '').trim();
  if (t.length > max) return 'At most $max characters';
  return null;
}

// ── Mark invoice as paid ────────────────────────────────────────────

class MarkPaidDialog extends StatefulWidget {
  final InvoiceModel invoice;

  const MarkPaidDialog({super.key, required this.invoice});

  @override
  State<MarkPaidDialog> createState() => _MarkPaidDialogState();
}

class _MarkPaidDialogState extends State<MarkPaidDialog> {
  final _formKey = GlobalKey<FormState>();
  final _reference = TextEditingController();
  final _reason = TextEditingController();
  String? _methodCode;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<PaymentProvider>().loadPaymentMethods();
    });
  }

  @override
  void dispose() {
    _reference.dispose();
    _reason.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final payments = context.watch<PaymentProvider>();

    // Prefer ACTIVE methods; fall back to the whole catalog if none is
    // flagged that way, so the dialog never ends up unusable.
    final all = payments.paymentMethods;
    final active = all.where((m) => m.status == 'ACTIVE').toList();
    final methods = active.isNotEmpty ? active : all;

    return _DialogShell(
      title: 'Mark as paid',
      submitLabel: 'Mark as paid',
      formKey: _formKey,
      onSubmit: () => context.read<PaymentProvider>().markInvoicePaid(
            widget.invoice.publicUuid,
            paymentMethodCode: _methodCode ?? '',
            transactionReference: _reference.text,
            reason: _reason.text,
          ),
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Text(
            'Invoice ${widget.invoice.invoiceNumber} · ${widget.invoice.displayTotal}',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
        if (payments.isLoadingMethods)
          const Padding(
            padding: EdgeInsets.only(bottom: 12),
            child: LinearProgressIndicator(),
          ),
        if (payments.methodsErrorMessage != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text(
              payments.methodsErrorMessage!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ),
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: DropdownButtonFormField<String>(
            value: methods.any((m) => m.code == _methodCode) ? _methodCode : null,
            decoration: const InputDecoration(
              labelText: 'Payment method',
              border: OutlineInputBorder(),
            ),
            items: methods
                .map((m) => DropdownMenuItem(value: m.code, child: Text(m.name)))
                .toList(),
            validator: (v) => (v == null || v.isEmpty) ? 'Required' : null,
            onChanged: (v) => setState(() => _methodCode = v),
          ),
        ),
        _field(
          _reference,
          'Transaction reference (optional)',
          validator: (v) => _maxLength(v, 255),
          autofocus: true,
        ),
        _field(
          _reason,
          'Reason (optional)',
          maxLines: 3,
          validator: (v) => _maxLength(v, 500),
          helper: 'Recorded in the audit log, not on the payment.',
        ),
      ],
    );
  }
}

// ── Refund payment ──────────────────────────────────────────────────

class RefundPaymentDialog extends StatefulWidget {
  final PaymentModel payment;

  const RefundPaymentDialog({super.key, required this.payment});

  @override
  State<RefundPaymentDialog> createState() => _RefundPaymentDialogState();
}

class _RefundPaymentDialogState extends State<RefundPaymentDialog> {
  final _formKey = GlobalKey<FormState>();
  final _reason = TextEditingController();

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _DialogShell(
      title: 'Refund payment',
      submitLabel: 'Refund',
      formKey: _formKey,
      onSubmit: () => context.read<PaymentProvider>().refundPayment(
            widget.payment.publicUuid,
            reason: _reason.text,
          ),
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Text(
            'Refund ${widget.payment.display} paid via '
            '${widget.payment.paymentMethodCode}? This cannot be undone.',
          ),
        ),
        _field(
          _reason,
          'Reason',
          maxLines: 3,
          autofocus: true,
          validator: (v) {
            final t = (v ?? '').trim();
            if (t.isEmpty) return 'Required';
            return _maxLength(t, 500);
          },
        ),
      ],
    );
  }
}