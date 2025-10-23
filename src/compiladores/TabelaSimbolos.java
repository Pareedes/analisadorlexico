package compiladores;

import java.util.HashMap;

public class TabelaSimbolos {
    
    private HashMap<String, Registro> simbolos = new HashMap<>();

    public boolean isPresent(String nome) {
        return simbolos.containsKey(nome);
    }

    public Registro add(String nome) {
        if (isPresent(nome)) {
            return simbolos.get(nome);
        }
        Registro novo = new Registro();
		novo.setLexema(nome);
		simbolos.put(nome, novo);
		return novo;
    }

    public Registro getRegistro(String nome) {
        return simbolos.get(nome);
    }

    public void delete(String nome) {
        simbolos.remove(nome);
    }

    public Registro get(String nome) {
        // Busca pelo registro usando os valores do HashMap
        for (Registro r : simbolos.values()) {
            if (r.getLexema().equals(nome)) {
                return r;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String result = "";
		for (String chave : simbolos.keySet()) {
			result += chave + "-> " + simbolos.get(chave) + "\n";
		}
		return result;
    }

}

