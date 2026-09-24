class AuthResponse {
  final String publicUuid;
  final String firstName;
  final String? lastName;
  final String email;
  final String status;
  final String platformRole;
  final bool firstPassword;
  final DateTime? emailVerifiedAt;
  final DateTime createdAt;
  final String? token;

  AuthResponse({
    required this.publicUuid,
    required this.firstName,
    this.lastName,
    required this.email,
    required this.status,
    required this.platformRole,
    required this.firstPassword,
    this.emailVerifiedAt,
    required this.createdAt,
    this.token,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      publicUuid: json['publicUuid'] as String,
      firstName: json['firstName'] as String,
      lastName: json['lastName'] as String?,
      email: json['email'] as String,
      status: json['status'] as String,
      platformRole: json['platformRole'] as String,
      firstPassword: json['firstPassword'] as bool? ?? false,
      emailVerifiedAt: json['emailVerifiedAt'] != null
          ? DateTime.parse(json['emailVerifiedAt'] as String)
          : null,
      createdAt: DateTime.parse(json['createdAt'] as String),
      token: json['token'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'publicUuid': publicUuid,
      'firstName': firstName,
      if (lastName != null) 'lastName': lastName,
      'email': email,
      'status': status,
      'platformRole': platformRole,
      'firstPassword': firstPassword,
      if (emailVerifiedAt != null)
        'emailVerifiedAt': emailVerifiedAt!.toIso8601String(),
      'createdAt': createdAt.toIso8601String(),
      if (token != null) 'token': token,
    };
  }

  bool get isCustomer => platformRole == 'CUSTOMER';
  bool get isSupport => platformRole == 'SUPPORT';
  bool get isPlatformAdmin => platformRole == 'PLATFORM_ADMIN';
  bool get isPlatformOwner => platformRole == 'PLATFORM_OWNER';
    bool get isActive => status == 'ACTIVE';

  // Regra do painel admin: só staff (SUPPORT/PLATFORM_ADMIN/PLATFORM_OWNER)
  // pode entrar; CUSTOMER é recusado logo no ecrã de login.
  bool get isStaff => !isCustomer;

  AuthResponse copyWith({
    String? publicUuid,
    String? firstName,
    String? lastName,
    String? email,
    String? status,
    String? platformRole,
    bool? firstPassword,
    DateTime? emailVerifiedAt,
    DateTime? createdAt,
    String? token,
  }) {
    return AuthResponse(
      publicUuid: publicUuid ?? this.publicUuid,
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      email: email ?? this.email,
      status: status ?? this.status,
      platformRole: platformRole ?? this.platformRole,
      firstPassword: firstPassword ?? this.firstPassword,
      emailVerifiedAt: emailVerifiedAt ?? this.emailVerifiedAt,
      createdAt: createdAt ?? this.createdAt,
      token: token ?? this.token,
    );
  }
}