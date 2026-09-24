import '../config/api_config.dart';
import '../model/auth_model.dart';
import 'api_client.dart';

class AuthService {
  Future<AuthResponse> login(String email, String password) async {
    final json = await ApiClient.post(
      ApiConfig.authLoginUrl,
      body: {'email': email, 'password': password},
      authenticated: false,
    );
    final auth = AuthResponse.fromJson(json as Map<String, dynamic>);

    if (auth.token != null) {
      await ApiClient.saveToken(auth.token!);
    }
    return auth;
  }

  Future<void> logout() async {
    await ApiClient.clearToken();
  }

  Future<AuthResponse> getCurrentUser(String publicUuid) async {
    final json = await ApiClient.get(ApiConfig.authByPublicUuidUrl(publicUuid));
    return AuthResponse.fromJson(json as Map<String, dynamic>);
  }

  Future<List<AuthResponse>> listStaff() async {
    final json = await ApiClient.get(ApiConfig.authStaffUrl);
    return (json as List)
        .map((item) => AuthResponse.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<AuthResponse> createStaffUser({
    required String email,
    required String password,
    required String firstName,
    String? lastName,
    String? phone,
    required String platformRole,
  }) async {
    final json = await ApiClient.post(
      '${ApiConfig.authUrl}/staff',
      body: {
        'email': email,
        'password': password,
        'firstName': firstName,
        if (lastName != null) 'lastName': lastName,
        if (phone != null) 'phone': phone,
        'platformRole': platformRole,
      },
    );
    return AuthResponse.fromJson(json as Map<String, dynamic>);
  }

  Future<AuthResponse> updatePlatformRole(String publicUuid, String platformRole) async {
    final json = await ApiClient.patch(
      ApiConfig.authPlatformRoleUrl(publicUuid),
      body: {'platformRole': platformRole},
    );
    return AuthResponse.fromJson(json as Map<String, dynamic>);
  }
}