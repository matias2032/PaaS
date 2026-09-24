class ApiException implements Exception {
  final int? statusCode;
  final String message;

  ApiException({required this.statusCode, required this.message});

  factory ApiException.fromResponseBody(int statusCode, String body) {
    String message;
    try {
      final decoded = body.isNotEmpty ? _tryDecodeJson(body) : null;
      message = decoded != null && decoded['message'] != null
          ? decoded['message'] as String
          : _defaultMessageFor(statusCode);
    } catch (_) {
      message = _defaultMessageFor(statusCode);
    }
    return ApiException(statusCode: statusCode, message: message);
  }

  factory ApiException.network() => ApiException(
        statusCode: null,
        message: 'Connection failure. Check your network and the server.',
      );

  factory ApiException.timeout() => ApiException(
        statusCode: null,
        message: 'The request took too long. Please try again.',
      );

  static dynamic _tryDecodeJson(String body) {
    // Local import to jsonDecode done in the file using this,
    // kept simple here to avoid coupling extra dependencies.
    return null;
  }

  static String _defaultMessageFor(int statusCode) {
    switch (statusCode) {
      case 400:
        return 'Invalid request.';
      case 401:
        return 'Session expired or invalid credentials.';
      case 403:
        return 'You do not have permission for this action.';
      case 404:
        return 'Resource not found.';
      case 409:
        return 'State conflict — the action cannot be completed.';
      default:
        return 'Unexpected error ($statusCode).';
    }
  }

  @override
  String toString() => message;
}