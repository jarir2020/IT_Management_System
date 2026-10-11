import angular from 'angular';
import { featureModules } from '../registry.js';

featureModules.reports.factory('ReportModel', ['$http', $http => ({
  importAssets: file => {
    const data = new FormData();
    data.append('file', file);
    return $http.post('/api/v1/reports/assets/import', data, {
      headers: { 'Content-Type': undefined },
      transformRequest: angular.identity
    }).then(response => response.data);
  }
})]);
