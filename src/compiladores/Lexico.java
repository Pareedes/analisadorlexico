package compiladores;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Lexico {
    private String nomeArquivo;
    private BufferedReader br;
    private char caractere;

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
    }

    public Token getNextToken() {
        
        StringBuilder lexema;
        Token token;

        try {

			while (caractere != 65535) { // -1 fim da stream
                lexema = new StringBuilder();
                token = new Token();
				if (Character.isLetter(caractere)) {
					while (Character.isLetter(caractere) || Character.isDigit(caractere)) {
                        lexema.append(caractere);
                        caractere = (char) br.read();
                    }
                    token.setClasse(ClasseToken.cId);
                    token.setValor(new ValorToken(lexema.toString()));
                    return token;

				} else if (Character.isDigit(caractere)) {
                    while (Character.isDigit(caractere)) {
                        lexema.append(caractere);
                        caractere = (char) br.read();
                    }
                    token.setClasse(ClasseToken.cInt);
                    token.setValor(new ValorToken(Integer.parseInt(lexema.toString())));
                    return token;

				} else if (Character.isWhitespace(caractere)) {
                    while (Character.isWhitespace(caractere)) {
                        caractere = (char) br.read();
                    }
				} else if (caractere == ';'){
                    token.setClasse(ClasseToken.cPontoVirgula);
                    caractere = (char) br.read();
                    return token;
				} else {
                    System.err.println("Caractere inválido: " + caractere);
                    System.exit(-1);
				}
			}
            token = new Token();
            token.setClasse(ClasseToken.cEOF);
            return token;

		} catch (IOException e) {
			System.err.println("Não foi possível abrir o arquivo ou ler do arquivo: " + nomeArquivo);
			e.printStackTrace();
		}
        return null; // Retorna null se não houver mais tokens
    }
}