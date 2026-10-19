import { IDIOMA_PADRAO, IDIOMAS, type Idioma } from './idiomas';

export type ProblemaDeTraducao = 'FALTANDO' | 'A_MAIS' | 'VAZIA' | 'VARIAVEIS_DIFERENTES';

export interface DivergenciaDeTraducao {
  idioma: Idioma;
  chave: string;
  problema: ProblemaDeTraducao;
}

/** Nome de cada {{variavel}} ou {{variavel, formato}} do texto, sem repetição e em ordem alfabética. */
function variaveisDoTexto(texto: string): string[] {
  const nomes = [...texto.matchAll(/\{\{\s*([^,}\s]+)[^}]*\}\}/g)].map((ocorrencia) => ocorrencia[1] ?? '');
  return [...new Set(nomes)].sort();
}

function mesmasVariaveis(texto: string, textoDeReferencia: string): boolean {
  return variaveisDoTexto(texto).join(',') === variaveisDoTexto(textoDeReferencia).join(',');
}

/**
 * Confere os três idiomas contra o pt-BR, a referência das chaves (spec, seção 8): mesmas chaves, nenhum texto vazio
 * e as mesmas variáveis em cada texto, já que uma variável a menos some da tela e uma a mais aparece como {{nome}}.
 * O compilador já barra chave faltando; esta conferência roda na CI e pega também o resto. Plural segue a mesma
 * regra: as formas (_one, _other e, se usada, _many) existem em todos os idiomas.
 */
export function divergenciasDasTraducoes(
  traducoesPorIdioma: Record<Idioma, Record<string, string>>,
): DivergenciaDeTraducao[] {
  const referencia = traducoesPorIdioma[IDIOMA_PADRAO];
  const divergencias: DivergenciaDeTraducao[] = [];

  for (const idioma of IDIOMAS) {
    const traducoes = traducoesPorIdioma[idioma];
    for (const [chave, textoDeReferencia] of Object.entries(referencia)) {
      const texto = traducoes[chave];
      if (texto === undefined) {
        divergencias.push({ idioma, chave, problema: 'FALTANDO' });
      } else if (texto.trim() === '') {
        divergencias.push({ idioma, chave, problema: 'VAZIA' });
      } else if (!mesmasVariaveis(texto, textoDeReferencia)) {
        divergencias.push({ idioma, chave, problema: 'VARIAVEIS_DIFERENTES' });
      }
    }
    for (const chave of Object.keys(traducoes)) {
      if (!Object.hasOwn(referencia, chave)) {
        divergencias.push({ idioma, chave, problema: 'A_MAIS' });
      }
    }
  }
  return divergencias;
}
