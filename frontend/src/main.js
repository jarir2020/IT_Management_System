import angular from 'angular';

window.angular = angular;

await import('angular-messages');
await import('@uirouter/angularjs');
await import('./app/app.module.js');
