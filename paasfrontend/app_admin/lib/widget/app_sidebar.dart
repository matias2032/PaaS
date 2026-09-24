import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../provider/auth_provider.dart';
import '/screens/login_screen.dart';
import '/screens/staff_screen.dart';

class AppSidebar extends StatelessWidget {
  final String currentRoute;
  final ValueChanged<Widget> onNavigate;

  const AppSidebar({
    super.key,
    required this.currentRoute,
    required this.onNavigate,
  });

  @override
  Widget build(BuildContext context) {
    final authProvider = context.watch<AuthProvider>();
    final user = authProvider.currentUser;

    return NavigationDrawer(
      children: [
        DrawerHeader(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Text(user?.firstName ?? '', style: Theme.of(context).textTheme.titleMedium),
              Text(user?.platformRole ?? '', style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
        // SUPPORT+ pode ver a lista de staff (mesma regra do backend:
        // GET /api/auth/staff exige hasRole('SUPPORT')).
        if (authProvider.isSupport ||
            authProvider.isPlatformAdmin ||
            authProvider.isPlatformOwner)
          ListTile(
            leading: const Icon(Icons.people_outline),
            title: const Text('Staff'),
            onTap: () => onNavigate(const StaffScreen()),
          ),
        // Espaço reservado para as próximas etapas — cada item entra
        // aqui condicionado ao platformRole mínimo exigido pelo backend
        // correspondente (INFRASTRUCTURE/BILLING = PLATFORM_ADMIN,
        // ORGANIZATION supervisão = SUPPORT, etc.), sem criar ficheiros
        // novos de sidebar — este é o único.
        const Divider(),
        ListTile(
          leading: const Icon(Icons.logout),
          title: const Text('Sair'),
          onTap: () async {
            await authProvider.logout();
            if (context.mounted) {
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
                (route) => false,
              );
            }
          },
        ),
      ],
    );
  }
}