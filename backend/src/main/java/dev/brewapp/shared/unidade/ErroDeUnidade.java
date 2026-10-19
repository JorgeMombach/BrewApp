package dev.brewapp.shared.unidade;

import dev.brewapp.shared.CodigoDeErro;
import dev.brewapp.shared.TipoDeErro;

/** Erros do catálogo de unidades (design da API, seção 1.6). Renomear uma constante muda o contrato com os clientes. */
public enum ErroDeUnidade implements CodigoDeErro {
    UNIDADE_INVALIDA("Unidade incompatível com a grandeza", TipoDeErro.REGRA_VIOLADA);

    private final String titulo;
    private final TipoDeErro tipoDeErro;

    ErroDeUnidade(String titulo, TipoDeErro tipoDeErro) {
        this.titulo = titulo;
        this.tipoDeErro = tipoDeErro;
    }

    @Override
    public String titulo() {
        return titulo;
    }

    @Override
    public TipoDeErro tipoDeErro() {
        return tipoDeErro;
    }
}
