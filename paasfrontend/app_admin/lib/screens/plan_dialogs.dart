import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/billing_model.dart';
import '../provider/billing_provider.dart';

// Dialogs of the billing screens: create/edit plan, resource limits and
// add price. Each one shows the provider error inside the dialog, and
// pops with `true` on success.

// ── Shared shell ────────────────────────────────────────────────────

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
    final error = context.watch<BillingProvider>().plansErrorMessage;

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
  TextInputType? keyboardType,
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
      keyboardType: keyboardType,
      validator: validator,
      decoration: InputDecoration(
        labelText: label,
        helperText: helper,
        border: const OutlineInputBorder(),
      ),
    ),
  );
}

String? _validateName(String? v) {
  final t = (v ?? '').trim();
  if (t.isEmpty) return 'Required';
  if (t.length > 100) return 'At most 100 characters';
  return null;
}

// ── Create plan ─────────────────────────────────────────────────────

class CreatePlanDialog extends StatefulWidget {
  const CreatePlanDialog({super.key});

  @override
  State<CreatePlanDialog> createState() => _CreatePlanDialogState();
}

class _CreatePlanDialogState extends State<CreatePlanDialog> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _description = TextEditingController();

  @override
  void dispose() {
    _name.dispose();
    _description.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _DialogShell(
      title: 'New plan',
      submitLabel: 'Create',
      formKey: _formKey,
      onSubmit: () => context.read<BillingProvider>().createPlan(
            name: _name.text,
            description: _description.text,
          ),
      children: [
        _field(_name, 'Name', validator: _validateName, autofocus: true),
        // The slug is generated from the name and cannot be edited later.
        ValueListenableBuilder<TextEditingValue>(
          valueListenable: _name,
          builder: (context, value, _) {
            final slug = BillingValidators.slugify(value.text);
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Text(
                'Slug (fixed after creation): ${slug.isEmpty ? '—' : slug}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            );
          },
        ),
        _field(_description, 'Description (optional)', maxLines: 3),
      ],
    );
  }
}

// ── Edit plan (name + description) ──────────────────────────────────

class EditPlanDialog extends StatefulWidget {
  final PlanModel plan;

  const EditPlanDialog({super.key, required this.plan});

  @override
  State<EditPlanDialog> createState() => _EditPlanDialogState();
}

class _EditPlanDialogState extends State<EditPlanDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name =
      TextEditingController(text: widget.plan.name);
  late final TextEditingController _description =
      TextEditingController(text: widget.plan.description ?? '');

  @override
  void dispose() {
    _name.dispose();
    _description.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _DialogShell(
      title: 'Edit plan',
      submitLabel: 'Save',
      formKey: _formKey,
      onSubmit: () => context.read<BillingProvider>().updatePlan(
            widget.plan.publicUuid,
            name: _name.text,
            description: _description.text,
          ),
      children: [
        _field(_name, 'Name', validator: _validateName, autofocus: true),
        _field(_description, 'Description (optional)', maxLines: 3),
        Text(
          'Slug: ${widget.plan.slug} (cannot be changed)',
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}

// ── Resource limits ─────────────────────────────────────────────────

String? _validateInt(String? v, {bool required = true}) {
  final t = (v ?? '').trim();
  if (t.isEmpty) return required ? 'Required' : null;
  final n = int.tryParse(t);
  if (n == null || n < 0) return 'Whole number, 0 or greater';
  return null;
}

String? _validateCpu(String? v) {
  final t = (v ?? '').trim().replaceAll(',', '.');
  if (t.isEmpty) return 'Required';
  final n = double.tryParse(t);
  if (n == null || !n.isFinite || n <= 0) return 'Must be greater than 0';
  return null;
}

int? _optionalInt(TextEditingController c) {
  final t = c.text.trim();
  return t.isEmpty ? null : int.parse(t);
}

class ResourceLimitsDialog extends StatefulWidget {
  final String planPublicUuid;
  final PlanResourceLimitModel? existing;

  const ResourceLimitsDialog({
    super.key,
    required this.planPublicUuid,
    this.existing,
  });

  @override
  State<ResourceLimitsDialog> createState() => _ResourceLimitsDialogState();
}

class _ResourceLimitsDialogState extends State<ResourceLimitsDialog> {
  final _formKey = GlobalKey<FormState>();

  late final _cpu = TextEditingController(
      text: widget.existing?.cpuLimit.toString() ?? '');
  late final _memory = TextEditingController(
      text: widget.existing?.memoryLimitMb.toString() ?? '');
  late final _storage = TextEditingController(
      text: widget.existing?.storageLimitMb.toString() ?? '');
  late final _projects = TextEditingController(
      text: widget.existing?.maxProjects.toString() ?? '');
  late final _services = TextEditingController(
      text: widget.existing?.maxServices.toString() ?? '');
  late final _domains = TextEditingController(
      text: widget.existing?.maxDomains.toString() ?? '');
  late final _envVars = TextEditingController(
      text: widget.existing?.maxEnvironmentVariables?.toString() ?? '');
  late final _bandwidth = TextEditingController(
      text: widget.existing?.bandwidthLimitMb?.toString() ?? '');

  @override
  void dispose() {
    for (final c in [
      _cpu,
      _memory,
      _storage,
      _projects,
      _services,
      _domains,
      _envVars,
      _bandwidth,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const number = TextInputType.number;

    return _DialogShell(
      title: 'Resource limits',
      submitLabel: 'Save',
      formKey: _formKey,
      onSubmit: () => context.read<BillingProvider>().setResourceLimits(
            planPublicUuid: widget.planPublicUuid,
            cpuLimit: double.parse(_cpu.text.trim().replaceAll(',', '.')),
            memoryLimitMb: int.parse(_memory.text.trim()),
            storageLimitMb: int.parse(_storage.text.trim()),
            maxProjects: int.parse(_projects.text.trim()),
            maxServices: int.parse(_services.text.trim()),
            maxDomains: int.parse(_domains.text.trim()),
            maxEnvironmentVariables: _optionalInt(_envVars),
            bandwidthLimitMb: _optionalInt(_bandwidth),
          ),
      children: [
        _field(_cpu, 'CPU limit (cores)',
            validator: _validateCpu,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            autofocus: true),
        _field(_memory, 'Memory limit (MB)',
            validator: _validateInt, keyboardType: number),
        _field(_storage, 'Storage limit (MB)',
            validator: _validateInt, keyboardType: number),
        _field(_projects, 'Max projects',
            validator: _validateInt, keyboardType: number),
        _field(_services, 'Max services',
            validator: _validateInt, keyboardType: number),
        _field(_domains, 'Max domains',
            validator: _validateInt, keyboardType: number),
        _field(_envVars, 'Max environment variables (optional)',
            validator: (v) => _validateInt(v, required: false),
            keyboardType: number),
        _field(_bandwidth, 'Bandwidth limit (MB, optional)',
            validator: (v) => _validateInt(v, required: false),
            keyboardType: number,
            helper: 'Saving replaces all limits; blank optional fields are cleared.'),
      ],
    );
  }
}

// ── Add price ───────────────────────────────────────────────────────

class AddPriceDialog extends StatefulWidget {
  final String planPublicUuid;

  const AddPriceDialog({super.key, required this.planPublicUuid});

  @override
  State<AddPriceDialog> createState() => _AddPriceDialogState();
}

class _AddPriceDialogState extends State<AddPriceDialog> {
  final _formKey = GlobalKey<FormState>();
  final _amount = TextEditingController();
  final _currency = TextEditingController(text: 'MZN');
  String _cycle = BillingStatuses.billingCycles.first;

  @override
  void dispose() {
    _amount.dispose();
    _currency.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _DialogShell(
      title: 'Set price',
      submitLabel: 'Save',
      formKey: _formKey,
      onSubmit: () => context.read<BillingProvider>().addPrice(
            planPublicUuid: widget.planPublicUuid,
            billingCycle: _cycle,
            amount: double.parse(_amount.text.trim().replaceAll(',', '.')),
            currency: _currency.text,
          ),
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: DropdownButtonFormField<String>(
            value: _cycle,
            decoration: const InputDecoration(
              labelText: 'Billing cycle',
              border: OutlineInputBorder(),
            ),
            items: BillingStatuses.billingCycles
                .map((c) => DropdownMenuItem(
                      value: c,
                      child: Text(BillingStatuses.cycleLabel(c)),
                    ))
                .toList(),
            onChanged: (v) {
              if (v != null) setState(() => _cycle = v);
            },
          ),
        ),
        _field(
          _amount,
          'Amount',
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          autofocus: true,
          validator: (v) {
            final t = (v ?? '').trim().replaceAll(',', '.');
            if (t.isEmpty) return 'Required';
            final n = double.tryParse(t);
            if (n == null || !n.isFinite || n < 0) return '0 or greater';
            return null;
          },
        ),
        _field(
          _currency,
          'Currency',
          helper: 'Setting a price closes the current price of this cycle.',
          validator: (v) => BillingValidators.isValidCurrency(
                  (v ?? '').trim().toUpperCase())
              ? null
              : '3-letter code, e.g. MZN',
        ),
      ],
    );
  }
}

// Public entry points for the create-plan page.
String? validatePlanName(String? v) => _validateName(v);
String? validateWholeNumber(String? v, {bool required = true}) =>
    _validateInt(v, required: required);
String? validateCpuLimit(String? v) => _validateCpu(v);

