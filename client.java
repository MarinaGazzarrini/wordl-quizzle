import java.io.BufferedReader;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class client {
	
	public static void main(String args[]) throws Exception {
		
		//stampe all'avvio del client
		System.out.println("start client");
		System.out.println();
		
		System.out.println("LISTA COMANDI: ");
		System.out.println("registra_utente nome password ->per effettuare la registrazione");
		System.out.println("login nome password ->per effettuare il login");
		System.out.println("logout ->per effettuare il logout");
		System.out.println("aggiungi_amico nomeAmico ->creare una relazione di amicizia con nomeAmico");
		System.out.println("lista_amici ->mostra la lista dei propri amici");
		System.out.println("mostra_punteggio ->restituisce il punteggio totale");
		System.out.println("mostra_classifica ->mostra la classifica degli amici ");
		System.out.println("sfida nomeAmico->mandare una richiesta di sfida");
		System.out.println("mostra_richiesta_sfida-> mostra la richiesta di sfida meno recente");
		System.out.println("wq --help ->visualizza comandi disponibili");
		System.out.println();
		
		//preparazione connessione tcp
		InetAddress host= InetAddress.getLocalHost();
		int port=7777;
		Socket sk= new Socket(host,port);
	
		//rmi
		Registry r; 
		
		//lettura\scrittura
		BufferedWriter writer=null;
		BufferedReader reader = null;
		reader = new BufferedReader(new InputStreamReader(sk.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(sk.getOutputStream()));
		
		int online=0; //0->non ho ancora effettuato il login    1->ho già fatto il login
		int termina=0; 
		int sfida=0,i;//0->non sono in sfida     1->sono in sfida
		int c=0;//se c=0 richiesta di mostrare inviti è fatta da console
		LinkedBlockingQueue<sfidantePorta> messaggi = new LinkedBlockingQueue<sfidantePorta>();//lista threadsafe contenente le richieste di sfida
		
		//preparazione udp
		InetAddress address =  InetAddress.getByName("Localhost");//l'ind del pc locale
		DatagramSocket clientSocket = new DatagramSocket(sk.getLocalPort());
		
		//lancio un thread che si occupa di gestire la ricezione udp
		udpClient ts = new udpClient(clientSocket,messaggi);
		Thread T = new Thread(ts);
		T.start();
		
		//preparazione lettura input
		String line="",rx;
		BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
			
			while(termina==0) {
				
				 i=0;
			
				while(sfida==1) {//SONO IN SFIDA
					
					//messaggio dal server
					rx= reader.readLine();
					
					if(i==0 && rx.equals("1235")) {//probabilmente il messaggio udp di risposta è andato perso
						sfida=0;
						System.out.println("Errore comunicazione sfida, sfida annullata!");
					}else {
		   		    
						if(i==0)System.out.println("La sfida ha inizio!");
						i++;
						if(i<6)System.out.println("Parola "+i+"/5");
		   		    
						System.out.println(rx.toString());
		   		    
						if(i==6) sfida=0;
						else {
		   		    	
							if(rx.contains("Tempo scaduto!")) {//timeout scaduto
								i=5;
			   		    	
							}else {
		   		    
								line=buf.readLine();//scrivo la traduzione
		   		    
								//mando al server
								writer.write(line);
								writer.newLine();
								writer.flush();
			    	
								if(i==5) {//sfida finita
			    		
									System.out.println("fine sfida attendi i risultati!");
			    		
								}
			    	
								if(rx.contains("Tempo scaduto!")) {//timeout scaduto
		   		    			i=5;
		   		    	
		   		    		}
		   		    
			   		    }}
				}	
				}
				
				
				
				if(line.equals("mostra_richiesta_sfida")) {//MOSTRA LA RICHIESTA DI SFIDA MENO RECENTE E LA TOGLIE DALLA LISTA
					
					if(online==0 && c==0)System.out.println("prima effettuare il login");
					else if(online==0 && c==1) {}
					else {
						if(messaggi.isEmpty()) {
							if (c==0)System.out.println("Nessuna richiesta di sfida");
						}else {
						Integer tempi=0;
						String ok;
						//stampo la richiesta all'inizio della lista
						int p=messaggi.peek().porta;//porta a cui inviare la rx se accetto la sfida
						while(tempi==0 && !messaggi.isEmpty()) {
							if((System.currentTimeMillis()-(messaggi.peek().tempoArrivo))<20000) tempi=1;//la sfida può avvenire
							else messaggi.poll();//rimuovo l'elemento perchè è passato troppo tempo
						}
						if(messaggi.isEmpty()) {
							System.out.println("Nessuna richiesta di sfida");
						}else {
						
						System.out.println("Hai una nuova richiesta di sfida da parte di "+(messaggi.poll().nome));
						System.out.println("Accetti?");
						line=buf.readLine();
						if(line.equals("si")) {
							ok="si";
							sfida=1;
							//devo dire al mio server che ho accettato la sfida
							 writer.write("sfida");
					    	 writer.newLine();
					    	 writer.flush();
					  	
						}else { 
							System.out.println("Hai rifiutato la sfida");
							ok="no";
							
						}
						
						//mando la risposta al server udp
						String mex=ok.toString();//messaggio che voglio inviare sotto forma di stringa
						byte[] sendBuffer= mex.getBytes();
						
						DatagramPacket mypacket = new DatagramPacket(sendBuffer,mex.getBytes().length,address,p);
						clientSocket.send(mypacket);//invio 
						
						}}}
					c=0;
					
				}
				
				if(sfida==0) line=buf.readLine();
				else line="mostra_richiesta_sfida";
				
				String[] richiesta = null;
				
				if(line.equals("logout")) {//RICHIESTA DI LOGOUT
					
					if(online==0)System.out.println("Non si può effettuare il logout prima di aver effettuato il login");
					else {
						  
				    	  writer.write("logout ");//invio richiesta di logout al server
				    	  writer.newLine();
				    	  writer.flush();
				    	  
				    	  
				    	  //risposta del server
				    		rx = reader.readLine();
				   		    
				   		    if(rx.equals("1")) {//1->tutto ok
				   		    	online=0;
				   		    	messaggi.clear();
				   		    	System.out.println("Logout effettuato con successo");
				   		    } else//2->errore
				   		    	System.out.println("Errore, logout non effettuato");
				    	  
					}
					
				}else if(line.equals("wq --help")){
					
					System.out.println("LISTA COMANDI: ");
					System.out.println("registra_utente nome password ->per effettuare la registrazione");
					System.out.println("login nome password ->per effettuare il login");
					System.out.println("logout ->per effettuare il logout");
					System.out.println("aggiungi_amico nomeAmico ->creare una relazione di amicizia con nomeAmico");
					System.out.println("lista_amici ->mostra la lista dei propri amici");
					System.out.println("mostra_punteggio ->restituisce il punteggio totale");
					System.out.println("mostra_classifica ->mostra la classifica degli amici ");
					System.out.println("sfida nomeAmico->mandare una richiesta di sfida");
					System.out.println("mostra_richiesta_sfida-> mostra la richiesta di sfida meno recente");
					System.out.println("wq --help ->visualizza comandi disponibili");
					System.out.println();
					
				}else if(line.equals("lista_amici")) {//RICHIESTA VISIONE LISTA DEGLI AMICI
					
					if(online==0)System.out.println("Prima è necessario effettuare il login");
					else {
						writer.write("lista_amici ");//invio richiesta al server
				    	writer.newLine();
				    	writer.flush();
				    	
				    	rx = reader.readLine();
			   		   
				    	String st[]=rx.split("\"");
			   		    int in=5;
			   		    
				    	if(st.length==3)System.out.println("Nessun amico");
				    	else {
				    		System.out.print("Lista amici: ");
				    		while(in<st.length) {
				    			System.out.print(st[in]+" ");
				    			in=in+4;
				    			
				    		}
				    		System.out.print("\n");
				    	}
					}
					line="mostra_richiesta_sfida";//alla fine di ogni richiesta stampo il mex di sfida meno recente
					c=1;
					
				}else if(line.equals("mostra_punteggio")) {//RICHIESTA VISIONE PUNTEGGIO
					
					if(online==0)System.out.println("Prima è necessario effettuare il login");
					else {
						writer.write("mostra_punteggio ");//invio richiesta al server
						writer.newLine();
						writer.flush();
						
						//risposta dal server
						rx= reader.readLine();
			   		    
			   		    System.out.println("Il punteggio: "+rx.toString());
				   }
					line="mostra_richiesta_sfida";
					c=1;
					
				}else if(line.equals("mostra_classifica")) {//RICHIESTA VISIONE CLASSIFICA
					
					if(online==0)System.out.println("Prima è necessario effettuare il login");
					else {
						writer.write("mostra_classifica ");//invio richiesta al server
				    	writer.newLine();
				    	writer.flush();
				    	
				    	
				    	rx=reader.readLine();
			   		    
				    	String st[]=rx.split("\"");
				    	int in=7;
				    	System.out.print("Classifica: ");
				    	
				    	while(in < st.length) {
				    		System.out.print(st[in]+st[in-3]+" ");
				    		in=in+6;
				    	}
				    	
				    	System.out.print("\n");
				    	
					}
					line="mostra_richiesta_sfida";
					c=1;
					
				}else if(line.equals("termina")){
					termina=1;
					
				}else {//caso in cui la richiesta preveda più parametri
					
					if(line.contains(" ")) {
						
						richiesta=line.split(" ");
					
					  if(richiesta[0].equals("registra_utente")) {//RICHIESTA REGISTRAZIONE
						
						 if(online==1)System.out.println("Impossibile effettuare la registrazione poichè si è già loggati");
						  else if(richiesta.length!=3)
								 	System.out.println("Impossibile effettuare la registrazione poichè il formato della richiesta non è corretto");
					       else {
					    	  
					    	  
					    	  String name=richiesta[1];
					    	  String pw=richiesta[2];
					    	  
					    	  //non metto anche ind ip perchè sono in locale
								r = LocateRegistry.getRegistry(6666);
								Remote RemoteObject=r.lookup("SERVER");//usato dal client per interrogare il registro RMI
								regInterface serverObject=(regInterface) RemoteObject;
								
								int controllo=serverObject.registry(name,pw);
									if(controllo==1)System.out.println("registrazione avvenuta con successo");
										else System.out.println("username già in uso, registrazione fallita");
					    	  
						
						}
						
						
					}else if(richiesta[0].equals("login")) {//RICHIESTA LOGIN
						
						if(online==1)System.out.println("Impossibile effettuare il login poichè già online");
						
						 else if(richiesta.length!=3)
							 	System.out.println("Impossibile effettuare il login poichè il formato della richiesta non è corretto");
				      else {
				    	  
				    	  //nome è in richiesta[1];
				    	  //password è in richiesta[2];
				    	  
				    	  String mex=richiesta[1]+" "+richiesta[2];
				    
				    	  writer.write(richiesta[0]+" "+mex);//invio richiesta di login al server
				    	  writer.newLine();
				    	  writer.flush();
				    	 
				    		rx= reader.readLine();
				   		   
				   		    
				   		    if(rx.equals("1")) {//1->tutto ok
				   		    	online=1;
				   		    	System.out.println("Login effettuato con successo");
				   		    }else if(rx.equals("2"))//2->password errata
				   		    	System.out.println("La password inserita non è corretta");
				   		    else if(rx.equals("3"))//3->utente non registrato
				   		    	System.out.println("L'utente non è registrato");
				   		    else//4->utente già online 
				   		    	System.out.println("L'utente è già online su un altro dispositivo");
				    	  
						
					}
					
					
						line="mostra_richiesta_sfida";
						c=1;
					
					
				     }else if(richiesta[0].equals("sfida")) {//RICHIESTA SFIDA
				    	 
				    	 if(online==0)System.out.println("Prima è necessario effettuare il login");
				    	   else if(richiesta.length!=2)System.out.println("Formato della richiesta errato");
				    	     else {
				    		   
				    		   writer.write(richiesta[0]+" "+richiesta[1]);//invio richiesta di sfida al server
						       writer.newLine();
						       writer.flush();
						       
						       System.out.println("Richiesta di sfida effettuata, attendi la risposta!");
						       
						       //risposta del server
							     rx=reader.readLine();
						   		  
						   		  if(rx.equals("0"))System.out.println("Impossibile effettuare la sfida: amico non presente nella lista o non online");
						   		  else if(rx.toString().equals("2"))System.out.println("Sfida non accettata");
						   		  else { 
						   			  sfida=1;
						   			  System.out.println("Sfida accettata");
						   		  
						   		  }
				    		 
				    	   }
				    	 
				     }else if(richiesta[0].equals("aggiungi_amico")) {//RICHIESTA AGGIUNTA AMICO
				    	 
				    	 if(online==0)System.out.println("Prima è necessario effettuare il login");
				    	 else if(richiesta.length!=2)System.out.println("Formato della richiesta errato");
				    	 else {
				    		 
				    		  writer.write(richiesta[0]+" "+richiesta[1]);//invio richiesta al server
						      writer.newLine();
						      writer.flush();
						      
						      //risposta del server
						      rx=reader.readLine();
					   		    
					   		    if(rx.equals("1")) {//1->tutto ok
					   		    	System.out.println(richiesta[1]+" aggiunto agli amici");
					   		    }else if(rx.equals("2"))//2->già presente tra gli amici
					   		    	System.out.println(richiesta[1]+" era già presente tra gli amici");
					   		    else//3->utente non esiste
					   		    	System.out.println("Impossibile aggiungere "+richiesta[1]+" agli amici perchè non è registrato");
				    		 
				    	 }
				    	 line="mostra_richiesta_sfida";
				    	 c=1;
				    	 
				     }else {
				    	 if(!line.equals("mostra_richiesta_sfida")) {
				    		 System.out.println("Richiesta non riconosciuta");
				    	 }
				     }
					}else {
						if(!line.equals("mostra_richiesta_sfida")) {
							 System.out.println("Richiesta non riconosciuta");
						}
				     }
				    	 
				    	 
				    	 
				     }
			
			
			
			
			
			
			
			}
			
			sk.close();
		
		
		
		
		
		
	}

}
