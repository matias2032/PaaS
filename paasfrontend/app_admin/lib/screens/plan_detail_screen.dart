import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/billing_model.dart';
import '../provider/billing_provider.dart';
import '../widget/common_widgets.dart' show ErrorBanner, ErrorState, StatusChip, formatDateTime;
import 'plan_dialogs.dart';

// Plan detail: info, resource limits and current prices, plus the status
// actions allowed for the current status. Reads the plan from the
// provider so it refreshes after every mutation.
class PlanDetailScreen extends StatelessWidget {
  final String planPublicUuid;

  const PlanDetailScreen({super.key, required this.planPublicUuid});

  Future<void> _openDialog(BuildContext context, Widget dialog) async {
    context.read<BillingProvider>().clearErrors();
    await showDialog<bool>(context: context, builder: (_) => dialog);
  }

  Future<void> _runStatusAction(
    BuildContext context,
    PlanModel plan,
    String action,
  ) async {
    final billing = context.read<BillingProvider>();
    billing.clearErrors();

    if (action == 'archive') {
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: const Text('Archive plan?'),
          content: Text(
            '"${plan.name}" will be archived. Archiving is meant to be '
            'permanent — the plan cannot be reactivated from this app.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('Archive'),
            ),
          ],
        ),
      );
      if (confirmed != true) return;
    }

    switch (action) {
      case 'deactivate':
        await billing.deactivatePlan(plan.publicUuid);
      case 'reactivate':
        await billing.reactivatePlan(plan.publicUuid);
      case 'archive':
        await billing.archivePlan(plan.publicUuid);
    }
  }

  @override
  Widget build(BuildContext context) {
    final billing = context.watch<BillingProvider>();
    final plan = billing.planByUuid(planPublicUuid);

    if (plan == null) {
      return Scaffold(
        appBar: AppBar(),
        body: const Center(child: Text('This plan is no longer available.')),
      );
    }

    final busy = billing.isUpdating(plan.publicUuid);
    final canDeactivate = BillingStatuses.canDeactivate(plan.status);
    final canReactivate = BillingStatuses.canReactivate(plan.status);
    final canArchive = BillingStatuses.canArchive(plan.status);

    return Scaffold(
      appBar: AppBar(
        title: Text(plan.name),
        actions: [
          if (canDeactivate || canReactivate || canArchive)
            PopupMenuButton<String>(
              enabled: !busy,
              onSelected: (action) => _runStatusAction(context, plan, action),
              itemBuilder: (_) => [
                if (canDeactivate)
                  const PopupMenuItem(value: 'deactivate', child: Text('Deactivate')),
                if (canReactivate)
                  const PopupMenuItem(value: 'reactivate', child: Text('Reactivate')),
                if (canArchive)
                  const PopupMenuItem(value: 'archive', child: Text('Archive')),
              ],
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (busy) const LinearProgressIndicator(),
          if (billing.plansErrorMessage != null) ...[
            ErrorBanner(
              message: billing.plansErrorMessage!,
              onDismiss: billing.clearErrors,
            ),
            const SizedBox(height: 12),
          ],
          _Section(
            title: 'Details',
            action: TextButton.icon(
              onPressed: busy
                  ? null
                  : () => _openDialog(context, EditPlanDialog(plan: plan)),
              icon: const Icon(Icons.edit_outlined),
              label: const Text('Edit'),
            ),
            children: [
  _InfoRow('Status', child: StatusChip(status: plan.status)),
              if (canDeactivate || canReactivate)
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Active'),
                  subtitle: Text(
                    canDeactivate
                        ? 'Visible to customers and open to new subscriptions.'
                        : 'Hidden from customers. Existing subscriptions are kept.',
                  ),
                  value: canDeactivate,
                  onChanged: busy
                      ? null
                      : (on) => _runStatusAction(
                            context,
                            plan,
                            on ? 'reactivate' : 'deactivate',
                          ),
                ),
              _InfoRow('Name', value: plan.name),
              _InfoRow('Slug', value: plan.slug),
              _InfoRow('Description', value: plan.description ?? '—'),
              _InfoRow('Created', value: formatDateTime(plan.createdAt)),
              _InfoRow('Updated', value: formatDateTime(plan.updatedAt)),
            ],
          ),
          const SizedBox(height: 12),
          _Section(
            title: 'Resource limits',
            action: TextButton.icon(
              onPressed: busy
                  ? null
                  : () => _openDialog(
                        context,
                        ResourceLimitsDialog(
                          planPublicUuid: plan.publicUuid,
                          existing: plan.resourceLimits,
                        ),
                      ),
              icon: const Icon(Icons.tune),
              label: Text(plan.resourceLimits == null ? 'Set' : 'Edit'),
            ),
            children: _limitRows(plan.resourceLimits),
          ),
          const SizedBox(height: 12),
          _Section(
            title: 'Current prices',
            action: TextButton.icon(
              onPressed: busy
                  ? null
                  : () => _openDialog(
                        context,
                        AddPriceDialog(planPublicUuid: plan.publicUuid),
                      ),
              icon: const Icon(Icons.add),
              label: const Text('Set price'),
            ),
            children: plan.prices.isEmpty
                ? const [_InfoRow('Prices', value: 'No current prices')]
                : plan.prices
                    .map((p) => _InfoRow(
                          BillingStatuses.cycleLabel(p.billingCycle),
                          value: '${p.display}  ·  since ${formatDateTime(p.effectiveFrom)}',
                        ))
                    .toList(),
          ),
        ],
      ),
    );
  }

  List<Widget> _limitRows(PlanResourceLimitModel? l) {
    if (l == null) return const [_InfoRow('Limits', value: 'Not set')];
    return [
      _InfoRow('CPU', value: '${l.cpuLimit} cores'),
      _InfoRow('Memory', value: '${l.memoryLimitMb} MB'),
      _InfoRow('Storage', value: '${l.storageLimitMb} MB'),
      _InfoRow('Max projects', value: '${l.maxProjects}'),
      _InfoRow('Max services', value: '${l.maxServices}'),
      _InfoRow('Max domains', value: '${l.maxDomains}'),
      _InfoRow('Max env. variables',
          value: l.maxEnvironmentVariables?.toString() ?? 'Not set'),
      _InfoRow('Bandwidth',
          value: l.bandwidthLimitMb == null ? 'Not set' : '${l.bandwidthLimitMb} MB'),
    ];
  }
}

class _Section extends StatelessWidget {
  final String title;
  final Widget action;
  final List<Widget> children;

  const _Section({
    required this.title,
    required this.action,
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