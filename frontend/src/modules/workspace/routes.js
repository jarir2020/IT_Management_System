import workspaceTemplate from './views/workspace.html?raw';

export function registerWorkspaceRoutes($stateProvider) {
  $stateProvider.state('workspace', {
    abstract: true,
    template: workspaceTemplate,
    controller: 'WorkspaceController as vm'
  });
}
