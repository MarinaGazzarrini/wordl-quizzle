import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface regInterface extends Remote  {
	
	//registrazione utente
	public int registry(String NomeUtente, String pw) throws RemoteException, IOException, Exception;

}
