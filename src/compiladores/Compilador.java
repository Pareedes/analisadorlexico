package compiladores;

public class Compilador {

    public static void main(String[] args) {
        Sintatico sintatico = new Sintatico("test.pas");
        sintatico.analisar();
    }
}

