// @vitest-environment node
import { describe, expect, it } from 'vitest';
import { lerConfiguracaoOidc } from './configuracaoOidc';

const VARIAVEIS_COMPLETAS = {
  VITE_OAUTH_ISSUER_URI: 'http://localhost:8180/realms/brewapp',
  VITE_OAUTH_CLIENT_ID_WEB: 'brewapp-web',
  VITE_OAUTH_CLIENT_ID_DESKTOP: 'brewapp-desktop',
};

describe('lerConfiguracaoOidc', () => {
  it('lê o emissor e os clients de cada plataforma', () => {
    expect(lerConfiguracaoOidc(VARIAVEIS_COMPLETAS)).toEqual({
      emissor: 'http://localhost:8180/realms/brewapp',
      clientIdWeb: 'brewapp-web',
      clientIdDesktop: 'brewapp-desktop',
    });
  });

  it.each(['VITE_OAUTH_ISSUER_URI', 'VITE_OAUTH_CLIENT_ID_WEB', 'VITE_OAUTH_CLIENT_ID_DESKTOP'])(
    'falha com o nome da variável quando %s falta',
    (nomeDaVariavel) => {
      const variaveis = { ...VARIAVEIS_COMPLETAS, [nomeDaVariavel]: undefined };

      expect(() => lerConfiguracaoOidc(variaveis)).toThrow(nomeDaVariavel);
    },
  );

  it('trata variável em branco como ausente', () => {
    expect(() => lerConfiguracaoOidc({ ...VARIAVEIS_COMPLETAS, VITE_OAUTH_CLIENT_ID_WEB: '  ' })).toThrow(
      'VITE_OAUTH_CLIENT_ID_WEB',
    );
  });
});
