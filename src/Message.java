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

	private String id = "";
	private String id_app = "";

	private long idm = 0;
	private long id_trans = 0;

	private short port = 0;
	private short port_succ = 0;
	private short port_diff = 0;
	
	private short size_mess = 0;
	private short size_nom = 0;
	private short size_content = 0;

	private int num_mess = 0;
	private int no_mess = 0;
	
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

			switch(prefix){
				case WELC:
					this.ip = argv.get(1);
					this.ip_diff = argv.get(3);
					this.port = (short)Integer.parseInt(argv.get(2));
					this.port_diff = (short)Integer.parseInt(argv.get(4));
				break;
				case NEWC:
					this.ip = argv.get(1);
					this.port = (short)Integer.parseInt(argv.get(2));
				break;
				case ACKC:
				break;
				case APPL:
					this.idm = Integer.parseInt(argv.get(1));
					this.id_app = argv.get(2);
					this.message_app = argv.get(3);
				break;
				case TEST:
					this.idm = Integer.parseInt(argv.get(1));
					this.ip_diff = argv.get(2);
					this.port_diff = (short)Integer.parseInt(argv.get(3));
				break;
				case DOWN:
				break;
				case WHOS:
					this.idm = Integer.parseInt(argv.get(1));
				break;
				case MEMB:
					this.idm = Integer.parseInt(argv.get(1));
					this.id = argv.get(2);
					this.ip = argv.get(3);
					this.port = (short)Integer.parseInt(argv.get(4));
				break;
				case GBYE:
					this.idm = Integer.parseInt(argv.get(1));
					this.ip = argv.get(2);
					this.port = (short)Integer.parseInt(argv.get(3));
					this.ip_succ = argv.get(4);
					this.port_succ = (short)Integer.parseInt(argv.get(5));
				break;
				case EYBG:
					this.idm = Integer.parseInt(argv.get(1));
				break;
			}
		}
	}

	/**
	 * Retourne le message sous forme de string
	 * @return Représentation du message en string
	 */
	public String toString(){
		String mess = "";

		switch(prefix){
			case WELC:
				mess = String.format("%s %s %04d %s %04d\n",
					prefix, ip, port, ip_diff, port_diff);
			break;
			case NEWC:
				mess = String.format("%s %s %04d\n",
					prefix, ip, port);
			break;
			case ACKC:
				mess = String.format("%s\n",prefix);
			break;
			case APPL:
				mess = String.format("%s %s %s %s\n",
					prefix, Tools.longToStr8b(idm), id_app, message_app);
			break;
			case TEST:
				mess = String.format("%s %s %s %04d\n",
					prefix, Tools.longToStr8b(idm), ip_diff, port_diff);
			break;
			case DOWN:
				mess = String.format("%s\n", prefix);
			break;
			case WHOS:
				mess = String.format("%s %s\n", 
					prefix, Tools.longToStr8b(idm));
			break;
			case MEMB:
				mess = String.format("%s %s %s %s %04d\n", 
					prefix, Tools.longToStr8b(idm), id, ip, port);
			break;
			case GBYE:
				mess = String.format("%s %s %s %04d %s %04d\n", 
					prefix, Tools.longToStr8b(idm), ip, port, ip_succ, port_succ);
			break;
			case EYBG:
				mess = String.format("%s %s\n", 
					prefix, Tools.longToStr8b(idm));
			break;
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
		this.ip = Tools.addZerosToIp(ip);
	}

	/**
	 * Modifie l'Ip_succ
	 * @param ip Nouvelle ip
	 */
	public void setIp_succ(String ip){
		this.ip_succ = Tools.addZerosToIp(ip);
	}

	/**
	 * Modifie l'ip_diff
	 * @param ip_diff Nouvelle ip
	 */
	public void setIp_diff(String ip_diff){
		this.ip_diff = Tools.addZerosToIp(ip_diff);
	}

	/**
	 * Modifie l'ID
	 * @param id Nouvel id
	 */
	public void setId(String id){
		if(id.length() > 8)
			this.id = id.substring(0, 8);
		else
			this.id = id;
	}

	/**
	 * Modifie l'id_app
	 * @param id_app Nouvelle id_app
	 */
	public void setId_app(String id_app){
		if(id_app.length() > 8)
			this.id_app = id_app.substring(0, 8);
		else
			this.id_app = id_app;
	}

	/**
	 * Donne la valeur courante du timestamp a idm
	 */
	public void setIdm(){
		this.idm = (new Date().getTime());
	}

	/**
	 * Donne la valeur courante du timestamp a id_trans
	 */
	public void setId_trans(){
		this.id_trans = (new Date().getTime());
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
	 * Modifie le port_diff
	 * @param port_diff Nouveau port_diff
	 */
	public void setPort_diff(short port_diff){
		this.port_diff = port_diff;
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
		return Tools.removeZerosFromIp(this.ip);
	}

	/**
	 * Retourne l'ip_succ
	 * @return ip_succ
	 */
	public String getIp_succ(){
		return Tools.removeZerosFromIp(this.ip_succ);
	}

	/**
	 * Retourne l'ip_diff
	 * @return ip_diff
	 */
	public String getIp_diff(){
		return Tools.removeZerosFromIp(this.ip_diff);
	}

	/**
	 * Retourne l'id
	 * @return id
	 */
	public String getId(){
		return this.id;
	}

	/**
	 * Retourne l'id_app
	 * @return id_app
	 */
	public String getId_app(){
		return this.id_app;
	}
	
	/**
	 * Retourne l'idm
	 * @return idm
	 */
	public int getIdm(){
		return Integer.parseInt(Tools.longToStr8b(this.idm));
	}

	/**
	 * Retourne l'id_trans
	 * @return id_trans
	 */
	public int getId_trans(){
		return Integer.parseInt(Tools.longToStr8b(this.id_trans));
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
	 * Retourne le port_diff
	 * @return port_diff
	 */
	public short getPort_diff(){
		return this.port_diff;
	}

}



