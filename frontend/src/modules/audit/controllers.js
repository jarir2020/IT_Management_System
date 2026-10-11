import { featureModules } from '../registry.js';

featureModules.audit.controller('AuditController', ['AuthService', 'AuditModel', function (AuthService, AuditModel) {
  const vm = this;
  vm.entries = [];
  vm.search = '';
  vm.loading = true;
  vm.error = '';
  vm.visibleEntries = () => {
    const query = vm.search.trim().toLowerCase();
    return vm.entries.filter(entry => !query ||
      `${entry.actorEmail || ''} ${entry.action} ${entry.entityType} ${entry.entityId || ''} ${entry.summary}`.toLowerCase().includes(query));
  };
  vm.load = () => {
    vm.loading = true;
    vm.error = '';
    AuditModel.recent(300)
      .then(entries => { vm.entries = entries; })
      .catch(error => { vm.error = error.data?.detail || error.data?.message || 'Could not load audit events.'; })
      .finally(() => { vm.loading = false; });
  };
  AuthService.me(true).then(() => vm.load());
}]);
