import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.net.Socket;

public class server extends UnicastRemoteObject implements regInterface {
	
	private static final long serialVersionUID = 1L;
	
	 //contiene i dati degli utenti in modo che siano facilmente e velocemente accessibili
	static ConcurrentHashMap<String,Utente> dati = new ConcurrentHashMap<String,Utente>();
	
	//cache parole da usare nella sfida
	static ConcurrentHashMap<Integer,trad> cache=new ConcurrentHashMap<Integer,trad>() ;
	


	public server() throws RemoteException{//costruttore
		super();// è qui che il server viene esportato
		
	}
	

	 public synchronized int registry(String NomeUtente, String pw)throws IOException, Exception {
		
		if(dati.containsKey(NomeUtente)) return 2;//nome utente esiste già
		else {
			Utente u= new Utente(NomeUtente,pw);
			dati.put(NomeUtente, u);//inserisco nella hash
			
			//metto i dati nel file Backup
			Utente.aggiornaFile(1, NomeUtente, pw,null,null,null,0);
		
		  return 1;//tutto ok
		}
	}
	
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ParseException{
	 
		//inizializzazione del server
		System.out.println("start server");
		
		server s;
		
		//per prima cosa copio i dati contenuti nel file all'interno della struttura dati
		
		File file=new File("Backup");
		
		if(file.exists()) { 
			
			JSONParser parser= new JSONParser();
			Object obj;
			JSONObject jo;
			BufferedReader reader=new BufferedReader (new FileReader ("Backup"));
			String line;
			Utente ut;
			
				while((line=reader.readLine())!=null) {//leggo il file riga per riga e so che ogni riga corrisponde ad un jsonobject
					obj=parser.parse(line);
					jo=(JSONObject)obj;
					ut=Utente.toja(jo);
					dati.put(ut.restituisciNome(), ut);
					
					
				}
			
			reader.close();
			
			
		}else {//creo il file
			file.createNewFile();
		}
		
		
		//leggo il file dizionario e inserisco le parole in una hashtable che ha come chiave un numero e come valore
		//una coppia <parola-traduzione>. inizialmente non avrò le traduzioni, ma solo le parole in italiano
		
		BufferedReader rd=new BufferedReader (new FileReader ("./Dizionario.txt"));
		String l;
		int ind=0;
		trad t;
		
			while((l=rd.readLine())!=null) {
				
				t=new trad(l);
				cache.put(ind, t);
				ind++;
			}
			
			rd.close();
		
		
	
		try {
			s = new server();
			
			//esportazione dell'oggetto non devo farla perchè ho esteso UnicastRemoteObject quindi è implicita
			
			//Creazione di un registry sulla porta 6666
			 LocateRegistry.createRegistry(6666);
			 Registry r=LocateRegistry.getRegistry(6666); 
			 
			 // Pubblicazione dello stub nel registry 
			 r.rebind("SERVER", s);
			 
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(7777);
			ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
			
			//ciclo infinito in cui ogni volta che accetto una nuova connessione sarà un thread a gestirla
			while(true) {
				
				Socket socket = serverSocket.accept();
				pool.execute(new worker(socket,dati,cache));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	

}
	
	
