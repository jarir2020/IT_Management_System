import { featureModules } from '../registry.js';

featureModules.auth.controller('LoginController', ['$state', 'AuthService', function ($state, AuthService) {
  const vm = this;
  vm.credentials = { email: '', password: '' };
  vm.error = '';
  vm.busy = false;

  vm.submit = () => {
    if (vm.busy) return;
    vm.busy = true;
    vm.error = '';
    AuthService.login(vm.credentials)
      .then(() => $state.go('workspace.dashboard'))
      .catch(error => {
        vm.error = error.data?.detail || error.data?.message || 'We could not sign you in. Check your email and password.';
      })
      .finally(() => { vm.busy = false; });
  };
}]);
