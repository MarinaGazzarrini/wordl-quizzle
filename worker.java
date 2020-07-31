import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class worker implements Runnable {
	
     private Socket sk;
     ConcurrentHashMap<String,Utente> dati;
     ConcurrentHashMap<Integer,trad> cache;

	worker(Socket sk,ConcurrentHashMap<String,Utente> dati,ConcurrentHashMap<Integer,trad> cache) {//costruttore
		
		this.sk=sk;
		this.dati=dati;
		this.cache=cache;
	}
	
	
	@SuppressWarnings({ "unchecked", "unused", "resource" })
	public void run() {
		
		BufferedReader reader=null;//letture
		BufferedWriter writer=null;//scritture
		int ok=1;
		

		  try {
			  
			reader = new BufferedReader(new InputStreamReader(sk.getInputStream()));
		    writer= new BufferedWriter(new OutputStreamWriter(sk.getOutputStream()));
		    String utente=null;
			
			  while(ok==1) {
			   
   		         String rx= reader.readLine();
			
   		         String[] richiesta= rx.split(" ");
			
			  switch(richiesta[0]) {
				
				case "login":
					Integer responso;
					if(dati.containsKey(richiesta[1])) {//se l'utente è registrato
						if(richiesta[2].equals(dati.get(richiesta[1]).restituisciPassword())) {//se la password è corretta
							if(dati.get(richiesta[1]).infoOnline()==0) {
								responso=1;//login effettuato
								dati.get(richiesta[1]).addfd(sk);//salvo il fd corrispondente
								dati.get(richiesta[1]).notOffline();//setto l'utente come online
								utente=richiesta[1];
							}else responso=4;//l'utente è già online su un altro dispositivo
						}else responso=2;//password errata
						
					}else responso=3;//utente non registrato
				
					
					writer.write(responso.toString());
					writer.newLine();
					writer.flush();
					
					break;
					
				case "logout":
					dati.get(utente).offline();
					utente=null;
					writer.write("1");
					writer.newLine();
					writer.flush();
					break;
					
				case "aggiungi_amico":
					
					Integer controllo;
					if(dati.containsKey(richiesta[1])) {//l'amico è registrato
						if((((dati.get(utente).aggiungiAmico(null))!=null) && (dati.get(utente).aggiungiAmico(null).contains(richiesta[1]))) || (utente.equals(richiesta[1])))controllo=2;//era già tra gli amici
						else { 
							//aggiorno hash sia dell'utente che dell'amico
							dati.get(utente).aggiungiAmico(richiesta[1]);
							dati.get(richiesta[1]).aggiungiAmico(utente);
							//aggiorno file json sia per l'utente sia per l'amico
							Utente.aggiornaFile(2, utente, null,richiesta[1],dati.get(utente).aggiungiAmico(null),dati.get(richiesta[1]).aggiungiAmico(null),0);
							controllo=1;//posso aggiungere l'amico
						}
						
					}else controllo=3;//amico non era registrato
					
					writer.write(controllo.toString());
					writer.newLine();
					writer.flush();
					
					break;
					
				case "mostra_classifica": 
					
					LinkedList<String> amici=dati.get(utente).aggiungiAmico(null);//lista degli amici
					int mioPunteggio=dati.get(utente).aggiornaPunteggio(0); //punteggio utente
					
					//uso un arraylist poiche è più efficiente fare il sort piuttosto che in una linkedlist
					ArrayList<utentePunteggio> classifica= new ArrayList<utentePunteggio>();//classifica
					classifica.add(new utentePunteggio(utente,mioPunteggio));//aggiungo l'utentente alla lista
					
					//faccio un arrayList di utentePunteggio
					for(String s: amici) {
						//aggiungo gli amici alla classifca
						classifica.add(new utentePunteggio(s,dati.get(s).aggiornaPunteggio(0)));
					}
					
					//ordino la classifica dal punteggio più alto al più basso
					Collections.sort(classifica);
				
					//poi passa a jsonobject e invia
					JSONObject obj= new JSONObject();
					JSONArray js =new JSONArray();
					for(utentePunteggio u: classifica) {
						js.add(u.jasonUP());	
					}
					obj.put("Classifica", js);
					
					writer.write(obj.toString());
					writer.newLine();
					writer.flush();
				
					
					break;
					
				case "mostra_punteggio":
					Integer p=dati.get(utente).aggiornaPunteggio(0);
					writer.write(p.toString());
					writer.newLine();
					writer.flush();
					break;
					
				case "lista_amici": 
					
					JSONObject oj= new JSONObject();
					JSONArray am =Utente.tojsl(dati.get(utente).aggiungiAmico(null));//array di oggetti json che conterrà la lista degli amici
					
					oj.put("Amici", am);
					
					//ora devo restituire oj al client
					writer.write(oj.toString());
					writer.newLine();
					writer.flush();
					break;
					
				case "sfida":
					
					if(richiesta.length==2) {//sono chi propone la sfida, devo prepararla
					
					//controllo se chi voglio sfidare è tra gli amici, se è in sfida o se è online
					if ((dati.get(utente).amico(richiesta[1])==0) || dati.get(richiesta[1]).infoSfida()==1 || dati.get(richiesta[1]).infoOnline()==0 || (utente.equals(richiesta[1])) ) {
						writer.write("0");
						writer.newLine();
						writer.flush();
						break;
						
					}else {//l'amico è presente nella lista
						
						//recupero indirizzo
						InetAddress address =dati.get(richiesta[1]).retfd().getInetAddress();
						int port =dati.get(richiesta[1]).retfd().getPort();
						
						DatagramSocket dt = new DatagramSocket();
						
						//preparo messaggio
						String mex=utente;//voglio inviare come messaggio il nome di chi richiede la sfida
						byte[] sendBuffer= mex.getBytes();
						byte[] buffer= new byte[1024];
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						
						//invio messaggio e setto il timer
						DatagramPacket mypacket = new DatagramPacket(sendBuffer,mex.getBytes().length,address,port);
						dt.send(mypacket);//invio 
						try {
						
								dt.setSoTimeout(30000);//30 secondi
								
								//attendo un messaggio di risposta
								dt.receive(packet);
						
						}catch(SocketTimeoutException e ){
							//timeout scaduto
							writer.write("0");
							writer.newLine();
							writer.flush();
							break;
							
							
						}
						
						 //leggo i dati spediti dal client
					     buffer = packet.getData();
					     BufferedReader br= new BufferedReader(new InputStreamReader(new ByteArrayInputStream (buffer)));
					     String ln = br.readLine();

					     
					     if(ln.contains("no")) {
					    	 
					    		//se la richiesta va male
								writer.write("2");
								writer.newLine();
								writer.flush();
								break;
					    	 
					     }
						
					
						//se la richiesta va bene
						writer.write("1");
						writer.newLine();
						writer.flush();
						
						//setto a zero il punteggio della sfida per entrambi i partecipanti
						dati.get(utente).punteggioSfida=0;
						dati.get(richiesta[1]).punteggioSfida=0;
						//salvo lo sfidante
						dati.get(utente).sfidante=richiesta[1];
						dati.get(richiesta[1]).sfidante=utente;
						//setto inizio sfida
						dati.get(utente).inizioSfida();
						dati.get(richiesta[1]).inizioSfida();
						
						
						//leggo k=5 parole dal file dizionario

						String line = null,tradotta = null;
						int n;
						
						for(int j=0; j<10; j++) {
							
							//genero casualmente un nuomero da 1 a 446
							n = (int) (Math.random() * 446); 
							
							line= cache.get(n).nome;
							if((cache.get(n).traduzione).equals("nessuna")){//se non ho gia la traduzione 
								
								//cerco la traduzione di line e le metto in tradotta
								String pathname1="https://api.mymemory.translated.net/get?q=";
								String pathname2="&langpair=it|en";
							
								URL url=new URL(pathname1+line+pathname2);
								URLConnection uc=url.openConnection();
								uc.connect();
								BufferedReader in=new BufferedReader(new InputStreamReader(uc.getInputStream()));
							 
								String traduzione;
								StringBuffer sb=new StringBuffer();
								
								while((traduzione=in.readLine())!=null){
									sb.append(traduzione);
								}
							 
								JSONObject obj1 = (JSONObject) new JSONParser().parse(sb.toString());
								JSONObject obj2 =(JSONObject) (obj1.get("responseData"));
								tradotta=(String) obj2.get("translatedText");
								
								cache.get(n).addtrad(tradotta.toLowerCase());
							 
							}else tradotta=cache.get(n).traduzione;
							 
							
							//inserisco line e tradotta nell'array parole dell'utente amico
							dati.get(richiesta[1]).inserisciParole(j, line, tradotta);
							j++;//il for gia incrementa di uno 
							
							
						}
						dati.get(richiesta[1]).par=1;//materiale per la sfida è pronto	
						}
					}
						
						//adesso tutte le parole da tradurre e quelle tradotte sono nell'array parole dello sfidato
						//ora può iniziare la sfida
					
					
					int j=0,corrette=0;
					String c,ln;
					
					//le info per la sfida sono nel profilo di chi è stato sfidato
					if(richiesta.length==2)c=richiesta[1];//sono lo sfidante
					else c=utente;//sono lo sfidato
					
					
					String con;
					int noc=0;
					
					if(richiesta.length!=2) {
						//sono lo sfidato
						con=c;//io
						
					}else {//sono lo sfidante
						con=utente;//io
					}
					
					//le informazioni sulle parole le leggo nell'array dello sfidato quindi in c
					
					if(richiesta.length!=2) {//sono lo sfidato. devo attendere che sia tutto pronto
					
						long time=System.currentTimeMillis();//lo utilizzo perchè udp potrebbe aver perso la risposta
						while((dati.get(c).par==0) && ((System.currentTimeMillis()-time)) < 20000 ) {
							TimeUnit.SECONDS.sleep(2);
						}//fino a che non è pronta 
					
						
						if(dati.get(c).par==1)dati.get(c).par=0;//riazzero
						else {
							writer.write("1235");//mando un codice di errore al client
							writer.newLine();
							writer.flush();
							break;
						}
					}
					long timer=System.currentTimeMillis();//tempo per la sfida= 3 minuti(180000 millisecondi)
					
					while(j<10 && noc==0) {
						
						writer.write(dati.get(c).leggiParola(j));
						writer.newLine();
						writer.flush();
						j++;
						
						if((ln=reader.readLine()).equals(dati.get(c).leggiParola(j))) {
							dati.get(con).puntiSfida(1);
							corrette++;
						}
						else dati.get(con).puntiSfida(2);
						j++;
						if((System.currentTimeMillis()-timer)>180000) {
							writer.write("Tempo scaduto!");
							writer.newLine();
							writer.flush();
							noc=1;
						}
						
						
					}
					
					
					dati.get(utente).aggiornaPunteggio(dati.get(utente).punteggioSfida);//aggiorno il punteggio nella classifica totale
					Utente.aggiornaFile(3, utente, null, null, null, null, dati.get(utente).punteggio);
					
					dati.get(utente).fineSfida();
					
					while(dati.get(dati.get(utente).nomeSfidante()).infoSfida()==1) {TimeUnit.SECONDS.sleep(2);}//fino a che anche l'altro non ha finito
						
					int pn=dati.get(dati.get(utente).nomeSfidante()).punteggioSfida;//punteggio avversario
					
					writer.write("Sfida terminata! Punteggio ottenuto: "+dati.get(utente).punteggioSfida+" Punteggio avversario: "+ pn+ " Totale parole corrette: "+corrette);
					writer.newLine();
					writer.flush();
					
					
					
						
					break;
			
			}
			
			  }
			
			} catch (IOException | ParseException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		
	}

}
