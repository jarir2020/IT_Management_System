import assetListTemplate from './views/list.html?raw';
import assetNewTemplate from './views/new.html?raw';
import assetDetailTemplate from './views/detail.html?raw';

export function registerAssetRoutes($stateProvider) {
  $stateProvider
    .state('workspace.assets', {
      url: '/assets',
      data: { permission: 'assets:read_own' },
      template: assetListTemplate,
      controller: 'AssetListController as vm'
    })
    .state('workspace.newAsset', {
      url: '/assets/new',
      data: { permission: 'assets:create' },
      template: assetNewTemplate,
      controller: 'AssetCreateController as vm'
    })
    .state('workspace.assetDetail', {
      url: '/assets/{assetId:int}',
      data: { permission: 'assets:read_own' },
      template: assetDetailTemplate,
      controller: 'AssetDetailController as vm'
    });
}
