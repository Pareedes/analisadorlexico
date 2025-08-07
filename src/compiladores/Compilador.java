package compiladores;

public class Compilador {

    public static void main(String[] args) {
        Lexico l = new Lexico("test.pas");
        Token token = l.getNextToken();

        while(token.getClasse() != ClasseToken.cEOF) {
            System.out.println(token);
            token = l.getNextToken();
        }
    }
}

