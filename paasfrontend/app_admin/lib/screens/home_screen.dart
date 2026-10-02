import 'package:flutter/material.dart';
import '/widget/app_sidebar.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
Widget _body = const Center(child: Text('Welcome to the admin panel.'));

  // A fresh key per navigation forces a brand-new State even when the
  // target is the same screen type as the current one (clicking the
  // active sidebar item), so every screen starts from its default view.
  // The key is created here, not in build(), so ordinary rebuilds of
  // HomeScreen keep the same State.
  void _navigate(Widget screen) {
    setState(() => _body = KeyedSubtree(key: UniqueKey(), child: screen));
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