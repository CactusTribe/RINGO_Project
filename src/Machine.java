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
	private boolean connected;
	
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
   * Constructeur par defaut
   */
	public Machine(){
		this.ident = getRandomIdent();
		this.ip_multdif = null;
		this.tcp_listenPort = 0;
		this.udp_listenPort = 0;
		this.udp_nextPort = 0;
		this.multdif_port = 0;
		this.last_msg = new Hashtable();

		try{
			this.tcp_socket = new ServerSocket(5900);
			this.ip = InetAddress.getLocalHost().getHostAddress();
			this.next_ip = ip;
		}catch(Exception e){
			e.printStackTrace();
		}

		describeMe();
	}

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
	   * Thread d'écoute multicast
	   */
		diff_listening = new Thread(new Runnable() {
			public void run(){
				while(true){
					try{
						diff_readMessages();
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
						udp_readMessages();
					}catch (Exception e){
						break;
					}
				}
			}
		});

		/**
	   * Thread d'écoute TCP
	   */
		tcp_listening = new Thread(new Runnable() {
			public void run(){

				while(true){
					try{

						Socket socket = tcp_socket.accept();
						pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

						Message mess = new Message();
						mess.setPrefix(ProtocoleToken.WELC);
						mess.setIp(next_ip);
						mess.setIp_diff(ip_multdif);
						mess.setPort((short)udp_nextPort);
						mess.setPort_diff((short)multdif_port);

						pw.print(mess.toString());
						pw.flush();

						tcp_readMessages(socket);
					
						pw.close();
						socket.close();

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
			tcp_readMessages(socket);
			socket.close();

		}catch (Exception e){
			System.out.println(e);
		}
	}

	/**
   * Méthode permetant de sortir de l'anneau
   * @throws IOException Lance une exception en cas de problème
   */
	public void leaveRing() throws IOException{
		Message msg = new Message();
		msg.setPrefix(ProtocoleToken.GBYE);
		msg.setIdm();
		msg.setIp(this.ip);
		msg.setPort(this.udp_listenPort);
		msg.setIp_succ(this.next_ip);
		msg.setPort_succ(this.udp_nextPort);

		udp_sendMsg(msg);
	}

	/**
   * Boucle de lecture des messages entrant TCP
   * @param socket Socket à ecouter
   * @throws IOException Lance une exception en cas de problème
   */
	public void tcp_readMessages(Socket socket) throws IOException{

		InetAddress ia = socket.getInetAddress();
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

		while(true){

			// Lecture du message entrant
			String st_mess = br.readLine();
			Message msg = new Message(st_mess);
			//System.out.println("  > "+this.ident +" read : "+ msg);

			// Ajout de l'action dans les logs
			toLogs(msg.toString(), ProtocoleToken.TCP, ProtocoleToken.RECEIVED,
				ia.getHostAddress(), socket.getPort());

			// Comportements définis en fonction du prefixe
			if(msg.getPrefix() == ProtocoleToken.WELC){

				// Modification des informations
				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();
				this.ip_multdif = msg.getIp_diff();
				this.multdif_port = msg.getPort_diff();
				this.mso = new MulticastSocket(this.multdif_port);
				this.mso.joinGroup(InetAddress.getByName(ip_multdif));

				// Envoi de la réponse de connexion
				Message rep = new Message();
				rep.setPrefix(ProtocoleToken.NEWC);
				rep.setIp(ip);
				rep.setPort((short)udp_listenPort);

				pw.print(rep.toString());
				pw.flush();

				this.connected = true;
			}
			else if(msg.getPrefix() == ProtocoleToken.NEWC){

				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();

				// Confirmation de connexion
				Message rep = new Message();
				rep.setPrefix(ProtocoleToken.ACKC);

				pw.print(rep.toString());
				pw.flush();

				this.connected = true;
				break;
			}
			else if(msg.getPrefix() == ProtocoleToken.ACKC){
				break;
			}
		}

		pw.close();
		br.close();
	}

	/**
   * Lecture des messages entrants UDP
   * @throws IOException Lance une exception en cas de problème
   */
	public void udp_readMessages() throws IOException{

		// Attente d'un paquet sur le port UDP
		byte[] data = new byte[512];
		DatagramPacket paquet = new DatagramPacket(data, data.length);
		dso.receive(paquet);

		String st = new String(paquet.getData(), 0, paquet.getLength());
		InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

		// Ajout dans les logs
		toLogs(st, ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
			isa.getAddress().getHostAddress(), isa.getPort());

		Message msg = new Message(st);
		//System.out.println("  > "+this.ident +" read : "+ msg);

		// Interprétation du message
		if(msg.getPrefix() == ProtocoleToken.TEST){

		}
		else if(msg.getPrefix() == ProtocoleToken.APPL){

		}
		else if(msg.getPrefix() == ProtocoleToken.WHOS){

		}
		else if(msg.getPrefix() == ProtocoleToken.MEMB){

		}
		else if(msg.getPrefix() == ProtocoleToken.GBYE){
			if(msg.getIp().equals(this.next_ip) && msg.getPort() == this.udp_nextPort){
				Message rep = new Message();
				rep.setPrefix(ProtocoleToken.EYBG);
				rep.setIdm();

				isa = new InetSocketAddress(this.next_ip, this.udp_nextPort);
				paquet = new DatagramPacket(rep.toString().getBytes(), rep.toString().length(), isa);
				dso.send(paquet);	

				last_msg.put(msg.getIdm(), msg.toString());

				this.next_ip = msg.getIp_succ();
				this.udp_nextPort = msg.getPort_succ();
			}

		}
		else if(msg.getPrefix() == ProtocoleToken.EYBG){
			last_msg.put(msg.getIdm(), msg.toString());
			this.next_ip = this.ip;
			this.udp_nextPort = this.udp_listenPort;
			this.connected = false;
		}

		if(last_msg.containsKey(msg.getIdm()) == false){
			isa = new InetSocketAddress(next_ip, udp_nextPort);
			paquet = new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), isa);
			dso.send(paquet);	
		}
		else{
			last_msg.remove(msg.getIdm());
		}
	}

	/**
   * Lecture des messages entrants UDP multicast
   * @throws IOException Lance une exception en cas de problème
   */
	public void diff_readMessages() throws IOException{

		// Attente d'un paquet sur le port UDP multicast
		byte[] data = new byte[512];
		DatagramPacket paquet = new DatagramPacket(data, data.length);
		mso.receive(paquet);

		String st = new String(paquet.getData(), 0, paquet.getLength());
		InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

		// Ajout dans les logs
		toLogs(st, ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
			isa.getHostName(), isa.getPort());

		Message msg = new Message(st);

		// Interprétation du message
		if(msg.getPrefix() == ProtocoleToken.DOWN){
			this.next_ip = ip;
			this.udp_nextPort = udp_listenPort;
			this.connected = false;
		}
	}

	/**
   * Lance une procédure de TEST via UDP
   * @throws IOException Lance une exception en cas de problème
   */
	public void udp_sendTest() throws IOException{
		Message test = new Message();
		test.setPrefix(ProtocoleToken.TEST);
		test.setIdm();
		test.setIp_diff(ip_multdif);
		test.setPort_diff(multdif_port);

		System.out.println("");
		System.out.println("  > "+this.ident+" send : "+test);	

		try{

			// Envoi du message de TEST
			InetSocketAddress isa = new InetSocketAddress(next_ip, udp_nextPort);
			DatagramPacket paquet = new DatagramPacket(test.toString().getBytes(), test.toString().length(), isa);
			dso.send(paquet);	

			// On ajoute le message dans l'historique des envois
			last_msg.put(test.getIdm(), test.toString());
			last_idm_test = test.getIdm();

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
						diff_sendDown();
						last_msg.remove(test.getIdm());
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
			diff_sendDown();
			e.printStackTrace();
		}
	}

	/**
   * Permet d'envoyer un message via UDP
   * @param msg Message à envoyer
   * @throws IOException Lance une exception en cas de problème
   */
	public void udp_sendMsg(Message msg) throws IOException{
		System.out.println("  > "+this.ident+" send : "+msg);	

		// On ajoute le message dans l'historique des envois
		last_msg.put(msg.getIdm(), msg.toString());

		// Envoi du message
		InetSocketAddress isa = new InetSocketAddress(next_ip, udp_nextPort);
		DatagramPacket paquet = new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), isa);
		dso.send(paquet);	
	}

	/**
   * Lance le message DOWN via UDP multicast
   * @throws IOException Lance une exception en cas de problème
   */
	public void diff_sendDown() throws IOException{
		Message dw = new Message();
		dw.setPrefix(ProtocoleToken.DOWN);

		// On ajoute le message dans l'historique des envois
		last_msg.put(dw.getIdm(), dw.toString());

		// Envoi du message
		InetSocketAddress isa = new InetSocketAddress(ip_multdif, multdif_port);
		DatagramPacket paquet = new DatagramPacket(dw.toString().getBytes(), dw.toString().length(), isa);
		dso.send(paquet);	
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
   * Fonction de transformation
   * @return Représentation sous forme de string
   */
	public String toString(){
		String m = String.format("%s [ %s | %s | %s | %d | %d | %d | %d ]",
			ident, ip, ip_multdif, next_ip, tcp_listenPort, udp_listenPort, udp_nextPort, multdif_port);

		return m;
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
   * Renvoi l'ip
   * @return ip
   */
	public String getIp(){
		return this.ip;
	}

	/**
   * Renvoi l'état de connexion
   * @return connected
   */
	public boolean isConnected(){
		return this.connected;
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