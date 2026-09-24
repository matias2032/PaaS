import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/infrastructure_model.dart';
import '../provider/infrastructure_provider.dart';
import 'infrastructure_widgets.dart';
import 'servers_screen.dart';

class CoolifyInstancesScreen extends StatefulWidget {
  const CoolifyInstancesScreen({super.key});

  @override
  State<CoolifyInstancesScreen> createState() => _CoolifyInstancesScreenState();
}

class _CoolifyInstancesScreenState extends State<CoolifyInstancesScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  void _load() {
    context.read<InfrastructureProvider>().loadCoolifyInstances();
  }

  void _openCreateDialog() {
    // Start the dialog without a stale error from a previous action.
    context.read<InfrastructureProvider>().clearErrors();
    showDialog(
      context: context,
      builder: (_) => const _CreateCoolifyInstanceDialog(),
    );
  }

  void _openServers(CoolifyInstanceModel instance) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ServersScreen(coolifyInstance: instance),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Coolify instances'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh',
            onPressed: _load,
          ),
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Add Coolify instance',
            onPressed: _openCreateDialog,
          ),
        ],
      ),
      body: Consumer<InfrastructureProvider>(
        builder: (context, provider, _) {
          final instances = provider.coolifyInstances;
          final error = provider.coolifyInstancesErrorMessage;

          if (provider.isLoadingCoolifyInstances && instances.isEmpty) {
            return const Center(child: CircularProgressIndicator());
          }
          if (error != null && instances.isEmpty) {
            return InfrastructureErrorState(message: error, onRetry: _load);
          }
          if (instances.isEmpty) {
            return const Center(child: Text('No Coolify instances yet.'));
          }

          return Column(
            children: [
              if (provider.isLoadingCoolifyInstances)
                const LinearProgressIndicator(),
              if (error != null)
                InfrastructureErrorBanner(
                  message: error,
                  onDismiss: provider.clearErrors,
                ),
              Expanded(
                child: ListView.separated(
                  itemCount: instances.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final instance = instances[index];
                    return ListTile(
                      leading: const Icon(Icons.cloud_outlined),
                      title: Text(instance.name),
                      subtitle: Text(instance.baseUrl),
                      trailing: InfrastructureStatusDropdown(
                        value: instance.status,
                        options: InfrastructureStatuses.coolifyInstance,
                        busy: provider.isUpdatingStatus(instance.publicUuid),
                        onChanged: (status) => provider
                            .updateCoolifyInstanceStatus(
                                instance.publicUuid, status),
                      ),
                      // Tap opens the servers registered on this instance.
                      onTap: () => _openServers(instance),
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _CreateCoolifyInstanceDialog extends StatefulWidget {
  const _CreateCoolifyInstanceDialog();

  @override
  State<_CreateCoolifyInstanceDialog> createState() =>
      _CreateCoolifyInstanceDialogState();
}

class _CreateCoolifyInstanceDialogState
    extends State<_CreateCoolifyInstanceDialog> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _baseUrlController = TextEditingController();
  final _apiTokenController = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _nameController.dispose();
    _baseUrlController.dispose();
    _apiTokenController.dispose();
    super.dispose();
  }

  String? _required(String? v) =>
      (v == null || v.trim().isEmpty) ? 'Required' : null;

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    final success = await context
        .read<InfrastructureProvider>()
        .createCoolifyInstance(
          name: _nameController.text,
          baseUrl: _baseUrlController.text,
          apiToken: _apiTokenController.text,
        );

    if (!mounted) return;
    setState(() => _submitting = false);
    if (success) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final errorMessage =
        context.watch<InfrastructureProvider>().coolifyInstancesErrorMessage;

    return AlertDialog(
      title: const Text('Add Coolify instance'),
      content: SizedBox(
        width: 480,
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(labelText: 'Name'),
                  validator: _required,
                ),
                TextFormField(
                  controller: _baseUrlController,
                  decoration: const InputDecoration(
                    labelText: 'Base URL',
                    hintText: 'https://coolify.example.com',
                  ),
                  keyboardType: TextInputType.url,
                  validator: _required,
                ),
                TextFormField(
                  controller: _apiTokenController,
                  decoration: const InputDecoration(labelText: 'API token'),
                  obscureText: true,
                  validator: _required,
                ),
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    'The token is stored encrypted and cannot be viewed again.',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
                if (errorMessage != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    errorMessage,
                    style:
                        TextStyle(color: Theme.of(context).colorScheme.error),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  height: 16,
                  width: 16,
                  child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Create'),
        ),
      ],
    );
  }
}