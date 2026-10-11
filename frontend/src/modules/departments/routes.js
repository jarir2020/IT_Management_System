import departmentsTemplate from './views/departments.html?raw';

export function registerDepartmentRoutes($stateProvider) {
  $stateProvider.state('workspace.departments', {
    url: '/departments',
    data: { permission: 'departments:read' },
    template: departmentsTemplate,
    controller: 'DepartmentManagementController as vm'
  });
}
