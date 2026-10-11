import loginTemplate from './views/login.html?raw';

export function registerAuthRoutes($stateProvider) {
  $stateProvider.state('login', {
    url: '/login',
    template: loginTemplate,
    controller: 'LoginController as vm'
  });
}
