import { featureModules } from '../registry.js';

featureModules.access.controller('RoleManagementController', ['AccessService', function (AccessService) {
  const vm = this;
  vm.roles = [];
  vm.loading = true;
  vm.error = '';
  AccessService.roles()
    .then(roles => { vm.roles = roles; })
    .catch(error => { vm.error = error.data?.detail || error.data?.message || 'Could not load roles.'; })
    .finally(() => { vm.loading = false; });
}]);
