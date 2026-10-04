package dev.brewapp.shared.web;

/**
 * Códigos de erro que valem para qualquer endpoint e não dependem do domínio (design da API, 1.6,
 * tabela "Códigos genéricos"). Renomear uma constante muda o contrato com os clientes.
 */
public enum ErroGenerico {

    VALIDACAO_FALHOU("Falha de validação"),
    CORPO_MALFORMADO("Corpo da requisição malformado"),
    REQUISICAO_INVALIDA("Requisição inválida"),
    NAO_AUTENTICADO("Não autenticado"),
    ACESSO_NEGADO("Acesso negado"),
    ROTA_NAO_ENCONTRADA("Rota não encontrada"),
    METODO_NAO_SUPORTADO("Método não suportado nesta rota"),
    TIPO_DE_RESPOSTA_NAO_SUPORTADO("Accept não suportado"),
    TIPO_DE_CONTEUDO_NAO_SUPORTADO("Content-Type não suportado"),
    ERRO_INTERNO("Erro interno");

    private final String titulo;

    ErroGenerico(String titulo) {
        this.titulo = titulo;
    }

    /** Texto para leitura humana em logs; nunca exibido ao usuário. */
    public String titulo() {
        return titulo;
    }
}
