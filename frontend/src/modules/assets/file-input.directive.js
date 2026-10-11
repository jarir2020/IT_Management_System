import { featureModules } from '../registry.js';

featureModules.assets.directive('assetFileInput', [function () {
  return {
    restrict: 'A',
    link(scope, element) {
      element.on('change', () => scope.$apply(() => {
        scope.vm.selectedFile = element[0].files[0] || null;
      }));
      scope.$watch('vm.selectedFile', file => {
        if (!file) element[0].value = '';
      });
      scope.$on('$destroy', () => element.off('change'));
    }
  };
}]);
