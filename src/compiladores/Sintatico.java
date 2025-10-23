package compiladores;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Sintatico {

    private Token token;
    private Lexico lexico;
    private TabelaSimbolos tabela = new TabelaSimbolos();
    private String rotulo = "";
    private String nomeArquivo; // Adicione esta linha
    private int contRotulo = 1;
    private int offsetVariavel = 0;
    private String nomeArquivoSaida;
    private String caminhoArquivoSaida;
    private BufferedWriter bw;
    private FileWriter fw;
    private static final int TAMANHO_INTEIRO = 4;
    private List<String> variaveis = new ArrayList<>();
    private List<String> sectionData = new ArrayList<>();
    private Registro registro;
    private String rotuloElse;

    public Sintatico(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
        lexico = new Lexico(nomeArquivo);
        token = lexico.getNextToken();

        nomeArquivoSaida = "queronemver.asm";
        caminhoArquivoSaida = Paths.get(nomeArquivoSaida).toAbsolutePath().toString();
        bw = null;
        fw = null;
        try {
            fw = new FileWriter(caminhoArquivoSaida, Charset.forName("UTF-8"));
            bw = new BufferedWriter(fw);
        } catch (Exception e) {
            System.err.println("Erro ao criar arquivo de saída");
        }
    }

    private void escreverCodigo(String instrucoes) {
        try {
            if (rotulo.isEmpty()) {
                bw.write(instrucoes + "\n");
            } else {
                bw.write(rotulo + ": " + instrucoes + "\n");
                rotulo = "";
            }
        } catch (IOException e) {
            System.err.println("Erro escrevendo no arquivo de saída");
        }
    }

    private String criarRotulo(String texto) {
        String retorno = "rotulo" + texto + contRotulo;
        contRotulo++;
        return retorno;
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
                Registro registro = tabela.add(token.getValor().getTexto());
                offsetVariavel = 0;
                registro.setCategoria(Categoria.PROGRAMA_PRINCIPAL);
                escreverCodigo("global main");
                escreverCodigo("extern printf");
                escreverCodigo("extern scanf\n");
                escreverCodigo("section .text");
                rotulo = "main";
                escreverCodigo("\t; Entrada do programa");
                escreverCodigo("\tpush ebp");
                escreverCodigo("\tmov ebp, esp");

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
            // AÇÃO A03: atribuir tipo e registrar variáveis na tabela de símbolos
            for (String nomeVar : variaveis) {
                Registro regVar = tabela.add(nomeVar);
                regVar.setCategoria(Categoria.VARIAVEL);
                regVar.setTipo(Tipo.INTEGER);
                regVar.setOffset(offsetVariavel);
                offsetVariavel += TAMANHO_INTEIRO;
            }
            variaveis.clear(); // Limpa a lista para próxima declaração
            token = lexico.getNextToken();
        } else {
            mostrarErro("Era esperado a palavra reservada 'integer' para o tipo da variável.");
        }
    }

    private void variaveis() {
        if (token.getClasse() == ClasseToken.cId) {
            int tamanho = 0;
            for (String var : variaveis) {
                tabela.get(var).setTipo(Tipo.INTEGER);
                tamanho += TAMANHO_INTEIRO;
            }
            escreverCodigo("\tsub esp, " + tamanho);
            variaveis.clear();
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
                // Captura o nome da variável antes de avançar o token
                String variavel = token.getValor().getTexto();
                token = lexico.getNextToken();
                if (token.getClasse() == ClasseToken.cAtrib) {
                    token = lexico.getNextToken();
                    expressao();
                    // --- AÇÃO A8 INÍCIO ---
                    if (!tabela.isPresent(variavel)) {
                        System.err.println("Variável " + variavel + " não foi declarada");
                        System.exit(-1);
                    } else {
                        Registro registro = tabela.get(variavel);
                        if (registro.getCategoria() != Categoria.VARIAVEL) {
                            System.err.println("Identificador " + variavel + " não é uma variável");
                            System.exit(-1);
                        } else {
                            escreverCodigo("\tmov edx, ebp");
                            escreverCodigo("\tlea eax, [edx - " + registro.getOffset() + "]");
                            escreverCodigo("\tpush eax");
                            escreverCodigo("\tpush @Integer");
                            escreverCodigo("\tcall scanf");
                            escreverCodigo("\tadd esp, 8");
                            if (!sectionData.contains("@Integer: db '%d',0")) {
                                sectionData.add("@Integer: db '%d',0");
                            }
                        }
                    }
                    // --- AÇÃO A8 FIM ---
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
                    if (token.getClasse() == ClasseToken.cId) {
                        // --- AÇÃO 09 INÍCIO ---
                        String variavel = token.getValor().getTexto();
                        if (!tabela.isPresent(variavel)) {
                            System.err.println("Variável " + variavel + " não foi declarada");
                            System.exit(-1);
                        } else {
                            Registro registro = tabela.get(variavel);
                            if (registro.getCategoria() != Categoria.VARIAVEL) {
                                System.err.println("Identificador " + variavel + " não é uma variável");
                                System.exit(-1);
                            } else {
                                escreverCodigo("\tpush dword[ebp - " + registro.getOffset() + "]");
                                escreverCodigo("\tpush @Integer");
                                escreverCodigo("\tcall printf");
                                escreverCodigo("\tadd esp, 8");
                                if (!sectionData.contains("@Integer: db '%d',0")) {
                                    sectionData.add("@Integer: db '%d',0");
                                }
                            }
                        }
                        // --- AÇÃO 09 FIM ---
                        token = lexico.getNextToken();
                    } else if (token.getClasse() == ClasseToken.cString) {
                        // --- AÇÃO 59 INÍCIO ---
                        String string = token.getValor().getTexto();
                        String rotuloString = criarRotulo("String");
                        escreverCodigo("\tpush " + rotuloString);
                        escreverCodigo("\tcall printf");
                        escreverCodigo("\tadd esp, 4");
                        sectionData.add(rotuloString + ": db '" + string + "', 0");
                        // --- AÇÃO 59 FIM ---
                        token = lexico.getNextToken();
                    } else if (token.getClasse() == ClasseToken.cInt) {
                        // --- AÇÃO 43 INÍCIO ---
                        escreverCodigo("\tpush " + token.getValor().getInteiro());
                        escreverCodigo("\tpush @Integer");
                        escreverCodigo("\tcall printf");
                        escreverCodigo("\tadd esp, 8");
                        if (!sectionData.contains("@Integer: db '%d',0")) {
                            sectionData.add("@Integer: db '%d',0");
                        }
                        // --- AÇÃO 43 FIM ---
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
                    if (token.getClasse() == ClasseToken.cString) {
                        // --- AÇÃO 59 INÍCIO ---
                        String string = token.getValor().getTexto();
                        String rotuloString = criarRotulo("String");
                        escreverCodigo("\tpush " + rotuloString);
                        escreverCodigo("\tcall printf");
                        escreverCodigo("\tadd esp, 4");
                        sectionData.add(rotuloString + ": db '" + string + "', 10, 0");
                        // --- AÇÃO 59 FIM ---
                        token = lexico.getNextToken();
                    } else if (token.getClasse() == ClasseToken.cId) {
                        // --- AÇÃO 09 INÍCIO ---
                        String variavel = token.getValor().getTexto();
                        if (!tabela.isPresent(variavel)) {
                            System.err.println("Variável " + variavel + " não foi declarada");
                            System.exit(-1);
                        } else {
                            Registro registro = tabela.get(variavel);
                            if (registro.getCategoria() != Categoria.VARIAVEL) {
                                System.err.println("Identificador " + variavel + " não é uma variável");
                                System.exit(-1);
                            } else {
                                escreverCodigo("\tpush dword[ebp - " + registro.getOffset() + "]");
                                escreverCodigo("\tpush @Integer");
                                escreverCodigo("\tcall printf");
                                escreverCodigo("\tadd esp, 8");
                                if (!sectionData.contains("@Integer: db '%d',0")) {
                                    sectionData.add("@Integer: db '%d',0");
                                }
                            }
                        }
                        // --- AÇÃO 09 FIM ---
                        token = lexico.getNextToken();
                    } else if (token.getClasse() == ClasseToken.cInt) {
                        // --- AÇÃO 43 INÍCIO ---
                        escreverCodigo("\tpush " + token.getValor().getInteiro());
                        escreverCodigo("\tpush @Integer");
                        escreverCodigo("\tcall printf");
                        escreverCodigo("\tadd esp, 8");
                        if (!sectionData.contains("@Integer: db '%d',0")) {
                            sectionData.add("@Integer: db '%d',0");
                        }
                        // --- AÇÃO 43 FIM ---
                        token = lexico.getNextToken();
                    } else {
                        mostrarErro("writeln requer pelo menos um parâmetro.");
                    }
                    if (token.getClasse() == ClasseToken.cParDir) {
                        // --- AÇÃO 61 INÍCIO ---
                        String novaLinha = "rotuloStringLN: db '',10,0";
                        if (!sectionData.contains(novaLinha)) {
                            sectionData.add(novaLinha);
                        }
                        escreverCodigo("\tpush rotuloStringLN");
                        escreverCodigo("\tcall printf");
                        escreverCodigo("\tadd esp, 4");
                        // --- AÇÃO 61 FIM --
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
