import { featureModules } from '../registry.js';

featureModules.access.factory('AccessService', ['$http', $http => ({
  roles: () => $http.get('/api/v1/roles').then(response => response.data),
  permissions: () => $http.get('/api/v1/permissions').then(response => response.data)
})]);
