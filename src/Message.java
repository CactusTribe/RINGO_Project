import java.util.*;

/**
 * Classe Message
 * Permet de d'utiliser des messages formatés plus simple d'utilisations
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */
public class Message{

	// Champs du message
	private ProtocoleToken prefix = null;
	private String ip = "";
	private String ip_succ = "";
	private String ip_diff = "";
	private short port = 0;
	private short port_succ = 0;
	private short port_diff = 0;
	private int idm = 0;
	private int id_trans = 0;
	private int id_app = 0;
	private int id = 0;
	private short size_mess = 0;
	private short size_nom = 0;
	private int num_mess = 0;
	private int no_mess = 0;
	private short size_content = 0;
	private String message_app = "";

	/**
	 * Constructeur par defaut
	 */
	public Message(){

	}

	/**
	 * Constructeur
	 *
	 * Construit un message à partir d'une string
	 * @param mess Message à construire
	 */
	public Message(String mess){
		if(mess.length() <= 512){
			ArrayList<String> argv = new ArrayList<String>(Arrays.asList(mess.split("\\s+")));

			this.prefix = ProtocoleToken.valueOf(argv.get(0));

			if(prefix == ProtocoleToken.WELC){
				this.ip = argv.get(1);
				this.ip_diff = argv.get(3);
				this.port = (short)Integer.parseInt(argv.get(2));
				this.port_diff = (short)Integer.parseInt(argv.get(4));
			}
			else if(prefix == ProtocoleToken.NEWC){
				this.ip = argv.get(1);
				this.port = (short)Integer.parseInt(argv.get(2));
			}
			else if(prefix == ProtocoleToken.ACKC){
			}
			else if(prefix == ProtocoleToken.APPL){
				this.idm = Integer.parseInt(argv.get(1));
				this.id_app = Integer.parseInt(argv.get(2));
				this.message_app = argv.get(3);
			}
			else if(prefix == ProtocoleToken.TEST){
				this.idm = Integer.parseInt(argv.get(1));
				this.ip_diff = argv.get(2);
				this.port_diff = (short)Integer.parseInt(argv.get(3));
			}
			else if(prefix == ProtocoleToken.DOWN){
			}
			else if(prefix == ProtocoleToken.WHOS){
				this.idm = Integer.parseInt(argv.get(1));
			}
			else if(prefix == ProtocoleToken.MEMB){
				this.idm = Integer.parseInt(argv.get(1));
			}
			else if(prefix == ProtocoleToken.GBYE){
				this.idm = Integer.parseInt(argv.get(1));
				this.ip = argv.get(2);
				this.port = (short)Integer.parseInt(argv.get(3));
				this.ip_succ = argv.get(4);
				this.port_succ = (short)Integer.parseInt(argv.get(5));
			}
			else if(prefix == ProtocoleToken.EYBG){
				this.idm = Integer.parseInt(argv.get(1));
			}
		}
	}

	/**
	 * Retourne le message sous forme de string
	 * @return Représentation du message en string
	 */
	public String toString(){
		String mess = "";

		if(prefix == ProtocoleToken.WELC){
			mess = String.format("%s %s %04d %s %04d\n",
				prefix,ip,port,ip_diff,port_diff);
		}
		else if(prefix == ProtocoleToken.NEWC){
			mess = String.format("%s %s %04d\n",
				prefix,ip,port);
		}
		else if(prefix == ProtocoleToken.ACKC){
			mess = String.format("%s\n",prefix);
		}
		else if(prefix == ProtocoleToken.APPL){
			mess = String.format("%s %08d %08d %s\n",
				prefix,idm,id_app,message_app);
		}
		else if(prefix == ProtocoleToken.TEST){
			mess = String.format("%s %08d %s %04d\n",
				prefix,idm,ip_diff,port_diff);
		}
		else if(prefix == ProtocoleToken.DOWN){
			mess = String.format("%s\n", prefix);
		}
		else if(prefix == ProtocoleToken.WHOS){
			mess = String.format("%s %08d\n", 
				prefix, idm);
		}
		else if(prefix == ProtocoleToken.MEMB){
			mess = String.format("%s %08d\n", 
				prefix, idm);
		}
		else if(prefix == ProtocoleToken.GBYE){
			mess = String.format("%s %08d %s %04d %s %04d\n", 
				prefix, idm, ip, port, ip_succ, port_succ);
		}
		else if(prefix == ProtocoleToken.EYBG){
			mess = String.format("%s %08d\n", 
				prefix, idm);
		}

		return mess;
	}

	/**
	 * Affichage le message
	 */
	public void print(){
		String mess = String.format("#########################################\n"
				+" - TYPE : %s\n"
				+" - IP : %s\n"
				+" - IP_SUCC : %s\n"
				+" - IP_DIFF : %s\n"
				+" - PORT : %04d\n"
				+" - PORT_SUCC : %04d\n"
				+" - PORT_DIFF : %04d\n"
				+" - IDM : %08d\n"
				+" - ID_TRANS : %08d\n"
				+" - ID_APP : %08d\n"
				+" - ID : %08d\n"
				+" - SIZE_MESS : %03d\n"
				+" - SIZE_NOM : %02d\n"
				+" - NUM_MESS : %08d\n"
				+" - NO_MESS : %08d\n"
				+" - SIZE_CONTENT : %03d\n"
				+"#########################################\n",
				prefix,ip,ip_succ,ip_diff,port,port_succ,port_diff,
				idm,id_trans,id_app,id,size_mess,size_nom,num_mess,
				no_mess,size_content);
	
		System.out.println(mess);
	}

	/**
	 * Modifie le prefixe
	 * @param pref Nouveau prefixe
	 */
	public void setPrefix(ProtocoleToken pref){
		this.prefix = pref;
	}

	/**
	 * Modifie l'IP
	 * @param ip Nouvelle ip
	 */
	public void setIp(String ip){
		this.ip = ip;
	}

	/**
	 * Modifie l'Ip_succ
	 * @param ip Nouvelle ip
	 */
	public void setIp_succ(String ip){
		this.ip_succ = ip;
	}

	/**
	 * Modifie le port
	 * @param p Nouveau port
	 */
	public void setPort(short p){
		this.port = p;
	}

	/**
	 * Modifie le port_succ
	 * @param p Nouveau port_succ
	 */
	public void setPort_succ(short p){
		this.port_succ = p;
	}

	/**
	 * Modifie l'ip_diff
	 * @param ip_diff Nouvelle ip
	 */
	public void setIp_diff(String ip_diff){
		this.ip_diff = ip_diff;
	}

	/**
	 * Modifie le port_diff
	 * @param port_diff Nouveau port_diff
	 */
	public void setPort_diff(short port_diff){
		this.port_diff = port_diff;
	}

	/**
	 * Donne la valeur courante du timestamp
	 */
	public void setIdm(){
		this.idm = (int) (new Date().getTime()/1000);
	}

	/**
	 * Retourne le prefixe
	 * @return prefix
	 */
	public ProtocoleToken getPrefix(){
		return this.prefix;
	}

	/**
	 * Retourne l'ip
	 * @return ip
	 */
	public String getIp(){
		return this.ip;
	}

	/**
	 * Retourne l'ip_succ
	 * @return ip_succ
	 */
	public String getIp_succ(){
		return this.ip_succ;
	}


	/**
	 * Retourne le port
	 * @return port
	 */
	public short getPort(){
		return this.port;
	}

	/**
	 * Retourne le port_succ
	 * @return port_succ
	 */
	public short getPort_succ(){
		return this.port_succ;
	}

	/**
	 * Retourne l'ip_diff
	 * @return ip_diff
	 */
	public String getIp_diff(){
		return this.ip_diff;
	}

	/**
	 * Retourne le port_diff
	 * @return port_diff
	 */
	public short getPort_diff(){
		return this.port_diff;
	}

	/**
	 * Retourne l'idm
	 * @return idm
	 */
	public int getIdm(){
		return this.idm;
	}
}



