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

                    // --- AÇÕES A49 e A22 INÍCIO ---
                    // (Substituindo o antigo bloco A8)

                    // A49: Verificação semântica
                    if (!tabela.isPresent(variavel)) {
                        System.err.println("Variável " + variavel + " não foi declarada");
                        System.exit(-1);
                    } else {
                        registro = tabela.get(variavel);
                        if (registro.getCategoria() != Categoria.VARIAVEL) {
                            System.err.println("O identificador " + variavel + "não é uma variável. A49");
                            System.exit(-1);
                        }

                        // A22: Geração de código para atribuição
                        // (Só executa se A49 passou)
                        escreverCodigo("\tpop eax");
                        escreverCodigo("\tmov dword[ebp - " + registro.getOffset() + "], eax");
                    }
                    // --- AÇÕES A49 e A22 FIM ---

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
                if (token.getClasse() == ClasseToken.cId) {
                    String varControle = token.getValor().getTexto();
                    if (!tabela.isPresent(varControle)) {
                        System.err.println("Variável " + varControle + " não foi declarada");
                        System.exit(-1);
                    }
                    registro = tabela.get(varControle);
                    token = lexico.getNextToken();
                    if (token.getClasse() == ClasseToken.cAtrib) {
                        token = lexico.getNextToken();
                        expressao(); // valor inicial
                        // --- AÇÃO 11 ---
                        escreverCodigo("\tpop dword[ebp - " + registro.getOffset() + "]");
                        String rotuloEntrada = criarRotulo("FOR");
                        String rotuloSaida = criarRotulo("FIMFOR");
                        rotulo = rotuloEntrada;
                        // --- FIM AÇÃO 11 ---

                        if (ehPalavraReservada("to")) {
                            token = lexico.getNextToken();
                            expressao(); // valor final
                            // --- AÇÃO 12 ---
                            escreverCodigo("\tpush ecx");
                            escreverCodigo("\tmov ecx, dword[ebp - " + registro.getOffset() + "]");
                            escreverCodigo("\tcmp ecx, dword[esp+4]"); // +4 por causa do ecx
                            escreverCodigo("\tjg " + rotuloSaida);
                            escreverCodigo("\tpop ecx");
                            // --- FIM AÇÃO 12 ---
                            if (ehPalavraReservada("do")) {
                                token = lexico.getNextToken();
                                sentencas();
                                // --- AÇÃO 13 INÍCIO ---
                                escreverCodigo("\tadd dword[ebp - " + registro.getOffset() + "], 1");
                                escreverCodigo("\tjmp " + rotuloEntrada);
                                rotulo = rotuloSaida;
                                // --- AÇÃO 13 FIM ---
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
                // --- AÇÃO 16 INÍCIO ---
                String rotuloWhile = criarRotulo("While");
                String rotuloFim = criarRotulo("FimWhile");
                rotulo = rotuloWhile;
                // --- AÇÃO 16 FIM ---

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

                // --- AÇÃO 17 INÍCIO ---
                escreverCodigo("\tcmp dword[esp], 0");
                escreverCodigo("\tje " + rotuloFim);
                // --- AÇÃO 17 FIM ---

                if (ehPalavraReservada("do")) {
                    token = lexico.getNextToken();
                    sentencas();

                    // --- AÇÃO 18 INÍCIO ---
                    escreverCodigo("\tjmp " + rotuloWhile);
                    rotulo = rotuloFim;
                    // --- AÇÃO 18 FIM ---

                    continue;
                } else {
                    mostrarErro("Era esperado 'do' após while.");
                }
            } else if (ehPalavraReservada("if")) {
                token = lexico.getNextToken();
                expressao();

                // --- AÇÃO 19 INÍCIO ---
                rotuloElse = criarRotulo("Else");
                String rotuloFim = criarRotulo("FimIf");
                escreverCodigo("\tcmp dword[esp], 0\n");
                escreverCodigo("\tje " + rotuloElse);
                // --- AÇÃO 19 FIM ---

                if (ehPalavraReservada("then")) {
                    token = lexico.getNextToken();
                    sentencas(); // Bloco 'then'

                    if (ehPalavraReservada("else")) {
                        // --- AÇÃO 20 INÍCIO ---
                        escreverCodigo("\tjmp " + rotuloFim);
                        // --- AÇÃO 20 FIM ---

                        // --- AÇÃO 25 INÍCIO ---
                        // Define o rótulo do 'else'
                        rotulo = rotuloElse;
                        // --- AÇÃO 25 FIM ---

                        token = lexico.getNextToken();
                        sentencas(); // Bloco 'else'

                        // --- AÇÃO 21 INÍCIO ---
                        // Define o rótulo do 'fimif'
                        rotulo = rotuloFim;
                        // --- AÇÃO 21 FIM ---

                    } else {
                        // Se não houver 'else', A25 define a saída
                        // --- AÇÃO 25 INÍCIO ---
                        rotulo = rotuloElse;
                        // --- AÇÃO 25 FIM ---
                    }
                    continue;
                } else {
                    mostrarErro("Era esperado 'then' após if.");
                }
            } else if (ehPalavraReservada("repeat")) {
                token = lexico.getNextToken();
                // --- AÇÃO 14 INÍCIO ---
                String rotRepeat = criarRotulo("Repeat");
                rotulo = rotRepeat;
                // --- AÇÃO 14 FIM ---
                sentencas();
                if (ehPalavraReservada("until")) {
                    token = lexico.getNextToken();
                    expressao();

                    // --- AÇÃO 15 INÍCIO ---
                    escreverCodigo("\tcmp dword[esp], 0");
                    escreverCodigo("\tje " + rotRepeat);
                    // --- AÇÃO 15 FIM ---

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

    private void termo() {
        fator();
        // Loop para operadores de alta precedência: *, /, AND
        while (token.getClasse() == ClasseToken.cMultiplicacao ||
                token.getClasse() == ClasseToken.cDivisao ||
                ehPalavraReservada("and")) {

            ClasseToken operadorAri = null;
            boolean isAnd = false;

            if (ehPalavraReservada("and")) {
                isAnd = true;
            } else {
                operadorAri = token.getClasse(); // Salva o operador aritmético
            }

            token = lexico.getNextToken();
            fator();

            if (isAnd) {
                // --- AÇÃO 27 (AND) INÍCIO ---
                // Implementado de forma semelhante à Ação 26, conforme solicitado.
                String rotSaida = criarRotulo("SaidaMTL");
                String rotFalso = criarRotulo("FalsoMTL");

                escreverCodigo("\tcmp dword [ESP + 4], 1"); // Compara Op1
                escreverCodigo("\tjne " + rotFalso); // Se Op1 != 1 (false), resultado é false
                escreverCodigo("\tcmp dword [ESP], 1"); // Compara Op2
                escreverCodigo("\tjne " + rotFalso); // Se Op2 != 1 (false), resultado é false

                // Se ambos são 1 (true)
                escreverCodigo("\tmov dword [ESP + 4], 1"); // resultado é 1 (true)
                escreverCodigo("\tjmp " + rotSaida);

                // Se um deles é 0 (false)
                rotulo = rotFalso;
                escreverCodigo("\tmov dword [ESP + 4], 0"); // resultado é 0 (false)

                // Saída
                rotulo = rotSaida;
                escreverCodigo("\tadd esp, 4"); // Limpa o segundo operando da pilha
                // --- AÇÃO 27 (AND) FIM ---
            } else if (operadorAri == ClasseToken.cMultiplicacao) {
                // --- AÇÃO 39 INÍCIO ---
                escreverCodigo("\tpop eax");
                escreverCodigo("\timul eax, dword [ESP]");
                escreverCodigo("\tmov dword [ESP], eax");
                // --- AÇÃO 39 FIM ---
            } else if (operadorAri == ClasseToken.cDivisao) {
                // --- AÇÃO 40 INÍCIO ---
                escreverCodigo("\tpop ecx");
                escreverCodigo("\tpop eax");
                escreverCodigo("\tidiv ecx");
                escreverCodigo("\tpush eax");
                // --- AÇÃO 40 FIM ---
            }
        }
    }

    private void fator() {
        if (ehPalavraReservada("not")) {
            token = lexico.getNextToken();
            fator();
            // --- AÇÃO A28 INÍCIO ---
            String rotFalso = criarRotulo("FalsoFL");
            String rotSaida = criarRotulo("SaidaFL");
            escreverCodigo("\tcmp dword [ESP], 1");
            escreverCodigo("\tjne " + rotFalso);
            escreverCodigo("\tmov dword [ESP], 0");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotFalso;
            escreverCodigo("\tmov dword [ESP], 1");
            rotulo = rotSaida;
            // --- AÇÃO A28 FIM ---
        } else if (token.getClasse() == ClasseToken.cParEsq) {
            token = lexico.getNextToken();
            expressao();
            if (token.getClasse() == ClasseToken.cParDir) {
                token = lexico.getNextToken();
            } else {
                mostrarErro("Era esperado ')' após expressão entre parênteses.");
            }
        } else if (token.getClasse() == ClasseToken.cId) {
            // --- AÇÃO 57 INÍCIO ---
            String variavel = token.getValor().getTexto();
            if (!tabela.isPresent(variavel)) {
                System.err.println("Variável " + variavel + " não foi declarada");
                System.exit(-1);
            } else {
                registro = tabela.get(variavel);
                if (registro.getCategoria() != Categoria.VARIAVEL) {
                    System.err.println("O identificador " + variavel + "não é uma variável. A57");
                    System.exit(-1);
                }
                // --- AÇÃO 55 INÍCIO ---
                escreverCodigo("\tpush dword[ebp - " + registro.getOffset() + "]");
                // --- AÇÃO 55 FIM ---
            }
            // --- AÇÃO 57 FIM ---
            token = lexico.getNextToken();
        } else if (token.getClasse() == ClasseToken.cInt) {
            // --- AÇÃO 41 INÍCIO ---
            escreverCodigo("\tpush " + token.getValor().getInteiro());
            // --- AÇÃO 41 FIM ---
            token = lexico.getNextToken();
        } else if (ehPalavraReservada("true")) {
            // --- AÇÃO 29 INÍCIO ---
            escreverCodigo("\tpush 1");
            // --- AÇÃO 29 FIM ---
            token = lexico.getNextToken();
        } else if (ehPalavraReservada("false")) {
            // --- AÇÃO 30 INÍCIO ---
            escreverCodigo("\tpush 0");
            // --- AÇÃO 30 FIM ---
            token = lexico.getNextToken();
        } else {
            mostrarErro("Era esperado identificador, inteiro, 'true', 'false', 'not' ou '(' na expressão.");
        }
    }

    /**
     * NOVO MÉTODO (Nível 5 de Precedência: 'or')
     * Este é o novo ponto de entrada principal para qualquer expressão.
     * Gramática: <expressao> ::= <expressao_relacional> [ or <expressao_relacional>
     * ]*
     */
    private void expressao() {
        expressaoRelacional(); // Chama o nível de precedência abaixo

        // Loop para o operador lógico 'or' (precedência mais baixa)
        while (ehPalavraReservada("or")) {

            token = lexico.getNextToken();
            expressaoRelacional(); // Chama o nível relacional novamente

            // --- AÇÃO 26 (OR) INÍCIO ---
            String rotSaida = criarRotulo("SaidaMEL");
            String rotVerdade = criarRotulo("VerdadeMEL");
            escreverCodigo("\tcmp dword [ESP + 4], 1");
            escreverCodigo("\tje " + rotVerdade);
            escreverCodigo("\tcmp dword [ESP], 1");
            escreverCodigo("\tje " + rotVerdade);
            escreverCodigo("\tmov dword [ESP + 4], 0");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotVerdade;
            escreverCodigo("\tmov dword [ESP + 4], 1");
            rotulo = rotSaida;
            escreverCodigo("\tadd esp, 4");
            // --- AÇÃO 26 (OR) FIM ---
        }
    }

    /**
     * MÉTODO RENOMEADO (Nível 4 de Precedência: Operadores Relacionais)
     * Este é o seu antigo método relacao(), agora chamado expressaoRelacional()
     * e corrigido para chamar expressaoSimples().
     * Gramática: <expressao_relacional> ::= <expressao_simples> [ (< | > | = | ...)
     * <expressao_simples> ]?
     */
    private void expressaoRelacional() {
        expressaoSimples(); // MODIFICADO (era expressao())

        if (token.getClasse() == ClasseToken.cIgual) { // Check for '=' operator
            token = lexico.getNextToken();
            expressaoSimples(); // MODIFICADO (era expressao())
            // --- AÇÃO A31 INÍCIO ---
            String rotFalso = criarRotulo("FalsoREL");
            String rotSaida = criarRotulo("SaidaREL");
            escreverCodigo("\tpop eax");
            escreverCodigo("\tcmp dword [ESP], eax");
            escreverCodigo("\tjne " + rotFalso);
            escreverCodigo("\tmov dword [ESP], 1");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotFalso;
            escreverCodigo("\tmov dword [ESP], 0");
            rotulo = rotSaida;
            // --- AÇÃO A31 FIM ---
        } else if (token.getClasse() == ClasseToken.cMaior) { // Check for '>' operator
            token = lexico.getNextToken();
            expressaoSimples(); // MODIFICADO (era expressao())
            // --- AÇÃO A32 INÍCIO ---
            String rotFalso = criarRotulo("FalsoREL");
            String rotSaida = criarRotulo("SaidaREL");
            escreverCodigo("\tpop eax");
            escreverCodigo("\tcmp dword [ESP], eax");
            escreverCodigo("\tjle " + rotFalso); // Jump if less than or equal
            escreverCodigo("\tmov dword [ESP], 1");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotFalso;
            escreverCodigo("\tmov dword [ESP], 0");
            rotulo = rotSaida;
            // --- AÇÃO A32 FIM ---
        } else if (token.getClasse() == ClasseToken.cMaiorIgual) { // Check for '>=' operator
            token = lexico.getNextToken();
            expressaoSimples(); // MODIFICADO (era expressao())
            // --- AÇÃO A33 INÍCIO ---
            String rotFalso = criarRotulo("FalsoREL");
            String rotSaida = criarRotulo("SaidaREL");
            escreverCodigo("\tpop eax");
            escreverCodigo("\tcmp dword [ESP], eax");
            escreverCodigo("\tjl " + rotFalso); // Jump if less than
            escreverCodigo("\tmov dword [ESP], 1");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotFalso;
            escreverCodigo("\tmov dword [ESP], 0");
            rotulo = rotSaida;
            // --- AÇÃO A33 FIM ---
        } else if (token.getClasse() == ClasseToken.cMenor) { // Check for '<' operator
            token = lexico.getNextToken();
            expressaoSimples(); // MODIFICADO (era expressao())
            // --- AÇÃO A34 INÍCIO ---
            String rotFalso = criarRotulo("FalsoREL");
            String rotSaida = criarRotulo("SaidaREL");
            escreverCodigo("\tpop eax");
            escreverCodigo("\tcmp dword [ESP], eax");
            escreverCodigo("\tjge " + rotFalso); // Jump if greater than or equal
            escreverCodigo("\tmov dword [ESP], 1");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotFalso;
            escreverCodigo("\tmov dword [ESP], 0");
            rotulo = rotSaida;
            // --- AÇÃO A34 FIM ---
        } else if (token.getClasse() == ClasseToken.cMenorIgual) { // Check for '<=' operator
            token = lexico.getNextToken();
            expressaoSimples(); // MODIFICADO (era expressao())
            // --- AÇÃO A35 INÍCIO ---
            String rotFalso = criarRotulo("FalsoREL");
            String rotSaida = criarRotulo("SaidaREL");
            escreverCodigo("\tpop eax");
            escreverCodigo("\tcmp dword [ESP], eax");
            escreverCodigo("\tjg " + rotFalso);
            escreverCodigo("\tmov dword [ESP], 1");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotFalso;
            escreverCodigo("\tmov dword [ESP], 0");
            rotulo = rotSaida;
            // --- AÇÃO A35 FIM ---
        } else if (token.getClasse() == ClasseToken.cDiferente) { // Check for '<>' operator
            token = lexico.getNextToken();
            expressaoSimples(); // MODIFICADO (era expressao())
            // --- AÇÃO A36 INÍCIO ---
            String rotFalso = criarRotulo("FalsoREL");
            String rotSaida = criarRotulo("SaidaREL");
            escreverCodigo("\tpop eax");
            escreverCodigo("\tcmp dword [ESP], eax");
            escreverCodigo("\tje " + rotFalso);
            escreverCodigo("\tmov dword [ESP], 1");
            escreverCodigo("\tjmp " + rotSaida);
            rotulo = rotFalso;
            escreverCodigo("\tmov dword [ESP], 0");
            rotulo = rotSaida;
            // --- AÇÃO A36 FIM ---
        }
    }

    /**
     * MÉTODO RENOMEADO (Nível 3 de Precedência: '+' e '-')
     * Este é o seu antigo método expressao(), agora focado apenas em adição e
     * subtração.
     * Gramática: <expressao_simples> ::= <termo> [ (+ | -) <termo> ]*
     */
    private void expressaoSimples() {
        termo();
        // Loop para operadores aritméticos de baixa precedência: +, -
        while (token.getClasse() == ClasseToken.cAdicao ||
                token.getClasse() == ClasseToken.cSubtracao) {

            ClasseToken operadorAri = token.getClasse(); // Salva o operador aritmético

            token = lexico.getNextToken();
            termo();

            if (operadorAri == ClasseToken.cAdicao) {
                // --- AÇÃO 37 INÍCIO ---
                escreverCodigo("\tpop eax");
                escreverCodigo("\tadd dword[ESP], eax");
                // --- AÇÃO 37 FIM ---
            } else if (operadorAri == ClasseToken.cSubtracao) {
                // --- AÇÃO 38 (Subtração) INÍCIO ---
                escreverCodigo("\tpop eax");
                escreverCodigo("\tsub dword[ESP], eax");
                // --- AÇÃO 38 (Subtração) FIM ---
            }
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
