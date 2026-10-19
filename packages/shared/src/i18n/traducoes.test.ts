import { describe, expect, it } from 'vitest';
import { GRANDEZAS, UNIDADES } from '../unidades/catalogo';
import { IDIOMAS } from './idiomas';
import { TRADUCOES, chaveDaGrandeza, chaveDaUnidade, chaveDaValidacao, chaveDoErro } from './traducoes';

describe('chaveDoErro', () => {
  it('usa a tradução do code da API e dos códigos do cliente', () => {
    expect(chaveDoErro('ACESSO_NEGADO')).toBe('erro.ACESSO_NEGADO');
    expect(chaveDoErro('UNIDADE_INVALIDA')).toBe('erro.UNIDADE_INVALIDA');
    expect(chaveDoErro('SEM_CONEXAO')).toBe('erro.SEM_CONEXAO');
  });

  it('cai na mensagem genérica com code ainda sem tradução', () => {
    expect(chaveDoErro('CODIGO_DE_BACKEND_MAIS_NOVO')).toBe('erro.desconhecido');
  });

  it('não confunde chave auxiliar com code', () => {
    expect(chaveDoErro('referencia')).toBe('erro.desconhecido');
  });
});

describe('chaveDaValidacao', () => {
  it('usa a tradução do code do item de errors', () => {
    expect(chaveDaValidacao('TAMANHO_INVALIDO')).toBe('validacao.TAMANHO_INVALIDO');
  });

  it('cai na mensagem genérica do campo com regra ainda sem código próprio', () => {
    expect(chaveDaValidacao('DECIMAL_MIN')).toBe('validacao.desconhecido');
  });
});

describe('traduções dos valores de domínio', () => {
  it.each(IDIOMAS)('%s traduz toda unidade e toda grandeza do catálogo', (idioma) => {
    const traducoesDoIdioma = TRADUCOES[idioma];

    for (const unidade of UNIDADES) {
      expect(traducoesDoIdioma[chaveDaUnidade(unidade)]).not.toBe('');
    }
    for (const grandeza of GRANDEZAS) {
      expect(traducoesDoIdioma[chaveDaGrandeza(grandeza)]).not.toBe('');
    }
  });
});
