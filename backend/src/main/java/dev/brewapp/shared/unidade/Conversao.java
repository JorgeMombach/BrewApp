package dev.brewapp.shared.unidade;

/** Conversão de uma unidade para a canônica da sua grandeza e de volta. */
interface Conversao {

    /** Da própria unidade canônica: não converte. */
    Conversao CANONICA = afim(1, 0);

    /**
     * Escala de sacarose (graus Plato e Brix): porcentagem em massa de extrato. As duas escalas vêm da mesma tabela
     * de sacarose e diferem menos que a precisão de um densímetro; as fórmulas são aproximações da tabela da ASBC.
     */
    Conversao ESCALA_DE_SACAROSE = new Conversao() {
        @Override
        public double paraCanonica(double grausDeExtrato) {
            return 1 + grausDeExtrato / (258.6 - grausDeExtrato / 258.2 * 227.1);
        }

        @Override
        public double daCanonica(double gravidadeEspecifica) {
            return -616.868
                    + 1111.14 * gravidadeEspecifica
                    - 630.272 * Math.pow(gravidadeEspecifica, 2)
                    + 135.997 * Math.pow(gravidadeEspecifica, 3);
        }
    };

    double paraCanonica(double valor);

    double daCanonica(double valorCanonico);

    /** Proporcional: canônica = valor × fator. */
    static Conversao linear(double fator) {
        return afim(fator, 0);
    }

    /** Com deslocamento de zero, como o °F: canônica = (valor − zero) × fator. */
    static Conversao afim(double fator, double zero) {
        return new Conversao() {
            @Override
            public double paraCanonica(double valor) {
                return (valor - zero) * fator;
            }

            @Override
            public double daCanonica(double valorCanonico) {
                return valorCanonico / fator + zero;
            }
        };
    }
}
