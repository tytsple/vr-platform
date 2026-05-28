const TOKEN_KEY = 'jwt';
const USER_KEY = 'user';

export function getToken() { return localStorage.getItem(TOKEN_KEY); }
export function setToken(token) { localStorage.setItem(TOKEN_KEY, token); }
export function removeToken() { localStorage.removeItem(TOKEN_KEY); }

export function getUser() {
  try { return JSON.parse(localStorage.getItem(USER_KEY)); }
  catch { return null; }
}
export function setUser(user) { localStorage.setItem(USER_KEY, JSON.stringify(user)); }
export function removeUser() { localStorage.removeItem(USER_KEY); }
