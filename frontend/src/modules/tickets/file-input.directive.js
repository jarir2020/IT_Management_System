import { featureModules } from '../registry.js';

featureModules.tickets.directive('ticketFileInput', [function () {
  return {
    restrict: 'A',
    link(scope, element) {
      element.on('change', () => scope.$apply(() => {
        scope.vm.selectedFile = element[0].files[0] || null;
      }));
      scope.$on('$destroy', () => element.off('change'));
    }
  };
}]);
