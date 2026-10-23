/**
 * Texto seguro para o console de uma falha de login, renovação ou logout: só o nome e a mensagem. O objeto inteiro não
 * serve, porque o ErrorResponse do oidc-client-ts guarda em "form" o formulário enviado ao provedor, com o refresh token
 * ou o code_verifier (Definition of Done: nenhum token nos logs).
 */
export function descreverFalha(erro: unknown): string {
  return erro instanceof Error ? `${erro.name}: ${erro.message}` : 'falha sem detalhes';
}
