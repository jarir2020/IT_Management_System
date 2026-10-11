import auditTemplate from './views/index.html?raw';

export function registerAuditRoutes($stateProvider) {
  $stateProvider.state('workspace.audit', {
    url: '/audit',
    data: { permission: 'audit:read' },
    template: auditTemplate,
    controller: 'AuditController as vm'
  });
}
