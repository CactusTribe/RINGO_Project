import java.util.*;
import java.net.*;
import java.io.*;
import java.sql.Timestamp;


public class Machine{

	private String ident;
	private String ip_multdif;
	private String next_ip;
	private int udp_listenPort;
	private int tcp_listenPort;
	private int udp_nextPort;
	private int muldif_port;

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

		//describeMe();
		//udp_listening();
		//tcp_listening();
	}

	public void udp_listening(){
		try{

			byte[] data = new byte[512];
			DatagramSocket dso = new DatagramSocket(this.udp_listenPort);
			DatagramPacket paquet = new DatagramPacket(data, data.length);

			System.out.println("Machine "+this.ident+" listen at "+ this.udp_listenPort+"..");

			while(true){
				dso.receive(paquet);
				String st = new String(paquet.getData(), 0, paquet.getLength());
				InetSocketAddress ia = (InetSocketAddress)paquet.getSocketAddress();

				if(this.udp_nextPort != 0){
					String mess = ia.getHostName()+":"+this.udp_nextPort+" "+st;
					paquet = new DatagramPacket(mess.getBytes(), mess.length(), ia);
					dso.send(paquet);	
				}
			}

		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public void tcp_listening(){
		try{



		}catch (Exception e){
			e.printStackTrace();
		}
	}

	private String getRandomIdent(){
		Date date = new Date();
		Timestamp time = new Timestamp(date.getTime());
		int since1970 = time.hashCode();
		return Integer.toString(since1970);
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
}