import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../provider/auth_provider.dart';
import '/provider/infrastructure_provider.dart';
import '/screens/coolify_instances_screen.dart';
import '/screens/login_screen.dart';
import '/screens/servers_screen.dart';
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
        // Owner only (mirrors GET/POST /api/auth/staff: hasRole('PLATFORM_OWNER')).
        if (authProvider.isPlatformOwner)
          ListTile(
            leading: const Icon(Icons.people_outline),
            title: const Text('Staff'),
            onTap: () {
              Navigator.of(context).pop(); // close the drawer
              onNavigate(const StaffScreen());
            },
          ),
        // PLATFORM_ADMIN or above (InfrastructureController: hasRole('PLATFORM_ADMIN')).
        if (authProvider.isAtLeastPlatformAdmin) ...[
          ListTile(
            leading: const Icon(Icons.cloud_outlined),
            title: const Text('Coolify instances'),
            onTap: () {
              Navigator.of(context).pop();
              onNavigate(const CoolifyInstancesScreen());
            },
          ),
          ListTile(
            leading: const Icon(Icons.dns_outlined),
            title: const Text('Servers'),
            onTap: () {
              Navigator.of(context).pop();
              onNavigate(const ServersScreen());
            },
          ),
        ],
        // Espaço reservado para as próximas etapas — cada item entra
        // aqui condicionado ao platformRole mínimo exigido pelo backend
        // correspondente (INFRASTRUCTURE/BILLING = PLATFORM_ADMIN,
        // ORGANIZATION supervisão = SUPPORT, etc.), sem criar ficheiros
        // novos de sidebar — este é o único.
        const Divider(),
        ListTile(
          leading: const Icon(Icons.logout),
         title: const Text('Log out'),
          onTap: () async {
            // Drop cached infrastructure data before the session ends.
            context.read<InfrastructureProvider>().reset();
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