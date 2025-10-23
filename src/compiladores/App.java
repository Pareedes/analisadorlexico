package compiladores;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {

		String nomeArquivo = "teste.pas";
		String caminhoArquivo = Paths.get(nomeArquivo).toAbsolutePath().toString();
		int c;
		char caractere;

		try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo, StandardCharsets.UTF_8))) {

			while ((c = br.read()) != -1) { // -1 fim da stream
				caractere = (char) c;
				System.out.println(c);
				if (Character.isLetter(caractere)) {
					System.out.println("Letra: " + caractere);
				} else if (Character.isDigit(caractere)) {
					System.out.println("Dígito: " + caractere);
				} else if (Character.isWhitespace(caractere)) {
					System.out.println("Espaço");
				} else if (caractere == '.'){
					System.out.println("Ponto");
				} else {
					System.out.println("Outra coisa: " + caractere);
				}
			}

		} catch (IOException e) {
			System.err.println("Não foi possível abrir o arquivo ou ler do arquivo: " + nomeArquivo);
			e.printStackTrace();
		}

	}
}
