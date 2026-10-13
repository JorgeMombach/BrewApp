package dev.brewapp.shared.unidade;

/**
 * Unidades de densidade e a conversão de cada uma para a canônica, SG (design da API, seção 1.11). Fonte da verdade
 * das conversões; a pré-visualização no aparelho usa a cópia em TypeScript (packages/shared/src/unidades), e as duas
 * são testadas contra packages/shared/casos-de-teste/densidade.json (spec 4.5).
 * Exemplo da BREW-18; o catálogo completo, com Quantidade e as demais unidades, vem na BREW-45.
 */
public enum UnidadeDeDensidade {
    SG {
        @Override
        double paraSg(double valor) {
            return valor;
        }

        @Override
        double deSg(double gravidadeEspecifica) {
            return gravidadeEspecifica;
        }
    },

    /** Graus Plato: porcentagem em massa de extrato. As duas fórmulas são aproximações da tabela da ASBC. */
    PLATO {
        @Override
        double paraSg(double grausPlato) {
            return 1 + grausPlato / (258.6 - grausPlato / 258.2 * 227.1);
        }

        @Override
        double deSg(double gravidadeEspecifica) {
            return -616.868
                    + 1111.14 * gravidadeEspecifica
                    - 630.272 * Math.pow(gravidadeEspecifica, 2)
                    + 135.997 * Math.pow(gravidadeEspecifica, 3);
        }
    };

    abstract double paraSg(double valor);

    abstract double deSg(double gravidadeEspecifica);

    /**
     * Converte passando pela canônica. Na mesma unidade devolve o valor sem conversão: as fórmulas de ida e volta do
     * Plato não são inversas exatas, e 12 °P não pode virar 12,007 °P.
     */
    public static double converter(double valor, UnidadeDeDensidade origem, UnidadeDeDensidade destino) {
        if (origem == destino) {
            return valor;
        }
        return destino.deSg(origem.paraSg(valor));
    }
}
