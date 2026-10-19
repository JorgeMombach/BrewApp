/** Idiomas do app (spec, seção 8), com os mesmos códigos da preferência idioma do GET /me (design da API). */
export const IDIOMAS = ['pt-BR', 'es', 'en'] as const;

export type Idioma = (typeof IDIOMAS)[number];

/** Usado quando nenhum idioma preferido do usuário é suportado. */
export const IDIOMA_PADRAO: Idioma = 'pt-BR';

export function ehIdioma(codigo: string): codigo is Idioma {
  return IDIOMAS.some((idioma) => idioma === codigo);
}

/** Variantes regionais caem no idioma da mesma língua: es-MX vira es, pt-PT vira pt-BR. */
const IDIOMA_DA_LINGUA: Record<string, Idioma> = {
  pt: 'pt-BR',
  es: 'es',
  en: 'en',
};

/**
 * Escolhe o idioma a partir das preferências do usuário em ordem (BCP 47, como o navigator.languages), aceitando
 * maiúsculas ou minúsculas. Vence a primeira preferência suportada, exata ou pela língua.
 */
export function idiomaPreferido(preferencias: readonly string[]): Idioma {
  for (const preferencia of preferencias) {
    const idiomaExato = IDIOMAS.find((idioma) => idioma.toLowerCase() === preferencia.toLowerCase());
    if (idiomaExato !== undefined) {
      return idiomaExato;
    }
    const lingua = preferencia.split('-')[0]?.toLowerCase() ?? '';
    const idiomaDaLingua = IDIOMA_DA_LINGUA[lingua];
    if (idiomaDaLingua !== undefined) {
      return idiomaDaLingua;
    }
  }
  return IDIOMA_PADRAO;
}
