import reportsTemplate from './views/index.html?raw';

export function registerReportRoutes($stateProvider) {
  $stateProvider.state('workspace.reports', {
    url: '/reports',
    data: { permission: 'reports:read' },
    template: reportsTemplate,
    controller: 'ReportsController as vm'
  });
}
