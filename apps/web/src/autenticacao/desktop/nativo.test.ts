// Os comandos do Rust são simulados pelo mockIPC do próprio Tauri, que precisa do window do jsdom
import { clearMocks, mockIPC } from '@tauri-apps/api/mocks';
import { afterEach, describe, expect, it } from 'vitest';
import { cofreDoDesktop, escutarRetornoDoLogin } from './nativo';

interface ChamadaDeComando {
  comando: string;
  argumentos: unknown;
}

function registrarChamadas(responder: (comando: string) => unknown = () => null) {
  const chamadas: ChamadaDeComando[] = [];
  mockIPC((comando, argumentos) => {
    chamadas.push({ comando, argumentos });
    return responder(comando);
  });
  return chamadas;
}

afterEach(() => {
  clearMocks();
});

describe('cofre do desktop', () => {
  it('chama os comandos do cofre com os nomes e argumentos do Rust', async () => {
    const credenciais = { refreshToken: 'refresh', idToken: 'id' };
    const chamadas = registrarChamadas((comando) => (comando === 'ler_credenciais' ? credenciais : null));

    await cofreDoDesktop.guardar(credenciais);
    const credenciaisLidas = await cofreDoDesktop.ler();
    await cofreDoDesktop.apagar();

    expect(credenciaisLidas).toEqual(credenciais);
    expect(chamadas).toEqual([
      { comando: 'guardar_credenciais', argumentos: { credenciais } },
      { comando: 'ler_credenciais', argumentos: {} },
      { comando: 'apagar_credenciais', argumentos: {} },
    ]);
  });
});

describe('retorno do login', () => {
  it('rejeita quando o servidor de loopback não consegue subir', async () => {
    mockIPC(() => Promise.reject(new Error('porta indisponível')));

    await expect(escutarRetornoDoLogin('<p>ok</p>')).rejects.toThrow('porta indisponível');
  });
});
