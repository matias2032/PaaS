import 'package:flutter/foundation.dart';
import '../model/auth_model.dart';
import '../repository/auth_repository.dart';
import '../service/api_exception.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthProvider extends ChangeNotifier {
  final AuthRepository _authRepository;

  AuthProvider({AuthRepository? authRepository})
      : _authRepository = authRepository ?? AuthRepository();

  AuthStatus _status = AuthStatus.unknown;
  AuthResponse? _currentUser;
  bool _isLoading = false;
  String? _errorMessage;

  // Lista de staff (usada pelo ecrã de listagem/criação — passo 7)
  List<AuthResponse> _staff = [];
  bool _isLoadingStaff = false;
  String? _staffErrorMessage;

  AuthStatus get status => _status;
  AuthResponse? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  bool get isAuthenticated => _status == AuthStatus.authenticated;

  List<AuthResponse> get staff => _staff;
  bool get isLoadingStaff => _isLoadingStaff;
  String? get staffErrorMessage => _staffErrorMessage;

  String? get platformRole => _currentUser?.platformRole;
  bool get isSupport => _currentUser?.isSupport ?? false;
  bool get isPlatformAdmin => _currentUser?.isPlatformAdmin ?? false;
  bool get isPlatformOwner => _currentUser?.isPlatformOwner ?? false;

  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final auth = await _authRepository.login(email, password);
      _currentUser = auth;
      _status = AuthStatus.authenticated;
      _isLoading = false;
      notifyListeners();
      return true;
    } on ApiException catch (e) {
      _errorMessage = e.message;
      _status = AuthStatus.unauthenticated;
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    await _authRepository.logout();
    _currentUser = null;
    _status = AuthStatus.unauthenticated;
    _staff = [];
    notifyListeners();
  }

  Future<void> loadStaff() async {
    _isLoadingStaff = true;
    _staffErrorMessage = null;
    notifyListeners();

    try {
      _staff = await _authRepository.listStaff();
      _isLoadingStaff = false;
      notifyListeners();
    } on ApiException catch (e) {
      _staffErrorMessage = e.message;
      _isLoadingStaff = false;
      notifyListeners();
    }
  }

  Future<bool> createStaffUser({
    required String email,
    required String password,
    required String firstName,
    String? lastName,
    String? phone,
    required String platformRole,
  }) async {
    _staffErrorMessage = null;
    try {
      final created = await _authRepository.createStaffUser(
        email: email,
        password: password,
        firstName: firstName,
        lastName: lastName,
        phone: phone,
        platformRole: platformRole,
      );
      _staff = [..._staff, created];
      notifyListeners();
      return true;
    } on ApiException catch (e) {
      _staffErrorMessage = e.message;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updatePlatformRole(String publicUuid, String platformRole) async {
    _staffErrorMessage = null;
    try {
      final updated = await _authRepository.updatePlatformRole(publicUuid, platformRole);
      _staff = _staff
          .map((user) => user.publicUuid == publicUuid ? updated : user)
          .toList();
      if (_currentUser?.publicUuid == publicUuid) {
        _currentUser = updated;
      }
      notifyListeners();
      return true;
    } on ApiException catch (e) {
      _staffErrorMessage = e.message;
      notifyListeners();
      return false;
    }
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}