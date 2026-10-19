package dev.brewapp.shared.unidade;

/**
 * Catálogo de unidades de medida (design da API, seção 1.11): cada unidade com a sua grandeza e a conversão para a
 * canônica. Fonte da verdade das conversões; a pré-visualização no aparelho usa a cópia em TypeScript
 * (packages/shared/src/unidades/catalogo.ts), e as duas são testadas contra os mesmos arquivos de
 * packages/shared/casos-de-teste (spec 4.5). Incluir uma unidade é acrescentar uma constante aqui e na cópia, com os
 * seus casos; o nome da constante é o código usado na API.
 */
public enum Unidade {
    SG(Grandeza.DENSIDADE, Conversao.CANONICA),
    PLATO(Grandeza.DENSIDADE, Conversao.ESCALA_DE_SACAROSE),
    /** Escala de sacarose, como o Plato. A leitura de refratômetro com álcool no mosto exige correção fora do catálogo. */
    BRIX(Grandeza.DENSIDADE, Conversao.ESCALA_DE_SACAROSE),
    /** Densidade absoluta, relativa à da água a 20 °C (0,9982067 g/cm³; Tanaka et al., 2001). */
    G_CM3(Grandeza.DENSIDADE, Conversao.linear(1 / 0.9982067)),

    CELSIUS(Grandeza.TEMPERATURA, Conversao.CANONICA),
    FAHRENHEIT(Grandeza.TEMPERATURA, Conversao.afim(1 / 1.8, 32)),

    BAR(Grandeza.PRESSAO, Conversao.CANONICA),
    /** Libra-força por polegada quadrada: 0,45359237 kg × 9,80665 m/s² / (0,0254 m)², em bar. */
    PSI(Grandeza.PRESSAO, Conversao.linear(0.45359237 * 9.80665 / (0.0254 * 0.0254) / 100_000)),
    KPA(Grandeza.PRESSAO, Conversao.linear(0.01)),

    L(Grandeza.VOLUME, Conversao.CANONICA),
    HL(Grandeza.VOLUME, Conversao.linear(100)),
    ML(Grandeza.VOLUME, Conversao.linear(0.001)),
    GAL_US(Grandeza.VOLUME, Conversao.linear(3.785411784)),
    GAL_UK(Grandeza.VOLUME, Conversao.linear(4.54609)),
    /** Barril de cerveja americano: 31 galões US. */
    BBL_US(Grandeza.VOLUME, Conversao.linear(31 * 3.785411784)),

    G(Grandeza.MASSA, Conversao.CANONICA),
    KG(Grandeza.MASSA, Conversao.linear(1000)),
    /** Onça avoirdupois: 1/16 de libra. */
    OZ(Grandeza.MASSA, Conversao.linear(453.59237 / 16)),
    LB(Grandeza.MASSA, Conversao.linear(453.59237));

    private final Grandeza grandeza;
    private final Conversao conversao;

    Unidade(Grandeza grandeza, Conversao conversao) {
        this.grandeza = grandeza;
        this.conversao = conversao;
    }

    public Grandeza grandeza() {
        return grandeza;
    }

    public boolean ehCanonica() {
        return conversao == Conversao.CANONICA;
    }

    public double paraCanonica(double valor) {
        return conversao.paraCanonica(valor);
    }

    /**
     * Converte passando pela canônica. Na mesma unidade devolve o valor sem conversão: as fórmulas de ida e volta da
     * escala de sacarose não são inversas exatas, e 12 °P não pode virar 12,007 °P.
     */
    public double converter(double valor, Unidade destino) {
        if (destino.grandeza != grandeza) {
            throw new IllegalArgumentException("Conversão entre grandezas diferentes: %s (%s) para %s (%s)"
                    .formatted(this, grandeza, destino, destino.grandeza));
        }
        if (destino == this) {
            return valor;
        }
        return destino.conversao.daCanonica(conversao.paraCanonica(valor));
    }
}
