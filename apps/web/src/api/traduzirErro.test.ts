import type { ProblemDetails } from '@brewapp/shared';
import i18next from 'i18next';
import { describe, expect, it } from 'vitest';
import { ErroDaApi } from './ErroDaApi';
import { traduzirErro } from './traduzirErro';

function problemDetails(dadosDoProblema: Partial<ProblemDetails>): ProblemDetails {
  return {
    type: 'https://brewapp.dev/erros/exemplo',
    title: 'Título só para log',
    status: 400,
    code: 'REQUISICAO_INVALIDA',
    params: {},
    correlationId: '4f1c2b',
    errors: [],
    ...dadosDoProblema,
  };
}

function erroDaResposta(corpo: ProblemDetails): Promise<ErroDaApi> {
  return ErroDaApi.daResposta(
    new Response(JSON.stringify(corpo), {
      status: corpo.status,
      headers: { 'Content-Type': 'application/problem+json' },
    }),
  );
}

async function traduzirNoIdioma(erroDaApi: ErroDaApi, idioma: string) {
  await i18next.changeLanguage(idioma);
  return traduzirErro(erroDaApi, i18next.t);
}

describe('traduzirErro', () => {
  it('exibe a tradução do code e a referência para o suporte, nunca o title', async () => {
    const erroDaApi = await erroDaResposta(problemDetails({ status: 403, code: 'ACESSO_NEGADO' }));

    const erroTraduzido = await traduzirNoIdioma(erroDaApi, 'pt-BR');

    expect(erroTraduzido.mensagem).toBe('Você não tem permissão para esta ação.');
    expect(erroTraduzido.referencia).toBe('Código para o suporte: 4f1c2b');
    expect(erroTraduzido.mensagem).not.toContain('Título só para log');
  });

  it('acompanha o idioma em uso', async () => {
    const erroDaApi = await erroDaResposta(problemDetails({ status: 403, code: 'ACESSO_NEGADO' }));

    expect((await traduzirNoIdioma(erroDaApi, 'es')).mensagem).toBe('No tienes permiso para esta acción.');
    expect((await traduzirNoIdioma(erroDaApi, 'en')).mensagem).toBe("You don't have permission for this action.");
  });

  it('traduz cada campo da falha de validação com os params, no formato de número do idioma', async () => {
    const erroDaApi = await erroDaResposta(
      problemDetails({
        code: 'VALIDACAO_FALHOU',
        errors: [
          { campo: 'nome', code: 'TAMANHO_INVALIDO', params: { min: 1, max: 100 } },
          { campo: 'capacidade.valor', code: 'ACIMA_DO_MAXIMO', params: { value: 10000 } },
          { campo: 'capacidade.unidade', code: 'DECIMAL_MIN', params: {} },
        ],
      }),
    );

    expect(await traduzirNoIdioma(erroDaApi, 'pt-BR')).toEqual({
      mensagem: 'Alguns campos precisam de correção.',
      referencia: 'Código para o suporte: 4f1c2b',
      campos: [
        { campo: 'nome', mensagem: 'O tamanho deve ficar entre 1 e 100.' },
        { campo: 'capacidade.valor', mensagem: 'O valor máximo é 10.000.' },
        { campo: 'capacidade.unidade', mensagem: 'Confira o valor deste campo.' },
      ],
    });
    expect((await traduzirNoIdioma(erroDaApi, 'en')).campos[1]?.mensagem).toBe('The maximum value is 10,000.');
  });

  it('exibe a mensagem genérica para um code ainda sem tradução', async () => {
    const erroDaApi = await erroDaResposta(problemDetails({ status: 409, code: 'CODIGO_DE_BACKEND_MAIS_NOVO' }));

    expect((await traduzirNoIdioma(erroDaApi, 'pt-BR')).mensagem).toBe('Ocorreu um erro inesperado.');
  });

  it('traduz a falha sem resposta, sem referência para o suporte', async () => {
    const erroDaApi = ErroDaApi.semConexao(new TypeError('Failed to fetch'));

    expect(await traduzirNoIdioma(erroDaApi, 'pt-BR')).toEqual({
      mensagem: 'Sem conexão com o servidor. Verifique sua internet e tente novamente.',
      referencia: null,
      campos: [],
    });
  });

  it('traduz a resposta fora do formato Problem Details', async () => {
    const erroDaApi = await ErroDaApi.daResposta(new Response('<html>Bad Gateway</html>', { status: 502 }));

    expect((await traduzirNoIdioma(erroDaApi, 'pt-BR')).mensagem).toBe(
      'O servidor respondeu de forma inesperada. Tente novamente mais tarde.',
    );
  });
});
