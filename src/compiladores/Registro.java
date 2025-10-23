package compiladores;

public class Registro {

    private String lexema;
    private Categoria categoria = Categoria.INDEFINIDA;
    private int offset;
    private Tipo tipo = Tipo.INDEFINIDO;

    public String getLexema() {
        return lexema;
    }

    public void setLexema(String lexema) {
        this.lexema = lexema;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "lexema: " + lexema + "\ncategoria: " + categoria + "\noffset: " + offset + "\ntipo: " + tipo;
    }
    
}
