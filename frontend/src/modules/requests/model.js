import { featureModules } from '../registry.js';

featureModules.requests.factory('EquipmentRequestModel', ['$http', $http => {
  const root = '/api/v1/requests';
  const detail = id => `${root}/${id}`;

  return {
    list: () => $http.get(root).then(response => response.data),
    get: id => $http.get(detail(id)).then(response => response.data),
    create: payload => $http.post(root, payload).then(response => response.data),
    approve: (id, note) => $http.post(`${detail(id)}/approve`, note ? { note } : {}).then(response => response.data),
    reject: (id, note) => $http.post(`${detail(id)}/reject`, { note }).then(response => response.data),
    sendToProcurement: (id, note) => $http.post(`${detail(id)}/procurement-needed`, { note }).then(response => response.data),
    purchase: (id, payload) => $http.post(`${detail(id)}/purchase`, payload).then(response => response.data),
    received: (id, note) => $http.post(`${detail(id)}/received`, note ? { note } : {}).then(response => response.data),
    allocate: (id, payload) => $http.post(`${detail(id)}/allocate`, payload).then(response => response.data),
    cancel: id => $http.post(`${detail(id)}/cancel`).then(response => response.data)
  };
}]);
