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

	private ServerSocket tcp_socket; 
	
	private LinkedList<String> logs = new LinkedList<String>();

	private boolean running = true;
	private Thread udp_listening;
	private Thread tcp_listening;

	private DatagramSocket dso;
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
		this.tcp_socket = new ServerSocket(tcp_listenPort);
		this.ip = InetAddress.getLocalHost().getHostAddress();
		this.next_ip = ip;

		udp_listening = new Thread(new Runnable() {
			public void run(){
				while(true){
					try{

						byte[] data = new byte[512];
						DatagramPacket paquet = new DatagramPacket(data, data.length);

						dso.receive(paquet);
						String st = new String(paquet.getData(), 0, paquet.getLength());
						InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

						toLogs(st, ProtocoleToken.UDP, ProtocoleToken.RECEIVED, 
							isa.getHostName(), isa.getPort());

						Message mess = new Message(st);

						if(last_msg.containsKey(mess.getIdm()) == false){

							isa = new InetSocketAddress(next_ip, udp_nextPort);
							paquet = new DatagramPacket(mess.toString().getBytes(), mess.toString().length(), isa);
							dso.send(paquet);	
						}
						else{
							last_msg.remove(mess.getIdm());
						}

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
						InetAddress ia = socket.getInetAddress();
						pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

						Message mess = new Message();
						mess.setPrefix(PrefixMsg.WELC);
						mess.setIp(ip);
						mess.setIp_diff(ip_multdif);
						mess.setPort((short)udp_listenPort);
						mess.setPort_diff((short)multdif_port);

						pw.print(mess.toString());
						pw.flush();

						readMessages(socket);
					
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
		udp_listening.start();
		tcp_listening.start();
	}

	public void tcp_connectTo(String ip, short port){
		try{

			Socket socket = new Socket(ip, port);
			
			readMessages(socket);

			socket.close();

		}catch (Exception e){
			System.out.println(e);
		}
	}

	public void sendTest() throws IOException{
		Message test = new Message();
		test.setPrefix(PrefixMsg.TEST);
		test.setIdm((int) (new Date().getTime()/1000));
		test.setIp_diff(ip_multdif);
		test.setPort_diff(multdif_port);

		System.out.println(test);

		last_msg.put(test.getIdm(), test.toString());

		InetSocketAddress isa = new InetSocketAddress(next_ip, udp_nextPort);
		DatagramPacket paquet = new DatagramPacket(test.toString().getBytes(), test.toString().length(), isa);
		dso.send(paquet);	
	}

	public void readMessages(Socket socket) throws IOException{

		InetAddress ia = socket.getInetAddress();
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

		while(true){

			String st_mess = br.readLine();
			Message msg = new Message(st_mess);

			toLogs(msg.toString(), ProtocoleToken.TCP, ProtocoleToken.RECEIVED,
				ia.getHostName(), socket.getPort());

			System.out.println(this.ident +" read "+ msg);

			if(msg.getPrefix() == PrefixMsg.WELC){
				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();

				Message rep = new Message();
				rep.setPrefix(PrefixMsg.NEWC);
				rep.setIp(ip);
				rep.setPort((short)udp_listenPort);

				pw.print(rep.toString());
				pw.flush();
			}
			else if(msg.getPrefix() == PrefixMsg.NEWC){
				this.udp_nextPort = msg.getPort();
				this.next_ip = msg.getIp();

				Message rep = new Message();
				rep.setPrefix(PrefixMsg.ACKC);

				pw.print(rep.toString());
				pw.flush();
				break;
			}
		}

		pw.close();
		br.close();
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

	public String getIdent(){
		return this.ident;
	}

	public boolean getState(){
		return this.running;
	}

	public short getPortTCP(){
		return this.tcp_listenPort;
	}

	public String getIp(){
		return this.ip;
	}

	public void stop(){
		try{
			this.dso.close();
			this.tcp_socket.close();
			this.running = false;
		}catch (Exception e){
			e.printStackTrace();
		}
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