import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Base64;


public class Conexao implements Runnable {

    private final Socket socket;
    private boolean conectado;

    public Conexao(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
    	
        conectado = true;
        
        //converte o formato da data para o GMT 
        String dataF = Util.formatarDataGMT(new Date());
        
        System.out.println("conectado " + socket.getInetAddress() + " - " + dataF + " \n");
        
        while (conectado) {
            try {
                
            	//cria uma requisicao 
            	
                Requisicao requisicao = Requisicao.lerRequisicao(socket.getInputStream());
                
                //se a conexao esta marcada para se manter viva entao seta keep alive e o timeout
                
                if (requisicao.isManterViva()) {
                    socket.setKeepAlive(true);
                    socket.setSoTimeout(requisicao.getTempoLimite());
                } else {
                    //se nao seta um valor menor suficiente para uma requisicao
                    socket.setSoTimeout(300);
                }
                
              
                //se o caminho for "/" então pega o arquivo index.html
                
                if (requisicao.getRecurso().equals("/")) {
                	
                	Resposta resposta = new Resposta(requisicao.getProtocolo(), 401, "Not Authorized");
                    File arquivo = new File("401.html");
                    
                    //lê todo o conteúdo do arquivo para bytes e gera o conteudo de resposta
                    resposta.setConteudoResposta(Files.readAllBytes(arquivo.toPath()));
                    
                         
                    String usernameColonPassword = "user:passwd";
                    String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
                    
                    BufferedReader httpResponseReader = null;
                    try {
                        
                        URL serverUrl = new URL("http://localhost:8080/index.html");
                        HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();
                     
                        // Define o método HTTP como GET
                        urlConnection.setRequestMethod("GET");
                     
                        // Inclui o HTTP Basic Authentication 
                        urlConnection.addRequestProperty("Authorization", basicAuthPayload);
                     
                        
                        httpResponseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                       
                        requisicao.setRecurso("index.html");
                     
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    } finally {
                     
                        if (httpResponseReader != null) {
                            try {
                                httpResponseReader.close();
                            } catch (IOException ioe) {
                                // Close quietly
                            }
                        }
                    }
                }    
 
                    //abre o arquivo
                    File arquivo = new File(requisicao.getRecurso().replaceFirst("/", ""));

                    Resposta resposta;

                    //se o arquivo existir então criamos a reposta com status 200
                    
                    if (arquivo.exists()) {
                        resposta = new Resposta(requisicao.getProtocolo(), 200, "OK");
         	
                    }else {
                    	
                        //se o arquivo não existe então temo a reposta com status 404
                    	
                        resposta = new Resposta(requisicao.getProtocolo(), 404, "Not Found");
                        arquivo = new File("404.html");
                    }
                    
                    //lê todo o conteúdo do arquivo para bytes e gera o conteudo de resposta
                    resposta.setConteudoResposta(Files.readAllBytes(arquivo.toPath()));
                    
                    //converte o formato da data para o GMT 
                    String dataFormatada = Util.formatarDataGMT(new Date());
                    
                    //cabeçalho da resposta HTTP/1.1
                    resposta.setCabecalho("Host", "http://localhost:8080/");
                    resposta.setCabecalho("Date", dataFormatada);
                    resposta.setCabecalho("Server", "MeuServidor/1.0");
                    resposta.setCabecalho("Content-Type", "text/html");
                    resposta.setCabecalho("Content-Length", resposta.getTamanhoResposta());
                   
                   
                    //cria o canal de resposta utilizando o outputStream
                    resposta.setSaida(socket.getOutputStream());
                    resposta.enviar();
                 
                
                
                
                
            } catch (IOException ex) {
            	
                //quando o tempo limite terminar encerra a thread
            	
                if (ex instanceof SocketTimeoutException) {
                    try {
                        conectado = false;
                        socket.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(Conexao.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }

        }
    }

}
