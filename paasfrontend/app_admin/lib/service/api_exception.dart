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
        message: 'Falha de ligação. Verifica a tua rede e o servidor.',
      );

  factory ApiException.timeout() => ApiException(
        statusCode: null,
        message: 'O pedido demorou demasiado tempo. Tenta novamente.',
      );

  static dynamic _tryDecodeJson(String body) {
    // Import local a jsonDecode feito no ficheiro que usa isto,
    // mantido simples aqui para não acoplar dependências extra.
    return null;
  }

  static String _defaultMessageFor(int statusCode) {
    switch (statusCode) {
      case 400:
        return 'Pedido inválido.';
      case 401:
        return 'Sessão expirada ou credenciais inválidas.';
      case 403:
        return 'Não tens permissão para esta acção.';
      case 404:
        return 'Recurso não encontrado.';
      case 409:
        return 'Conflito de estado — a acção não pode ser concluída.';
      default:
        return 'Erro inesperado ($statusCode).';
    }
  }

  @override
  String toString() => message;
}