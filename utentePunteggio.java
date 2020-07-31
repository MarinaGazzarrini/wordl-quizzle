import org.json.simple.JSONObject;

//classe utilizzata per facilitare la creazione della classifica degli utenti
public  class utentePunteggio implements Comparable<utentePunteggio> {
	String utente;
	int punteggio;

	
	public utentePunteggio(String utente,int punteggio) {
		this.utente=utente;
		this.punteggio=punteggio;
		
	}

	
	int restituisciPunteggio(){
		return punteggio;
	}
	
	String restituisciUtente() {
		return utente;
	}

	@Override
	public int compareTo(utentePunteggio o) {
		//ordino in modo decrescente
		if(punteggio < o.restituisciPunteggio()) return 1;
		else return -1;
	}
	

	@SuppressWarnings("unchecked")
	public JSONObject jasonUP() {
		JSONObject oj= new JSONObject();//nuovo ogg json
		oj.put("Nome", utente);
		oj.put("Punteggio", punteggio);
		return oj;
	}
}

