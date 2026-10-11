import { featureModules } from '../registry.js';

const categories = [
  { value: 'LAPTOP', label: 'Laptop' },
  { value: 'DESKTOP', label: 'Desktop' },
  { value: 'MONITOR', label: 'Monitor' },
  { value: 'PRINTER', label: 'Printer' },
  { value: 'SOFTWARE_LICENSE', label: 'Software license' },
  { value: 'OTHER', label: 'Other' }
];

function errorMessage(error, fallback) {
  return error.data?.detail || error.data?.message || fallback;
}

function dateInput(value) {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(`${value}T00:00:00`);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function dateAfter(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

featureModules.requests.controller('EquipmentRequestListController', [
  'AuthService', 'EquipmentRequestModel',
  function (AuthService, EquipmentRequestModel) {
    const vm = this;
    vm.requests = [];
    vm.search = '';
    vm.statusFilter = '';
    vm.loading = true;
    vm.error = '';
    vm.can = permission => vm.user?.permissions?.includes(permission) ?? false;
    vm.label = value => value?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());

    vm.load = () => {
      vm.loading = true;
      vm.error = '';
      AuthService.me(true)
        .then(user => { vm.user = user; return EquipmentRequestModel.list(); })
        .then(requests => { vm.requests = requests; })
        .catch(error => { vm.error = errorMessage(error, 'Could not load equipment requests.'); })
        .finally(() => { vm.loading = false; });
    };

    vm.filteredRequests = () => {
      const query = vm.search.trim().toLowerCase();
      return vm.requests.filter(request => {
        const matchesStatus = !vm.statusFilter || request.status === vm.statusFilter;
        const searchable = `${request.itemName} ${request.category} ${request.requester.firstName} ${request.requester.lastName} ${request.departmentName || ''}`.toLowerCase();
        return matchesStatus && (!query || searchable.includes(query));
      });
    };
    vm.load();
  }
]);

featureModules.requests.controller('EquipmentRequestCreateController', [
  '$state', 'EquipmentRequestModel',
  function ($state, EquipmentRequestModel) {
    const vm = this;
    vm.categories = categories;
    vm.form = { category: 'LAPTOP', itemName: '', justification: '' };
    vm.saving = false;
    vm.error = '';

    vm.submit = form => {
      if (form.$invalid || vm.saving) return;
      vm.saving = true;
      vm.error = '';
      EquipmentRequestModel.create(vm.form)
        .then(request => $state.go('workspace.requestDetail', { requestId: request.id }))
        .catch(error => { vm.error = errorMessage(error, 'Could not submit this equipment request.'); })
        .finally(() => { vm.saving = false; });
    };
  }
]);

featureModules.requests.controller('EquipmentRequestDetailController', [
  '$state', '$stateParams', 'AuthService', 'EquipmentRequestModel', 'AssetModel',
  function ($state, $stateParams, AuthService, EquipmentRequestModel, AssetModel) {
    const vm = this;
    vm.request = null;
    vm.user = null;
    vm.assets = [];
    vm.selectedAssetId = null;
    vm.decisionNote = '';
    vm.stockReason = '';
    vm.procurementNote = '';
    vm.allocationNote = '';
    vm.purchase = { vendor: '', purchaseReference: '', cost: null, expectedDeliveryDate: dateAfter(14), notes: '' };
    vm.loading = true;
    vm.error = '';
    vm.working = false;
    vm.can = permission => vm.user?.permissions?.includes(permission) ?? false;
    vm.label = value => value?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
    vm.isOwner = () => vm.request?.requester?.id === vm.user?.id;
    vm.canReview = () => (vm.can('requests:approve_department') || vm.can('requests:approve_all'))
      && vm.request?.status === 'PENDING_APPROVAL' && !vm.isOwner();
    vm.canAllocate = () => vm.can('requests:allocate')
      && ['APPROVED', 'READY_FOR_ALLOCATION'].includes(vm.request?.status);

    vm.load = () => {
      vm.loading = true;
      vm.error = '';
      return AuthService.me(true)
        .then(user => {
          vm.user = user;
          return EquipmentRequestModel.get($stateParams.requestId);
        })
        .then(request => {
          vm.request = request;
          if (vm.canAllocate()) {
            return AssetModel.list().then(assets => {
              vm.assets = assets.filter(asset => asset.status === 'AVAILABLE' && asset.category === request.category);
            });
          }
          vm.assets = [];
          return null;
        })
        .catch(error => { vm.error = errorMessage(error, 'Could not load this equipment request.'); })
        .finally(() => { vm.loading = false; });
    };

    const runAction = (action, fallback) => {
      if (vm.working) return;
      vm.working = true;
      vm.error = '';
      action()
        .then(request => { vm.request = request; vm.decisionNote = ''; vm.stockReason = ''; vm.procurementNote = ''; return vm.load(); })
        .catch(error => { vm.error = errorMessage(error, fallback); })
        .finally(() => { vm.working = false; });
    };

    vm.approve = () => runAction(() => EquipmentRequestModel.approve(vm.request.id, vm.decisionNote), 'Could not approve this request.');
    vm.reject = form => {
      if (form.$invalid) return;
      runAction(() => EquipmentRequestModel.reject(vm.request.id, vm.decisionNote), 'Could not reject this request.');
    };
    vm.sendToProcurement = form => {
      if (form.$invalid) return;
      runAction(() => EquipmentRequestModel.sendToProcurement(vm.request.id, vm.stockReason), 'Could not send this request to procurement.');
    };
    vm.recordPurchase = form => {
      if (form.$invalid) return;
      const payload = { ...vm.purchase, expectedDeliveryDate: dateInput(vm.purchase.expectedDeliveryDate) };
      runAction(() => EquipmentRequestModel.purchase(vm.request.id, payload), 'Could not record the purchase.');
    };
    vm.markReceived = () => runAction(() => EquipmentRequestModel.received(vm.request.id, vm.procurementNote), 'Could not mark this purchase as received.');
    vm.allocate = () => {
      if (!vm.selectedAssetId) return;
      runAction(() => EquipmentRequestModel.allocate(vm.request.id, { assetId: vm.selectedAssetId, note: vm.allocationNote }), 'Could not allocate this asset.');
    };
    vm.cancel = () => runAction(() => EquipmentRequestModel.cancel(vm.request.id), 'Could not cancel this request.');
    vm.load();
  }
]);
