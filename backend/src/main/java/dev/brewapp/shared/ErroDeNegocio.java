package dev.brewapp.shared;

import java.util.Map;
import java.util.Objects;

/**
 * Erro de negócio lançado por domínio e casos de uso. O código, o título e o tipo vêm juntos do
 * {@link CodigoDeErro}; os {@code params} levam o que o cliente precisa para montar a mensagem traduzida:
 *
 * <pre>{@code
 * throw new ErroDeNegocio(ErroDeLote.LOTE_ENVASADO, Map.of("loteId", loteId, "envasadoEm", envasadoEm));
 * }</pre>
 */
public class ErroDeNegocio extends RuntimeException {

    private final CodigoDeErro codigoDeErro;
    private final Map<String, Object> params;

    public ErroDeNegocio(CodigoDeErro codigoDeErro) {
        this(codigoDeErro, Map.of());
    }

    public ErroDeNegocio(CodigoDeErro codigoDeErro, Map<String, Object> params) {
        super(Objects.requireNonNull(codigoDeErro, "codigoDeErro").titulo());
        this.codigoDeErro = codigoDeErro;
        this.params = Map.copyOf(params);
    }

    public CodigoDeErro codigoDeErro() {
        return codigoDeErro;
    }

    public Map<String, Object> params() {
        return params;
    }
}
