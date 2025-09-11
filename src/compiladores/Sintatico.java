package compiladores;

public class Sintatico {

    private Token token;
    private Lexico lexico;

    public Sintatico(String nomeArquivo) {
        lexico = new Lexico(nomeArquivo);
        token = lexico.getNextToken();  
    }

    public void analisar() {
        programa();
    }

    //<programa> ::= program id {A01} ; <corpo> . {A45}
    private void programa() {
        if (ehPalavraReservada("program")) {
            token = lexico.getNextToken();
            if (token.getClasse() == ClasseToken.cId) {
                token = lexico.getNextToken();
                // {A01}
                if (token.getClasse() == ClasseToken.cPontoVirgula) {
                    token = lexico.getNextToken();
                    corpo();
                    if (token.getClasse() == ClasseToken.cPonto) {
                        token = lexico.getNextToken();
                        // {A45}
                    } else {
                        mostrarErro("Era esperado um '.'");
                    }
                } else {
                    mostrarErro("Era esperado um ';'");
                }
            } else {
                mostrarErro("Era esperado nome do programa");
            }
        } else {
            mostrarErro("Era esperado a palavra reservada 'program'");
        }
    }

    private void corpo() {

    }

    private void mostrarErro (String erro) {
        System.out.println("[" + token.getLinha() + "," + token.getColuna() + "] - Erro sintático. " + erro);
        System.exit(-1);
    }

    private boolean ehPalavraReservada(String palavra) {
        return token.getClasse() == ClasseToken.cPalRes &&
               token.getValor().getTexto().equalsIgnoreCase(palavra);
    }
}
