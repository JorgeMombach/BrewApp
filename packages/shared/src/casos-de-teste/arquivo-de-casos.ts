/**
 * Formato dos arquivos de packages/shared/casos-de-teste (spec 4.5), lidos também pelos testes Java
 * (CasosDeTesteCompartilhados). Atribuir o JSON importado a este tipo faz o compilador conferir a estrutura.
 */
export interface ArquivoDeCasos {
  grandeza: string;
  referencia: string;
  casos: CasoDeConversao[];
}

/** Converter `de` para a unidade `para` resulta em `esperado`, com até `tolerancia` de diferença. */
export interface CasoDeConversao {
  descricao: string;
  de: { valor: number; unidade: string };
  para: string;
  esperado: number;
  tolerancia: number;
}
