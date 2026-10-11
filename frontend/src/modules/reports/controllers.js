import { featureModules } from '../registry.js';

function errorMessage(error, fallback) {
  return error.data?.detail || error.data?.message || fallback;
}

featureModules.reports.controller('ReportsController', [
  'AuthService', 'ReportModel',
  function (AuthService, ReportModel) {
    const vm = this;
    vm.user = null;
    vm.selectedFile = null;
    vm.importResult = null;
    vm.importError = '';
    vm.importing = false;
    vm.can = permission => vm.user?.permissions?.includes(permission) ?? false;

    vm.selectFile = file => {
      vm.selectedFile = file;
      vm.importResult = null;
      vm.importError = '';
    };

    vm.importAssets = () => {
      if (!vm.selectedFile || vm.importing) return;
      vm.importing = true;
      vm.importError = '';
      vm.importResult = null;
      ReportModel.importAssets(vm.selectedFile)
        .then(result => { vm.importResult = result; vm.selectedFile = null; })
        .catch(error => { vm.importError = errorMessage(error, 'Could not import this workbook.'); })
        .finally(() => { vm.importing = false; });
    };

    AuthService.me(true).then(user => { vm.user = user; });
  }
]);
