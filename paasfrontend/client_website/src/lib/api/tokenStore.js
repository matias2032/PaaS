// Guarda o access token em memória (não em localStorage/sessionStorage).
// Motivo: reduzir exposição a XSS. O custo é perder a sessão num refresh
// de página (F5) — isso será resolvido quando adicionarmos refresh token
// via cookie httpOnly no futuro.

let accessToken = null;

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token) {
  accessToken = token;
}

export function clearAccessToken() {
  accessToken = null;
}