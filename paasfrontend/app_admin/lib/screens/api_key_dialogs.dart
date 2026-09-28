import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../model/api_key_model.dart';
import '../provider/api_key_provider.dart';

/// Asks for the revocation reason and runs the revocation. Pops with
/// `true` on success. On failure the error is shown inside the dialog.
class RevokeApiKeyDialog extends StatefulWidget {
  final String organizationPublicUuid;
  final String keyPublicUuid;
  final String keyName;

  const RevokeApiKeyDialog({
    super.key,
    required this.organizationPublicUuid,
    required this.keyPublicUuid,
    required this.keyName,
  });

  @override
  State<RevokeApiKeyDialog> createState() => _RevokeApiKeyDialogState();
}

class _RevokeApiKeyDialogState extends State<RevokeApiKeyDialog> {
  final _formKey = GlobalKey<FormState>();
  final _reasonController = TextEditingController();
  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final provider = context.read<ApiKeyProvider>();
    setState(() {
      _submitting = true;
      _error = null;
    });

    final ok = await provider.revokeKey(
      widget.organizationPublicUuid,
      widget.keyPublicUuid,
      reason: _reasonController.text,
    );
    if (!mounted) return;

    if (ok) {
      Navigator.of(context).pop(true);
      return;
    }

    // Keep the message inside the dialog and clear it from the provider,
    // so the banner of the screen behind does not repeat it.
    final message = provider.errorMessage;
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
      title: Text('Revoke ${widget.keyName}'),
      content: SizedBox(
        width: 420,
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Revocation is permanent: the key stops working and cannot '
                'be reactivated. The reason is stored on the key and '
                'returned by the API to the organization\'s members.',
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _reasonController,
                enabled: !_submitting,
                autofocus: true,
                minLines: 3,
                maxLines: 5,
                maxLength: ApiKeyValidators.maxRevocationReasonLength,
                decoration: const InputDecoration(
                  labelText: 'Reason',
                  border: OutlineInputBorder(),
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return 'A revocation reason is required.';
                  }
                  return null;
                },
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
              : const Text('Revoke'),
        ),
      ],
    );
  }
}