import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/auth_model.dart';
import '../provider/auth_provider.dart';

const _platformRoles = ['SUPPORT', 'PLATFORM_ADMIN', 'PLATFORM_OWNER'];

class StaffScreen extends StatefulWidget {
  const StaffScreen({super.key});

  @override
  State<StaffScreen> createState() => _StaffScreenState();
}

class _StaffScreenState extends State<StaffScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AuthProvider>().loadStaff();
    });
  }

  void _openCreateDialog() {
    showDialog(
      context: context,
      builder: (_) => const _CreateStaffDialog(),
    );
  }

  void _openRoleDialog(AuthResponse user) {
    showDialog(
      context: context,
      builder: (_) => _ChangeRoleDialog(user: user),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Staff'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Criar staff',
            onPressed: _openCreateDialog,
          ),
        ],
      ),
      body: Consumer<AuthProvider>(
        builder: (context, authProvider, _) {
          if (authProvider.isLoadingStaff) {
            return const Center(child: CircularProgressIndicator());
          }
          if (authProvider.staffErrorMessage != null) {
            return Center(child: Text(authProvider.staffErrorMessage!));
          }
          if (authProvider.staff.isEmpty) {
            return const Center(child: Text('Nenhum utilizador de staff.'));
          }
          return ListView.separated(
            itemCount: authProvider.staff.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final user = authProvider.staff[index];
              return ListTile(
                title: Text('${user.firstName} ${user.lastName ?? ''}'.trim()),
                subtitle: Text(user.email),
                trailing: Chip(label: Text(user.platformRole)),
                onTap: () => _openRoleDialog(user),
              );
            },
          );
        },
      ),
    );
  }
}

class _CreateStaffDialog extends StatefulWidget {
  const _CreateStaffDialog();

  @override
  State<_CreateStaffDialog> createState() => _CreateStaffDialogState();
}

class _CreateStaffDialogState extends State<_CreateStaffDialog> {
  final _formKey = GlobalKey<FormState>();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  String _platformRole = _platformRoles.first;
  bool _submitting = false;

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    final success = await context.read<AuthProvider>().createStaffUser(
          email: _emailController.text.trim(),
          password: _passwordController.text,
          firstName: _firstNameController.text.trim(),
          lastName: _lastNameController.text.trim().isEmpty
              ? null
              : _lastNameController.text.trim(),
          phone: _phoneController.text.trim().isEmpty
              ? null
              : _phoneController.text.trim(),
          platformRole: _platformRole,
        );

    if (!mounted) return;
    setState(() => _submitting = false);
    if (success) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final errorMessage = context.watch<AuthProvider>().staffErrorMessage;

    return AlertDialog(
      title: const Text('Criar staff'),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _firstNameController,
                decoration: const InputDecoration(labelText: 'Primeiro nome'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Obrigatório' : null,
              ),
              TextFormField(
                controller: _lastNameController,
                decoration: const InputDecoration(labelText: 'Último nome (opcional)'),
              ),
              TextFormField(
                controller: _emailController,
                decoration: const InputDecoration(labelText: 'Email'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Obrigatório' : null,
              ),
              TextFormField(
                controller: _phoneController,
                decoration: const InputDecoration(labelText: 'Telefone (opcional)'),
              ),
              TextFormField(
                controller: _passwordController,
                decoration: const InputDecoration(labelText: 'Palavra-passe'),
                obscureText: true,
                validator: (v) => (v == null || v.length < 8)
                    ? 'Mínimo 8 caracteres'
                    : null,
              ),
              DropdownButtonFormField<String>(
                initialValue: _platformRole,
                decoration: const InputDecoration(labelText: 'Platform role'),
                items: _platformRoles
                    .map((role) => DropdownMenuItem(value: role, child: Text(role)))
                    .toList(),
                onChanged: (value) => setState(() => _platformRole = value!),
              ),
              if (errorMessage != null) ...[
                const SizedBox(height: 12),
                Text(errorMessage,
                    style: TextStyle(color: Theme.of(context).colorScheme.error)),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Criar'),
        ),
      ],
    );
  }
}

class _ChangeRoleDialog extends StatefulWidget {
  final AuthResponse user;
  const _ChangeRoleDialog({required this.user});

  @override
  State<_ChangeRoleDialog> createState() => _ChangeRoleDialogState();
}

class _ChangeRoleDialogState extends State<_ChangeRoleDialog> {
  late String _selectedRole = widget.user.platformRole;
  bool _submitting = false;

  Future<void> _submit() async {
    setState(() => _submitting = true);
    final success = await context
        .read<AuthProvider>()
        .updatePlatformRole(widget.user.publicUuid, _selectedRole);
    if (!mounted) return;
    setState(() => _submitting = false);
    if (success) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final errorMessage = context.watch<AuthProvider>().staffErrorMessage;

    return AlertDialog(
      title: Text('Alterar role — ${widget.user.firstName}'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          DropdownButtonFormField<String>(
            initialValue: _selectedRole,
            decoration: const InputDecoration(labelText: 'Platform role'),
            // CUSTOMER incluído aqui — é o único ecrã onde faz sentido
            // (revoga acesso de staff, ver UpdatePlatformRoleRequestDTO).
            items: ['CUSTOMER', ..._platformRoles]
                .map((role) => DropdownMenuItem(value: role, child: Text(role)))
                .toList(),
            onChanged: (value) => setState(() => _selectedRole = value!),
          ),
          if (errorMessage != null) ...[
            const SizedBox(height: 12),
            Text(errorMessage,
                style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Guardar'),
        ),
      ],
    );
  }
}