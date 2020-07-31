import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.LinkedBlockingQueue;

public class udpClient implements Runnable {
	
	DatagramSocket clientSocket;
	LinkedBlockingQueue<sfidantePorta> messaggi;//lista thread safe in cui andrò a inserire le richieste di sfida
	
	udpClient(DatagramSocket clientSocket,LinkedBlockingQueue<sfidantePorta> messaggi){//costruttore
		this.clientSocket=clientSocket;
		this.messaggi=messaggi;
		
	}
	
	public void run() {
		
		while(true) {
		
		//in attesa di messaggi su udp
		try {
			
			byte[] buffer= new byte[1024];
			//creo un datagramPacket per memorizzare il pacchetto ricveuto
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			clientSocket.receive(packet);
			long tempoArrivo=System.currentTimeMillis();//mi serve perchè le richieste di sfida non sono valide per sempre
			  
			 //leggo i dati spediti dal server
		     buffer = packet.getData();
		     BufferedReader br= new BufferedReader(new InputStreamReader(new ByteArrayInputStream (buffer)));
		     String line = br.readLine();//in line ho il nome dello sfidante
		     
		     messaggi.offer(new sfidantePorta(line, packet.getPort(),tempoArrivo));//inserisce in coda
		     
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
		
		
		
	}
	

}

