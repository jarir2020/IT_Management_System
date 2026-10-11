import angular from 'angular';
import { featureModules } from '../registry.js';

featureModules.tickets.factory('TicketModel', ['$http', $http => {
  const root = '/api/v1/tickets';

  return {
    list: () => $http.get(root).then(response => response.data),
    get: id => $http.get(`${root}/${id}`).then(response => response.data),
    create: payload => $http.post(root, payload).then(response => response.data),
    update: (id, payload) => $http.put(`${root}/${id}`, payload).then(response => response.data),
    agents: () => $http.get(`${root}/agents`).then(response => response.data),
    assign: (id, payload) => $http.patch(`${root}/${id}/assignee`, payload).then(response => response.data),
    setStatus: (id, status) => $http.patch(`${root}/${id}/status`, { status }).then(response => response.data),
    comment: (id, payload) => $http.post(`${root}/${id}/comments`, payload).then(response => response.data),
    upload: (id, file) => {
      const data = new FormData();
      data.append('file', file);
      return $http.post(`${root}/${id}/attachments`, data, {
        headers: { 'Content-Type': undefined },
        transformRequest: angular.identity
      }).then(response => response.data);
    },
    deleteAttachment: (ticketId, attachmentId) =>
      $http.delete(`${root}/${ticketId}/attachments/${attachmentId}`).then(response => response.data),
    delete: id => $http.delete(`${root}/${id}`),
    downloadUrl: (ticketId, attachmentId) => `${root}/${ticketId}/attachments/${attachmentId}/download`
  };
}]);
