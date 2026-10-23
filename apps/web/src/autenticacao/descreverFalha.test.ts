// @vitest-environment node
import { ErrorResponse } from 'oidc-client-ts';
import { describe, expect, it } from 'vitest';
import { descreverFalha } from './descreverFalha';

describe('descreverFalha', () => {
  it('descreve a recusa do provedor sem o formulário enviado, que leva o refresh token', () => {
    const recusa = new ErrorResponse(
      { error: 'invalid_grant', error_description: 'Token is not active' },
      new URLSearchParams({ grant_type: 'refresh_token', refresh_token: 'refresh-token-secreto' }),
    );

    const descricao = descreverFalha(recusa);

    expect(descricao).toContain('Token is not active');
    expect(descricao).not.toContain('secreto');
  });

  it('descreve falha de rede e valor que não é erro', () => {
    expect(descreverFalha(new TypeError('Failed to fetch'))).toBe('TypeError: Failed to fetch');
    expect(descreverFalha('qualquer coisa')).toBe('falha sem detalhes');
  });
});
