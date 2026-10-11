import { featureModules } from '../registry.js';

featureModules.workspace.controller('WorkspaceController', ['$state', 'AuthService', function ($state, AuthService) {
  const vm = this;
  vm.user = null;
  vm.state = $state;
  vm.can = permission => vm.user?.permissions?.includes(permission) ?? false;
  vm.initials = user => `${user?.firstName?.[0] || ''}${user?.lastName?.[0] || ''}`.toUpperCase();

  AuthService.me().then(user => { vm.user = user; });
  vm.logout = () => AuthService.logout().finally(() => $state.go('login'));
}]);
