import java.util.*;
import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;


public class Machine implements Runnable{

	private String ident;
	private String ip_multdif;
	private String next_ip;
	private int udp_listenPort;
	private int tcp_listenPort;
	private int udp_nextPort;
	private int muldif_port;

	private LinkedList<String> logs = new LinkedList<String>();

	private boolean running = true;
	private Thread udp_listening = null;

	private DatagramSocket dso = null;

	public Machine(){
		this.ident = getRandomIdent();
		this.ip_multdif = null;
		this.next_ip = null;
		this.tcp_listenPort = 0;
		this.udp_listenPort = 0;
		this.udp_nextPort = 0;
		this.muldif_port = 0;

		describeMe();
	}

	public Machine(String ip_multdif, int tcp_listenPort, int udp_listenPort, int muldif_port){
		this.ident = getRandomIdent();
		this.ip_multdif = ip_multdif;
		this.next_ip = null;
		this.tcp_listenPort = tcp_listenPort;
		this.udp_listenPort = udp_listenPort;
		this.udp_nextPort = 0;
		this.muldif_port = muldif_port;
		try{
			this.dso = new DatagramSocket(this.udp_listenPort);
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
						InetSocketAddress ia = (InetSocketAddress)paquet.getSocketAddress();

						Date current_time = new Date();
						String str_cur_time = (new SimpleDateFormat("HH:mm")).format(current_time);
						logs.add(" > ("+str_cur_time+") UDP "+paquet.getLength()+" bytes received from "+ia.getHostName()+":"+ia.getPort()+" : ["+((st.length() > 30) ? st.substring(0,30)+".." : st)+"]");

						if(udp_nextPort != 0){
							String mess = ia.getHostName()+":"+ia.getPort()+" "+st;
							paquet = new DatagramPacket(mess.getBytes(), mess.length(), ia);
							dso.send(paquet);	
						}

					}catch (Exception e){
						break;
					}
				}
			}
		});
	}

	public void run(){
		udp_listening.start();
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
		String m = String.format("%s [%s | %s | %d | %d | %d | %d]",
			ident, ip_multdif, next_ip, tcp_listenPort, udp_listenPort, udp_nextPort, muldif_port);

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

	public void stop(){
		this.dso.close();
		this.running = false;
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