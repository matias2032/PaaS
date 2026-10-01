import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/billing_model.dart';
import '../provider/billing_provider.dart';
import '../widget/common_widgets.dart' show ErrorBanner;
import 'plan_detail_screen.dart';
import 'plan_dialogs.dart'
    show validatePlanName, validateWholeNumber, validateCpuLimit;

// Full plan registration: details, resource limits and prices in one
// page. The backend has no single "create everything" endpoint, so the
// submit runs createPlan -> setResourceLimits -> addPrice (one per
// filled cycle). If a later step fails the plan already exists, so the
// user is taken to its detail screen (with the error banner) to finish.
class CreatePlanScreen extends StatefulWidget {
  const CreatePlanScreen({super.key});

  @override
  State<CreatePlanScreen> createState() => _CreatePlanScreenState();
}

class _CreatePlanScreenState extends State<CreatePlanScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _description = TextEditingController();

  final _cpu = TextEditingController();
  final _memory = TextEditingController();
  final _storage = TextEditingController();
  final _projects = TextEditingController();
  final _services = TextEditingController();
  final _domains = TextEditingController();
  final _envVars = TextEditingController();
  final _bandwidth = TextEditingController();

  final _currency = TextEditingController(text: 'MZN');
  final Map<String, TextEditingController> _amounts = {
    for (final c in BillingStatuses.billingCycles) c: TextEditingController(),
  };

  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<BillingProvider>().clearErrors();
    });
  }

  @override
  void dispose() {
    for (final c in [
      _name, _description, _cpu, _memory, _storage, _projects,
      _services, _domains, _envVars, _bandwidth, _currency,
      ..._amounts.values,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  double _parseDouble(String s) => double.parse(s.trim().replaceAll(',', '.'));

  int? _optionalInt(TextEditingController c) {
    final t = c.text.trim();
    return t.isEmpty ? null : int.parse(t);
  }

  PlanModel? _findBySlug(BillingProvider billing, String slug) {
    for (final p in billing.plans) {
      if (p.slug == slug) return p;
    }
    return null;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final billing = context.read<BillingProvider>();
    final navigator = Navigator.of(context);
    billing.clearErrors();
    setState(() => _submitting = true);

    final slug = BillingValidators.slugify(_name.text);

    // 1. Plan
    final created = await billing.createPlan(
      name: _name.text,
      description: _description.text,
    );
    if (!mounted) return;
    if (!created) {
      setState(() => _submitting = false); // error shown in the banner
      return;
    }

    var plan = _findBySlug(billing, slug);
    if (plan == null) {
      await billing.loadPlans();
      if (!mounted) return;
      plan = _findBySlug(billing, slug);
    }
    if (plan == null) {
      // Created, but not findable: back to the list, which reloads.
      navigator.pop();
      return;
    }
    final planUuid = plan.publicUuid;

    // 2. Resource limits
    var allOk = await billing.setResourceLimits(
      planPublicUuid: planUuid,
      cpuLimit: _parseDouble(_cpu.text),
      memoryLimitMb: int.parse(_memory.text.trim()),
      storageLimitMb: int.parse(_storage.text.trim()),
      maxProjects: _optionalInt(_projects),
      maxServices: _optionalInt(_services),
      maxDomains: _optionalInt(_domains),
      maxEnvironmentVariables: _optionalInt(_envVars),
      bandwidthLimitMb: _optionalInt(_bandwidth),
    );

    // 3. Prices (only the cycles with an amount)
    if (allOk) {
      for (final entry in _amounts.entries) {
        if (entry.value.text.trim().isEmpty) continue;
        final ok = await billing.addPrice(
          planPublicUuid: planUuid,
          billingCycle: entry.key,
          amount: _parseDouble(entry.value.text),
          currency: _currency.text,
        );
        if (!ok) {
          allOk = false;
          break;
        }
      }
    }
    if (!mounted) return;

    if (allOk) {
      navigator.pop();
    } else {
      // The plan exists but is incomplete: finish it on the detail screen,
      // where the provider error is shown.
      navigator.pushReplacement(
        MaterialPageRoute(
          builder: (_) => PlanDetailScreen(planPublicUuid: planUuid),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final error = context.watch<BillingProvider>().plansErrorMessage;
    const number = TextInputType.number;
    const decimal = TextInputType.numberWithOptions(decimal: true);

    return Scaffold(
      appBar: AppBar(title: const Text('New plan')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (_submitting) const LinearProgressIndicator(),
            if (error != null) ...[
              ErrorBanner(
                message: error,
                onDismiss: context.read<BillingProvider>().clearErrors,
              ),
              const SizedBox(height: 12),
            ],
            _Section(
              title: 'Details',
              children: [
                _field(_name, 'Name',
                    validator: validatePlanName, autofocus: true),
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
            ),
            const SizedBox(height: 12),
            _Section(
              title: 'Resource limits',
              children: [
                _field(_cpu, 'CPU limit (cores)',
                    validator: validateCpuLimit, keyboardType: decimal),
                _field(_memory, 'Memory limit (MB)',
                    validator: validateWholeNumber, keyboardType: number),
                _field(_storage, 'Storage limit (MB)',
                    validator: validateWholeNumber, keyboardType: number),
                _field(_projects, 'Max projects (optional — blank = unlimited)',
                    validator: (v) => validateWholeNumber(v, required: false),
                    keyboardType: number),
                _field(_services, 'Max services (optional — blank = unlimited)',
                    validator: (v) => validateWholeNumber(v, required: false),
                    keyboardType: number),
                _field(_domains, 'Max domains (optional — blank = unlimited)',
                    validator: (v) => validateWholeNumber(v, required: false),
                    keyboardType: number),
                _field(_envVars, 'Max environment variables (optional)',
                    validator: (v) => validateWholeNumber(v, required: false),
                    keyboardType: number),
                _field(_bandwidth, 'Bandwidth limit (MB, optional)',
                    validator: (v) => validateWholeNumber(v, required: false),
                    keyboardType: number),
              ],
            ),
            const SizedBox(height: 12),
            _Section(
              title: 'Prices',
              children: [
                _field(
                  _currency,
                  'Currency',
                  helper: 'Applies to every price below.',
                  validator: (v) => BillingValidators.isValidCurrency(
                          (v ?? '').trim().toUpperCase())
                      ? null
                      : '3-letter code, e.g. MZN',
                ),
                for (final cycle in BillingStatuses.billingCycles)
                  _field(
                    _amounts[cycle]!,
                    '${BillingStatuses.cycleLabel(cycle)} price (optional)',
                    keyboardType: decimal,
                    validator: (v) {
                      final t = (v ?? '').trim().replaceAll(',', '.');
                      if (t.isEmpty) return null;
                      final n = double.tryParse(t);
                      if (n == null || !n.isFinite || n < 0) return '0 or greater';
                      return null;
                    },
                  ),
              ],
            ),
            const SizedBox(height: 20),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: _submitting ? null : () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                const SizedBox(width: 8),
                FilledButton(
                  onPressed: _submitting ? null : _submit,
                  child: _submitting
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Create plan'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
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
}

class _Section extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const _Section({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            const Divider(),
            const SizedBox(height: 4),
            ...children,
          ],
        ),
      ),
    );
  }
}