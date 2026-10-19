import { ehIdioma, idiomaPreferido, type Idioma } from '@brewapp/shared';

/** Escolha manual do seletor de idioma, guardada no navegador. */
const CHAVE_DO_IDIOMA_ESCOLHIDO = 'brewapp.idioma';

/**
 * Idioma ao abrir o app: a escolha manual guardada e, sem ela, as preferências do navegador. A preferência idioma do
 * GET /me entra na frente das duas com o login (BREW-6).
 */
export function idiomaInicial(): Idioma {
  return lerIdiomaEscolhido() ?? idiomaPreferido(navigator.languages);
}

export function guardarIdiomaEscolhido(idioma: Idioma): void {
  try {
    localStorage.setItem(CHAVE_DO_IDIOMA_ESCOLHIDO, idioma);
  } catch {
    // Armazenamento bloqueado (como em algumas janelas privadas): a escolha vale só até fechar o app
  }
}

function lerIdiomaEscolhido(): Idioma | null {
  try {
    const idiomaGuardado = localStorage.getItem(CHAVE_DO_IDIOMA_ESCOLHIDO);
    return idiomaGuardado !== null && ehIdioma(idiomaGuardado) ? idiomaGuardado : null;
  } catch {
    return null;
  }
}
