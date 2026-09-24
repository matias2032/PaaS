import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../config/api_config.dart';
import 'api_exception.dart';

class ApiClient {
  static const _secureStorage = FlutterSecureStorage();
  static const _tokenKey = 'auth_token';

  static Future<String?> get _token async {
    return _secureStorage.read(key: _tokenKey);
  }

  static Future<void> saveToken(String token) async {
    await _secureStorage.write(key: _tokenKey, value: token);
  }

  static Future<void> clearToken() async {
    await _secureStorage.delete(key: _tokenKey);
  }

  static Future<Map<String, String>> _headers({bool authenticated = true}) async {
    if (!authenticated) return ApiConfig.defaultHeaders;

    final token = await _token;
    if (token == null) {
      throw ApiException(statusCode: 401, message: 'Session not started.');
    }
    return ApiConfig.authHeaders(token);
  }

  static Future<dynamic> get(String url, {bool authenticated = true}) async {
    return _send(() async {
      final headers = await _headers(authenticated: authenticated);
      return http.get(Uri.parse(url), headers: headers).timeout(ApiConfig.timeout);
    });
  }

  static Future<dynamic> post(
    String url, {
    Map<String, dynamic>? body,
    bool authenticated = true,
  }) async {
    return _send(() async {
      final headers = await _headers(authenticated: authenticated);
      return http
          .post(Uri.parse(url), headers: headers, body: body != null ? jsonEncode(body) : null)
          .timeout(ApiConfig.timeout);
    });
  }

  static Future<dynamic> patch(
    String url, {
    Map<String, dynamic>? body,
    bool authenticated = true,
  }) async {
    return _send(() async {
      final headers = await _headers(authenticated: authenticated);
      return http
          .patch(Uri.parse(url), headers: headers, body: body != null ? jsonEncode(body) : null)
          .timeout(ApiConfig.timeout);
    });
  }

  static Future<dynamic> put(
    String url, {
    Map<String, dynamic>? body,
    bool authenticated = true,
  }) async {
    return _send(() async {
      final headers = await _headers(authenticated: authenticated);
      return http
          .put(Uri.parse(url), headers: headers, body: body != null ? jsonEncode(body) : null)
          .timeout(ApiConfig.timeout);
    });
  }

  static Future<dynamic> _send(Future<http.Response> Function() request) async {
    http.Response response;
    try {
      response = await request();
    } on ApiException {
      rethrow;
    } catch (_) {
      throw ApiException.network();
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return null;
      return jsonDecode(response.body);
    }

    String message = ApiException.fromResponseBody(response.statusCode, '').message;
    if (response.body.isNotEmpty) {
      try {
        final decoded = jsonDecode(response.body);
        if (decoded is Map && decoded['message'] != null) {
          message = decoded['message'] as String;
        }
      } catch (_) {
        // corpo não é JSON — mantém a mensagem por omissão
      }
    }
    throw ApiException(statusCode: response.statusCode, message: message);
  }
}