import java.util.*;

public class RINGO_Project{

	public static LinkedList<Machine> machines = new LinkedList<Machine>();
	public static Scanner sc = new Scanner(System.in);

	public static void main(String[] args){

		boolean exit = false; 

		System.out.println("------------------------------------------\n");
		System.out.println("              <*> New RING <*>            \n");
		printStats();
		
		String buff = "";

		while(exit != true){
			System.out.println("[a]Add [r]Remove [s]Stats [q]Quit");
			buff = sc.nextLine();

			switch(buff){
				case "a":
					addNewMachine();
				break;
				case "r":
				break;
				case "s":
					printStats();
				break;
				case "q":
					exit = true;
				break;
				default:
				break;
			}
		}

		Machine m1 = new Machine("192.168.0.0", 5900, 6000, 7000);
		
		Message mess = new Message("WELC127.000.000.001127.000.000.002127.000.000.00360016002700000000000000000000000000000000000003040000000800000002056");
		mess.print();
		System.out.println(mess);
	}

	public static void printStats(){
		System.out.println("	*-----------* Stats *-----------*");
		System.out.println("	| Nb machines : "+machines.size());
		System.out.println("	|  - IDENT [IP_MULT | NEXT_IP | TCP | UDP | NEXT_UDP | MULT]");
		for(int i=0; i<machines.size(); i++)
			System.out.println("	|  - "+machines.get(i));
		System.out.println("	*-------------------------------*");
	}

	public static void addNewMachine(){
		String ip = "";
		int tcp_port = 0;
		int udp_port = 0;
		int multdif_port = 0;

		System.out.println("	*-----------* Adding machine *-----------*");
		System.out.print("	| IP : ");
		ip = sc.nextLine();
		System.out.print("	| TCP port : ");
		tcp_port = sc.nextInt();
		System.out.print("	| UDP port : ");
		udp_port = sc.nextInt();
		System.out.print("	| Multdif port : ");
		multdif_port = sc.nextInt();
		sc.nextLine();
		System.out.println("	*----------------------------------------*");

		Machine m = new Machine(ip, tcp_port, udp_port, multdif_port);
		machines.add(m);
		System.out.println(" -> New machine "+m.getIdent()+" run at "+ip+"\n");
		printStats();
	}
}
