import java.util.*;
import java.net.*;
import java.io.*;

/**
 * RINGO_Project est une application permetant de gérer un anneau
 * (Utilisée également comme outil de développement afin de faciliter les tests) 	
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public class RINGO_Project{

	// Premiers ports à utiliser
	public static final short first_tcp = 5900; 
	public static final short first_udp = 6000;
	public static final short first_diff = 7000;

	public static short current_tcp = first_tcp;
	public static short current_udp = first_udp;
	public static short current_diff = first_diff;

	// Gestion des commandes
	public static Scanner input = new Scanner(System.in);
	public static String argl; // Ligne de commande brute
  public static ArrayList<String> argv = new ArrayList<String>(); // Liste des arguments


  public static LinkedList<Machine> machines = new LinkedList<Machine>();
  public static DatagramSocket dso = null;

	public static void main(String[] args){

		System.out.println("<*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*>");
		System.out.println("#                                                          #");
		System.out.println("#                   <*> RINGO Project <*>                  #");
		System.out.println("#                                                          #");
		System.out.println("# Authors: Lefranc Joaquim, Plat Guillaume, Skoda Jérôme   #");
		System.out.println("<*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*><*>\n");

		/*
		Message mess = new Message("WELC 192.168.001.025 6000 127.000.000.001 7000\n");
		mess.print(); // Debug
		System.out.println(mess); // toString()
		*/

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
      System.out.print("[a]Add [r]Remove [c]Connect [d]Duplication [D]Disconnect\n[l]Logs [W]Write [t]Test [w]Who [s]Stats [q]Quit : ");
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
      newMachine();
    }
    else if(argv.get(0).equals("r")){
    	removeMachine();
    }
    else if(argv.get(0).equals("c")){
    	connectMachine();
    }
    else if(argv.get(0).equals("d")){
    	connectMachineToDup();
    }
    else if(argv.get(0).equals("D")){
    	disconnectMachine();
    }
    else if(argv.get(0).equals("l")){
    	printLogs();
    }
    else if(argv.get(0).equals("W")){
      writeMessage();
    }
    else if(argv.get(0).equals("t")){
      testRing();
    }
    else if(argv.get(0).equals("w")){
      whosRing();
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
        System.out.format("Command %s doesn't exist.\n", argl);
    }
  }

  /**
   * Affiche la liste des machines
   */
	public static void printStats(){
		System.out.println("\n");
		System.out.println("  |  N° - (state) IDENT [    HOST    |  IP_MULT  |    NEXT_IP   | TCP | UDP | NXT_UDP | MULT | DUPL | UDP_DUP ]");
		System.out.println("  |  ----------------------------------------------------------------------------------------------------------");
		for(int i=0; i<machines.size(); i++)
			System.out.println("  |  "+i+" - ("+((machines.get(i).udp_isConnected()) ? "C" : "A") +") "+machines.get(i));
		System.out.println("  |");
		System.out.println("  | Nb machines : "+machines.size());
		System.out.println("\n");
	}


	/**
   * Affiche les logs d'une machine
   */
	public static void printLogs(){
	  if(argv.size() <= 1)
  		System.out.println("Usage: l <num_machine>");
  	else{
			try { 

				int num_machine = Integer.parseInt(argv.get(1));
				if(num_machine < machines.size()){
					System.out.println("");
					System.out.println("  Machine "+num_machine);
					System.out.println("  *------------------*");
					LinkedList<String> logs = machines.get(num_machine).getLogs();
					for(int i=0; i<logs.size(); i++)
						System.out.println("  |"+logs.get(logs.size()-1-i));
					System.out.println("\n");
				}
				else{
					System.out.println("Error : machine doesn't exist.");
				}

			}
			catch (Exception e){
			 System.out.println("Usage: l <num_machine>");
			}
		}	
	}

	/**
   * Ajoute une nouvelle machine avec des valeurs par défaut si les champs
   * sont vides.
   */
	public static void newMachine(){
		String ip = "";
		short tcp_port = 0;
		short udp_port = 0;
		short multdif_port = 0;

		if(argv.size() <= 1){
			System.out.println("");
			System.out.println("  *-----------* Add machine *-----------*");
			System.out.println("  | - Press ENTER for default value.");
			System.out.println("  |-------------------------------------");
			System.out.print("  | IP diff : ");
			ip = input.nextLine();
			if(ip.equals("")) ip = "225.1.2.4";

			System.out.print("  | TCP port : ");
			try { 
				tcp_port = (short)Integer.parseInt(input.nextLine());
			} 
			catch (Exception e){ 
				tcp_port = current_tcp; 
				current_tcp++;
			};

			System.out.print("  | UDP port : ");
			try { 
				udp_port = (short)Integer.parseInt(input.nextLine());
			} 
			catch (Exception e){ 
				udp_port = current_udp;
				current_udp++;
			};

			System.out.print("  | Multdif port : ");
			try { 
				multdif_port = (short)Integer.parseInt(input.nextLine());
			} 
			catch (Exception e){ 
				//multdif_port = 7000;
				
				multdif_port = current_diff;
				current_diff++;
				
			};

			System.out.println("  *-------------------------------------*");

			try{

				Machine m = new Machine(ip, tcp_port, udp_port, multdif_port);
				machines.add(m);
				(new Thread(m)).start();

				System.out.println(" -> New machine "+m.getIdent()+" run at "+ m.getIp() + " TCP("+m.getPortTCP()+") UDP(" + m.getPortUDP()+")");
				printStats();

			}catch (Exception e){
				System.out.println(e);
			}
		}
		else{
			int nb_machines = Integer.parseInt(argv.get(1));
			System.out.println("");
			for(int i=0; i<nb_machines; i++){

				try{
					Machine m = new Machine("225.1.2.4", current_tcp, current_udp, current_diff);
					current_tcp++;
					current_udp++;
					current_diff++;

					machines.add(m);
					(new Thread(m)).start();
					System.out.println(" -> New machine "+m.getIdent()+" run at "+ m.getIp() + " TCP("+m.getPortTCP()+") UDP(" + m.getPortUDP()+")");

				}catch (Exception e){
					System.out.println(e);
				}

			}

			printStats();
		}

	}

	/**
   * Supprime une machine de la liste
   */
	public static void removeMachine(){
    if(argv.size() <= 1)
  		System.out.println("Usage: r <num_machine>");

  	else{
      System.out.println(" -> Remove machine number : "+argv.get(1));
			try {

				int num_machine = Integer.parseInt(argv.get(1));
				if(num_machine < machines.size()){
					machines.get(num_machine).stop();
					machines.remove(num_machine);
					printStats();
				}
				else
					System.out.println("Error : machine doesn't exist.");

			}
			catch (Exception e){
			 System.out.println("Usage: r <num_machine>");
			}
		}
	}

	/**
   * Connecte une machine à une autre sur l'anneau
   */
	public static void connectMachine(){
		if(argv.size() <= 2)
			System.out.println("Usage: c <machine1> <machine2> OR c <machine> <ip> <port> (Connect machine1 to machine2 in TCP)");
		else{
			// Connexion via numero
			if(argv.size() == 3){
				try{
					int m1 = Integer.parseInt(argv.get(1));
					int m2 = Integer.parseInt(argv.get(2));

					if(m1 < machines.size() && m2 < machines.size()){

						if(machines.get(m1).tcp_isConnected() == false){
							System.out.println("");
							machines.get(m1).tcp_connectTo(machines.get(m2).getIp(), machines.get(m2).getPortTCP(), false);
							printStats();
						}
						else
							System.out.println("Error : machine already connected.");
					}
					else
						System.out.println("Error : machine doesn't exist.");

					
				}catch (Exception e){
					System.out.println("Usage: c <machine1> <machine2> OR c <machine> <ip> <port> (Connect machine1 to machine2 in TCP)");
				}				
			}
			// Connexion via IP/PORT
			else if(argv.size() == 4){
				try{
					int m = Integer.parseInt(argv.get(1));
					String ip = argv.get(2);
					short port = (short)Integer.parseInt(argv.get(3));

					if(m < machines.size()){

						if(machines.get(m).tcp_isConnected() == false){
							System.out.println("");
							machines.get(m).tcp_connectTo(ip, port, false);
							printStats();
						}
						else
							System.out.println("Error : machine already connected.");
					}
					else
						System.out.println("Error : machine doesn't exist.");

					
				}catch (Exception e){
					System.out.println("Usage: c <machine1> <machine2> OR c <machine> <ip> <port> (Connect machine1 to machine2 in TCP)");
				}	
			}

		}
	}


	/**
   * Connecte une machine à une autre qui devient un duplicateur
   */
	public static void connectMachineToDup(){
		if(argv.size() <= 2)
			System.out.println("Usage: d <machine1> <duplicator> OR d <machine> <ip> <port> (Connect machine1 to duplicator in TCP)");
		else{
			// Connexion via numero
			if(argv.size() == 3){
				try{
					int m1 = Integer.parseInt(argv.get(1));
					int m2 = Integer.parseInt(argv.get(2));

					if(m1 < machines.size() && m2 < machines.size()){

						if(machines.get(m1).tcp_isConnected() == false){
							System.out.println("");
							machines.get(m1).tcp_connectTo(machines.get(m2).getIp(), machines.get(m2).getPortTCP(), true);
							printStats();
						}
						else
							System.out.println("Error : machine already connected.");
					}
					else
						System.out.println("Error : machine doesn't exist.");

					
				}catch (Exception e){
					System.out.println("Usage: d <machine1> <duplicator> OR d <machine> <ip> <port> (Connect machine1 to duplicator in TCP)");
				}		
			}
			// Connexion via IP/PORT
			else if(argv.size() == 4){
				try{
					int m = Integer.parseInt(argv.get(1));
					String ip = argv.get(2);
					short port = (short)Integer.parseInt(argv.get(3));

					if(m < machines.size()){

						if(machines.get(m).tcp_isConnected() == false){
							System.out.println("");
							machines.get(m).tcp_connectTo(ip, port, true);
							printStats();
						}
						else
							System.out.println("Error : machine already connected.");
					}
					else
						System.out.println("Error : machine doesn't exist.");

					
				}catch (Exception e){
					System.out.println("Usage: d <machine1> <duplicator> OR d <machine> <ip> <port> (Connect machine1 to duplicator in TCP)");
				}	
			}
		}
	}

	/**
   * Deconnecte une machine
   */
	public static void disconnectMachine(){
		if(argv.size() <= 1)
			System.out.println("Usage: D <machine> (Disonnect machine)");
		else{
			try{
				int m1 = Integer.parseInt(argv.get(1));

				if(m1 < machines.size()){
					if(machines.get(m1).udp_isConnected() == true){
						System.out.println("");
						machines.get(m1).leaveRing();
						printStats();
					}
					else
						System.out.println("Error : machine is not connected.");
				}
				else
					System.out.println("Error : machine doesn't exist.");

				
			}catch (Exception e){
				System.out.println("Usage: D <machine> (Disonnect machine)");
			}
		}
	}

	/**
   * Lance un test à partir du point donné en argument
   */
	public static void testRing(){
		if(argv.size() <= 1)
			System.out.println("Usage: t <machine> (Send TEST into the ring)");
		else{
			try{
				int m1 = Integer.parseInt(argv.get(1));

				if(m1 < machines.size()){
					machines.get(m1).testRing();
				}
				else
					System.out.println("Error : machine doesn't exist.");

				
			}catch (Exception e){
				System.out.println("Usage: t <machine> (Send TEST into the ring)");
			}
		}	
	}

	/**
   * Lance un WHOS à partir du point donné en argument
   */
	public static void whosRing(){
		if(argv.size() <= 1)
			System.out.println("Usage: w <machine> (Send WHOS into the ring)");
		else{
			try{
				int m1 = Integer.parseInt(argv.get(1));

				if(m1 < machines.size()){
					machines.get(m1).whosRing();
				}
				else
					System.out.println("Error : machine doesn't exist.");

				
			}catch (Exception e){
				System.out.println("Usage: w <machine> (Send WHOS into the ring)");
			}
		}	
	}

	/**
   * Ecrit un message sur le port UDP choisi
   */
	public static void writeMessage(){
		int udp_port = 0;
		String mess = "";

		System.out.println("");
		System.out.println("  *-----------* Write message *-----------*");
		System.out.print("  | UDP port : ");
		udp_port = input.nextInt();
		input.nextLine();
		System.out.print("  | Message : ");
		mess = input.nextLine()+"\n";
		System.out.println("  *---------------------------------------*");
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

	/**
   * Eteint toutes les machines.
   */
	public static void stopMachines(){
		for(int i=0; i<machines.size(); i++){
			machines.get(i).stop();
		}
	}
}
