import { registerAuthRoutes } from './auth/routes.js';
import { registerWorkspaceRoutes } from './workspace/routes.js';
import { registerDashboardRoutes } from './dashboard/routes.js';
import { registerUserRoutes } from './users/routes.js';
import { registerDepartmentRoutes } from './departments/routes.js';
import { registerAccessRoutes } from './access/routes.js';
import { registerTicketRoutes } from './tickets/routes.js';
import { registerAssetRoutes } from './assets/routes.js';
import { registerEquipmentRequestRoutes } from './requests/routes.js';

// Route composition stays small; each route definition belongs to its feature.
export function registerFeatureRoutes($stateProvider) {
  registerAuthRoutes($stateProvider);
  registerWorkspaceRoutes($stateProvider);
  registerDashboardRoutes($stateProvider);
  registerUserRoutes($stateProvider);
  registerDepartmentRoutes($stateProvider);
  registerAccessRoutes($stateProvider);
  registerTicketRoutes($stateProvider);
  registerAssetRoutes($stateProvider);
  registerEquipmentRequestRoutes($stateProvider);
}
