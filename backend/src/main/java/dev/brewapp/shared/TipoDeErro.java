package dev.brewapp.shared;

/**
 * Natureza de um erro de negócio, sem conhecer HTTP: a camada web traduz cada tipo para um status
 * (design da API, seção 1.6).
 */
public enum TipoDeErro {

    /** Recurso não existe ou pertence a outra organização (404). */
    NAO_ENCONTRADO,

    /** Conflito com o estado atual, como nome duplicado ou lote num estado que não aceita a operação (409). */
    CONFLITO,

    /** Requisição bem formada que viola uma regra de domínio (422). */
    REGRA_VIOLADA
}
