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

    // <programa> ::= program id {A01} ; <corpo> . {A45}
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

    // <corpo> ::= <declara> <rotina> {A44} begin <sentencas> end {A46}
    private void corpo() {
        declara();
        rotina(); // Adiciona chamada para tratar procedure/function
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
        } else if (token.getClasse() == ClasseToken.cId) {
            mostrarErro("Era esperado a palavra reservada 'var' antes da declaração de variáveis.");
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

    private void dvar() {
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

    private void rotina() {
        while (ehPalavraReservada("procedure") || ehPalavraReservada("function")) {
            token = lexico.getNextToken();
            if (token.getClasse() == ClasseToken.cId) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cPontoVirgula) {
                    token = lexico.getNextToken();
                } else {
                    mostrarErro("Era esperado ';' após procedure/function.");
                }
            } else {
                mostrarErro("Era esperado identificador após procedure/function.");
            }
        }
    }

    private void sentencas() {
        while (true) {
            if (token.getClasse() == ClasseToken.cId) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cAtrib) {
                    token = lexico.getNextToken();
                    expressao();
                    if (token.getClasse() == ClasseToken.cPontoVirgula) {
                        token = lexico.getNextToken();
                        continue;
                    } else {
                        mostrarErro("Era esperado ';' após a atribuição.");
                    }
                } else {
                    mostrarErro("Era esperado ':=' após o identificador.");
                }
            } else if (ehPalavraReservada("begin")) {
                token = lexico.getNextToken();
                sentencas();
                if (ehPalavraReservada("end")) {
                    token = lexico.getNextToken();
                    if (token.getClasse() == ClasseToken.cPontoVirgula) {
                        token = lexico.getNextToken();
                    }
                    continue;
                } else {
                    mostrarErro("Era esperado 'end' para fechar bloco.");
                }
            } else if (ehPalavraReservada("read")) {
                token = lexico.getNextToken();
                // Consome parâmetros entre parênteses, se houver
                if (token.getClasse() == ClasseToken.cParEsq) {
                    token = lexico.getNextToken();
                    // Consome id ou int
                    if (token.getClasse() == ClasseToken.cId ||
                            token.getClasse() == ClasseToken.cInt) {
                        token = lexico.getNextToken();
                    }
                    // Consome ')'
                    if (token.getClasse() == ClasseToken.cParDir) {
                        token = lexico.getNextToken();
                    } else {
                        mostrarErro("Era esperado ')' após parâmetro de read.");
                    }
                }
                if (token.getClasse() == ClasseToken.cPontoVirgula) {
                    token = lexico.getNextToken();
                    continue;
                } else {
                    mostrarErro("Era esperado ';' após read.");
                }
            } else if (ehPalavraReservada("for")) {
                token = lexico.getNextToken();
                // Consome variável de controle
                if (token.getClasse() == ClasseToken.cId) {
                    token = lexico.getNextToken();
                    if (token.getClasse() == ClasseToken.cAtrib) {
                        token = lexico.getNextToken();
                        expressao(); // valor inicial
                        if (ehPalavraReservada("to")) {
                            token = lexico.getNextToken();
                            expressao(); // valor final
                            if (ehPalavraReservada("do")) {
                                token = lexico.getNextToken();
                                sentencas(); // bloco do for
                                continue;
                            } else {
                                mostrarErro("Era esperado 'do' após 'to' no for.");
                            }
                        } else {
                            mostrarErro("Era esperado 'to' após valor inicial no for.");
                        }
                    } else {
                        mostrarErro("Era esperado ':=' após variável de controle no for.");
                    }
                } else {
                    mostrarErro("Era esperado identificador após 'for'.");
                }
            } else if (ehPalavraReservada("while")) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cParEsq) {
                    token = lexico.getNextToken();
                    expressao();
                    if (token.getClasse() == ClasseToken.cParDir) {
                        token = lexico.getNextToken();
                    } else {
                        mostrarErro("Era esperado ')' após expressão do while.");
                    }
                } else {
                    expressao();
                }
                if (ehPalavraReservada("do")) {
                    token = lexico.getNextToken();
                    sentencas();
                    continue;
                } else {
                    mostrarErro("Era esperado 'do' após while.");
                }
            } else if (ehPalavraReservada("if")) {
                token = lexico.getNextToken();
                expressao();
                if (ehPalavraReservada("then")) {
                    token = lexico.getNextToken();
                    sentencas();
                    if (ehPalavraReservada("else")) {
                        token = lexico.getNextToken();
                        sentencas();
                    }
                    continue;
                } else {
                    mostrarErro("Era esperado 'then' após if.");
                }
            } else if (ehPalavraReservada("true") || ehPalavraReservada("false")) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cPontoVirgula) {
                    token = lexico.getNextToken();
                    continue;
                } else {
                    mostrarErro("Era esperado ';' após true/false.");
                }
            } else if (ehPalavraReservada("repeat")) {
                token = lexico.getNextToken();
                sentencas();
                if (ehPalavraReservada("until")) {
                    token = lexico.getNextToken();
                    expressao();
                    if (token.getClasse() == ClasseToken.cPontoVirgula) {
                        token = lexico.getNextToken();
                        continue;
                    } else {
                        mostrarErro("Era esperado ';' após until.");
                    }
                } else {
                    mostrarErro("Era esperado a palavra reservada 'until'");
                }
            } else if (ehPalavraReservada("write")) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cParEsq) {
                    token = lexico.getNextToken();
                    // Exige pelo menos um parâmetro
                    if (token.getClasse() == ClasseToken.cString ||
                            token.getClasse() == ClasseToken.cId ||
                            token.getClasse() == ClasseToken.cInt) {
                        token = lexico.getNextToken();
                    } else {
                        mostrarErro("write requer pelo menos um parâmetro.");
                    }
                    if (token.getClasse() == ClasseToken.cParDir) {
                        token = lexico.getNextToken();
                    } else {
                        mostrarErro("Era esperado ')' após parâmetro de write.");
                    }
                } else {
                    mostrarErro("Era esperado '(' após write.");
                }
                if (token.getClasse() == ClasseToken.cPontoVirgula) {
                    token = lexico.getNextToken();
                    continue;
                } else {
                    mostrarErro("Era esperado ';' após write.");
                }
            } else if (ehPalavraReservada("writeln")) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cParEsq) {
                    token = lexico.getNextToken();
                    // Exige pelo menos um parâmetro
                    if (token.getClasse() == ClasseToken.cString ||
                            token.getClasse() == ClasseToken.cId ||
                            token.getClasse() == ClasseToken.cInt) {
                        token = lexico.getNextToken();
                    } else {
                        mostrarErro("writeln requer pelo menos um parâmetro.");
                    }
                    if (token.getClasse() == ClasseToken.cParDir) {
                        token = lexico.getNextToken();
                    } else {
                        mostrarErro("Era esperado ')' após parâmetro de writeln.");
                    }
                } else {
                    mostrarErro("Era esperado '(' após writeln.");
                }
                if (token.getClasse() == ClasseToken.cPontoVirgula) {
                    token = lexico.getNextToken();
                    continue;
                } else {
                    mostrarErro("Era esperado ';' após writeln.");
                }
            } else if (ehPalavraReservada("end")) {
                break;
            } else {
                break;
            }
        }
    }

    private void expressao() {
        termo();
        while (ehPalavraReservada("or")) {
            token = lexico.getNextToken();
            termo();
        }
    }

    private void termo() {
        fator();
        while (ehPalavraReservada("and")) {
            token = lexico.getNextToken();
            fator();
        }
    }

    private void fator() {
        if (ehPalavraReservada("not")) {
            token = lexico.getNextToken();
            fator();
        } else if (token.getClasse() == ClasseToken.cParEsq) {
            token = lexico.getNextToken();
            expressao();
            if (token.getClasse() == ClasseToken.cParDir) {
                token = lexico.getNextToken();
            } else {
                mostrarErro("Era esperado ')' após expressão entre parênteses.");
            }
        } else if (token.getClasse() == ClasseToken.cId || token.getClasse() == ClasseToken.cInt) {
            token = lexico.getNextToken();
            // ERRO: dois operandos seguidos sem operador
            if (token.getClasse() == ClasseToken.cId || token.getClasse() == ClasseToken.cInt) {
                mostrarErro("Era esperado um operador entre os operandos.");
            }
            // Operadores relacionais
            while (token.getClasse() == ClasseToken.cMaior ||
                    token.getClasse() == ClasseToken.cMenor ||
                    token.getClasse() == ClasseToken.cMaiorIgual ||
                    token.getClasse() == ClasseToken.cMenorIgual ||
                    token.getClasse() == ClasseToken.cIgual ||
                    token.getClasse() == ClasseToken.cDiferente) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cId || token.getClasse() == ClasseToken.cInt) {
                    token = lexico.getNextToken();
                    // ERRO: dois operandos seguidos sem operador
                    if (token.getClasse() == ClasseToken.cId || token.getClasse() == ClasseToken.cInt) {
                        mostrarErro("Era esperado um operador entre os operandos.");
                    }
                } else {
                    mostrarErro("Era esperado identificador ou inteiro após operador relacional.");
                }
            }
            // Operadores aritméticos
            while (token.getClasse() == ClasseToken.cAdicao ||
                    token.getClasse() == ClasseToken.cSubtracao ||
                    token.getClasse() == ClasseToken.cMultiplicacao ||
                    token.getClasse() == ClasseToken.cDivisao) {
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cId || token.getClasse() == ClasseToken.cInt) {
                    token = lexico.getNextToken();
                    // ERRO: dois operandos seguidos sem operador
                    if (token.getClasse() == ClasseToken.cId || token.getClasse() == ClasseToken.cInt) {
                        mostrarErro("Era esperado um operador entre os operandos.");
                    }
                } else {
                    mostrarErro("Era esperado identificador ou inteiro após operador aritmético.");
                }
            }
        } else {
            mostrarErro("Era esperado identificador, inteiro ou operador lógico na expressão.");
        }
    }

    private void mostrarErro(String erro) {
        System.out.println("[" + token.getLinha() + "," + token.getColuna() + "] - Erro sintático. " + erro);
        System.exit(-1);
    }

    private boolean ehPalavraReservada(String palavra) {
        return token.getClasse() == ClasseToken.cPalRes &&
                token.getValor().getTexto().equalsIgnoreCase(palavra);
    }
}
