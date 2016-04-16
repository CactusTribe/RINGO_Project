public class Message{

	private PrefixMsg prefix = null;

	private String ip = new String(new char[15]);
	private String ip_succ = new String(new char[15]);
	private String ip_diff = new String(new char[15]);
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
	
	public Message(){
		this.prefix = PrefixMsg.WELC;
		this.ip = "127.000.000.001";
		this.ip_succ = "127.000.000.002";
		this.ip_diff = "127.000.000.003";
		this.port = 6001;
		this.port_succ = 6002;
		this.port_diff = 7000;
		this.idm = 99999999;
		this.id_trans = 99999999;
		this.id_app = 99999999;
		this.id = 99999999;
		this.size_mess = 3;
		this.size_nom = 4;
		this.num_mess = 8;
		this.no_mess = 2;
		this.size_content = 56;
	}

	public Message(String mess){
		if(mess.length() <= 512){
			this.prefix = PrefixMsg.getPrefix(mess.substring(0,4));
			this.ip = mess.substring(4,19);
			this.ip_succ = mess.substring(19,34);
			this.ip_diff = mess.substring(34,49);
			this.port = (short)Integer.parseInt(mess.substring(49,53));
			this.port_succ = (short)Integer.parseInt(mess.substring(53,57));
			this.port_diff = (short)Integer.parseInt(mess.substring(57,61));
			this.idm = Integer.parseInt(mess.substring(61,69));
			this.id_trans = Integer.parseInt(mess.substring(69,77));
			this.id_app = Integer.parseInt(mess.substring(77,85));
			this.id = Integer.parseInt(mess.substring(85,93));
			this.size_mess = (short)Integer.parseInt(mess.substring(93,96));
			this.size_nom = (short)Integer.parseInt(mess.substring(96,98));
			this.num_mess = Integer.parseInt(mess.substring(98,106));
			this.no_mess = Integer.parseInt(mess.substring(106,114));
			this.size_content = (short)Integer.parseInt(mess.substring(114,117));
		}
	}

	public String toString(){
		String mess = String.format("%s%s%s%s%04d%04d%04d%08d%08d%08d%08d%03d%02d%08d%08d%03d",
			prefix,ip,ip_succ,ip_diff,port,port_succ,port_diff,
			idm,id_trans,id_app,id,size_mess,size_nom,num_mess,
			no_mess,size_content);

		return mess;
	}

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
}