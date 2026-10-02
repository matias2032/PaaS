import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../provider/auth_provider.dart';
import '../widget/password_form_field.dart';
import 'home_screen.dart';

class ChangePasswordScreen extends StatefulWidget {
  final bool forced;

  const ChangePasswordScreen({super.key, this.forced = false});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _currentController = TextEditingController();
  final _newController = TextEditingController();
  final _confirmController = TextEditingController();
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    // Drop any stale message (e.g. from a previous login attempt).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<AuthProvider>().clearError();
    });
  }

  @override
  void dispose() {
    _currentController.dispose();
    _newController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final authProvider = context.read<AuthProvider>();
    setState(() => _submitting = true);
    final success = await authProvider.changePassword(
      currentPassword: _currentController.text,
      newPassword: _newController.text,
    );
    if (!mounted) return;
    setState(() => _submitting = false);
    if (!success) return;

    if (widget.forced) {
      // Temporary password replaced: continue into the app.
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => authProvider.mustChangePassword
              ? const ChangePasswordScreen(forced: true)
              : const HomeScreen(),
        ),
      );
    } else {
      // Opened from the sidebar: just go back.
      final messenger = ScaffoldMessenger.of(context);
      Navigator.of(context).pop();
      messenger.showSnackBar(
        const SnackBar(content: Text('Password changed.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      // Enquanto forced=true (fluxo de first_password), o utilizador
      // não pode fugir desta tela sem mudar a password — regra 1.
      canPop: !widget.forced,
      child: Scaffold(
        appBar: widget.forced
            ? null
            : AppBar(title: const Text('Change password')),
        body: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 400),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Consumer<AuthProvider>(
                builder: (context, authProvider, _) {
                  return Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        if (widget.forced) ...[
                          Text(
                            'Set your password',
                            style: Theme.of(context).textTheme.headlineSmall,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 8),
                          const Text(
                            'You are using a temporary password. You must set a new one before continuing.',
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 24),
                        ],
                        PasswordFormField(
                          controller: _currentController,
                          label: 'Current password',
                          validator: (v) =>
                              (v == null || v.isEmpty) ? 'Required' : null,
                        ),
                        const SizedBox(height: 16),
                        PasswordFormField(
                          controller: _newController,
                          label: 'New password',
                          validator: (v) {
                            if (v == null || v.length < 8) {
                              return 'Minimum 8 characters';
                            }
                            if (v == _currentController.text) {
                              return 'Must be different from the current password';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        PasswordFormField(
                          controller: _confirmController,
                          label: 'Confirm new password',
                          validator: (v) => v != _newController.text
                              ? 'Passwords do not match'
                              : null,
                        ),
                        if (authProvider.errorMessage != null) ...[
                          const SizedBox(height: 16),
                          Text(
                            authProvider.errorMessage!,
                            style: TextStyle(
                                color: Theme.of(context).colorScheme.error),
                            textAlign: TextAlign.center,
                          ),
                        ],
                        const SizedBox(height: 24),
                        FilledButton(
                          onPressed: _submitting ? null : _submit,
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
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}