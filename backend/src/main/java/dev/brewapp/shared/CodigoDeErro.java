package dev.brewapp.shared;

/**
 * Código de erro de negócio: contrato estável com os clientes, que traduzem o {@code name()} (design da API, 1.6).
 * Implementado por um enum por módulo, que reúne os erros daquele módulo, por exemplo:
 *
 * <pre>{@code
 * enum ErroDeLote implements CodigoDeErro {
 *     LOTE_ENVASADO("Lote já envasado", TipoDeErro.CONFLITO);
 *     ...
 * }
 * }</pre>
 *
 * Renomear uma constante muda o contrato: exige atualizar o design da API e as traduções dos clientes.
 */
public interface CodigoDeErro {

    /** O código em UPPER_SNAKE_CASE; nos enums, vem do nome da constante. */
    String name();

    /** Texto para leitura humana em logs; nunca exibido ao usuário. */
    String titulo();

    TipoDeErro tipoDeErro();
}
