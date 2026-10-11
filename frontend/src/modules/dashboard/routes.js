import dashboardTemplate from './views/dashboard.html?raw';

export function registerDashboardRoutes($stateProvider) {
  $stateProvider.state('workspace.dashboard', {
    url: '/dashboard',
    template: dashboardTemplate,
    controller: 'DashboardController as vm'
  });
}
