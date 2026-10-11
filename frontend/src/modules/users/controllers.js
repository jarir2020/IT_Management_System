import { featureModules } from '../registry.js';

featureModules.users.controller('UserManagementController', [
  '$q', 'UserService', 'DepartmentService', 'AccessService',
  function ($q, UserService, DepartmentService, AccessService) {
    const vm = this;
    vm.users = [];
    vm.departments = [];
    vm.roles = [];
    vm.search = '';
    vm.loading = true;
    vm.saving = false;
    vm.error = '';
    vm.success = '';
    vm.editorOpen = false;

    const blankForm = () => ({
      firstName: '', lastName: '', email: '', password: '',
      departmentId: null, roleIds: [], enabled: true
    });
    vm.form = blankForm();

    vm.load = () => {
      vm.loading = true;
      $q.all([UserService.list(), AccessService.roles(), DepartmentService.list()])
        .then(([users, roles, departments]) => {
          vm.users = users;
          vm.roles = roles;
          vm.departments = departments;
        })
        .catch(error => { vm.error = error.data?.detail || error.data?.message || 'Could not load the user directory.'; })
        .finally(() => { vm.loading = false; });
    };

    vm.filteredUsers = () => {
      const query = vm.search.trim().toLowerCase();
      if (!query) return vm.users;
      return vm.users.filter(user => `${user.firstName} ${user.lastName} ${user.email} ${user.department?.name || ''}`.toLowerCase().includes(query));
    };
    vm.activeCount = () => vm.users.filter(user => user.enabled).length;

    vm.newUser = () => {
      vm.form = blankForm();
      vm.editingId = null;
      vm.error = '';
      vm.editorOpen = true;
    };

    vm.editUser = user => {
      vm.form = {
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        password: '',
        departmentId: user.department?.id || null,
        roleIds: user.roles.map(role => role.id),
        enabled: user.enabled
      };
      vm.editingId = user.id;
      vm.error = '';
      vm.editorOpen = true;
    };

    vm.toggleRole = roleId => {
      const index = vm.form.roleIds.indexOf(roleId);
      if (index < 0) vm.form.roleIds.push(roleId);
      else vm.form.roleIds.splice(index, 1);
    };

    vm.saveUser = form => {
      if (form.$invalid || vm.saving || vm.form.roleIds.length === 0) return;
      vm.saving = true;
      vm.error = '';
      const payload = { ...vm.form, departmentId: vm.form.departmentId || null };
      const save = vm.editingId
        ? UserService.update(vm.editingId, payload)
        : UserService.create(payload);
      save.then(() => {
        vm.editorOpen = false;
        vm.success = vm.editingId ? 'Account changes saved.' : 'New account created.';
        vm.load();
      }).catch(error => {
        vm.error = error.data?.detail || error.data?.message || 'Could not save this account.';
      }).finally(() => { vm.saving = false; });
    };

    vm.toggleStatus = user => {
      vm.error = '';
      vm.success = '';
      UserService.setEnabled(user.id, !user.enabled).then(() => {
        vm.success = user.enabled ? `${user.firstName}'s account was deactivated.` : `${user.firstName}'s account was reactivated.`;
        vm.load();
      }).catch(error => {
        vm.error = error.data?.detail || error.data?.message || 'Could not update this account.';
      });
    };

    vm.load();
  }
]);
