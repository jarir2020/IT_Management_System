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

function localDate(value) {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function apiDate(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : localDate(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function todayInputDate() {
  const today = new Date();
  return new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

function assetPayload(form) {
  return { ...form, purchaseDate: apiDate(form.purchaseDate), warrantyExpiresOn: apiDate(form.warrantyExpiresOn) };
}

featureModules.assets.controller('AssetListController', ['AuthService', 'AssetModel', function (AuthService, AssetModel) {
  const vm = this;
  vm.assets = [];
  vm.search = '';
  vm.statusFilter = '';
  vm.loading = true;
  vm.error = '';
  vm.can = permission => vm.user?.permissions?.includes(permission) ?? false;

  vm.load = () => {
    vm.loading = true;
    vm.error = '';
    AuthService.me(true)
      .then(user => { vm.user = user; return AssetModel.list(); })
      .then(assets => { vm.assets = assets; })
      .catch(error => { vm.error = errorMessage(error, 'Could not load assets.'); })
      .finally(() => { vm.loading = false; });
  };

  vm.label = value => value?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
  vm.filteredAssets = () => {
    const query = vm.search.trim().toLowerCase();
    return vm.assets.filter(asset => {
      const matchesStatus = !vm.statusFilter || asset.status === vm.statusFilter;
      const searchable = `${asset.assetTag} ${asset.name} ${asset.category} ${asset.assignedTo?.firstName || ''} ${asset.assignedTo?.lastName || ''}`.toLowerCase();
      return matchesStatus && (!query || searchable.includes(query));
    });
  };
  vm.load();
}]);

featureModules.assets.controller('AssetCreateController', ['$state', 'AssetModel', function ($state, AssetModel) {
  const vm = this;
  vm.categories = categories;
  vm.form = { assetTag: '', name: '', category: 'LAPTOP', purchaseDate: null, purchaseCost: null, warrantyExpiresOn: null };
  vm.saving = false;
  vm.error = '';

  vm.submit = form => {
    if (form.$invalid || vm.saving) return;
    vm.saving = true;
    vm.error = '';
    AssetModel.create(assetPayload(vm.form))
      .then(asset => $state.go('workspace.assetDetail', { assetId: asset.id }))
      .catch(error => { vm.error = errorMessage(error, 'Could not add this asset.'); })
      .finally(() => { vm.saving = false; });
  };
}]);

featureModules.assets.controller('AssetDetailController', [
  '$state', '$stateParams', '$window', 'AuthService', 'AssetModel',
  function ($state, $stateParams, $window, AuthService, AssetModel) {
    const vm = this;
    vm.categories = categories;
    vm.asset = null;
    vm.users = [];
    vm.loading = true;
    vm.error = '';
    vm.saving = false;
    vm.working = false;
    vm.uploading = false;
    vm.editing = false;
    vm.selectedFile = null;
    vm.documentType = 'PURCHASE_RECEIPT';
    vm.assignment = { userId: null, note: '' };
    vm.returnInfo = { conditionNotes: '', needsMaintenance: false };
    vm.maintenanceForm = { summary: '', vendor: '', notes: '', cost: null, startedOn: todayInputDate() };

    vm.can = permission => vm.user?.permissions?.includes(permission) ?? false;
    vm.label = value => value?.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
    vm.currentAssignment = () => vm.asset?.assignments?.find(item => !item.returnedAt) || null;
    vm.canEdit = () => vm.can('assets:update') && vm.asset?.status !== 'RETIRED';
    vm.canAssign = () => vm.can('assets:assign') && vm.asset?.status === 'AVAILABLE';
    vm.canReturn = () => vm.can('assets:assign') && vm.asset?.status === 'ASSIGNED' && !!vm.currentAssignment();
    vm.canStartMaintenance = () => vm.can('assets:maintenance')
      && ['AVAILABLE', 'UNDER_MAINTENANCE'].includes(vm.asset?.status)
      && !vm.asset?.maintenance?.some(item => item.status === 'IN_PROGRESS');
    vm.canRetire = () => vm.can('assets:delete') && (vm.asset?.status === 'AVAILABLE'
      || vm.asset?.status === 'RETIRED'
      || (vm.asset?.status === 'UNDER_MAINTENANCE'
        && !vm.asset?.maintenance?.some(item => item.status === 'IN_PROGRESS')));

    const syncForm = () => {
      vm.editForm = {
        assetTag: vm.asset.assetTag,
        name: vm.asset.name,
        category: vm.asset.category,
        description: vm.asset.description,
        manufacturer: vm.asset.manufacturer,
        modelNumber: vm.asset.modelNumber,
        serialNumber: vm.asset.serialNumber,
        purchaseDate: localDate(vm.asset.purchaseDate),
        purchaseCost: vm.asset.purchaseCost,
        warrantyExpiresOn: localDate(vm.asset.warrantyExpiresOn)
      };
    };

    vm.load = () => {
      vm.loading = true;
      vm.error = '';
      return AuthService.me(true)
        .then(user => { vm.user = user; return AssetModel.get($stateParams.assetId); })
        .then(asset => {
          vm.asset = asset;
          syncForm();
          if (vm.can('assets:assign')) return AssetModel.users().then(users => { vm.users = users; });
          return null;
        })
        .catch(error => { vm.error = errorMessage(error, 'Could not load this asset.'); })
        .finally(() => { vm.loading = false; });
    };

    const replaceAsset = asset => { vm.asset = asset; syncForm(); };
    vm.save = form => {
      if (form.$invalid || vm.saving) return;
      vm.saving = true;
      vm.error = '';
      AssetModel.update(vm.asset.id, assetPayload(vm.editForm))
        .then(asset => { replaceAsset(asset); vm.editing = false; })
        .catch(error => { vm.error = errorMessage(error, 'Could not update asset details.'); })
        .finally(() => { vm.saving = false; });
    };

    vm.assign = () => {
      if (!vm.assignment.userId || vm.working) return;
      vm.working = true;
      vm.error = '';
      AssetModel.assign(vm.asset.id, vm.assignment)
        .then(asset => { replaceAsset(asset); vm.assignment = { userId: null, note: '' }; })
        .catch(error => { vm.error = errorMessage(error, 'Could not assign this asset.'); })
        .finally(() => { vm.working = false; });
    };

    vm.returnAsset = () => {
      const assignment = vm.currentAssignment();
      if (!assignment || vm.working) return;
      vm.working = true;
      vm.error = '';
      AssetModel.returnAsset(vm.asset.id, assignment.id, vm.returnInfo)
        .then(asset => { replaceAsset(asset); vm.returnInfo = { conditionNotes: '', needsMaintenance: false }; })
        .catch(error => { vm.error = errorMessage(error, 'Could not record this return.'); })
        .finally(() => { vm.working = false; });
    };

    vm.startMaintenance = form => {
      if (form.$invalid || vm.working) return;
      vm.working = true;
      vm.error = '';
      AssetModel.startMaintenance(vm.asset.id, { ...vm.maintenanceForm, startedOn: apiDate(vm.maintenanceForm.startedOn) })
        .then(asset => { replaceAsset(asset); vm.maintenanceForm = { summary: '', vendor: '', notes: '', cost: null, startedOn: todayInputDate() }; form.$setPristine(); })
        .catch(error => { vm.error = errorMessage(error, 'Could not record maintenance.'); })
        .finally(() => { vm.working = false; });
    };

    vm.changeMaintenance = (record, status) => {
      if (vm.working) return;
      vm.working = true;
      vm.error = '';
      AssetModel.changeMaintenance(vm.asset.id, record.id, status)
        .then(replaceAsset)
        .catch(error => { vm.error = errorMessage(error, 'Could not update maintenance.'); })
        .finally(() => { vm.working = false; });
    };

    vm.uploadDocument = () => {
      if (!vm.selectedFile || vm.uploading) return;
      vm.uploading = true;
      vm.error = '';
      AssetModel.uploadDocument(vm.asset.id, vm.documentType, vm.selectedFile)
        .then(asset => { replaceAsset(asset); vm.selectedFile = null; })
        .catch(error => { vm.error = errorMessage(error, 'Could not upload this document.'); })
        .finally(() => { vm.uploading = false; });
    };

    vm.downloadDocument = document => {
      $window.location.href = AssetModel.downloadUrl(vm.asset.id, document.id);
    };

    vm.deleteDocument = document => {
      vm.error = '';
      AssetModel.deleteDocument(vm.asset.id, document.id)
        .then(replaceAsset)
        .catch(error => { vm.error = errorMessage(error, 'Could not remove this document.'); });
    };

    vm.retire = () => {
      if (!vm.canRetire() || vm.asset.status === 'RETIRED'
          || !$window.confirm(`Retire asset ${vm.asset.assetTag}? It will remain in history.`)) return;
      AssetModel.retire(vm.asset.id)
        .then(replaceAsset)
        .catch(error => { vm.error = errorMessage(error, 'Could not retire this asset.'); });
    };

    vm.load();
  }
]);
