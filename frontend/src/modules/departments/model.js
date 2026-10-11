import { featureModules } from '../registry.js';

featureModules.departments.factory('DepartmentService', ['$http', $http => ({
  list: () => $http.get('/api/v1/departments').then(response => response.data),
  create: payload => $http.post('/api/v1/departments', payload).then(response => response.data),
  update: (id, payload) => $http.put(`/api/v1/departments/${id}`, payload).then(response => response.data),
  deactivate: id => $http.delete(`/api/v1/departments/${id}`).then(response => response.data)
})]);
