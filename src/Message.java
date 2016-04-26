import java.util.*;

/**
 * Classe Message
 * Permet de d'utiliser des messages formatés plus simple d'utilisations
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */
public class Message{

	// Champs du message (Protocole)
	private ProtocoleToken prefix = null;
	private String ip = "";
	private String ip_succ = "";
	private String ip_diff = "";
	private String id = "";

	private long idm = 0;
	private short port = 0;
	private short port_succ = 0;
	private short port_diff = 0;
	
	// Champs du message (Applications)
	private AppToken id_app = null;
	private TransToken trans_token = null;

	private short size_mess = 0;
	private short size_nom = 0;
	private short size_content = 0;

	private long id_trans = 0;
	private int num_mess = 0;
	private int no_mess = 0;
	
	private String nom_fichier = "";
	private String message_app = "";
	private String file_content = "";

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
	 * @throws MalformedMsgException Lance une exception si le message est malformé
	 */
	public Message(String mess) throws MalformedMsgException{
		ArrayList<String> argv = new ArrayList<String>(Arrays.asList(mess.split("\\s+")));

		try{
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
					this.id_app = AppToken.valueOf(argv.get(2).replace("#",""));

					// Mise en forme pour l'application DIFF
					if(this.id_app == AppToken.DIFF){

						this.size_mess = (short)Integer.parseInt(argv.get(3));
						String tmp_msg = "";
						for(int i=4; i < argv.size(); i++)
							tmp_msg += argv.get(i)+" ";
						this.message_app = tmp_msg.substring(0, tmp_msg.length()-1);

					}
					// Mise en forme pour l'application TRANS
					else if(this.id_app == AppToken.TRANS){

						this.trans_token = TransToken.valueOf(argv.get(3));

						// TRANS Requete
						if(this.trans_token == TransToken.REQ){

							this.size_nom = (short)Integer.parseInt(argv.get(4));
							String tmp_file_name = "";
							for(int i=5; i < argv.size(); i++)
								tmp_file_name += argv.get(i)+" ";
							this.nom_fichier = tmp_file_name.substring(0, tmp_file_name.length()-1);			

						}
						// TRANS Confirm
						else if(this.trans_token == TransToken.ROK){

							this.id_trans = Integer.parseInt(argv.get(4));
							this.size_nom = (short)Integer.parseInt(argv.get(5));
							String tmp_file_name = "";
							for(int i=6; i < argv.size()-1; i++)
								tmp_file_name += argv.get(i)+" ";
							this.nom_fichier = tmp_file_name.substring(0, tmp_file_name.length()-1);
							this.num_mess = Integer.parseInt(argv.get(argv.size()-1));

						}
						// TRANS Envoi
						else if(this.trans_token == TransToken.SEN){

							this.id_trans = Integer.parseInt(argv.get(4));
							this.no_mess = Integer.parseInt(argv.get(5));
							this.size_content = (short)Integer.parseInt(argv.get(6));
							String tmp_file_content = "";
							for(int i=7; i < argv.size(); i++)
								tmp_file_content += argv.get(i)+" ";
							this.file_content = tmp_file_content.substring(0, tmp_file_content.length()-1);

						}
					}
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

				case DUPL:
					this.ip = argv.get(1);
					this.port = (short)Integer.parseInt(argv.get(2));
					this.ip_diff = argv.get(3);
					this.port_diff = (short)Integer.parseInt(argv.get(4));
				break;

				case ACKD:
					this.port = (short)Integer.parseInt(argv.get(1));
				break;

				case NOTC:
				break;
			}

		}catch (Exception e){
			throw new MalformedMsgException();
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
				if(this.id_app == AppToken.DIFF){
					mess = String.format("%s %s %s %03d %s\n",
						prefix, Tools.longToStr8b(idm), id_app, size_mess, message_app);				
				}
				else if(this.id_app == AppToken.TRANS){

					if(this.trans_token == TransToken.REQ){
						mess = String.format("%s %s %s %s %02d %s\n",
							prefix, Tools.longToStr8b(idm), id_app, trans_token, size_nom, nom_fichier);	
					}
					else if(this.trans_token == TransToken.ROK){
						mess = String.format("%s %s %s %s %s %02d %s %08d\n",
							prefix, Tools.longToStr8b(idm), id_app, trans_token, Tools.longToStr8b(id_trans), size_nom, nom_fichier, num_mess);
					}
					else if(this.trans_token == TransToken.SEN){
						mess = String.format("%s %s %s %s %s %08d %03d %s\n",
							prefix, Tools.longToStr8b(idm), id_app, trans_token, Tools.longToStr8b(id_trans), no_mess, size_content, file_content);
					}

				}
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

			case DUPL:
				mess = String.format("%s %s %04d %s %04d\n", 
					prefix,  ip, port, ip_diff, port_diff);
			break;

			case ACKD:
				mess = String.format("%s %04d\n", 
					prefix, port);
			break;

			case NOTC:
				mess = String.format("%s\n", prefix);
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
				+" - ID_APP : %s\n"
				+" - ID : %s\n"
				+" - SIZE_MESS : %03d\n"
				+" - SIZE_NOM : %02d\n"
				+" - NUM_MESS : %08d\n"
				+" - NO_MESS : %08d\n"
				+" - SIZE_CONTENT : %03d\n"
				+" - MESS_APP : %s\n"
				+"#########################################\n",
				prefix,ip,ip_succ,ip_diff,port,port_succ,port_diff,
				idm,id_trans,id_app,id,size_mess,size_nom,num_mess,
				no_mess,size_content,message_app);
	
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
	public void setId_app(AppToken id_app){
		this.id_app = id_app;
	}

	/**
	 * Donne la valeur courante du timestamp a idm
	 */
	public void setIdm(){
		this.idm = ((Long)System.nanoTime()).hashCode();
	}

	/**
	 * Donne la valeur courante du timestamp a id_trans
	 */
	public void setId_trans(){
		this.id_trans = ((Long)System.nanoTime()).hashCode();
	}

	/**
	 * Donne la valeur courante du timestamp a id_trans
	 * @param id Nouvel ID
	 */
	public void setId_trans(long id){
		this.id_trans = id;
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
	 * Modifie le size_mess
	 * @param size Nouvelle taille
	 */
	public void setSize_mess(short size){
		this.size_mess = size;
	}

	/**
	 * Modifie le size_content
	 * @param size Nouvelle taille
	 */
	public void setSize_content(short size){
		this.size_content = size;
	}

	/**
	 * Modifie le size_nom
	 * @param size Nouvelle taille
	 */
	public void setSize_nom(short size){
		this.size_nom = size;
	}

	/**
	 * Modifie le num_mess
	 * @param num Nombre de messages
	 */
	public void setNum_mess(int num){
		this.num_mess = num;
	}

	/**
	 * Modifie le no_mess
	 * @param no Numero du message
	 */
	public void setNo_mess(int no){
		this.no_mess = no;
	}

	/**
	 * Modifie le nom du fichier
	 * @param name Nouveau nom
	 */
	public void setNom_fichier(String name){
		this.nom_fichier = name;
	}

	/**
	 * Modifie le message d'application
	 * @param msg Nouveau msg
	 */
	public void setMessage_app(String msg){
		this.message_app = msg;
	}

	/**
	 * Modifie le contenu du fichier
	 * @param content Nouveau contenu
	 */
	public void setFile_content(String content){
		this.file_content = content;
	}

	/**
	 * Modifie le token de l'application transfert
	 * @param token Nouveau token
	 */
	public void setTrans_token(TransToken token){
		this.trans_token = token;
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
	public AppToken getId_app(){
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

	/**
	 * Retourne le size_mess
	 * @return size_mess
	 */
	public short getSize_mess(){
		return this.size_mess;
	}

	/**
	 * Retourne le size_content
	 * @return size_content
	 */
	public short getSize_content(){
		return this.size_content;
	}

	/**
	 * Retourne le size_nom
	 * @return size_nom
	 */
	public short getSize_nom(){
		return this.size_nom;
	}

	/**
	 * Retourne le num_mess
	 * @return num_mess
	 */
	public int getNum_mess(){
		return this.num_mess;
	}

	/**
	 * Retourne le no_mess
	 * @return no_mess
	 */
	public int getNo_mess(){
		return this.no_mess;
	}

	/**
	 * Retourne le nom_fichier
	 * @return nom_fichier
	 */
	public String getNom_fichier(){
		return this.nom_fichier;
	}

	/**
	 * Retourne le message_app
	 * @return message_app
	 */
	public String getMessage_app(){
		return this.message_app;
	}

	/**
	 * Retourne le file_content
	 * @return file_content
	 */
	public String getFile_content(){
		return this.file_content;
	}

	/**
	 * Retourne le token de transfert
	 * @return trans_token
	 */
	public TransToken getTrans_token(){
		return this.trans_token;
	}
}
