package compiladores;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Lexico {
    private String nomeArquivo;
    private BufferedReader br;
    private char caractere;
    private List<String> palavrasReservadas;
    private int linha;
    private int coluna;

    public Lexico(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
        String caminhoArquivo = Paths.get(nomeArquivo).toAbsolutePath().toString();
        try {
            br = new BufferedReader(new FileReader(caminhoArquivo, StandardCharsets.UTF_8));
            caractere = (char) br.read();
        } catch (IOException ex) {
            System.out.println("Erro ao abrir o arquivo: " + nomeArquivo);
            System.out.println("Caminho do arquivo: " + caminhoArquivo);
        }
        palavrasReservadas = Arrays.asList("program", "begin", "end", "var", "integer", "procedure",
              "function", "read", "write", "writeln", "for", "do", "repeat",
              "until", "while", "if", "then", "else", "or", "and", "not",
              "true", "false");

        linha = 1;
        coluna = 1;
    }

    public Token getNextToken() {
        
        StringBuilder lexema;
        Token token;

        try {

			while (caractere != 65535) { // -1 fim da stream
                lexema = new StringBuilder();
                token = new Token(linha, coluna);
				if (Character.isLetter(caractere)) {
					while (Character.isLetter(caractere) || Character.isDigit(caractere)) {
                        lexema.append(caractere);
                        caractere = (char) br.read();
                         coluna++;
                    }
                    if (palavrasReservadas.contains(lexema.toString().toLowerCase())) {
                        token.setClasse(ClasseToken.cPalRes);
                    } else {
                        token.setClasse(ClasseToken.cId);
                    }
                    token.setValor(new ValorToken(lexema.toString()));
                    return token;

				} else if (Character.isDigit(caractere)) {
                    while (Character.isDigit(caractere)) {
                        lexema.append(caractere);
                        caractere = (char) br.read();
                        coluna++;
                    }
                    token.setClasse(ClasseToken.cInt);
                    token.setValor(new ValorToken(Integer.parseInt(lexema.toString())));
                    return token;

				} else if (caractere == ' ' || caractere == '\t'){ 
                    while (caractere == ' ' || caractere == '\t') {
                        caractere = (char) br.read();
                        coluna++;
                    }

				} else if (Character.isWhitespace(caractere)) {
                    while (Character.isWhitespace(caractere)) {
                        if (caractere == '\n') {
                            linha++;
                            coluna = 0;
                        }
                        caractere = (char) br.read();
                        coluna++;
                    }
                } else if (caractere == ';'){
                    token.setClasse(ClasseToken.cPontoVirgula);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

                } else if (caractere == ','){
                    token.setClasse(ClasseToken.cVirgula);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

                } else if (caractere == '(') {
                    token.setClasse(ClasseToken.cParEsq);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

                } else if (caractere == ')') {
                    token.setClasse(ClasseToken.cParDir);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

                } else if (caractere == '*') {
                    token.setClasse(ClasseToken.cMultiplicacao);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

                } else if (caractere == '+') {
                    token.setClasse(ClasseToken.cAdicao);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

                } else if (caractere == '-') {
                    token.setClasse(ClasseToken.cSubtracao);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

                } else if (caractere == '.') {
                    token.setClasse(ClasseToken.cPonto);
                    caractere = (char) br.read();
                    coluna++;
                    return token;

				}  else if (caractere == ':') {
                    token.setClasse(ClasseToken.cDoisPontos);
                    caractere = (char) br.read();
                    coluna++;
                    if (caractere == '=') {
                        token.setClasse(ClasseToken.cAtrib);
                        caractere = (char) br.read();
                        coluna++;
                    } 
                    return token;

                // STRING
                }else if (caractere == '\'') {
                    caractere = (char) br.read();
                    coluna++;
                    while (caractere != '\'' && caractere != 65535 && caractere != '\n') {
                        lexema.append(caractere);
                        caractere = (char) br.read();
                        coluna++;
                    }
                    if (caractere == '\'') {
                        caractere = (char) br.read();
                        coluna++;
                        token.setClasse(ClasseToken.cString);
                        token.setValor(new ValorToken(lexema.toString()));
                        return token;
                    }
                    else {
                        System.err.println("String não fechada corretamente na linha " + linha + ", coluna " + coluna);
                        System.exit(-1);
                    }

                // COMENTARIO
                } else if (caractere == '{') {
                    caractere = (char) br.read();
                    coluna++;
                    while (caractere != '}' && caractere != 65535) {
                        if (caractere == '\n') {
                            linha++;
                            coluna = 0;
                        }
                        caractere = (char) br.read();
                        coluna++;
                        if (caractere == 65535) {
                            System.err.println("Comentário não finalizado antes do fim do arquivo na linha " + linha + ", coluna " + coluna);
                            System.exit(-1);
                        }
                    }
                    if (caractere == '}') {
                        caractere = (char) br.read();
                        coluna++;
                    }

                } else if (caractere == '>'){
                    token.setClasse(ClasseToken.cMaior);
                    caractere = (char) br.read();
                    coluna++;
                    if (caractere == '=') {
                        token.setClasse(ClasseToken.cMaiorIgual);
                        caractere = (char) br.read();
                        coluna++;
                    }
                    return token;

				} else if (caractere == '<'){
                    token.setClasse(ClasseToken.cMaior);
                    caractere = (char) br.read();
                    coluna++;
                    if (caractere == '=') {
                        token.setClasse(ClasseToken.cMenorIgual);
                        caractere = (char) br.read();
                        coluna++;
                    }
                    return token;

				} 
                    else {
                    System.err.println("Caractere inválido: " + caractere);
                    System.exit(-1);
				}
			}
            token = new Token(linha, coluna);
            token.setClasse(ClasseToken.cEOF);
            return token;
            

		} catch (IOException e) {
			System.err.println("Não foi possível abrir o arquivo ou ler do arquivo: " + nomeArquivo);
			e.printStackTrace();
		}
        return null; // Retorna null se não houver mais tokens
    }
}