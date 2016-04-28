import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Classe Machine
 * Permet de lancer une machine	
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public class Machine implements Runnable{

	public static final int MAX_SIZE_MSG = 512;
	public static final int MAX_SIZE_FILE_CONTENT = 462;

	// Informations de connexion
	private String ident;
	private String ip;
	private String ip_multdif;
	private String next_ip;
	private short udp_listenPort;
	private short tcp_listenPort;
	private short udp_nextPort;
	private short multdif_port;

	// Informations de connexion du deuxieme anneau
	private String ip_multdif_dup;
	private String next_ip_dup;
	private short udp_nextPort_dup;
	private short multdif_port_dup;

	// Status
	private boolean udp_connected;
	private boolean tcp_connected;

	private boolean duplicator;
	private boolean connectedToDuplicator;

	// Runnables d'écoutes
	private Runnable udp_listening;
	private Runnable tcp_listening;
	private Runnable multicast_listening;

	private int delay = 3000; // Interval de temps pour le TEST en ms
	private int last_idm_test; // L'id du dernier message TEST envoyé

	// Entrées / Sorties
	private ServerSocket tcp_socket; 
	private DatagramSocket dso;
	private MulticastSocket mso;
	private PrintWriter pw;
	private BufferedReader br;

	// Liste des messages déjà lu
	private Hashtable<Integer,String> received_msg;
	// Liste des messages envoyés
	private Hashtable<Integer,String> sent_msg;
	// Historique des recus
	private LinkedList<String> logs;
	// Liste des applications disponibles
	private LinkedList<AppToken> apps;

	// Application TRANS
	private int cur_file_trans; // L'id de transaction de fichier courante
	private int nb_msg_total; // Nombre de messages à recevoir
	private int nb_msg_received; // Nombre de messages recus
	private String name_file_receveid = ""; // Nom du fichier recu
	private ArrayList<byte[]> file_receveid; // Fichier recu

	/**
   * Constructeur
   * @param ip_diff Adresse IP de multicast
	 * @param tcp_port Port d'écoute TCP
	 * @param udp_port Port d'écoute UDP
	 * @param diff_port Port d'écoute UDP multicast
	 * @throws IOException Lance une exception en cas de problème
   */
	public Machine(String ip_diff, short tcp_port, short udp_port, short diff_port) throws Exception{

		this.ident = getRandomIdent();
		this.ip_multdif = ip_diff;
		this.tcp_listenPort = tcp_port;
		this.udp_listenPort = udp_port;
		this.udp_nextPort = udp_port;
		this.multdif_port = diff_port;
		this.received_msg = new Hashtable<Integer,String>();
		this.sent_msg = new Hashtable<Integer,String>();
		this.logs = new LinkedList<String>();

		// Ajout des applications supportées
		this.apps = new LinkedList<AppToken>();
		this.apps.add(AppToken.DIFF);
		this.apps.add(AppToken.TRANS);

		this.udp_connected = false;
		this.tcp_connected = false;
		this.duplicator = false;
		this.connectedToDuplicator = false;

		this.tcp_socket = new ServerSocket(tcp_listenPort);
		this.dso = new DatagramSocket(this.udp_listenPort);

		this.mso = new MulticastSocket(this.multdif_port);
		this.mso.joinGroup(InetAddress.getByName(this.ip_multdif));

		// Recherche de l'ip 192.x.x.x (Nécessaire pour linux)
		Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
		while(en.hasMoreElements()){
	    NetworkInterface ni = en.nextElement();
	    Enumeration<InetAddress> ee = ni.getInetAddresses();

	    while(ee.hasMoreElements()) {
        InetAddress ia = ee.nextElement();
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
		tcp_listening = new Runnable() {
			public void run(){

				while(true){
					try{

						Socket socket = tcp_socket.accept();
						InetAddress ia = socket.getInetAddress();

						pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
						br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						
						if(duplicator == false){
							tcp_sendMsg(ProtocoleToken.WELC);
							tcp_connected = true;
						}
						else{
							tcp_sendMsg(ProtocoleToken.NOTC);
							tcp_connected = false;
						}

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
		};

		/**
	   * Thread d'écoute UDP
	   */
		udp_listening = new Runnable() {
			public void run(){
				while(true){
					try{

						// Attente d'un paquet sur le port UDP
						byte[] data = new byte[MAX_SIZE_MSG];
						DatagramPacket paquet = new DatagramPacket(data, data.length);
						dso.receive(paquet);

						String st = new String(paquet.getData(), 0, paquet.getLength());
						InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

						Message msg = new Message(st);

						// Si on a pas déjà recu le message
						if(received_msg.containsKey(msg.getIdm()) == false){

							// Interpretation du message
							udp_readMessage(msg);

							// Ajout dans les logs
							toLogs(msg.toString(), ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
								isa.getAddress().getHostAddress(), isa.getPort());

						}
					}catch (MalformedMsgException e){
						//System.out.println("udp_listening: malformed message");
					}catch (SocketException e){
						break;
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		};

		/**
	   * Thread d'écoute multicast
	   */
		multicast_listening = new Runnable(){
			public void run(){
				while(true){
					try{

						// Attente d'un paquet sur le port UDP multicast
						byte[] data = new byte[MAX_SIZE_MSG];
						DatagramPacket paquet = new DatagramPacket(data, data.length);
						mso.receive(paquet);

						String st = new String(paquet.getData(), 0, paquet.getLength());
						InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

						// Interpretation du message
						Message msg = new Message(st);
						diff_readMessage(msg);

						// Ajout dans les logs
						toLogs(msg.toString(), ProtocoleToken.DIFF, ProtocoleToken.RECEIVED, 
							isa.getAddress().getHostAddress(), isa.getPort());

					}catch (Exception e){
						break;
					}
				}
			}
		};
	}


	/**
   * Méthode lancée lors de l'appel à Machine.start()
   */
	public void run(){
		(new Thread(tcp_listening)).start();
		(new Thread(udp_listening)).start();
		(new Thread(multicast_listening)).start();
	}

	/**
   * Lance une connexion vers l'ip et le port distant
   * @param ip IP de la machine distante
   * @param port Port de la machine distante
   * @param duplication True pour se connecter a une entité qui deviendra doubleur
   */
	public void tcp_connectTo(String ip, short port, boolean duplication){
		try{

			InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(ip), port);
			Socket socket = new Socket();

			socket.connect(isa, 1000);
			tcp_connected = true;

			if(duplication)
				this.connectedToDuplicator = true;

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

				if(this.connectedToDuplicator){
					tcp_sendMsg(ProtocoleToken.DUPL);
				}
				else{
					// On se retire de l'ancien groupe
					this.mso.leaveGroup(InetAddress.getByName(this.ip_multdif));
					// Modification des informations de multicast
					this.ip_multdif = msg.getIp_diff();
					this.multdif_port = msg.getPort_diff();

					// Fermeture de l'ancien socket
					this.mso.close();
					// Nouveau point d'écoute multicast
					this.mso = new MulticastSocket(this.multdif_port);
					this.mso.joinGroup(InetAddress.getByName(this.ip_multdif));
					(new Thread(multicast_listening)).start();

					tcp_sendMsg(ProtocoleToken.NEWC);
				}
			break;

			case NEWC:
				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();

				tcp_sendMsg(ProtocoleToken.ACKC);
				this.udp_connected = true;
				this.tcp_connected = false;
			break;

			case DUPL:
				this.udp_nextPort_dup = msg.getPort();
				this.next_ip_dup = msg.getIp();

				tcp_sendMsg(ProtocoleToken.ACKD);
				this.tcp_connected = false;
				this.duplicator = true;
			break;

			case ACKC:
				this.udp_connected = true;
				this.tcp_connected = false;
			break;

			case ACKD:
				this.udp_nextPort = msg.getPort();
				this.udp_connected = true;
				this.tcp_connected = false;
			break;

			case NOTC:
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
				udp_sendMsg(msg);
			break;

			case APPL:

				if(msg.getId_app() == AppToken.DIFF){
					udp_sendMsg(msg);
				}
				else if(msg.getId_app() == AppToken.TRANS){
					trans_readMsg(msg);
				}

			break;

			case WHOS:
				udp_sendMsg(msg);
				udp_sendNewMsg(ProtocoleToken.MEMB);
			break;

			case MEMB:
				udp_sendMsg(msg);
			break;

			case GBYE:
				if(msg.getIp().equals(this.next_ip) && msg.getPort() == this.udp_nextPort){

					udp_sendNewMsg(ProtocoleToken.EYBG);

					this.next_ip = msg.getIp_succ();
					this.udp_nextPort = msg.getPort_succ();

					if(this.ip.equals(this.next_ip) && this.udp_listenPort == this.udp_nextPort)
						this.udp_connected = false;
				}
				else udp_sendMsg(msg);
			break;

			case EYBG:
				this.next_ip = this.ip;
				this.udp_nextPort = this.udp_listenPort;
				this.udp_connected = false;
			break;
		}
				
		received_msg.put(msg.getIdm(), msg.toString());
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
				this.udp_connected = false;
				this.next_ip = ip;
				this.udp_nextPort = udp_listenPort;
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
				msg.setPort(udp_nextPort);
				msg.setPort_diff(multdif_port);
			break;

			case NEWC:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.NEWC);
				msg.setIp(ip);
				msg.setPort(udp_listenPort);
			break;

			case DUPL:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.DUPL);
				msg.setIp(ip);
				msg.setIp_diff(ip_multdif);
				msg.setPort(udp_listenPort);
				msg.setPort_diff(multdif_port);
			break;

			case ACKC:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.ACKC);
			break;

			case ACKD:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.ACKD);
				msg.setPort(udp_listenPort);
			break;

			case NOTC:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.NOTC);
			break;
		}

		if(msg != null){
			pw.print(msg.toString());
			pw.flush();
		}
	}

	/**
   * Permet d'envoyer un message existant via UDP
   * @param msg Message à envoyer
   * @throws IOException Lance une exception en cas de problème
   */
	public void udp_sendMsg(Message msg) throws IOException{

		if(msg != null){

			InetSocketAddress isa = null;
			DatagramPacket paquet = null;
			byte[] data = msg.toString().getBytes();

			// Si la machine est un duplicateur
			if(isDuplicator()){
				// Si c'est un message de TEST on vérifie la destination
				if(msg.getPrefix() == ProtocoleToken.TEST){

					if(msg.getIp_diff().equals(ip_multdif) && (msg.getPort_diff() == multdif_port))
						isa = new InetSocketAddress(next_ip, udp_nextPort);
					else
						isa = new InetSocketAddress(next_ip_dup, udp_nextPort_dup);

					paquet = new DatagramPacket(data, data.length, isa);
					dso.send(paquet);
				}
				else{
					// Envoi sur l'anneau principal
					isa = new InetSocketAddress(next_ip, udp_nextPort);
					paquet = new DatagramPacket(data, data.length, isa);
					dso.send(paquet);	

					// Envoi sur le deuxieme anneau
					isa = new InetSocketAddress(next_ip_dup, udp_nextPort_dup);
					paquet = new DatagramPacket(data, data.length, isa);
					dso.send(paquet);
				}
			}
			else{
				isa = new InetSocketAddress(next_ip, udp_nextPort);
				paquet = new DatagramPacket(data, data.length, isa);
				dso.send(paquet);	
			} 

			// Ajout dans la liste des messages envoyés
			sent_msg.put(msg.getIdm(), msg.toString());
		}
	}

	/**
   * Permet d'envoyer un nouveau message via UDP
   * @param token Type de message à envoyer
   * @throws IOException Lance une exception en cas de problème
   */
	public void udp_sendNewMsg(ProtocoleToken token) throws IOException{
		
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

			case WHOS:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.WHOS);
				msg.setIdm();
			break;

			case MEMB:
				msg = new Message();
				msg.setPrefix(ProtocoleToken.MEMB);
				msg.setIdm();
				msg.setId(this.ident);
				msg.setIp(this.ip);
				msg.setPort(this.udp_listenPort);
			break;
		}

		udp_sendMsg(msg);
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
			// Envoi du message
			InetSocketAddress isa = new InetSocketAddress(ip_multdif, multdif_port);
			DatagramPacket paquet = new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), isa);
			dso.send(paquet);
		}
	}

	/**
   * Méthode permetant d'executer une application
   * @param app Identifiant de l'application
   */
	public void executeApp(AppToken app){
		if(this.apps.contains(app)){
			System.out.println("\n -> Execution of "+ app);

			switch(app){
				case DIFF:
					app_DIFF();
				break;
				case TRANS:
					app_TRANS();
				break;
			}

		}
		else{
			System.out.println(" -> Error: "+ app +" is not supported.");
		}
	}

	/**
   * Application de diffusion
   */
	public void app_DIFF(){
		String st_msg = "";
		Scanner sc = new Scanner(System.in);

		System.out.println("");
		System.out.print("  | Message : ");
		st_msg = sc.nextLine();
		System.out.println("");

		Message msg = new Message();
		msg.setPrefix(ProtocoleToken.APPL);
		msg.setIdm();
		msg.setId_app(AppToken.DIFF);
		msg.setSize_mess((short)st_msg.length());
		msg.setMessage_app(st_msg);

		try{
			udp_sendMsg(msg);
		}catch(Exception e){
			System.out.println(e);
		}
	}

	/**
   * Application de transfert de fichiers
   */
	public void app_TRANS(){
		String file_name = "";
		Scanner sc = new Scanner(System.in);

		System.out.println("");
		System.out.print("  | File : ");
		file_name = sc.nextLine();
		System.out.println("");

		Message msg = new Message();
		msg.setPrefix(ProtocoleToken.APPL);
		msg.setIdm();
		msg.setId_app(AppToken.TRANS);
		msg.setTrans_token(TransToken.REQ);
		msg.setSize_nom((short)file_name.length());
		msg.setNom_fichier(file_name);

		try{
			udp_sendMsg(msg);
		}catch(Exception e){
			System.out.println(e);
		}
	}

	/**
   * Méthode interpretant un message d'application TRANS
   * @throws IOException Lance une exception en cas de problème
   */
	public void trans_readMsg(Message msg) throws IOException{
		switch(msg.getTrans_token()){

			case REQ:

				String file_name = msg.getNom_fichier();
				File file = new File(file_name);

				// Si le fichier existe
				if(file.isFile()){

					int nb_messages = (int)(file.length() / MAX_SIZE_FILE_CONTENT) + 1;

					Message confirm = new Message();
					confirm.setPrefix(ProtocoleToken.APPL);
					confirm.setIdm();
					confirm.setId_app(AppToken.TRANS);
					confirm.setTrans_token(TransToken.ROK);
					confirm.setId_trans();
					confirm.setSize_nom((short)file_name.length());
					confirm.setNom_fichier(file_name);
					confirm.setNum_mess(nb_messages);

					udp_sendMsg(confirm);

					// Division du fichier en morceaux
					byte[] content = Files.readAllBytes(Paths.get(file_name));
					ArrayList<byte[]> p_content = new ArrayList<byte[]>();
					int len = content.length;

					for(int i=0; i < len; i += MAX_SIZE_FILE_CONTENT){
						byte[] p = Arrays.copyOfRange(content, i, Math.min(len, i + MAX_SIZE_FILE_CONTENT));
						p_content.add(p);
					}
					
					// Envoi de toutes les parties
					int no_mess = 0;
			    for(byte[] part : p_content) {

						Message p_file = new Message();
						p_file.setPrefix(ProtocoleToken.APPL);
						p_file.setIdm();
						p_file.setId_app(AppToken.TRANS);
						p_file.setTrans_token(TransToken.SEN);
						p_file.setId_trans(confirm.getId_trans());
			      p_file.setNo_mess(no_mess);
						p_file.setSize_content((short)part.length);
						p_file.setFile_content(part);

						udp_sendMsg(p_file);
						no_mess++;
			    }

				}
				else{
					// Si la machine n'a pas le fichier elle envoi au prochain
					udp_sendMsg(msg);
				}

			break;

			case ROK:

				// Préparation pour recevoir le fichier
				this.cur_file_trans = msg.getId_trans();
				this.nb_msg_total = msg.getNum_mess();
				this.nb_msg_received = 0;
			 	this.name_file_receveid = "cpy_"+msg.getNom_fichier();
			 	this.file_receveid = new ArrayList<byte[]>();

			break;

			case SEN:
				// Si l'id de transmission du message correspond à notre transaction
				if(msg.getId_trans() == this.cur_file_trans){

					if(msg.getNo_mess() == nb_msg_received){
						file_receveid.add(msg.getFile_content());
						this.nb_msg_received++;
					}

					// Une fois que toute les parties sont présentes, on écrit le fichier
					if(this.nb_msg_received == this.nb_msg_total){

						FileOutputStream out = new FileOutputStream(this.name_file_receveid);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();

						for(int i=0; i < file_receveid.size(); i++){
							bos.write(file_receveid.get(i));
						}

						byte[] file_array = bos.toByteArray();
						out.write(file_array);

						out.close();
						bos.close();
					}
				}
				else{
					udp_sendMsg(msg);
				}

			break;
		}
	}

	/**
   * Méthode permetant d'envoyer un WHOS
   * @throws IOException Lance une exception en cas de problème
   */
	public void whosRing() throws IOException{
		udp_sendNewMsg(ProtocoleToken.WHOS);
	}

	/**
   * Méthode permetant de sortir de l'anneau
   * @throws IOException Lance une exception en cas de problème
   */
	public void leaveRing() throws IOException{
		udp_sendNewMsg(ProtocoleToken.GBYE);
	}

	/**
   * Lance une procédure de TEST via UDP
   * @throws Exception Lance une exception en cas de problème
   */
	public void testRing() throws Exception{

		udp_sendNewMsg(ProtocoleToken.TEST);

		boolean brokenRing = true;
		long start_time = System.currentTimeMillis();

		// Début du timer de la procédure de TEST
		while(System.currentTimeMillis() - start_time < delay){

			if(received_msg.containsKey(last_idm_test) == true){
				System.out.println("\n -> Structure is correct.\n");
				brokenRing = false;
				break;
			}
		}
		
		if(brokenRing){
			System.out.println("\n -> Structure is broken. [DOWN] sent on multicast.\n");
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
		String str_cur_time = (new SimpleDateFormat("HH:mm:ss")).format(current_time);

		String st_direct = "";
		if(direction == ProtocoleToken.RECEIVED)
			st_direct = "received from";
		else if (direction == ProtocoleToken.SENT)
			st_direct = "sent to";

		logs.add(String.format(" > (%s) %s %d bytes %s %s:%04d : \n  | - [ %s ]\n  | ", 
			str_cur_time, mode , msg.getBytes().length, st_direct, ip, port,
			 ((msg.length() > 100) ? msg.substring(0,100)+".." : msg.substring(0,msg.length()-1))));

	}

	/**
   * Fonction de transformation
   * @return Représentation sous forme de string
   */
	public String toString(){
		String m = String.format("%s [ %s | %s | %s | %d | %d | %d | %d | %s | %d ]",
			ident, ip, ip_multdif, next_ip, tcp_listenPort, udp_listenPort, udp_nextPort, multdif_port, duplicator, udp_nextPort_dup);

		return m;
	}

	/**
   * Renvoi un identifiant unique basé sur le temps écoulé depuis 1970
   * @return String random ident
   */
	private String getRandomIdent(){
		Date date = new Date();
		Timestamp time = new Timestamp(date.getTime());
		int since1970 = time.hashCode();
		return Tools.intToStr8b(since1970);
	}

	/**
   * Renvoi les logs
   * @return logs
   */
	public LinkedList<String> getLogs(){
		return this.logs;
	}

	/**
   * Renvoi la liste des applications disponibles
   * @return apps
   */
	public LinkedList<AppToken> getApps(){
		return this.apps;
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
   * La machine est un duplicateur ou non
   * @return duplicator
   */
	public boolean isDuplicator(){
		return this.duplicator;
	}

	/**
   * Ferme tout les sockets
   */
	public void stop(){
		try{
			this.dso.close();
			this.mso.leaveGroup(InetAddress.getByName(this.ip_multdif));
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