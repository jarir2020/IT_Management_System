import angular from 'angular';

window.angular = angular;

await import('angular-messages');
await import('@uirouter/angularjs');
await import('./app/app.module.js');

// Bootstrap only after the async imports have registered the root AngularJS module.
const appRoot = document.getElementById('app');
if (!appRoot) throw new Error('Unable to find the AngularJS application root.');
angular.bootstrap(appRoot, ['itDeskApp']);
