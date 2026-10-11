import { featureModules } from '../registry.js';

featureModules.reports.directive('reportFileInput', ['$parse', function ($parse) {
  return {
    restrict: 'A',
    link(scope, element, attributes) {
      const expression = $parse(attributes.reportFileInput);
      const onChange = event => scope.$apply(() => expression(scope, { $file: event.target.files?.[0] || null }));
      element.on('change', onChange);
      scope.$on('$destroy', () => element.off('change', onChange));
    }
  };
}]);
