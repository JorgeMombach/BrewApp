import {
  createContext,
  type ReactNode,
  use,
  useCallback,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
} from 'react';
import { descreverFalha } from './descreverFalha';
import type { AutenticacaoDaPlataforma } from './plataforma';
import type { SituacaoDaSessao } from './sessao';

export interface Sessao {
  situacao: SituacaoDaSessao;
  /** Login em andamento: no desktop, o usuário está no navegador do sistema */
  entrando: boolean;
  falhaAoEntrar: boolean;
  /** Se o login segue fora do app (navegador do sistema), para a tela avisar o usuário */
  abreNavegadorExterno: boolean;
  entrar: () => void;
  sair: () => void;
  concluirEntradaPorRedirect: AutenticacaoDaPlataforma['concluirEntradaPorRedirect'];
}

const ContextoDeSessao = createContext<Sessao | null>(null);

interface PropriedadesDoProvedorDeSessao {
  autenticacao: AutenticacaoDaPlataforma;
  children: ReactNode;
}

export function ProvedorDeSessao({ autenticacao, children }: PropriedadesDoProvedorDeSessao) {
  const {
    gerenciadorDeSessao,
    entrar: entrarNaPlataforma,
    abreNavegadorExterno,
    concluirEntradaPorRedirect,
  } = autenticacao;
  const situacao = useSyncExternalStore(gerenciadorDeSessao.assinar, gerenciadorDeSessao.situacaoAtual);
  const [entrando, setEntrando] = useState(false);
  const [falhaAoEntrar, setFalhaAoEntrar] = useState(false);
  // Quem fecha o navegador sem entrar pode clicar de novo: só a tentativa mais recente atualiza a tela
  const ultimaTentativa = useRef(0);

  const entrar = useCallback(() => {
    ultimaTentativa.current += 1;
    const tentativa = ultimaTentativa.current;
    setEntrando(true);
    setFalhaAoEntrar(false);
    entrarNaPlataforma()
      .catch((erro: unknown) => {
        console.error('Falha no login', descreverFalha(erro));
        if (tentativa === ultimaTentativa.current) {
          setFalhaAoEntrar(true);
        }
      })
      .finally(() => {
        if (tentativa === ultimaTentativa.current) {
          setEntrando(false);
        }
      });
  }, [entrarNaPlataforma]);

  const sair = useCallback(() => {
    gerenciadorDeSessao.sair().catch((erro: unknown) => {
      // A sessão local já acabou; sem a confirmação, o Keycloak a encerra sozinho por inatividade
      console.warn('Sessão encerrada no app, mas o provedor não confirmou o logout', descreverFalha(erro));
    });
  }, [gerenciadorDeSessao]);

  const sessao = useMemo(
    () => ({ situacao, entrando, falhaAoEntrar, abreNavegadorExterno, entrar, sair, concluirEntradaPorRedirect }),
    [situacao, entrando, falhaAoEntrar, abreNavegadorExterno, entrar, sair, concluirEntradaPorRedirect],
  );
  return <ContextoDeSessao value={sessao}>{children}</ContextoDeSessao>;
}

export function useSessao(): Sessao {
  const sessao = use(ContextoDeSessao);
  if (sessao === null) {
    throw new Error('useSessao usado fora do ProvedorDeSessao');
  }
  return sessao;
}
