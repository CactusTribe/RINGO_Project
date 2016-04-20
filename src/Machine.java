import java.util.*;
import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;


public class Machine implements Runnable{

	private String ident;
	private String ip;
	private String ip_multdif;
	private String next_ip;
	private short udp_listenPort;
	private short tcp_listenPort;
	private short udp_nextPort;
	private short multdif_port;
	private boolean connected;

	private ServerSocket tcp_socket; 
	
	private LinkedList<String> logs = new LinkedList<String>();

	private Thread udp_listening;
	private Thread tcp_listening;
	private Thread diff_listening;

	private Thread timerTest; // Boucle du timer
	private long start_time; // Premiere mesure du temps
	private int delay = 5000; // Interval de temps pour le TEST en ms
	private int last_idm_test; // L'id du dernier message TEST envoyé

	private DatagramSocket dso;
	private MulticastSocket mso;
	private PrintWriter pw;
	private BufferedReader br;

	private Hashtable last_msg;

	public Machine(){
		this.ident = getRandomIdent();
		this.ip_multdif = null;
		this.tcp_listenPort = 0;
		this.udp_listenPort = 0;
		this.udp_nextPort = 0;
		this.multdif_port = 0;
		this.last_msg = new Hashtable();

		try{
			this.tcp_socket = new ServerSocket(tcp_listenPort);
			this.ip = InetAddress.getLocalHost().getHostAddress();
			this.next_ip = ip;
		}catch(Exception e){
			e.printStackTrace();
		}

		describeMe();
	}

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
		this.ip = InetAddress.getLocalHost().getHostAddress();
		this.next_ip = ip;


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

		tcp_listening = new Thread(new Runnable() {
			public void run(){

				while(true){
					try{

						Socket socket = tcp_socket.accept();
						pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

						Message mess = new Message();
						mess.setPrefix(PrefixMsg.WELC);
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

	public void run(){
		diff_listening.start();
		udp_listening.start();
		tcp_listening.start();
	}

	public void tcp_connectTo(String ip, short port){
		try{

			Socket socket = new Socket(ip, port);
			tcp_readMessages(socket);
			socket.close();

		}catch (Exception e){
			System.out.println(e);
		}
	}

	public void leaveRing() throws IOException{
		Message msg = new Message();
		msg.setPrefix(PrefixMsg.GBYE);
		msg.setIdm();
		msg.setIp(this.ip);
		msg.setPort(this.udp_listenPort);
		msg.setIp_succ(this.next_ip);
		msg.setPort_succ(this.udp_nextPort);

		udp_sendMsg(msg);
	}

	public void tcp_readMessages(Socket socket) throws IOException{

		InetAddress ia = socket.getInetAddress();
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

		while(true){

			String st_mess = br.readLine();
			Message msg = new Message(st_mess);

			toLogs(msg.toString(), ProtocoleToken.TCP, ProtocoleToken.RECEIVED,
				ia.getHostName(), socket.getPort());

			System.out.println("  > "+this.ident +" read : "+ msg);

			if(msg.getPrefix() == PrefixMsg.WELC){

				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();
				this.ip_multdif = msg.getIp_diff();
				this.multdif_port = msg.getPort_diff();
				this.mso = new MulticastSocket(this.multdif_port);
				this.mso.joinGroup(InetAddress.getByName(ip_multdif));

				Message rep = new Message();
				rep.setPrefix(PrefixMsg.NEWC);
				rep.setIp(ip);
				rep.setPort((short)udp_listenPort);

				pw.print(rep.toString());
				pw.flush();

				this.connected = true;
			}
			else if(msg.getPrefix() == PrefixMsg.NEWC){

				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();

				Message rep = new Message();
				rep.setPrefix(PrefixMsg.ACKC);

				pw.print(rep.toString());
				pw.flush();

				this.connected = true;
				break;
			}
			else if(msg.getPrefix() == PrefixMsg.ACKC){
				break;
			}
		}

		pw.close();
		br.close();
	}

	public void udp_readMessages() throws IOException{

		byte[] data = new byte[512];
		DatagramPacket paquet = new DatagramPacket(data, data.length);

		dso.receive(paquet);
		String st = new String(paquet.getData(), 0, paquet.getLength());
		InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

		toLogs(st, ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
			isa.getHostName(), isa.getPort());

		Message msg = new Message(st);

		if(msg.getPrefix() == PrefixMsg.TEST){

		}
		else if(msg.getPrefix() == PrefixMsg.APPL){

		}
		else if(msg.getPrefix() == PrefixMsg.WHOS){

		}
		else if(msg.getPrefix() == PrefixMsg.MEMB){

		}
		else if(msg.getPrefix() == PrefixMsg.GBYE){
			if(msg.getIp().equals(this.next_ip) && msg.getPort() == this.udp_nextPort){
				Message rep = new Message();
				rep.setPrefix(PrefixMsg.EYBG);
				rep.setIdm();

				isa = new InetSocketAddress(this.next_ip, this.udp_nextPort);
				paquet = new DatagramPacket(rep.toString().getBytes(), rep.toString().length(), isa);
				dso.send(paquet);	

				last_msg.put(msg.getIdm(), msg.toString());

				this.next_ip = msg.getIp_succ();
				this.udp_nextPort = msg.getPort_succ();
			}

		}
		else if(msg.getPrefix() == PrefixMsg.EYBG){
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

	public void diff_readMessages() throws IOException{

		byte[] data = new byte[512];
		DatagramPacket paquet = new DatagramPacket(data, data.length);
		
		mso.receive(paquet);
		String st = new String(paquet.getData(), 0, paquet.getLength());
		InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

		toLogs(st, ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
			isa.getHostName(), isa.getPort());

		Message msg = new Message(st);

		if(msg.getPrefix() == PrefixMsg.DOWN){
			this.next_ip = ip;
			this.udp_nextPort = udp_listenPort;
			this.connected = false;
		}
	}

	public void udp_sendTest() throws IOException{
		Message test = new Message();
		test.setPrefix(PrefixMsg.TEST);
		test.setIdm();
		test.setIp_diff(ip_multdif);
		test.setPort_diff(multdif_port);

		System.out.println("");
		System.out.println("  > "+this.ident+" send : "+test);	

		try{

			InetSocketAddress isa = new InetSocketAddress(next_ip, udp_nextPort);
			DatagramPacket paquet = new DatagramPacket(test.toString().getBytes(), test.toString().length(), isa);
			dso.send(paquet);	

			last_msg.put(test.getIdm(), test.toString());
			last_idm_test = test.getIdm();

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

	public void udp_sendMsg(Message msg) throws IOException{
		System.out.println("  > "+this.ident+" send : "+msg);	

		InetSocketAddress isa = new InetSocketAddress(next_ip, udp_nextPort);
		DatagramPacket paquet = new DatagramPacket(msg.toString().getBytes(), msg.toString().length(), isa);
		dso.send(paquet);	
	}

	public void diff_sendDown() throws IOException{
		Message dw = new Message();
		dw.setPrefix(PrefixMsg.DOWN);

		last_msg.put(dw.getIdm(), dw.toString());

		InetSocketAddress isa = new InetSocketAddress(ip_multdif, multdif_port);
		DatagramPacket paquet = new DatagramPacket(dw.toString().getBytes(), dw.toString().length(), isa);
		dso.send(paquet);	
	}

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

	private String getRandomIdent(){
		Date date = new Date();
		Timestamp time = new Timestamp(date.getTime());
		int since1970 = time.hashCode();
		return Integer.toString(since1970);
	}

	public LinkedList<String> getLogs(){
		return this.logs;
	}

	public String toString(){
		String m = String.format("%s [ %s | %s | %s | %d | %d | %d | %d ]",
			ident, ip, ip_multdif, next_ip, tcp_listenPort, udp_listenPort, udp_nextPort, multdif_port);

		return m;
	}

	public String getIdent(){
		return this.ident;
	}

	public short getPortTCP(){
		return this.tcp_listenPort;
	}

	public short getPortUDP(){
		return this.udp_listenPort;
	}

	public String getIp(){
		return this.ip;
	}

	public boolean isConnected(){
		return this.connected;
	}

	public void stop(){
		try{
			this.dso.close();
			this.mso.close();
			this.tcp_socket.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

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

	public static void main(String[] args){
		if(args.length < 4){
			System.out.println("Usage: Machine <ip_multdif> <tcp_port> <udp_port> <multdif_port>");
			System.exit(1);
		}

		try{

			String ip_multdif = args[0];
			short tcp_port = (short)Integer.parseInt(args[1]);
			short udp_port = (short)Integer.parseInt(args[2]);
			short multdif_port = (short)Integer.parseInt(args[3]);

			Machine m = new Machine(ip_multdif, tcp_port, udp_port, multdif_port);
			Thread t_machine = new Thread(m);
			t_machine.start();

		}catch (Exception e){
			System.out.println("Usage: Machine <ip_multdif> <tcp_port> <udp_port> <multdif_port>");
		}
	}
}