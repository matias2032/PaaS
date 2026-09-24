import 'package:flutter/material.dart';
import '/widget/app_sidebar.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
Widget _body = const Center(child: Text('Welcome to the admin panel.'));

  void _navigate(Widget screen) {
    setState(() => _body = screen);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
 appBar: AppBar(title: const Text('Admin Panel')),
      drawer: AppSidebar(currentRoute: '', onNavigate: _navigate),
      body: _body,
    );
  }
}