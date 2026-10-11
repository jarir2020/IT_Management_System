import { featureModules } from '../registry.js';

featureModules.audit.factory('AuditModel', ['$http', $http => ({
  recent: limit => $http.get('/api/v1/audit', { params: { limit } }).then(response => response.data)
})]);
