import { featureModules } from '../registry.js';

featureModules.users.factory('UserService', ['$http', $http => ({
  list: () => $http.get('/api/v1/users').then(response => response.data),
  create: payload => $http.post('/api/v1/users', payload).then(response => response.data),
  update: (id, payload) => $http.put(`/api/v1/users/${id}`, payload).then(response => response.data),
  setEnabled: (id, enabled) => $http.patch(`/api/v1/users/${id}/status`, { enabled }).then(response => response.data)
})]);
