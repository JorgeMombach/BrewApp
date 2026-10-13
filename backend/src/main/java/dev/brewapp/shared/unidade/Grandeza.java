package dev.brewapp.shared.unidade;

import java.util.Arrays;

/** Grandezas do catálogo de unidades (design da API, seção 1.11). Cada uma tem uma única unidade canônica. */
public enum Grandeza {
    DENSIDADE,
    TEMPERATURA,
    PRESSAO,
    VOLUME,
    MASSA;

    /** A unidade em que o valor canônico é guardado e comparado. */
    public Unidade unidadeCanonica() {
        return Arrays.stream(Unidade.values())
                .filter(unidade -> unidade.grandeza() == this && unidade.ehCanonica())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Grandeza sem unidade canônica no catálogo: " + this));
    }
}
