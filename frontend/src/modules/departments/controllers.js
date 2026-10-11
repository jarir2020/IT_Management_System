import { featureModules } from '../registry.js';

featureModules.departments.controller('DepartmentManagementController', ['DepartmentService', function (DepartmentService) {
  const vm = this;
  vm.departments = [];
  vm.loading = true;
  vm.saving = false;
  vm.error = '';
  vm.success = '';
  vm.editorOpen = false;
  vm.form = { name: '', code: '', description: '', active: true };
  vm.activeCount = () => vm.departments.filter(department => department.active).length;

  vm.load = () => {
    vm.loading = true;
    DepartmentService.list()
      .then(departments => { vm.departments = departments; })
      .catch(error => { vm.error = error.data?.detail || error.data?.message || 'Could not load departments.'; })
      .finally(() => { vm.loading = false; });
  };

  vm.newDepartment = () => {
    vm.form = { name: '', code: '', description: '', active: true };
    vm.editingId = null;
    vm.error = '';
    vm.editorOpen = true;
  };

  vm.editDepartment = department => {
    vm.form = { name: department.name, code: department.code, description: department.description || '', active: department.active };
    vm.editingId = department.id;
    vm.error = '';
    vm.editorOpen = true;
  };

  vm.saveDepartment = form => {
    if (form.$invalid || vm.saving) return;
    vm.saving = true;
    vm.error = '';
    const save = vm.editingId
      ? DepartmentService.update(vm.editingId, vm.form)
      : DepartmentService.create(vm.form);
    save.then(() => {
      vm.editorOpen = false;
      vm.success = vm.editingId ? 'Department changes saved.' : 'Department created.';
      vm.load();
    }).catch(error => {
      vm.error = error.data?.detail || error.data?.message || 'Could not save this department.';
    }).finally(() => { vm.saving = false; });
  };

  vm.deactivate = department => {
    vm.error = '';
    DepartmentService.deactivate(department.id).then(() => {
      vm.success = `${department.name} was deactivated.`;
      vm.load();
    }).catch(error => {
      vm.error = error.data?.detail || error.data?.message || 'Could not deactivate this department.';
    });
  };

  vm.load();
}]);
