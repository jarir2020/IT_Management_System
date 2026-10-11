import usersTemplate from './views/users.html?raw';

export function registerUserRoutes($stateProvider) {
  $stateProvider.state('workspace.users', {
    url: '/users',
    data: { permission: 'users:read' },
    template: usersTemplate,
    controller: 'UserManagementController as vm'
  });
}
