import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../provider/auth_provider.dart';
import '../repository/auth_repository.dart';

// Edit profile (admin side): first name, last name and phone are editable;
// the email is the login and is shown read-only. Opened as a full-screen
// route from the sidebar.
class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _phoneController = TextEditingController();
  bool _submitting = false;

  // The in-memory user comes from the login response, so it can be stale.
  // Reload it before showing the form: the fields are pre-filled from it
  // and the backend overwrites whatever is sent.
  bool _loading = true;
  bool _loadFailed = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final provider = context.read<AuthProvider>();
    provider.clearError();
    setState(() {
      _loading = true;
      _loadFailed = false;
    });

    final ok = await provider.refreshCurrentUser();
    if (!mounted) return;
    final user = provider.currentUser;
    _firstNameController.text = user?.firstName ?? '';
    _lastNameController.text = user?.lastName ?? '';
    _phoneController.text = user?.phone ?? '';
    setState(() {
      _loading = false;
      _loadFailed = !ok;
    });
  }

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);

    setState(() => _submitting = true);
    final success = await context.read<AuthProvider>().updateProfile(
          firstName: _firstNameController.text,
          lastName: _lastNameController.text,
          phone: _phoneController.text,
        );
    if (!mounted) return;
    setState(() => _submitting = false);

    if (success) {
      navigator.pop();
      messenger.showSnackBar(
        const SnackBar(content: Text('Profile updated.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final user = authProvider.currentUser;
    // Nothing to save until a field actually differs from the current value.
    final changed =
        _firstNameController.text.trim() != (user?.firstName ?? '') ||
            _lastNameController.text.trim() != (user?.lastName ?? '') ||
            _phoneController.text.trim() != (user?.phone ?? '');

    if (_loading || _loadFailed) {
      return Scaffold(
        appBar: AppBar(title: const Text('Edit profile')),
        body: Center(
          child: _loading
              ? const CircularProgressIndicator()
              : Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      authProvider.errorMessage ?? 'Failed to load your profile.',
                      style: TextStyle(color: Theme.of(context).colorScheme.error),
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton(
                      onPressed: _load,
                      child: const Text('Retry'),
                    ),
                  ],
                ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Edit profile')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 400),
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  TextFormField(
                    controller: _firstNameController,
                    textInputAction: TextInputAction.next,
                    onChanged: (_) => setState(() {}), // refreshes `changed`
                    decoration: const InputDecoration(
                      labelText: 'First name',
                      border: OutlineInputBorder(),
                    ),
                    validator: (v) {
                      final t = (v ?? '').trim();
                      if (t.isEmpty) return 'First name is required';
                      if (t.length > AuthRepository.maxNameLength) {
                        return 'At most ${AuthRepository.maxNameLength} characters';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _lastNameController,
                    textInputAction: TextInputAction.next,
                    onChanged: (_) => setState(() {}),
                    decoration: const InputDecoration(
                      labelText: 'Last name (optional)',
                      border: OutlineInputBorder(),
                    ),
                    validator: (v) =>
                        (v ?? '').trim().length > AuthRepository.maxNameLength
                            ? 'At most ${AuthRepository.maxNameLength} characters'
                            : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    initialValue: user?.email ?? '',
                    enabled: false,
                    decoration: const InputDecoration(
                      labelText: 'Email',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    onChanged: (_) => setState(() {}), // refreshes `changed`
                    onFieldSubmitted: (_) {
                      if (changed && !_submitting) _submit();
                    },
                    decoration: const InputDecoration(
                      labelText: 'Phone number',
                      border: OutlineInputBorder(),
                    ),
                    validator: (v) {
                      final t = (v ?? '').trim();
                      // Unchanged: not re-validated, so a legacy number (or no
                      // number at all) does not block editing the name.
                      if (t == (user?.phone ?? '')) return null;
                      if (t.isEmpty) return 'Phone number cannot be empty';
                      return AuthRepository.phonePattern.hasMatch(t)
                          ? null
                          : 'Digits, spaces, +, - and ( ) only (7 to 30 characters)';
                    },
                  ),
                  if (authProvider.errorMessage != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      authProvider.errorMessage!,
                      style: TextStyle(color: Theme.of(context).colorScheme.error),
                      textAlign: TextAlign.center,
                    ),
                  ],
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: (_submitting || !changed) ? null : _submit,
                    child: _submitting
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Save'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

