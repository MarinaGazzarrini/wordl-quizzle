//classe di appoggio utilizzata per le traduzioni delle parole della sfida
public class trad {
	
	String nome;
	String traduzione="nessuna";//inizialmente non avrò traduzioni
	
	trad(String nome){
		
		this.nome=nome;
		
	}
	
	void addtrad(String s) {
		traduzione=s;
	}

}
