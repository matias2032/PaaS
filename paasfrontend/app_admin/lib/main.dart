import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'provider/api_key_provider.dart';
import 'provider/audit_log_provider.dart';
import 'provider/auth_provider.dart';
import 'provider/billing_provider.dart';
import 'provider/infrastructure_provider.dart';
import 'provider/organization_provider.dart';
import 'screens/login_screen.dart';

void main() {
  runApp(const AppAdmin());
}

class AppAdmin extends StatelessWidget {
  const AppAdmin({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => InfrastructureProvider()),
        ChangeNotifierProvider(create: (_) => BillingProvider()),
        ChangeNotifierProvider(create: (_) => OrganizationProvider()),
        ChangeNotifierProvider(create: (_) => ApiKeyProvider()),
        ChangeNotifierProvider(create: (_) => AuditLogProvider()),
      ],
      child: MaterialApp(
title: 'Admin Panel',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
          useMaterial3: true,
        ),
        home: const LoginScreen(),
      ),
    );
  }
}