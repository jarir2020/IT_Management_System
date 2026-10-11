import angular from 'angular';

// Each feature module owns its controllers and models; the root app composes them here.
export const featureModules = {
  auth: angular.module('itDesk.auth', []),
  workspace: angular.module('itDesk.workspace', []),
  dashboard: angular.module('itDesk.dashboard', []),
  users: angular.module('itDesk.users', []),
  departments: angular.module('itDesk.departments', []),
  access: angular.module('itDesk.access', []),
  tickets: angular.module('itDesk.tickets', []),
  assets: angular.module('itDesk.assets', []),
  requests: angular.module('itDesk.requests', []),
  reports: angular.module('itDesk.reports', [])
};
