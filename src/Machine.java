import java.util.*;
import java.sql.Timestamp;
import java.util.Date;


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
		this.udp_listenPort = 0;
		this.tcp_listenPort = 0;
		this.udp_nextPort = 0;
		this.muldif_port = 0;

		describeMe();
	}

	public Machine(String ip_multdif, int udp_listenPort, int tcp_listenPort, int muldif_port){
		this.ident = getRandomIdent();
		this.ip_multdif = ip_multdif;
		this.next_ip = null;
		this.udp_listenPort = udp_listenPort;
		this.tcp_listenPort = tcp_listenPort;
		this.udp_nextPort = 0;
		this.muldif_port = muldif_port;

		describeMe();
	}

	private String getRandomIdent(){
		Date date = new Date();
		Timestamp time = new Timestamp(date.getTime());
		int since1970 = time.hashCode();
		return Integer.toString(since1970);
	}

	public void describeMe(){
		System.out.println("*****************************");
		System.out.println(" -> ident : " + this.ident);
		System.out.println(" -> ip_multdif : " + this.ip_multdif);
		System.out.println(" -> next_ip : " + this.next_ip);
		System.out.println(" -> udp_listenPort : " + this.udp_listenPort);
		System.out.println(" -> tcp_listenPort : " + this.tcp_listenPort);
		System.out.println(" -> udp_nextPort : " + this.udp_nextPort);
		System.out.println(" -> muldif_port : " + this.muldif_port);
		System.out.println("-----------------------------");
	}
}