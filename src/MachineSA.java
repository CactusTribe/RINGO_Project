import java.util.*;
import java.net.*;
import java.io.*;

/**
 * MachineSA est une application permetant de lancer une 
 * machine standalone	
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public class MachineSA{

	// Gestion des commandes standalone
	public static Machine m;
	public static Thread t_machine;
	public static Scanner input = new Scanner(System.in);
	public static String argl; // Ligne de commande brute
  public static ArrayList<String> argv = new ArrayList<String>(); // Liste des arguments

	/**
   * Méthode main permetant un lancement standalone de la machine
   * @param args Paramètres de lancement
   */
	public static void main(String[] args){
		
		if(args.length < 4){
			System.out.println("Usage: Machine <ip_multdif> <tcp_port> <udp_port> <multdif_port>");
			System.exit(1);
		}

		try{

			System.out.println("<*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*>");
			System.out.println("#                                                          #");
			System.out.println("#                      <*> Machine <*>                     #");
			System.out.println("#                                                          #");
			System.out.println("# Authors: Lefranc Joaquim, Plat Guillaume, Skoda Jérôme   #");
			System.out.println("<*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*>\n");
			System.out.println("  | Init..");

			String ip_multdif = args[0];
			short tcp_port = (short)Integer.parseInt(args[1]);
			short udp_port = (short)Integer.parseInt(args[2]);
			short multdif_port = (short)Integer.parseInt(args[3]);

			m = new Machine(ip_multdif, tcp_port, udp_port, multdif_port);
			t_machine = new Thread(m);
			t_machine.start();

			System.out.println(" -> New machine "+m.getIdent()+" run at "+ m.getIp() + " TCP("+m.getPortTCP()+") UDP(" + m.getPortUDP()+")\n");

		}catch (Exception e){
			System.out.println("Usage: Machine <ip_multdif> <tcp_port> <udp_port> <multdif_port>");
		}

		// Boucle d'execution des commandes
		while(true){
		  display_prompt();
		  read_command();
		  tokenize_command();
		  execute_command();
		}
	}

  /**
   * Affiche une nouvelle ligne pour la prochaine commande
   */
  public static void display_prompt(){
      System.out.print("[c]Connect [d]Disconnect [l]Logs [t]Test \n[w]Who [i]Info [q]Quit : ");
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
			System.exit(0);
    }  
    else if(argv.get(0).equals("c")){
    	if(argv.size() > 2)
    		m.tcp_connectTo(argv.get(1), (short)Integer.parseInt(argv.get(2)));
    	else
    		System.out.println("Usage: c <ip> <tcp_port> (Connect machine to another in TCP)");
    }
    else if(argv.get(0).equals("d")){
    	try{
    		System.out.println("");
    		m.leaveRing();
    	}catch (Exception e){
    		e.printStackTrace();
    	}
    }
    else if(argv.get(0).equals("l")){
			System.out.println("");
			LinkedList<String> logs = m.getLogs();
			for(int i=0; i<logs.size(); i++)
				System.out.println("  |"+logs.get(logs.size()-1-i));
			System.out.println("\n");
    }
    else if(argv.get(0).equals("t")){
    	try{
      	m.testRing();
      }catch (Exception e){
    		e.printStackTrace();
    	}
    }
    else if(argv.get(0).equals("w")){
    	try{
      	m.whosRing();
      }catch (Exception e){
    		e.printStackTrace();
    	}
    }
    else if(argv.get(0).equals("i")){
    	System.out.println("");
    	System.out.println("  | Machine ID : "+m.getIdent());
    	System.out.println("  | IP : "+m.getIp());
    	System.out.println("  | TCP : "+m.getPortTCP()+" | UDP : "+m.getPortUDP());
    	System.out.println("  | IP multicast : "+m.getIp_diff());
    	System.out.println("  | UDP multicast : "+m.getPort_diff());
    	System.out.println("  | State : "+ ((m.udp_isConnected()) ? " <-> Connected to "+m.getIp_succ()+":"+m.getPort_succ() : " <x> Deconnected") );
    	System.out.println("");
    }
    else if(argv.get(0).equals("q")){
    	m.stop();
      System.out.println("Bye.");
			System.exit(0);
    }
    else{
        System.out.format("Command %s doesn't exist.\n", argl);
    }
  }

}