import 'package:flutter/material.dart';

// Small widgets shared by the infrastructure screens (Coolify instances
// and Servers), so the two screens do not duplicate them.

/// Inline status selector for a list row. Shows a spinner while the
/// PATCH is in flight so the row cannot be changed twice.
class InfrastructureStatusDropdown extends StatelessWidget {
  final String value;
  final List<String> options;
  final bool busy;
  final ValueChanged<String> onChanged;

  const InfrastructureStatusDropdown({
    super.key,
    required this.value,
    required this.options,
    required this.busy,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    // A status the client does not know about would break the dropdown
    // (value must exist in items) — include it defensively.
    final items = options.contains(value) ? options : [value, ...options];

    return SizedBox(
      width: 150,
      child: Align(
        alignment: Alignment.centerRight,
        child: busy
            ? const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : DropdownButton<String>(
                value: value,
                underline: const SizedBox.shrink(),
                items: items
                    .map((s) => DropdownMenuItem(value: s, child: Text(s)))
                    .toList(),
                onChanged: (v) {
                  if (v != null && v != value) onChanged(v);
                },
              ),
      ),
    );
  }
}

/// Dismissible error strip shown above a list that is already loaded,
/// so a failed mutation does not replace the whole list with an error.
class InfrastructureErrorBanner extends StatelessWidget {
  final String message;
  final VoidCallback onDismiss;

  const InfrastructureErrorBanner({
    super.key,
    required this.message,
    required this.onDismiss,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;

    return Material(
      color: scheme.errorContainer,
      child: Padding(
        padding: const EdgeInsets.only(left: 16, right: 4),
        child: Row(
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Text(
                  message,
                  style: TextStyle(color: scheme.onErrorContainer),
                ),
              ),
            ),
            IconButton(
              icon: Icon(Icons.close, color: scheme.onErrorContainer),
              tooltip: 'Dismiss',
              onPressed: onDismiss,
            ),
          ],
        ),
      ),
    );
  }
}

/// Full-screen error with a retry button, for when the list could not
/// be loaded at all.
class InfrastructureErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const InfrastructureErrorState({
    super.key,
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: onRetry, child: const Text('Retry')),
        ],
      ),
    );
  }
}