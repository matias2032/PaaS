import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/infrastructure_model.dart';
import '../provider/infrastructure_provider.dart';
import 'infrastructure_widgets.dart';

// Used in two ways: from the sidebar (all servers, no filter) and pushed
// from the Coolify instances screen (only that instance's servers).
class ServersScreen extends StatefulWidget {
  final CoolifyInstanceModel? coolifyInstance;

  const ServersScreen({super.key, this.coolifyInstance});

  @override
  State<ServersScreen> createState() => _ServersScreenState();
}

class _ServersScreenState extends State<ServersScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _load();
      // Needed by the create dialog's dropdowns.
      final provider = context.read<InfrastructureProvider>();
      provider.loadCoolifyInstances();
      provider.loadServerProviders();
    });
  }

  void _load() {
    context.read<InfrastructureProvider>().loadServers(
          coolifyInstancePublicUuid: widget.coolifyInstance?.publicUuid,
        );
  }

  void _openCreateDialog() {
    final provider = context.read<InfrastructureProvider>();
    provider.clearErrors();

    // Preselect the filtered instance only if it can actually be chosen
    // (dropdown value must exist in its items).
    final presetUuid = widget.coolifyInstance?.publicUuid;
    final canPreset = presetUuid != null &&
        provider.activeCoolifyInstances.any((i) => i.publicUuid == presetUuid);

    showDialog(
      context: context,
      builder: (_) => _CreateServerDialog(
        presetInstanceUuid: canPreset ? presetUuid : null,
      ),
    );
  }

  // 4 -> "4", 2.5 -> "2.5"
  static String _fmt(double v) =>
      v == v.roundToDouble() ? v.toStringAsFixed(0) : v.toStringAsFixed(1);

  @override
  Widget build(BuildContext context) {
    final instance = widget.coolifyInstance;

    return Scaffold(
      appBar: AppBar(
        title: Text(instance == null ? 'Servers' : 'Servers — ${instance.name}'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh',
            onPressed: _load,
          ),
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Add server',
            onPressed: _openCreateDialog,
          ),
        ],
      ),
      body: Consumer<InfrastructureProvider>(
        builder: (context, provider, _) {
          final servers = provider.servers;
          final error = provider.serversErrorMessage;

          if (provider.isLoadingServers && servers.isEmpty) {
            return const Center(child: CircularProgressIndicator());
          }
          if (error != null && servers.isEmpty) {
            return InfrastructureErrorState(message: error, onRetry: _load);
          }
          if (servers.isEmpty) {
            return const Center(child: Text('No servers yet.'));
          }

          return Column(
            children: [
              if (provider.isLoadingServers) const LinearProgressIndicator(),
              if (error != null)
                InfrastructureErrorBanner(
                  message: error,
                  onDismiss: provider.clearErrors,
                ),
              Expanded(
                child: ListView.separated(
                  itemCount: servers.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final s = servers[index];

                    final network = [
                      if (s.hostname != null && s.hostname!.isNotEmpty)
                        s.hostname!,
                      if (s.publicIp != null && s.publicIp!.isNotEmpty)
                        s.publicIp!,
                      if (s.region != null && s.region!.isNotEmpty) s.region!,
                    ].join(' · ');

                    return ListTile(
                      leading: const Icon(Icons.dns_outlined),
                      title: Text(s.name),
                      isThreeLine: network.isNotEmpty,
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${s.coolifyInstanceName} · '
                            '${s.serverProviderName ?? 'No provider'}',
                          ),
                          Text(
                            '${_fmt(s.totalCpu)} vCPU · '
                            '${_fmt(s.totalMemoryGb)} GB RAM · '
                            '${_fmt(s.totalStorageGb)} GB storage',
                          ),
                          if (network.isNotEmpty) Text(network),
                        ],
                      ),
                      trailing: InfrastructureStatusDropdown(
                        value: s.status,
                        options: InfrastructureStatuses.server,
                        busy: provider.isUpdatingStatus(s.publicUuid),
                        onChanged: (status) =>
                            provider.updateServerStatus(s.publicUuid, status),
                      ),
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

// ── Create dialog ───────────────────────────────────────────────────

class _CreateServerDialog extends StatefulWidget {
  final String? presetInstanceUuid;

  const _CreateServerDialog({this.presetInstanceUuid});

  @override
  State<_CreateServerDialog> createState() => _CreateServerDialogState();
}

class _CreateServerDialogState extends State<_CreateServerDialog> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _coolifyServerUuidController = TextEditingController();
  final _hostnameController = TextEditingController();
  final _publicIpController = TextEditingController();
  final _regionController = TextEditingController();
  final _cpuController = TextEditingController();
  final _memoryGbController = TextEditingController();
  final _storageGbController = TextEditingController();

  late String? _instanceUuid = widget.presetInstanceUuid;
  int? _providerId;
  bool _submitting = false;

  @override
  void dispose() {
    _nameController.dispose();
    _coolifyServerUuidController.dispose();
    _hostnameController.dispose();
    _publicIpController.dispose();
    _regionController.dispose();
    _cpuController.dispose();
    _memoryGbController.dispose();
    _storageGbController.dispose();
    super.dispose();
  }

  String? _required(String? v) =>
      (v == null || v.trim().isEmpty) ? 'Required' : null;

  double? _parsePositive(String? raw) {
    final v = double.tryParse((raw ?? '').trim().replaceAll(',', '.'));
    return (v != null && v > 0) ? v : null;
  }

  String? _positiveNumber(String? v) =>
      _parsePositive(v) == null ? 'Enter a number greater than 0' : null;

  String? _optionalIp(String? v) {
    if (v == null || v.trim().isEmpty) return null;
    return InfrastructureValidators.isValidIp(v)
        ? null
        : 'Not a valid IPv4/IPv6 address';
  }

  String? _nullIfBlank(String v) => v.trim().isEmpty ? null : v.trim();

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    // The form takes GB (friendlier); the API stores MB.
    final memoryMb = (_parsePositive(_memoryGbController.text)! * 1024).round();
    final storageMb =
        (_parsePositive(_storageGbController.text)! * 1024).round();

    final success = await context.read<InfrastructureProvider>().createServer(
          coolifyInstancePublicUuid: _instanceUuid!,
          idServerProvider: _providerId,
          name: _nameController.text,
          coolifyServerUuid: _coolifyServerUuidController.text,
          hostname: _nullIfBlank(_hostnameController.text),
          publicIp: _nullIfBlank(_publicIpController.text),
          region: _nullIfBlank(_regionController.text),
          totalCpu: _parsePositive(_cpuController.text)!,
          totalMemoryMb: memoryMb,
          totalStorageMb: storageMb,
        );

    if (!mounted) return;
    setState(() => _submitting = false);
    if (success) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<InfrastructureProvider>();
    final activeInstances = provider.activeCoolifyInstances;
    final errorMessage = provider.serversErrorMessage;

    const numberKeyboard = TextInputType.numberWithOptions(decimal: true);

    return AlertDialog(
      title: const Text('Add server'),
      content: SizedBox(
        width: 480,
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: _instanceUuid,
                  isExpanded: true,
                  decoration: InputDecoration(
                    labelText: 'Coolify instance',
                    helperText: activeInstances.isEmpty
                        ? 'No active Coolify instances available'
                        : null,
                  ),
                  items: activeInstances
                      .map((i) => DropdownMenuItem(
                            value: i.publicUuid,
                            child: Text(i.name),
                          ))
                      .toList(),
                  onChanged: (v) => setState(() => _instanceUuid = v),
                  validator: (v) => v == null ? 'Required' : null,
                ),
                DropdownButtonFormField<int?>(
                  initialValue: _providerId,
                  isExpanded: true,
                  decoration:
                      const InputDecoration(labelText: 'Provider (optional)'),
                  items: [
                    const DropdownMenuItem<int?>(
                        value: null, child: Text('None')),
                    ...provider.serverProviders.map(
                      (p) => DropdownMenuItem<int?>(
                        value: p.idServerProvider,
                        child: Text(p.name),
                      ),
                    ),
                  ],
                  onChanged: (v) => setState(() => _providerId = v),
                ),
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(labelText: 'Name'),
                  validator: _required,
                ),
                TextFormField(
                  controller: _coolifyServerUuidController,
                  decoration:
                      const InputDecoration(labelText: 'Coolify server UUID'),
                  validator: _required,
                ),
                TextFormField(
                  controller: _hostnameController,
                  decoration:
                      const InputDecoration(labelText: 'Hostname (optional)'),
                ),
                TextFormField(
                  controller: _publicIpController,
                  decoration:
                      const InputDecoration(labelText: 'Public IP (optional)'),
                  validator: _optionalIp,
                ),
                TextFormField(
                  controller: _regionController,
                  decoration:
                      const InputDecoration(labelText: 'Region (optional)'),
                ),
                TextFormField(
                  controller: _cpuController,
                  decoration: const InputDecoration(labelText: 'CPU (vCPU)'),
                  keyboardType: numberKeyboard,
                  validator: _positiveNumber,
                ),
                TextFormField(
                  controller: _memoryGbController,
                  decoration: const InputDecoration(labelText: 'Memory (GB)'),
                  keyboardType: numberKeyboard,
                  validator: _positiveNumber,
                ),
                TextFormField(
                  controller: _storageGbController,
                  decoration: const InputDecoration(labelText: 'Storage (GB)'),
                  keyboardType: numberKeyboard,
                  validator: _positiveNumber,
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