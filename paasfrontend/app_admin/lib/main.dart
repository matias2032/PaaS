import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'provider/auth_provider.dart';
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
      ],
      child: MaterialApp(
        title: 'Painel Administrativo',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
          useMaterial3: true,
        ),
        home: const LoginScreen(),
      ),
    );
  }
}