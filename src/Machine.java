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
	private short muldif_port;

	private ServerSocket tcp_socket; 
	
	private LinkedList<String> logs = new LinkedList<String>();

	private boolean running = true;
	private Thread udp_listening = null;
	private Thread tcp_listening = null;

	private DatagramSocket dso = null;

	public Machine(){
		this.ident = getRandomIdent();
		this.ip_multdif = null;
		this.tcp_listenPort = 0;
		this.udp_listenPort = 0;
		this.udp_nextPort = 0;
		this.muldif_port = 0;

		try{
			this.tcp_socket = new ServerSocket(tcp_listenPort);
			this.ip = InetAddress.getLocalHost().getHostAddress();
			this.next_ip = ip;
		}catch(Exception e){
			e.printStackTrace();
		}

		describeMe();
	}

	public Machine(String ip_multdif, short tcp_listenPort, short udp_listenPort, short muldif_port){
		this.ident = getRandomIdent();
		this.ip_multdif = ip_multdif;
		this.tcp_listenPort = tcp_listenPort;
		this.udp_listenPort = udp_listenPort;
		this.udp_nextPort = udp_listenPort;
		this.muldif_port = muldif_port;


		try{

			this.dso = new DatagramSocket(this.udp_listenPort);
			this.tcp_socket = new ServerSocket(tcp_listenPort);
			this.ip = InetAddress.getLocalHost().getHostAddress();
			this.next_ip = ip;

		}catch (Exception e){
			e.printStackTrace();
		}

		udp_listening = new Thread(new Runnable() {
			public void run(){
				while(true){
					try{
					
						byte[] data = new byte[512];
						DatagramPacket paquet = new DatagramPacket(data, data.length);

						dso.receive(paquet);
						String st = new String(paquet.getData(), 0, paquet.getLength());
						InetSocketAddress isa = (InetSocketAddress)paquet.getSocketAddress();

						Date current_time = new Date();
						String str_cur_time = (new SimpleDateFormat("HH:mm")).format(current_time);

						logs.add(String.format(" > (%s) UDP %d bytes received from %s:%04d : \n   	| - [ %s ]\n   	| ", 
							str_cur_time, paquet.getLength(), isa.getHostName(), isa.getPort(), ((st.length() > 100) ? st.substring(0,100)+".." : st.substring(0, st.length()-1))));

						if(udp_nextPort != 0){
							String mess = st;
							paquet = new DatagramPacket(mess.getBytes(), mess.length(), isa);
							dso.send(paquet);	
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
						BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

						Message mess = new Message();
						mess.setPrefix(PrefixMsg.WELC);
						mess.setIp(ip);
						mess.setIp_diff(ip_multdif);
						mess.setPort((short)udp_listenPort);
						mess.setPort_diff((short)muldif_port);

						Date current_time = new Date();
						String str_cur_time = (new SimpleDateFormat("HH:mm")).format(current_time);

						logs.add(String.format(" > (%s) TCP %d bytes sent to %s:%04d : \n   	| - [ %s ]\n   	| ", 
							str_cur_time, mess.toString().length(), ia.getHostName(), socket.getPort(),
							 ((mess.toString().length() > 100) ? mess.toString().substring(0,100)+".." : mess.toString().substring(0,mess.toString().length()-1))));

						pw.print(mess.toString());
						pw.flush();

						/*
						String mess = br.readLine();
						System.out.println(mess);

						br.close();
						*/

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

			InetAddress ia = socket.getInetAddress();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			String st_mess = br.readLine();
			Message mess_recu = new Message(st_mess);

			Date current_time = new Date();
			String str_cur_time = (new SimpleDateFormat("HH:mm")).format(current_time);

			logs.add(String.format(" > (%s) TCP %d bytes received from %s:%04d : \n   	| - [ %s ]\n   	| ", 
				str_cur_time, mess_recu.toString().length(), ia.getHostName(), socket.getPort(),
				 ((mess_recu.toString().length() > 100) ? mess_recu.toString().substring(0,100)+".." : mess_recu.toString().substring(0,mess_recu.toString().length()-1))));

			System.out.println(mess_recu);

			/*
			pw.print("Bonjour serveur !\n");
			pw.flush();
			*/

			pw.close();
			br.close();
			socket.close();

		}catch (Exception e){
			System.out.println(e);
		}
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
		String m = String.format("%s [%s | %s | %s | %d | %d | %d | %d]",
			ident, ip, ip_multdif, next_ip, tcp_listenPort, udp_listenPort, udp_nextPort, muldif_port);

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
		System.out.println(" -> muldif_port : " + this.muldif_port);
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