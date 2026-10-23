import { GerenciadorDeSessao } from '../autenticacao/GerenciadorDeSessao';
import type { AutenticacaoDaPlataforma } from '../autenticacao/plataforma';
import type { TokensDaSessao } from '../autenticacao/sessao';

/** Autenticação sem Keycloak para os testes de tela: o login termina quando o teste chamar concluirEntrada. */
export function criarAutenticacaoDeTeste(
  opcoes: Pick<AutenticacaoDaPlataforma, 'concluirEntradaPorRedirect'> & { abreNavegadorExterno?: boolean } = {},
) {
  let concluir: () => void = () => undefined;
  let falhar: (erro: Error) => void = () => undefined;
  const sessoesEncerradas: TokensDaSessao[] = [];
  const gerenciadorDeSessao = new GerenciadorDeSessao({
    renovar: () => Promise.reject(new Error('Renovação não usada nos testes de tela')),
    encerrarNoProvedor: (tokens) => {
      sessoesEncerradas.push(tokens);
      return Promise.resolve();
    },
    cofre: { ler: () => Promise.resolve(null), guardar: () => Promise.resolve(), apagar: () => Promise.resolve() },
    agora: Date.now,
  });
  const autenticacao: AutenticacaoDaPlataforma = {
    gerenciadorDeSessao,
    abreNavegadorExterno: opcoes.abreNavegadorExterno ?? true,
    concluirEntradaPorRedirect: opcoes.concluirEntradaPorRedirect,
    entrar: () =>
      new Promise<void>((resolver, rejeitar) => {
        concluir = () => {
          void gerenciadorDeSessao
            .registrarEntrada({
              tokenDeAcesso: 'acesso',
              expiraEm: Date.now() + 300_000,
              refreshToken: 'refresh',
              idToken: 'id',
            })
            .then(resolver);
        };
        falhar = rejeitar;
      }),
  };
  return {
    autenticacao,
    sessoesEncerradas,
    concluirEntrada: () => {
      concluir();
    },
    falharEntrada: (erro: Error) => {
      falhar(erro);
    },
  };
}
