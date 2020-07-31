import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


//classe che contiene tutte le info relative all'utente
public class Utente implements Serializable {

	private static final long serialVersionUID = 1L;//default
	private final String nomeUtente;
	private final String password;
	private  LinkedList<String> amici;//lista degli amici
	int punteggio;//punteggio totale ottenuto
	private int online;//mi dice se in questo momento l'utente è online
	private int inSfida;//mi dice se in questo momento sta giocando
	Socket fd; //fd tcp
	
	//info necessarie per la sfida
	private String[] parole=new String[10];//array che contiene le parole da tradurre con le rispettive traduzioni
	int par=0;//per sapere se è tutto pronto per la sfida
	String sfidante;//significativo solo se l'utente è in sfida
	int punteggioSfida=0;//punteggio sfida in corso
	
	public Utente(String name, String pw) {//costruttore
		nomeUtente=name;
		password=pw;
		amici=new LinkedList<String>();
		punteggio=0;
		inSfida=0;
		online=0;
	}
	
	//aggiorna/restituisce il punteggio della sfida corrente
	synchronized int puntiSfida(int y) {
		if(y==2)punteggioSfida=punteggioSfida-1;
		else punteggioSfida=punteggioSfida+y;
		return punteggioSfida;
	}
	
	
	//restiruisce il nome dello sfidante
	synchronized String nomeSfidante() {
		return sfidante;
	}
	
	//metodo per inserire le parole nell'array parole
	synchronized void inserisciParole(int i, String italiano, String inglese) {
		parole[i]=italiano;
		parole[i+1]=inglese;
	}
	
	//restituisce la parola di indice i
	synchronized String leggiParola(int i) {
		return parole[i];
	}
	
	//svuota l'array parole
	synchronized void pulisciParole() {
		for(int i=0; i<10; i++)parole[i]=null;
	}
	
	//metodo per restituire il nome dell'utente
	synchronized String restituisciNome() {
		return nomeUtente;
	}
	
	
	//metodo per restituire la password
	synchronized String restituisciPassword() {
		return password;
	}
	
	//metodo per modificare il punteggio e restituirlo
	synchronized int aggiornaPunteggio(int x) {
		punteggio=punteggio+x;
		return punteggio;
	}
	
	//aggiungo fd
	synchronized void addfd(Socket fdk) {
		fd=fdk;
	}
	
	
	//restituisco fd
	synchronized Socket retfd() {
		return fd;
	}
	
	
	//metodo per aggiungere un amico e restituire la lista degli amici
	synchronized LinkedList<String>  aggiungiAmico(String a){
		if(a!=null) {
			if(amici==null)amici=new LinkedList<String>();
			amici.add(a);
		}
		 return amici;
	}
	
	
	//mi dice se a è presente nella lista di amici dell'utente
	synchronized int amico(String a) {
		int pres=0;
		for(String b: amici) {
			if (b.equals(a))pres=1;
			}
		return pres;
	}
	
	
	//metodo per vedere se l'utente è online
	synchronized int infoOnline() {
		return online;
	}
	
	//metodo per vedere se l'utente è in sfida
	synchronized int infoSfida() {
		return inSfida;
	}
	
	//metodo per passare offline
	synchronized int offline() {
		if(online==1) {
			online=0;
			return 0;//è andato tutto ok
		}
		else return 1;//era gà offline
		
	}
	
	//metodo per passare online
	synchronized int notOffline() {
		if(online==0) {
			online=1;
			return 0;
		}else return 1;
	}
	
	//setto l'inizio della sfida
	synchronized void inizioSfida() {
			inSfida=1;
		
	}
	
	
	//setto la fine della sfida
	synchronized void fineSfida() {
			inSfida=0;
		
	}
	
	
	
	//da java a json
	@SuppressWarnings("unchecked")
	
	synchronized public JSONObject tojs() {
		JSONObject oj= new JSONObject();//nuovo ogg json
		oj.put("Nome", nomeUtente);
		oj.put("Password", password);
		oj.put("Punteggio", punteggio);
		if(amici != null) {
			JSONArray am =new JSONArray();//array di oggetti json che conterrà la lista degli amici
				for(String s: amici) {//inserisco gli amici
					JSONObject obj= new JSONObject();//creo un oggetto jason
					obj.put("Amico",s);
					am.add(obj);//aggiungo all'array json
				}
			oj.put("Amici", am);
		}
		return oj; //mi restituisce un oggetto json che corrisponde all'utente
	}
	
	
	
	//da json a java
	synchronized public static Utente toja(JSONObject joC) {
		JSONArray am=(JSONArray)joC.get("Amici");
		String NameU =(String) joC.get("Nome");
		String pw=(String)joC.get("Password");
		long p=(long) joC.get("Punteggio");
		Utente  u = new Utente(NameU,pw);
		u.aggiornaPunteggio((int)p);
		if(am!=null) {
			int dim=am.size();
			for(int i=0; i <dim; i++) {
				JSONObject job=(JSONObject)am.get(i);
				String a= (String)job.get("Amico");
				u.aggiungiAmico(a);
			}
		}
		return u;
		
	}
	
	//linkedlist da java a json
	@SuppressWarnings("unchecked")
	synchronized public static JSONArray tojsl(LinkedList<String> l) {
		JSONArray am =new JSONArray();//array di oggetti json che conterrà la lista degli amici
		for(String s: l) {//inserisco gli amici
			JSONObject obj= new JSONObject();//creo un oggetto jason
			obj.put("Amico",s);
			am.add(obj);//aggiungo all'array json
		}
		
		return am;
	}
	

@SuppressWarnings("unchecked")
//metodo per aggiornare il file JSON 
synchronized public static void aggiornaFile(int codice,String NomeUtente, String pw,String amico,LinkedList<String> amiciUtente,LinkedList<String>amiciAmico, int punteggio) throws IOException, ParseException {
	
	
	
	if(codice==1) {//operazione di registry, devo inserire un nuovo utente nel file
		FileWriter w=new FileWriter("Backup",true);//scrivo in append
		BufferedWriter b=new BufferedWriter (w);
		Utente u= new Utente(NomeUtente,pw);
	    b.write(u.tojs().toJSONString());
	    b.write("\n");//inserisco in ogni riga un jsonobject
		b.close();
		
	}else if(codice==2 || codice==3) {//aggiunta amico o aggiornamento punteggio
		
		JSONParser parser= new JSONParser();
		Object obj;
		JSONObject jo;
		BufferedReader reader=new BufferedReader (new FileReader ("Backup"));
		String line;
		JSONArray array;
		
		File tmp=new File("Tmp");
		tmp.createNewFile();
		FileWriter wr=new FileWriter("Tmp",true);//scrivo in append
		BufferedWriter buf=new BufferedWriter (wr);
		
		
		while((line=reader.readLine())!=null) {
			obj=parser.parse(line);
			jo=(JSONObject)obj;
			if(jo.get("Nome").equals(NomeUtente)) {
				
				if(codice==2) {//devo aggiornare la lista degli amici
					array=Utente.tojsl(amiciUtente);
					jo.replace("Amici", array);
					//scrivo sul file Tmp
					
				
				}else {//devo aggiornare il punteggio
					jo.replace("Punteggio", punteggio);
					
					
				}
				
				buf.write(jo.toJSONString());
				buf.write("\n");
				
			}else if(jo.get("Nome").equals(amico)) {//devo aggiornare la lista degli amici
				array=Utente.tojsl(amiciAmico);
				jo.replace("Amici", array);
				//scrivo sul file Tmp
				buf.write(jo.toJSONString());
				buf.write("\n");
				
			}else {
				//altrimenti riscrivo l'oggetto senza modifiche
				buf.write(jo.toJSONString());
				buf.write("\n");
			}
	}
		buf.close();
		reader.close();
		
		//elimino il file backup
		File el= new File("Backup");
		el.delete();
		
		File rename= new File("Backup");
		
		//rinomino il file tmp
		tmp.renameTo(rename);
		
		
		
		
	}
		
	}
	
	
		
		
		
		
}
	

	

