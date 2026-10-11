import { featureModules } from '../registry.js';

featureModules.dashboard.controller('DashboardController', ['AuthService', function (AuthService) {
  const vm = this;
  vm.user = null;
  vm.features = [
    { number: '01', title: 'Identity and access', description: 'User accounts, role assignments, and protected permissions are in place.', status: 'Ready' },
    { number: '02', title: 'Departments', description: 'Organize staff by department and keep assignments current.', status: 'Ready' },
    { number: '03', title: 'Service desk', description: 'Create support tickets, add notes, and follow resolution progress.', status: 'Ready' },
    { number: '04', title: 'Assets and requests', description: 'Inventory, equipment approvals, procurement, and allocation are available.', status: 'Ready' },
    { number: '05', title: 'Reports and data', description: 'Export PDF/Excel reports and import asset workbooks with row feedback.', status: 'Ready' }
  ];
  AuthService.me().then(user => { vm.user = user; });
}]);
