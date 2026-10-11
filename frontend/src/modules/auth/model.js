import { featureModules } from '../registry.js';

featureModules.auth.factory('AuthService', ['$http', '$q', ($http, $q) => {
  let currentUser = null;

  return {
    csrf() {
      return $http.get('/api/v1/auth/csrf').then(response => response.data.token);
    },
    me(force = false) {
      if (currentUser && !force) return $q.when(currentUser);
      return $http.get('/api/v1/auth/me').then(response => {
        currentUser = response.data;
        return currentUser;
      }).catch(error => {
        currentUser = null;
        return $q.reject(error);
      });
    },
    login(credentials) {
      return this.csrf()
        .then(() => $http.post('/api/v1/auth/login', credentials))
        .then(response => {
          currentUser = response.data;
          return this.csrf().then(() => this.me(true));
        });
    },
    logout() {
      return this.csrf()
        .then(() => $http.post('/api/v1/auth/logout'))
        .finally(() => { currentUser = null; });
    },
    clear() {
      currentUser = null;
    }
  };
}]);
