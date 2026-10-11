import angular from 'angular';
import '../styles.css';
import '../modules/index.js';
import { featureModules } from '../modules/registry.js';
import { registerFeatureRoutes } from '../modules/routes.js';

const featureDependencies = Object.values(featureModules).map(feature => feature.name);
const app = angular.module('itDeskApp', ['ui.router', 'ngMessages', ...featureDependencies]);

app.config(['$stateProvider', '$urlRouterProvider', ($stateProvider, $urlRouterProvider) => {
  registerFeatureRoutes($stateProvider);
  $urlRouterProvider.otherwise('/login');
}]);

// Keep the signed-in user's permission check at the shared route boundary.
app.run(['$transitions', 'AuthService', ($transitions, AuthService) => {
  $transitions.onStart({ to: state => state.name.startsWith('workspace') }, transition =>
    AuthService.me(true).then(user => {
      const permission = transition.to().data?.permission;
      if (!permission || user.permissions?.includes(permission)) return true;
      return transition.router.stateService.target('workspace.dashboard');
    }).catch(() => transition.router.stateService.target('login'))
  );

  $transitions.onStart({ to: 'login' }, transition =>
    AuthService.me(true)
      .then(() => transition.router.stateService.target('workspace.dashboard'))
      .catch(() => true)
  );

  AuthService.csrf().catch(() => {});
}]);
