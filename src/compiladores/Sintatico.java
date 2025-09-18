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

    //<corpo> ::= <declara> <rotina> {A44} begin <sentencas> end {A46}
    private void corpo() {
        declara();
        //rotina();
        // {A44}
        if (ehPalavraReservada("begin")) {
            token = lexico.getNextToken();
            sentencas();
            if (ehPalavraReservada("end")) {
                token = lexico.getNextToken();
                // {A46}
            } else {
                mostrarErro("Era esperado a palavra reservada 'end'");
            }
        } else {
            mostrarErro("Era esperado a palavra reservada 'begin'");
        }
    }

    private void declara() {
        if (ehPalavraReservada("var")) {
            token = lexico.getNextToken();
            dvar();
            mais_dc();
        }
    }

    // <mais_dc> ::= ; <cont_dc>
    private void mais_dc() {
        if (token.getClasse() == ClasseToken.cPontoVirgula) {
            token = lexico.getNextToken();
            cont_dc();
        } else {
            mostrarErro("Era esperado um ';' no final das declarações Var.");
        }

    }

    private void cont_dc() {
        if (token.getClasse() == ClasseToken.cId) {
            dvar();
            mais_dc();
        }
    }

    
    private void dvar(){
        variaveis();
        if (token.getClasse() == ClasseToken.cDoisPontos) {
            token = lexico.getNextToken();
            tipo_var();
        } else {
            mostrarErro("Era esperado ':' após o nome da variável.");
        }
    }

    private void tipo_var() {
        if (ehPalavraReservada("integer")) {
            token = lexico.getNextToken();
        } else {
            mostrarErro("Era esperado a palavra reservada 'integer' para o tipo da variável.");
        }
    }

    private void variaveis() {
        if (token.getClasse() == ClasseToken.cId) {
            token = lexico.getNextToken();
            mais_var();
        } else {
            mostrarErro("Era esperado o ID na declaração de variáveis.");
        }
    }

    private void mais_var() {
        if (token.getClasse() == ClasseToken.cVirgula) {
            token = lexico.getNextToken();
            variaveis();
        }
    }

    private void sentencas() {

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
