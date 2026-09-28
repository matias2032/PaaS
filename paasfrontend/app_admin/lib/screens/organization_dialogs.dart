import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/organization_model.dart';
import '../provider/organization_provider.dart';

/// Asks for the suspension reason and runs the suspension. Pops with
/// `true` on success. On failure the error is shown inside the dialog.
class SuspendOrganizationDialog extends StatefulWidget {
  final String publicUuid;
  final String organizationName;

  const SuspendOrganizationDialog({
    super.key,
    required this.publicUuid,
    required this.organizationName,
  });

  @override
  State<SuspendOrganizationDialog> createState() =>
      _SuspendOrganizationDialogState();
}

class _SuspendOrganizationDialogState extends State<SuspendOrganizationDialog> {
  final _formKey = GlobalKey<FormState>();
  final _reasonController = TextEditingController();
  bool _submitting = false;
  // Irreversible, so it always starts off.
  bool _revokeApiKeys = false;
  String? _error;

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final provider = context.read<OrganizationProvider>();
    setState(() {
      _submitting = true;
      _error = null;
    });

    final ok = await provider.suspendOrganization(
      widget.publicUuid,
      reason: _reasonController.text,
      revokeApiKeys: _revokeApiKeys,
    );
    if (!mounted) return;

    if (ok) {
      Navigator.of(context).pop(true);
      return;
    }

    // Keep the message inside the dialog and clear it from the provider,
    // so the banner of the screen behind does not repeat it.
    final message = provider.organizationsErrorMessage;
    provider.clearErrors();
    setState(() {
      _submitting = false;
      _error = message;
    });
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;

    return AlertDialog(
      title: Text('Suspend ${widget.organizationName}'),
      content: SizedBox(
        width: 420,
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'While suspended, no changes are allowed in this '
                'organization. The reason is stored on the organization and '
                'returned by the API to its members, so write it accordingly.',
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _reasonController,
                enabled: !_submitting,
                autofocus: true,
                minLines: 3,
                maxLines: 5,
                maxLength: OrganizationValidators.maxSuspensionReasonLength,
                decoration: const InputDecoration(
                  labelText: 'Reason',
                  border: OutlineInputBorder(),
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return 'A suspension reason is required.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 8),
              CheckboxListTile(
                value: _revokeApiKeys,
                onChanged: _submitting
                    ? null
                    : (value) =>
                        setState(() => _revokeApiKeys = value ?? false),
                contentPadding: EdgeInsets.zero,
                controlAffinity: ListTileControlAffinity.leading,
                title: const Text('Also revoke all API keys'),
                subtitle: Text(
                  'Irreversible. Lifting the suspension will NOT restore '
                  'them: the owner has to create new keys and update every '
                  'integration. Leave unchecked if the suspension may be '
                  'temporary; the keys are already refused while the '
                  'organization is suspended.',
                  style: _revokeApiKeys
                      ? TextStyle(color: scheme.error)
                      : null,
                ),
              ),
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(_error!, style: TextStyle(color: scheme.error)),
              ],
            ],
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
          style: FilledButton.styleFrom(
            backgroundColor: scheme.error,
            foregroundColor: scheme.onError,
          ),
          child: _submitting
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Suspend'),
        ),
      ],
    );
  }
}