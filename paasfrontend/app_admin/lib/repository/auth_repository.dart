import '../model/auth_model.dart';
import '../service/api_exception.dart';
import '../service/auth_service.dart';

class AuthRepository {
  final AuthService _authService;

  AuthRepository({AuthService? authService})
      : _authService = authService ?? AuthService();

  /// Login com regra de negócio do painel admin: um utilizador CUSTOMER
  /// nunca deve entrar aqui, mesmo que as credenciais estejam correctas —
  /// é recusado explicitamente antes de o provider guardar sessão.
  Future<AuthResponse> login(String email, String password) async {
    final auth = await _authService.login(email, password);

    if (!auth.isStaff) {
      await _authService.logout(); // limpa o token já guardado pelo service
      throw ApiException(
        statusCode: 403,
        message: 'Esta conta não tem acesso ao painel administrativo.',
      );
    }

    return auth;
  }

  Future<void> logout() async {
    await _authService.logout();
  }

  Future<AuthResponse> getCurrentUser(String publicUuid) async {
    return _authService.getCurrentUser(publicUuid);
  }

  Future<List<AuthResponse>> listStaff() async {
    return _authService.listStaff();
  }

 static const String defaultStaffPassword = '12345678';

  Future<AuthResponse> createStaffUser({
    required String email,
    required String firstName,
    String? lastName,
    String? phone,
    required String platformRole,
  }) async {
    if (platformRole == 'CUSTOMER') {
      throw ApiException(
        statusCode: 400,
        message: 'CUSTOMER is not a valid role for staff creation.',
      );
    }
    return _authService.createStaffUser(
      email: email,
      firstName: firstName,
      lastName: lastName,
      phone: phone,
      platformRole: platformRole,
    );
  }

  Future<AuthResponse> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    return _authService.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
  }

  Future<AuthResponse> updateUserActiveStatus(String publicUuid, bool active) async {
    return _authService.updateUserActiveStatus(publicUuid, active);
  }

  Future<AuthResponse> updatePlatformRole(String publicUuid, String platformRole) async {
    return _authService.updatePlatformRole(publicUuid, platformRole);
  }
}