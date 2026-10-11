import requestListTemplate from './views/list.html?raw';
import requestNewTemplate from './views/new.html?raw';
import requestDetailTemplate from './views/detail.html?raw';

export function registerEquipmentRequestRoutes($stateProvider) {
  $stateProvider
    .state('workspace.requests', {
      url: '/requests',
      data: { permission: 'requests:read_own' },
      template: requestListTemplate,
      controller: 'EquipmentRequestListController as vm'
    })
    .state('workspace.newRequest', {
      url: '/requests/new',
      data: { permission: 'requests:create' },
      template: requestNewTemplate,
      controller: 'EquipmentRequestCreateController as vm'
    })
    .state('workspace.requestDetail', {
      url: '/requests/{requestId:int}',
      data: { permission: 'requests:read_own' },
      template: requestDetailTemplate,
      controller: 'EquipmentRequestDetailController as vm'
    });
}
