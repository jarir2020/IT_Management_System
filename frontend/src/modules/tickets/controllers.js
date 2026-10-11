import { featureModules } from '../registry.js';
import { Client } from '@stomp/stompjs';

const categories = [
  { value: 'HARDWARE', label: 'Hardware' },
  { value: 'SOFTWARE', label: 'Software' },
  { value: 'ACCESS', label: 'Account or access' },
  { value: 'NETWORK', label: 'Network' },
  { value: 'OTHER', label: 'Other' }
];

const priorities = [
  { value: 'LOW', label: 'Low' },
  { value: 'NORMAL', label: 'Normal' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' }
];

function errorMessage(error, fallback) {
  return error.data?.detail || error.data?.message || fallback;
}

featureModules.tickets.controller('TicketListController', ['TicketModel', function (TicketModel) {
  const vm = this;
  vm.tickets = [];
  vm.search = '';
  vm.statusFilter = '';
  vm.loading = true;
  vm.error = '';

  vm.load = () => {
    vm.loading = true;
    vm.error = '';
    TicketModel.list()
      .then(tickets => { vm.tickets = tickets; })
      .catch(error => { vm.error = errorMessage(error, 'Could not load tickets.'); })
      .finally(() => { vm.loading = false; });
  };

  vm.filteredTickets = () => {
    const query = vm.search.trim().toLowerCase();
    return vm.tickets.filter(ticket => {
      const matchesStatus = !vm.statusFilter || ticket.status === vm.statusFilter;
      const searchable = `${ticket.title} ${ticket.id} ${ticket.requester.firstName} ${ticket.requester.lastName} ${ticket.assignee?.firstName || ''} ${ticket.assignee?.lastName || ''}`.toLowerCase();
      return matchesStatus && (!query || searchable.includes(query));
    });
  };
  vm.label = value => value?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
  vm.load();
}]);

featureModules.tickets.controller('TicketCreateController', ['$state', 'TicketModel', function ($state, TicketModel) {
  const vm = this;
  vm.categories = categories;
  vm.priorities = priorities;
  vm.form = { title: '', description: '', category: 'HARDWARE', priority: 'NORMAL' };
  vm.error = '';
  vm.saving = false;

  vm.submit = form => {
    if (form.$invalid || vm.saving) return;
    vm.saving = true;
    vm.error = '';
    TicketModel.create(vm.form)
      .then(ticket => $state.go('workspace.ticketDetail', { ticketId: ticket.id }))
      .catch(error => { vm.error = errorMessage(error, 'Could not create this ticket.'); })
      .finally(() => { vm.saving = false; });
  };
}]);

featureModules.tickets.controller('TicketDetailController', [
  '$state', '$stateParams', '$window', '$rootScope', '$scope', 'AuthService', 'TicketModel',
  function ($state, $stateParams, $window, $rootScope, $scope, AuthService, TicketModel) {
    const vm = this;
    vm.ticket = null;
    vm.user = null;
    vm.agents = [];
    vm.categories = categories;
    vm.priorities = priorities;
    vm.loading = true;
    vm.saving = false;
    vm.assigning = false;
    vm.commenting = false;
    vm.uploading = false;
    vm.error = '';
    vm.editing = false;
    vm.comment = { message: '', internalNote: false };
    vm.chatMessages = [];
    vm.chatDraft = '';
    vm.chatConnected = false;
    vm.chatError = '';
    vm.selectedFile = null;
    let chatClient = null;

    vm.can = permission => vm.user?.permissions?.includes(permission) ?? false;
    vm.isRequester = () => vm.ticket?.requester?.id === vm.user?.id;
    vm.isAdministrator = () => vm.can('tickets:delete');
    vm.canManage = () => vm.isAdministrator() || vm.ticket?.assignee?.id === vm.user?.id;
    vm.canEdit = () => vm.ticket && vm.ticket.status !== 'CLOSED'
      && ((vm.isRequester() && vm.ticket.status === 'OPEN')
        || (vm.can('tickets:update') && vm.canManage()));
    vm.canClose = () => vm.ticket?.status === 'RESOLVED'
      && (vm.isRequester() || (vm.can('tickets:status') && vm.canManage()));
    vm.label = value => value?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());

    const connectChat = ticketId => {
      const protocol = $window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      chatClient = new Client({
        brokerURL: `${protocol}//${$window.location.host}/ws`,
        reconnectDelay: 4000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          $rootScope.$applyAsync(() => { vm.chatConnected = true; vm.chatError = ''; });
          chatClient.subscribe(`/topic/tickets/${ticketId}/chat`, frame => {
            const message = JSON.parse(frame.body);
            $rootScope.$applyAsync(() => {
              if (!vm.chatMessages.some(existing => existing.id === message.id)) vm.chatMessages.push(message);
            });
          });
        },
        onWebSocketClose: () => $rootScope.$applyAsync(() => { vm.chatConnected = false; }),
        onStompError: frame => $rootScope.$applyAsync(() => {
          vm.chatConnected = false;
          vm.chatError = frame.headers.message || 'Ticket chat could not connect.';
        })
      });
      chatClient.activate();
    };

    vm.sendChat = form => {
      if (!vm.chatDraft.trim() || !vm.chatConnected || !chatClient?.connected || vm.ticket.status === 'CLOSED') return;
      chatClient.publish({
        destination: `/app/tickets/${vm.ticket.id}/chat`,
        body: JSON.stringify({ message: vm.chatDraft.trim() })
      });
      vm.chatDraft = '';
      form.$setPristine();
      form.$setUntouched();
    };

    const copyTicketToForm = () => {
      vm.editForm = {
        title: vm.ticket.title,
        description: vm.ticket.description,
        category: vm.ticket.category,
        priority: vm.ticket.priority
      };
      vm.selectedAssigneeId = vm.ticket.assignee?.id || null;
    };

    vm.load = () => {
      vm.loading = true;
      vm.error = '';
      return AuthService.me(true)
        .then(user => {
          vm.user = user;
          return TicketModel.get($stateParams.ticketId);
        })
        .then(ticket => {
          vm.ticket = ticket;
          copyTicketToForm();
          const supporting = vm.can('tickets:assign')
            ? TicketModel.agents().then(agents => { vm.agents = agents; }) : Promise.resolve();
          return Promise.all([
            supporting,
            TicketModel.chatMessages(ticket.id)
              .then(messages => { vm.chatMessages = messages; })
              .catch(() => { vm.chatError = 'Could not load earlier chat messages.'; })
          ]).then(() => connectChat(ticket.id));
        })
        .catch(error => { vm.error = errorMessage(error, 'Could not load this ticket.'); })
        .finally(() => { vm.loading = false; });
    };

    vm.saveDetails = form => {
      if (form.$invalid || vm.saving) return;
      vm.saving = true;
      vm.error = '';
      TicketModel.update(vm.ticket.id, vm.editForm)
        .then(ticket => { vm.ticket = ticket; copyTicketToForm(); vm.editing = false; })
        .catch(error => { vm.error = errorMessage(error, 'Could not save ticket details.'); })
        .finally(() => { vm.saving = false; });
    };

    vm.assign = () => {
      if (vm.assigning) return;
      vm.assigning = true;
      vm.error = '';
      TicketModel.assign(vm.ticket.id, { userId: vm.selectedAssigneeId || null })
        .then(ticket => { vm.ticket = ticket; copyTicketToForm(); })
        .catch(error => { vm.error = errorMessage(error, 'Could not assign this ticket.'); })
        .finally(() => { vm.assigning = false; });
    };

    vm.changeStatus = status => {
      vm.error = '';
      TicketModel.setStatus(vm.ticket.id, status)
        .then(ticket => { vm.ticket = ticket; copyTicketToForm(); })
        .catch(error => { vm.error = errorMessage(error, 'Could not update the ticket status.'); });
    };

    vm.addComment = form => {
      if (form.$invalid || vm.commenting) return;
      vm.commenting = true;
      vm.error = '';
      TicketModel.comment(vm.ticket.id, vm.comment)
        .then(ticket => {
          vm.ticket = ticket;
          vm.comment = { message: '', internalNote: false };
          form.$setPristine();
          form.$setUntouched();
        })
        .catch(error => { vm.error = errorMessage(error, 'Could not add this comment.'); })
        .finally(() => { vm.commenting = false; });
    };

    vm.uploadAttachment = () => {
      if (!vm.selectedFile || vm.uploading) return;
      vm.uploading = true;
      vm.error = '';
      TicketModel.upload(vm.ticket.id, vm.selectedFile)
        .then(ticket => { vm.ticket = ticket; vm.selectedFile = null; })
        .catch(error => { vm.error = errorMessage(error, 'Could not upload this file.'); })
        .finally(() => { vm.uploading = false; });
    };

    vm.downloadAttachment = attachment => {
      $window.location.href = TicketModel.downloadUrl(vm.ticket.id, attachment.id);
    };

    vm.deleteAttachment = attachment => {
      vm.error = '';
      TicketModel.deleteAttachment(vm.ticket.id, attachment.id)
        .then(ticket => { vm.ticket = ticket; })
        .catch(error => { vm.error = errorMessage(error, 'Could not remove this attachment.'); });
    };

    vm.deleteTicket = () => {
      if (!vm.isAdministrator() || !$window.confirm('Delete this ticket and its attachments?')) return;
      TicketModel.delete(vm.ticket.id)
        .then(() => $state.go('workspace.tickets'))
        .catch(error => { vm.error = errorMessage(error, 'Could not delete this ticket.'); });
    };

    vm.load();
    $scope.$on('$destroy', () => { if (chatClient) chatClient.deactivate(); });
  }
]);
