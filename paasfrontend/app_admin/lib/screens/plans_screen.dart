import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/billing_model.dart';
import '../provider/billing_provider.dart';
import '../widget/common_widgets.dart' show ErrorBanner, ErrorState, StatusChip;
import 'create_plan_screen.dart';
import 'plan_detail_screen.dart';
// Plan catalog (admin view): every plan regardless of status. Tapping a
// plan drills down to PlanDetailScreen. Nested Scaffold inside the
// HomeScreen body, same as StaffScreen.
class PlansScreen extends StatefulWidget {
  const PlansScreen({super.key});

  @override
  State<PlansScreen> createState() => _PlansScreenState();
}

class _PlansScreenState extends State<PlansScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<BillingProvider>().loadPlans();
    });
  }

  Future<void> _openCreateScreen() async {
    context.read<BillingProvider>().clearErrors();
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const CreatePlanScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final billing = context.watch<BillingProvider>();

    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
               onPressed: _openCreateScreen,
        icon: const Icon(Icons.add),
        label: const Text('New plan'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('Plans', style: Theme.of(context).textTheme.headlineSmall),
                const Spacer(),
                IconButton(
                  tooltip: 'Reload',
                  icon: const Icon(Icons.refresh),
                  onPressed: billing.isLoadingPlans ? null : billing.loadPlans,
                ),
              ],
            ),
            const SizedBox(height: 8),
            Expanded(child: _buildContent(billing)),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BillingProvider billing) {
    if (billing.plans.isEmpty) {
      if (!billing.hasLoadedPlans || billing.isLoadingPlans) {
        return const Center(child: CircularProgressIndicator());
      }
      if (billing.plansErrorMessage != null) {
        return ErrorState(
          message: billing.plansErrorMessage!,
          onRetry: billing.loadPlans,
        );
      }
      return const Center(child: Text('No plans yet. Create the first one.'));
    }

    return Column(
      children: [
        if (billing.isLoadingPlans) const LinearProgressIndicator(),
        if (billing.plansErrorMessage != null)
          ErrorBanner(
            message: billing.plansErrorMessage!,
            onDismiss: billing.clearErrors,
          ),
        Expanded(
          child: ListView.separated(
            itemCount: billing.plans.length,
            separatorBuilder: (_, __) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final plan = billing.plans[index];
              return _PlanCard(
                plan: plan,
                onTap: () {
                  billing.clearErrors();
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) =>
                          PlanDetailScreen(planPublicUuid: plan.publicUuid),
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

class _PlanCard extends StatelessWidget {
  final PlanModel plan;
  final VoidCallback onTap;

  const _PlanCard({required this.plan, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

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
                    child: Text(plan.name, style: theme.textTheme.titleMedium),
                  ),
                  StatusChip(status: plan.status),
                ],
              ),
              Text(plan.slug, style: theme.textTheme.bodySmall),
              const SizedBox(height: 8),
              if (plan.prices.isEmpty)
                Text('No current prices', style: theme.textTheme.bodySmall)
              else
                Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  children: plan.prices
                      .map((p) => Chip(
                            visualDensity: VisualDensity.compact,
                            label: Text(
                              '${BillingStatuses.cycleLabel(p.billingCycle)}: '
                              '${p.display}',
                            ),
                          ))
                      .toList(),
                ),
            ],
          ),
        ),
      ),
    );
  }
}