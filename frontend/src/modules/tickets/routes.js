import ticketListTemplate from './views/list.html?raw';
import ticketNewTemplate from './views/new.html?raw';
import ticketDetailTemplate from './views/detail.html?raw';

export function registerTicketRoutes($stateProvider) {
  $stateProvider
    .state('workspace.tickets', {
      url: '/tickets',
      data: { permission: 'tickets:read' },
      template: ticketListTemplate,
      controller: 'TicketListController as vm'
    })
    .state('workspace.newTicket', {
      url: '/tickets/new',
      data: { permission: 'tickets:create' },
      template: ticketNewTemplate,
      controller: 'TicketCreateController as vm'
    })
    .state('workspace.ticketDetail', {
      url: '/tickets/{ticketId:int}',
      data: { permission: 'tickets:read' },
      template: ticketDetailTemplate,
      controller: 'TicketDetailController as vm'
    });
}
