package compiladores;

public class Token {
    private ClasseToken classe;
    private ValorToken valor;
    private int linha;
    private int coluna;
    
    public Token(int linha2, int coluna2) {
        this.linha = linha2;
        this.coluna = coluna2;
    }
    
    public ClasseToken getClasse() {
        return classe;
    }
    public void setClasse(ClasseToken classe) {
        this.classe = classe;
    }
    public ValorToken getValor() {
        return valor;
    }
    public void setValor(ValorToken valor) {
        this.valor = valor;
    }
    public int getLinha() {
        return linha;
    }
    public void setLinha(int linha) {
        this.linha = linha;
    }
    public int getColuna() {
        return coluna;
    }
    public void setColuna(int coluna) {
        this.coluna = coluna;
    }
     @Override
    public String toString() {
        return "linha=" + linha + ", coluna=" + coluna + " - Token [classe=" + classe + ((valor != null) ? ", valor=" + valor : "") + "]";
    }

}
