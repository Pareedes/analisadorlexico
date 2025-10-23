package compiladores;

public enum Categoria {

    FUNCAO("Função"),
    PROCEDIMENTO("Procedimento"),
    PROGRAMA_PRINCIPAL("Programa Principal"),
    VARIAVEL("Variável"),
    PARAMETRO("Parâmetro"),
    TIPO("Tipo"),
    INDEFINIDA("Indefinida");

    private String descricao;

    private Categoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
