import java.util.*;
import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Classe Machine
 * Permet de lancer une machine	
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public class Machine implements Runnable{

	// Informations de connexion
	private String ident;
	private String ip;
	private String ip_multdif;
	private String next_ip;
	private short udp_listenPort;
	private short tcp_listenPort;
	private short udp_nextPort;
	private short multdif_port;

	// Status
	private boolean udp_connected;
	private boolean tcp_connected;
	
	// Threads d'écoutes
	private Thread udp_listening;
	private Thread tcp_listening;
	private Thread diff_listening;

	private Thread timerTest; // Boucle du timer
	private long start_time; // Premiere mesure du temps
	private int delay = 5000; // Interval de temps pour le TEST en ms
	private int last_idm_test; // L'id du dernier message TEST envoyé

	// Entrées / Sorties
	private ServerSocket tcp_socket; 
	private DatagramSocket dso;
	private MulticastSocket mso;
	private PrintWriter pw;
	private BufferedReader br;

	// Historique des envois
	private Hashtable last_msg;
	// Historique des recus
	private LinkedList<String> logs = new LinkedList<String>();

	/**
   * Constructeur
   * @param ip_multdif Adresse IP de multicast
	 * @param tcp_listenPort Port d'écoute TCP
	 * @param udp_listenPort Port d'écoute UDP
	 * @param multdif_port Port d'écoute UDP multicast
	 * @throws IOException Lance une exception en cas de problème
   */
	public Machine(String ip_multdif, short tcp_listenPort, short udp_listenPort, short multdif_port) throws Exception{

		this.ident = getRandomIdent();
		this.ip_multdif = ip_multdif;
		this.tcp_listenPort = tcp_listenPort;
		this.udp_listenPort = udp_listenPort;
		this.udp_nextPort = udp_listenPort;
		this.multdif_port = multdif_port;
		this.last_msg = new Hashtable();

		this.dso = new DatagramSocket(this.udp_listenPort);
		this.mso = new MulticastSocket(this.multdif_port);
		this.mso.joinGroup(InetAddress.getByName(ip_multdif));
		this.tcp_socket = new ServerSocket(tcp_listenPort);

		// Recherche de l'ip 192.x.x.x (Nécessaire pour linux)
		Enumeration en = NetworkInterface.getNetworkInterfaces();
		while(en.hasMoreElements()){
	    NetworkInterface ni=(NetworkInterface) en.nextElement();
	    Enumeration ee = ni.getInetAddresses();

	    while(ee.hasMoreElements()) {
        InetAddress ia= (InetAddress) ee.nextElement();
        if(ia.getHostAddress().matches("192.168.1.[0-9]*")){
        	this.ip = ia.getHostAddress();
        	break;
        }
	    }
	    if(!this.ip.equals(""))
	    	break;
		 }

		this.next_ip = ip;

		/**
	   * Thread d'écoute TCP
	   */
		tcp_listening = new Thread(new Runnable() {
			public void run(){

				while(true){
					try{

						Socket socket = tcp_socket.accept();
						InetAddress ia = socket.getInetAddress();
						tcp_connected = true;

						pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
						br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						
						tcp_sendMsg(ProtocoleToken.WELC);

						// Lecture des messages entrant
						while(tcp_connected){

							String st_mess = br.readLine();
							Message msg = new Message(st_mess);
							
							// Ajout de l'action dans les logs
							toLogs(msg.toString(), ProtocoleToken.TCP, ProtocoleToken.RECEIVED,
								ia.getHostAddress(), socket.getPort());

							// Interpretation du message
							tcp_readMessage(msg);
						}

						br.close();
						pw.close();
						socket.close();

					}catch (Exception e){
						break;
					}
				}
			}
		});

		/**
	   * Thread d'écoute UDP
	   */
		udp_listening = new Thread(new Runnable() {
			public void run(){
				while(true){
					try{

						// Attente d'un paquet sur le port UDP
						byte[] data = new byte[512];
						DatagramPacket paquet = new DatagramPacket(data, data.length);
						dso.receive(paquet);

						String st = new String(paquet.getData(), 0, paquet.getLength());
						InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

						// Ajout dans les logs
						toLogs(st, ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
							isa.getAddress().getHostAddress(), isa.getPort());

						// Interpretation du message
						Message msg = new Message(st);
						udp_readMessage(msg);

						// Renvoi du message s'il n'a pas fait le tour
						if(last_msg.containsKey(msg.getIdm()) == false){
							isa = new InetSocketAddress(next_ip, udp_nextPort);
							paquet = new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), isa);
							dso.send(paquet);	
						}
						else{
							last_msg.remove(msg.getIdm());
						}

					}catch (Exception e){
						break;
					}
				}
			}
		});

		/**
	   * Thread d'écoute multicast
	   */
		diff_listening = new Thread(new Runnable() {
			public void run(){
				while(true){
					try{

						// Attente d'un paquet sur le port UDP multicast
						byte[] data = new byte[512];
						DatagramPacket paquet = new DatagramPacket(data, data.length);
						mso.receive(paquet);

						String st = new String(paquet.getData(), 0, paquet.getLength());
						InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

						// Ajout dans les logs
						toLogs(st, ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
							isa.getHostName(), isa.getPort());

						// Interpretation du message
						Message msg = new Message(st);
						diff_readMessage(msg);

					}catch (Exception e){
						break;
					}
				}
			}
		});
	}

	/**
   * Méthode lancée lors de l'appel à Machine.start()
   */
	public void run(){
		diff_listening.start();
		udp_listening.start();
		tcp_listening.start();
	}

	/**
   * Lance une connexion vers l'ip et le port distant
   * @param ip IP de la machine distante
   * @param port Port de la machine distante
   */
	public void tcp_connectTo(String ip, short port){
		try{

			InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(ip), port);
			Socket socket = new Socket();

			socket.connect(isa, 1000);
			tcp_connected = true;

			pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// Lecture des messages entrant
			while(tcp_connected){

				String st_mess = br.readLine();
				Message msg = new Message(st_mess);
				
				// Ajout de l'action dans les logs
				toLogs(msg.toString(), ProtocoleToken.TCP, ProtocoleToken.RECEIVED,
					isa.getAddress().getHostAddress(), socket.getPort());

				// Interpretation du message
				tcp_readMessage(msg);
			}

			br.close();
			pw.close();

			socket.close();

		}catch (Exception e){
			System.out.println(e);
		}
	}

	/**
   * Interpretation du message TCP
   * @param msg Message 
   * @throws IOException Lance une exception en cas de problème
   */
	public void tcp_readMessage(Message msg) throws IOException{

		// Comportements définis en fonction du prefixe
		switch(msg.getPrefix()){
			case WELC:
				// Modification des informations
				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();
				this.ip_multdif = msg.getIp_diff();
				this.multdif_port = msg.getPort_diff();
				this.mso = new MulticastSocket(this.multdif_port);
				this.mso.joinGroup(InetAddress.getByName(ip_multdif));

				tcp_sendMsg(ProtocoleToken.NEWC);
			break;

			case NEWC:
				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();

				tcp_sendMsg(ProtocoleToken.ACKC);
				this.udp_connected = true;
				this.tcp_connected = false;
			break;

			case ACKC:
				this.udp_connected = true;
				this.tcp_connected = false;
			break;
		}
	}

	/**
   * Interpretation du message UDP
   * @param msg Message 
   * @throws IOException Lance une exception en cas de problème
   */
	public void udp_readMessage(Message msg) throws IOException{

		switch(msg.getPrefix()){
			case TEST:
			break;
			case APPL:
			break;
			case WHOS:
			break;
			case MEMB:
			break;
			case GBYE:
				if(msg.getIp().equals(this.next_ip) && msg.getPort() == this.udp_nextPort){

					udp_sendMsg(ProtocoleToken.EYBG);

					this.next_ip = msg.getIp_succ();
					this.udp_nextPort = msg.getPort_succ();

					if(this.ip.equals(this.next_ip) && this.udp_listenPort == this.udp_nextPort)
						this.udp_connected = false;
				}
			break;
			case EYBG:
				last_msg.put(msg.getIdm(), msg.toString());
				this.next_ip = this.ip;
				this.udp_nextPort = this.udp_listenPort;
				this.udp_connected = false;
			break;
		}
	}

	/**
   * Interpretation du message UDP multicast
   * @param msg Message 
   * @throws IOException Lance une exception en cas de problème
   */
	public void diff_readMessage(Message msg) throws IOException{

		// Comportements définis en fonction du prefixe
		switch(msg.getPrefix()){
			case DOWN:
				this.next_ip = ip;
				this.udp_nextPort = udp_listenPort;
				this.udp_connected = false;
			break;
		}
	}

	/**
   * Permet d'envoyer un message via TCP
   * @param token Type de message à envoyer
   * @throws IOException Lance une exception en cas de problème
   */
	public void tcp_sendMsg(ProtocoleToken token) throws IOException{
		
		Message msg = null; 

		switch(token){
			case WELC:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.WELC);
				msg.setIp(next_ip);
				msg.setIp_diff(ip_multdif);
				msg.setPort((short)udp_nextPort);
				msg.setPort_diff((short)multdif_port);
			break;
			case NEWC:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.NEWC);
				msg.setIp(ip);
				msg.setPort((short)udp_listenPort);
			break;
			case ACKC:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.ACKC);
			break;
		}

		if(msg != null){
			pw.print(msg.toString());
			pw.flush();
		}
	}

	/**
   * Permet d'envoyer un message via UDP
   * @param token Type de message à envoyer
   * @throws IOException Lance une exception en cas de problème
   */
	public void udp_sendMsg(ProtocoleToken token) throws IOException{
		
		Message msg = null; 

		switch(token){

			case TEST:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.TEST);
				msg.setIdm();
				msg.setIp_diff(ip_multdif);
				msg.setPort_diff(multdif_port);
				last_idm_test = msg.getIdm();
			break;

			case GBYE:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.GBYE);
				msg.setIdm();
				msg.setIp(this.ip);
				msg.setPort(this.udp_listenPort);
				msg.setIp_succ(this.next_ip);
				msg.setPort_succ(this.udp_nextPort);
			break;

			case EYBG:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.EYBG);
				msg.setIdm();
			break;
		}

		if(msg != null){
			// On ajoute le message dans l'historique des envois
			last_msg.put(msg.getIdm(), msg.toString());

			// Envoi du message
			InetSocketAddress isa = new InetSocketAddress(next_ip, udp_nextPort);
			DatagramPacket paquet = new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), isa);
			dso.send(paquet);	

			//System.out.println("");
			//System.out.println("  > "+this.ident+" send : "+msg);
		}
	}

	/**
   * Envoi le message correspondant au token via UDP multicast
   * @param token Type de message à envoyer
   * @throws IOException Lance une exception en cas de problème
   */
	public void diff_sendMsg(ProtocoleToken token) throws IOException{

		Message msg = null; 

		switch(token){
			case DOWN:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.DOWN);
			break;
		}

		if(msg != null){
			// On ajoute le message dans l'historique des envois
			last_msg.put(msg.getIdm(), msg.toString());

			// Envoi du message
			InetSocketAddress isa = new InetSocketAddress(ip_multdif, multdif_port);
			DatagramPacket paquet = new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), isa);
			dso.send(paquet);				
		}
	}

	/**
   * Méthode permetant de sortir de l'anneau
   * @throws IOException Lance une exception en cas de problème
   */
	public void leaveRing() throws IOException{
		udp_sendMsg(ProtocoleToken.GBYE);
	}

	/**
   * Lance une procédure de TEST via UDP
   * @throws IOException Lance une exception en cas de problème
   */
	public void testRing() throws IOException{
		try{
			udp_sendMsg(ProtocoleToken.TEST);

			// Début du timer de la procédure de TEST
			start_time = System.currentTimeMillis();
			while(true){
				if(System.currentTimeMillis() - start_time < delay){

					if(last_msg.containsKey(last_idm_test) == false){
						System.out.println(" -> Structure is correct.");
						break;
					}
				}
				else{
					if(last_msg.containsKey(last_idm_test) == true){
						System.out.println(" -> Structure is broken. [DOWN] sent on multicast.");
						diff_sendMsg(ProtocoleToken.DOWN);
						last_msg.remove(last_idm_test);
						break;
					}	
					else{
						System.out.println(" -> Structure is correct.");
						break;
					}
				}
				Thread.sleep(1);
			}

		}catch (Exception e){
			System.out.println(" -> Structure is broken. [DOWN] sent on multicast.");
			diff_sendMsg(ProtocoleToken.DOWN);
		}
	}


	/**
   * Inscrit un message dans les logs
   *
   * @param msg Message a inscrire
   * @param mode UDP ou TCP
   * @param direction SENT ou RECEIVED
   * @param ip IP de la source
   * @param port Port de la source
   */
	public void toLogs(String msg, ProtocoleToken mode, ProtocoleToken direction ,String ip, int port){
		Date current_time = new Date();
		String str_cur_time = (new SimpleDateFormat("HH:mm")).format(current_time);

		String st_direct = "";
		if(direction == ProtocoleToken.RECEIVED)
			st_direct = "received from";
		else if (direction == ProtocoleToken.SENT)
			st_direct = "sent to";

		logs.add(String.format(" > (%s) %s %d bytes %s %s:%04d : \n  | - [ %s ]\n  | ", 
			str_cur_time, mode ,msg.length(), st_direct, ip, port,
			 ((msg.length() > 100) ? msg.substring(0,100)+".." : msg.substring(0,msg.length()-1))));

	}

	/**
   * Fonction de transformation
   * @return Représentation sous forme de string
   */
	public String toString(){
		String m = String.format("%s [ %s | %s | %s | %d | %d | %d | %d ]",
			ident, ip, ip_multdif, next_ip, tcp_listenPort, udp_listenPort, udp_nextPort, multdif_port);

		return m;
	}

	/**
   * Renvoi un identifiant unique basé sur le timestamp depuis 1970
   * @return String random ident
   */
	private String getRandomIdent(){
		Date date = new Date();
		Timestamp time = new Timestamp(date.getTime());
		int since1970 = time.hashCode();
		return Integer.toString(since1970);
	}

	/**
   * Renvoi les logs
   * @return logs
   */
	public LinkedList<String> getLogs(){
		return this.logs;
	}

	/**
   * Renvoi l'identifiant
   * @return ident
   */
	public String getIdent(){
		return this.ident;
	}

	/**
   * Renvoi le port TCP
   * @return tcp_listenPort
   */
	public short getPortTCP(){
		return this.tcp_listenPort;
	}

	/**
   * Renvoi le port UDP
   * @return udp_listenPort
   */
	public short getPortUDP(){
		return this.udp_listenPort;
	}

	/**
   * Renvoi le port diff
   * @return multdif_port
   */
	public short getPort_diff(){
		return this.multdif_port;
	}

	/**
   * Renvoi le port succ
   * @return udp_nextPort
   */
	public short getPort_succ(){
		return this.udp_nextPort;
	}

	/**
   * Renvoi l'ip
   * @return ip
   */
	public String getIp(){
		return this.ip;
	}

	/**
   * Renvoi l'ip_succ
   * @return next_ip
   */
	public String getIp_succ(){
		return this.next_ip;
	}

	/**
   * Renvoi l'ip_diff
   * @return ip_multdif
   */
	public String getIp_diff(){
		return this.ip_multdif;
	}

	/**
   * Renvoi l'état de connexion TCP
   * @return tcp_connected
   */
	public boolean tcp_isConnected(){
		return this.tcp_connected;
	}

	/**
   * Renvoi l'état de connexion UDP
   * @return udp_connected
   */
	public boolean udp_isConnected(){
		return this.udp_connected;
	}

	/**
   * Ferme tout les sockets
   */
	public void stop(){
		try{
			this.dso.close();
			this.mso.close();
			this.tcp_socket.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
   * Affiche la description
   */
	public void describeMe(){
		System.out.println("*****************************");
		System.out.println(" -> ident : " + this.ident);
		System.out.println(" -> ip_multdif : " + this.ip_multdif);
		System.out.println(" -> next_ip : " + this.next_ip);
		System.out.println(" -> tcp_listenPort : " + this.tcp_listenPort);
		System.out.println(" -> udp_listenPort : " + this.udp_listenPort);
		System.out.println(" -> udp_nextPort : " + this.udp_nextPort);
		System.out.println(" -> multdif_port : " + this.multdif_port);
		System.out.println("-----------------------------");
	}

}