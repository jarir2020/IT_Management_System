import rolesTemplate from './views/roles.html?raw';

export function registerAccessRoutes($stateProvider) {
  $stateProvider.state('workspace.roles', {
    url: '/roles',
    data: { permission: 'roles:read' },
    template: rolesTemplate,
    controller: 'RoleManagementController as vm'
  });
}
