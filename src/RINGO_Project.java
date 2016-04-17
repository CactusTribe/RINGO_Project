import java.util.*;
import java.net.*;
import java.io.*;

public class RINGO_Project{

	public static Scanner input = new Scanner(System.in);
	public static String argl; // Ligne de commande brute
  public static ArrayList<String> argv = new ArrayList<String>(); // Liste des arguments
  public static LinkedList<Machine> machines = new LinkedList<Machine>();

  public static DatagramSocket dso = null;

	public static void main(String[] args){

		boolean exit = false; 

		System.out.println("------------------------------------------\n");
		System.out.println("              <*> New RING <*>            \n");
		
		while(true){
		  display_prompt();
		  read_command();
		  tokenize_command();
		  execute_command();
		}
		
		/*
		Message mess = new Message("WELC127.000.000.001127.000.000.002127.000.000.00360016002700000000000000000000000000000000000003040000000800000002056");
		mess.print(); // Debug
		System.out.println(mess); // toString()
		*/
	}

  /**
   * Affiche une nouvelle ligne pour la prochaine commande
   */
  public static void display_prompt(){
      System.out.print("[a]Add [r]Remove [l]Logs [w]Write [s]Stats [q]Quit : ");
  }
  
  /**
   * Stock la commande complete dans argl
   */
  public static void read_command(){
      try{
          argl = input.nextLine();   
      }catch (NoSuchElementException e){ // Exception levé lors de Ctrl+D
          argl = "exit";
          System.out.println("Bye.");
      }
  }
  
  /**
   * Découpe la commande en arguments distincts
   */
  public static void tokenize_command(){
      argv = new ArrayList<String>(Arrays.asList(argl.split("\\s+")));
  }

  /**
   * Execute la commande contenue dans argl
   */
  public static void execute_command(){
    if(argv.get(0).equals("exit")){
    	stopMachines();
			System.exit(0);
    }  
    else if(argv.get(0).equals("a")){
      addNewMachine();
    }
    else if(argv.get(0).equals("r")){
      
    }
    else if(argv.get(0).equals("l")){
    	if(argv.size() <= 1)
    		System.out.println("Usage: l <num_machine>");

    	else{
	      System.out.print(" -> Machine number : "+argv.get(1));
				try { 
					int num_machine = Integer.parseInt(argv.get(1));
					printLogs(num_machine);
				}
				catch (Exception e){
				 System.out.println("Usage: l <num_machine>");
				}
			}
    }
    else if(argv.get(0).equals("w")){
      writeMessage();
    }
    else if(argv.get(0).equals("s")){
      printStats();
    }
    else if(argv.get(0).equals("q")){
      stopMachines();
      System.out.println("Bye.");
			System.exit(0);
    }
    else{
        System.out.format("La commande %s n'existe pas.\n", argl);
    }
  }

	public static void printStats(){
		System.out.println("");
		System.out.println("	*-----------* Stats *-----------*");
		System.out.println("	|  N° - (state) IDENT [IP_MULT | NEXT_IP | TCP | UDP | NEXT_UDP | MULT]");
		System.out.println("	|  --------------------------------------------------------------------");
		for(int i=0; i<machines.size(); i++)
			System.out.println("	|  "+i+" - ("+((machines.get(i).getState()) ? "R" : "P") +") "+machines.get(i));
		System.out.println("	|");
		System.out.println("	| Nb machines : "+machines.size());
		System.out.println("	*-------------------------------*\n");
	}

	public static void printLogs(int num_machine){
		if(num_machine < machines.size()){
			System.out.println("");
			System.out.println("	*-----------* Logs *-----------*");
			LinkedList<String> logs = machines.get(num_machine).getLogs();
			for(int i=0; i<logs.size(); i++)
				System.out.println("	|"+logs.get(logs.size()-1-i));
			System.out.println("	*-------------------------------*\n");
		}
		else{
			System.out.println(" Error : machine doesn't exist.");
		}
	}

	public static void addNewMachine(){
		String ip = "";
		int tcp_port = 0;
		int udp_port = 0;
		int multdif_port = 0;

		System.out.println("");
		System.out.println("	*-----------* Add machine *-----------*");

		System.out.print("	| IP : ");
		ip = input.nextLine();
		if(ip.equals("")) ip = "127.0.0.1";

		System.out.print("	| TCP port : ");
		try { tcp_port = Integer.parseInt(input.nextLine());} 
		catch (Exception e){ tcp_port = 5900;};

		System.out.print("	| UDP port : ");
		try { udp_port = Integer.parseInt(input.nextLine());} 
		catch (Exception e){ udp_port = 6000;};

		System.out.print("	| Multdif port : ");
		try { multdif_port = Integer.parseInt(input.nextLine());} 
		catch (Exception e){ multdif_port = 7000;};

		System.out.println("	*-------------------------------------*");

		Machine m = new Machine(ip, tcp_port, udp_port, multdif_port);
		machines.add(m);
		(new Thread(m)).start();
		System.out.println(" -> New machine "+m.getIdent()+" run at "+ip);
		printStats();
	}

	public static void writeMessage(){
		int udp_port = 0;
		String mess = "";

		System.out.println("");
		System.out.println("	*-----------* Write message *-----------*");
		System.out.print("	| UDP port : ");
		udp_port = input.nextInt();
		input.nextLine();
		System.out.print("	| Message : ");
		mess = input.nextLine();
		System.out.println("	*---------------------------------------*");
		System.out.println("\n -> Message sent to "+udp_port+".\n");

		try{

			if(dso == null)
				dso = new DatagramSocket(8000);
	
			InetSocketAddress ai = new InetSocketAddress("127.0.0.1", udp_port);
			DatagramPacket paquet = new DatagramPacket(mess.getBytes(), mess.length(), ai);
			dso.send(paquet);

		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void stopMachines(){
		for(int i=0; i<machines.size(); i++){
			machines.get(i).stop();
		}
	}
}
