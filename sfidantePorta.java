//classe di appoggio utilizzata per gestire la comunicazione udp lato client

public class sfidantePorta {
	String nome; //nome di chi ha richiesto la sfida
	int porta; //porta da cui proviene il mex
	long tempoArrivo; //tempo di arrivo del messaggio
	
	sfidantePorta(String nome, int porta, long tempoArrivo){
		
		this.nome=nome;
		this.porta=porta;
		this.tempoArrivo=tempoArrivo;
		
	}
}
