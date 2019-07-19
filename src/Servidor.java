
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Servidor {

    public static void main(String[] args) throws IOException {
        
    	// cria um socket servidor na porta 8080 
         
        ServerSocket servidor = new ServerSocket(8080); 
        
        ExecutorService pool = Executors.newFixedThreadPool(10);

        while (true) {
            
        	//a cada solicitação de conexão cria uma nova thread
            
        	pool.execute(new Conexao(servidor.accept()));
        }
    }
}
