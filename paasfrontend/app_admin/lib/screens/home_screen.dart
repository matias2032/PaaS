import 'package:flutter/material.dart';
import '/widget/app_sidebar.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  Widget _body = const Center(child: Text('Bem-vindo ao painel administrativo.'));

  void _navigate(Widget screen) {
    setState(() => _body = screen);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Painel Administrativo')),
      drawer: AppSidebar(currentRoute: '', onNavigate: _navigate),
      body: _body,
    );
  }
}