import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../provider/auth_provider.dart';
import '/provider/api_key_provider.dart';
import '/provider/audit_log_provider.dart';
import '/provider/billing_provider.dart';
import '/provider/infrastructure_provider.dart';
import '/provider/organization_provider.dart';
import '/provider/payment_provider.dart';
import '/screens/audit_logs_screen.dart';
import '/screens/change_password_screen.dart';
import '/screens/coolify_instances_screen.dart';
import '/screens/edit_profile_screen.dart';
import '/screens/login_screen.dart';
import '/screens/organizations_screen.dart';
import '/screens/payments_screen.dart';
import '/screens/plans_screen.dart';
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
        // SUPPORT or above (AdminOrganizationController: hasRole('SUPPORT')).
        // Suspend / lift inside the detail screen are gated separately by
        // isAtLeastPlatformAdmin.
        if (authProvider.isAtLeastSupport)
          ListTile(
            leading: const Icon(Icons.apartment_outlined),
            title: const Text('Organizations'),
            onTap: () {
              Navigator.of(context).pop();
              onNavigate(const OrganizationsScreen());
            },
          ),
        // SUPPORT or above (PaymentController reads: hasRole('SUPPORT')).
        // Mark-paid / refund inside the invoice detail are gated separately
        // by isAtLeastPlatformAdmin.
        if (authProvider.isAtLeastSupport)
          ListTile(
            leading: const Icon(Icons.payments_outlined),
            title: const Text('Payments'),
            onTap: () {
              Navigator.of(context).pop();
              onNavigate(const PaymentsScreen());
            },
          ),
        // SUPPORT or above (AuditLogController: hasRole('SUPPORT')).
        if (authProvider.isAtLeastSupport)
          ListTile(
            leading: const Icon(Icons.history),
            title: const Text('Audit log'),
            onTap: () {
              Navigator.of(context).pop();
              onNavigate(const AuditLogsScreen());
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
          // BillingController catalog endpoints: hasRole('PLATFORM_ADMIN').
          ListTile(
            leading: const Icon(Icons.sell_outlined),
            title: const Text('Plans'),
            onTap: () {
              Navigator.of(context).pop();
              onNavigate(const PlansScreen());
            },
          ),
        ],
        // Next modules go here, each gated by the minimum platformRole
        // required by its backend controller (ORGANIZATION supervision =
        // SUPPORT, etc.). This is the only sidebar file.
        const Divider(),
        ListTile(
          leading: const Icon(Icons.person_outline),
          title: const Text('Edit profile'),
          onTap: () {
            final navigator = Navigator.of(context);
            navigator.pop(); // close the drawer
            navigator.push(
              MaterialPageRoute(builder: (_) => const EditProfileScreen()),
            );
          },
        ),
        ListTile(
          leading: const Icon(Icons.lock_outline),
          title: const Text('Change password'),
          onTap: () {
            final navigator = Navigator.of(context);
            navigator.pop(); // close the drawer
            navigator.push(
              MaterialPageRoute(builder: (_) => const ChangePasswordScreen()),
            );
          },
        ),
        ListTile(
          leading: const Icon(Icons.logout),
          title: const Text('Log out'),
          onTap: () async {
            // Drop cached admin data before the session ends.
            context.read<InfrastructureProvider>().reset();
            context.read<BillingProvider>().reset();
            context.read<OrganizationProvider>().reset();
            context.read<ApiKeyProvider>().reset();
            context.read<AuditLogProvider>().reset();
            context.read<PaymentProvider>().reset();
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