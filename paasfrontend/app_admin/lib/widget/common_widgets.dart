import 'package:flutter/material.dart';

// Small widgets shared by the screens of every module (infrastructure,
// billing, organization, ...). They only depend on plain values, never on
// module models.

// ── Status ──────────────────────────────────────────────────────────

/// Colored chip for an entity status. Colors are keyed by the status
/// string; anything unknown (e.g. ARCHIVED) falls back to grey.
class StatusChip extends StatelessWidget {
  final String status;

  const StatusChip({super.key, required this.status});

  Color get _color {
    switch (status) {
      case 'ACTIVE':
        return Colors.green;
      case 'INACTIVE':
        return Colors.orange;
      case 'SUSPENDED':
      case 'REVOKED':
        return Colors.red;
      case 'EXPIRED':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Chip(
      label: Text(status),
      visualDensity: VisualDensity.compact,
      side: BorderSide.none,
      backgroundColor: _color.withAlpha(40),
      labelStyle: TextStyle(color: _color, fontWeight: FontWeight.w600),
    );
  }
}

/// Inline status selector for a list row. Shows a spinner while the
/// PATCH is in flight so the row cannot be changed twice.
class StatusDropdown extends StatelessWidget {
  final String value;
  final List<String> options;
  final bool busy;
  final ValueChanged<String> onChanged;

  const StatusDropdown({
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

// ── Errors ──────────────────────────────────────────────────────────

/// Dismissible error shown above an already-loaded list, so a failed
/// mutation does not replace the whole list with an error.
class ErrorBanner extends StatelessWidget {
  final String message;
  final VoidCallback onDismiss;

  const ErrorBanner({
    super.key,
    required this.message,
    required this.onDismiss,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.fromLTRB(12, 4, 4, 4),
      decoration: BoxDecoration(
        color: scheme.errorContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Icon(Icons.error_outline, color: scheme.onErrorContainer),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(color: scheme.onErrorContainer),
            ),
          ),
          IconButton(
            icon: Icon(Icons.close, color: scheme.onErrorContainer),
            tooltip: 'Dismiss',
            onPressed: onDismiss,
          ),
        ],
      ),
    );
  }
}

/// Full-area error with a retry button, used only when there is nothing
/// to show yet.
class ErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const ErrorState({
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
          Icon(
            Icons.error_outline,
            size: 40,
            color: Theme.of(context).colorScheme.error,
          ),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}

// ── Formatting ──────────────────────────────────────────────────────

/// Local date-time as `yyyy-MM-dd HH:mm`.
String formatDateTime(DateTime value) {
  final local = value.toLocal();
  String two(int n) => n.toString().padLeft(2, '0');
  return '${local.year}-${two(local.month)}-${two(local.day)} '
      '${two(local.hour)}:${two(local.minute)}';
}