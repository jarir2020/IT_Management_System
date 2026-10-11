import { featureModules } from '../registry.js';

featureModules.dashboard.controller('DashboardController', ['AuthService', function (AuthService) {
  const vm = this;
  vm.user = null;
  vm.features = [
    { number: '01', title: 'Identity and access', description: 'User accounts, role assignments, and protected permissions are in place.', status: 'Ready' },
    { number: '02', title: 'Departments', description: 'Organize staff by department and keep assignments current.', status: 'Ready' },
    { number: '03', title: 'Service desk', description: 'Create support tickets, add notes, and follow resolution progress.', status: 'Ready' },
    { number: '04', title: 'Assets and inventory', description: 'Equipment inventory and assignment workflows are next in the plan.', status: 'Next phase' }
  ];
  AuthService.me().then(user => { vm.user = user; });
}]);
