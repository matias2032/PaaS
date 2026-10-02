import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/auth_model.dart';
import '../provider/auth_provider.dart';
import '../repository/auth_repository.dart';

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

    Future<void> _openResetDialog(AuthResponse user) async {
    final reset = await showDialog<bool>(
      context: context,
      builder: (_) => _ResetPasswordDialog(user: user),
    );
    if (reset == true && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Password reset. ${user.firstName} must set a new one at next login.',
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Staff'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Create staff user',
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
            return const Center(child: Text('No staff users yet.'));
          }
          return ListView.separated(
            itemCount: authProvider.staff.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final user = authProvider.staff[index];
              return ListTile(
                title: Text('${user.firstName} ${user.lastName ?? ''}'.trim()),
                subtitle: Text(
                  user.firstPassword
                      ? '${user.email} · must change password'
                      : user.email,
                ),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Chip(label: Text(user.platformRole)),
                    const SizedBox(width: 8),
                    Tooltip(
                      message: user.isActive ? 'Deactivate' : 'Activate',
                      child: Switch(
                        value: user.isActive,
                        onChanged: (value) => context
                            .read<AuthProvider>()
                            .updateUserActiveStatus(user.publicUuid, value),
                      ),
                    ),
                    const SizedBox(width: 4),
                    // Mirrors the backend rule: only strictly lower ranks can
                    // be reset, and this screen is owner-only, so another
                    // PLATFORM_OWNER is never resettable.
                    IconButton(
                      tooltip: user.isPlatformOwner
                          ? "Cannot reset another owner's password"
                          : 'Reset password',
                      icon: const Icon(Icons.lock_reset),
                      onPressed: user.isPlatformOwner
                          ? null
                          : () => _openResetDialog(user),
                    ),
                  ],
                ),
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
  String _platformRole = _platformRoles.first;
  bool _submitting = false;

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);

    final success = await context.read<AuthProvider>().createStaffUser(
          email: _emailController.text.trim(),
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
      title: const Text('Create staff user'),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _firstNameController,
                decoration: const InputDecoration(labelText: 'First name'),
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Required' : null,
              ),
              TextFormField(
                controller: _lastNameController,
                decoration: const InputDecoration(labelText: 'Last name (optional)'),
              ),
              TextFormField(
                controller: _emailController,
                decoration: const InputDecoration(labelText: 'Email'),
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Required' : null,
              ),
              TextFormField(
                controller: _phoneController,
                decoration: const InputDecoration(labelText: 'Phone (optional)'),
              ),
              DropdownButtonFormField<String>(
                initialValue: _platformRole,
                decoration: const InputDecoration(labelText: 'Platform role'),
                items: _platformRoles
                    .map((role) => DropdownMenuItem(value: role, child: Text(role)))
                    .toList(),
                onChanged: (value) => setState(() => _platformRole = value!),
              ),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Temporary password: ${AuthRepository.defaultStaffPassword} '
                  '(the user must change it on first login).',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
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
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Create'),
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
      title: Text('Change role — ${widget.user.firstName}'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          DropdownButtonFormField<String>(
            initialValue: _selectedRole,
            decoration: const InputDecoration(labelText: 'Platform role'),
            // CUSTOMER included here — the only screen where it makes
            // sense (revokes staff access, see UpdatePlatformRoleRequestDTO).
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
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Save'),
        ),
      ],
    );
  }
}


class _ResetPasswordDialog extends StatefulWidget {
  final AuthResponse user;
  const _ResetPasswordDialog({required this.user});

  @override
  State<_ResetPasswordDialog> createState() => _ResetPasswordDialogState();
}

class _ResetPasswordDialogState extends State<_ResetPasswordDialog> {
  bool _submitting = false;
  String? _error;

  Future<void> _submit() async {
    setState(() {
      _submitting = true;
      _error = null;
    });

    final error = await context
        .read<AuthProvider>()
        .resetStaffPassword(widget.user.publicUuid);

    if (!mounted) return;
    if (error == null) {
      Navigator.of(context).pop(true);
    } else {
      setState(() {
        _submitting = false;
        _error = error;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('Reset password — ${widget.user.firstName}'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "${widget.user.email}'s password will be set to "
            '${AuthRepository.defaultStaffPassword}. They will have to choose '
            'a new one the next time they sign in.',
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(_error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
        ],
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
                  height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Reset'),
        ),
      ],
    );
  }
}