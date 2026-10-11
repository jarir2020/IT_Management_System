import angular from 'angular';
import { featureModules } from '../registry.js';

featureModules.assets.factory('AssetModel', ['$http', $http => {
  const root = '/api/v1/assets';
  const detail = id => `${root}/${id}`;

  return {
    list: () => $http.get(root).then(response => response.data),
    get: id => $http.get(detail(id)).then(response => response.data),
    users: () => $http.get(`${root}/users`).then(response => response.data),
    create: payload => $http.post(root, payload).then(response => response.data),
    update: (id, payload) => $http.put(detail(id), payload).then(response => response.data),
    assign: (id, payload) => $http.post(`${detail(id)}/assignments`, payload).then(response => response.data),
    returnAsset: (id, assignmentId, payload) =>
      $http.post(`${detail(id)}/assignments/${assignmentId}/return`, payload).then(response => response.data),
    startMaintenance: (id, payload) =>
      $http.post(`${detail(id)}/maintenance`, payload).then(response => response.data),
    changeMaintenance: (id, maintenanceId, status) =>
      $http.patch(`${detail(id)}/maintenance/${maintenanceId}`, { status }).then(response => response.data),
    uploadDocument: (id, type, file) => {
      const data = new FormData();
      data.append('type', type);
      data.append('file', file);
      return $http.post(`${detail(id)}/documents`, data, {
        headers: { 'Content-Type': undefined },
        transformRequest: angular.identity
      }).then(response => response.data);
    },
    deleteDocument: (id, documentId) =>
      $http.delete(`${detail(id)}/documents/${documentId}`).then(response => response.data),
    retire: id => $http.patch(`${detail(id)}/retire`).then(response => response.data),
    downloadUrl: (id, documentId) => `${detail(id)}/documents/${documentId}/download`
  };
}]);
