import java.util.*;

public class Message{

	private PrefixMsg prefix = null;

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
	
	public Message(){

	}

	public Message(String mess){
		if(mess.length() <= 512){
			ArrayList<String> argv = new ArrayList<String>(Arrays.asList(mess.split("\\s+")));

			this.prefix = PrefixMsg.getPrefix(argv.get(0));

			if(prefix == PrefixMsg.WELC){
				this.ip = argv.get(1);
				this.ip_diff = argv.get(3);
				this.port = (short)Integer.parseInt(argv.get(2));
				this.port_diff = (short)Integer.parseInt(argv.get(4));
			}
			else if(prefix == PrefixMsg.NEWC){
				this.ip = argv.get(1);
				this.port = (short)Integer.parseInt(argv.get(2));
			}
			else if(prefix == PrefixMsg.ACKC){
			}
			else if(prefix == PrefixMsg.APPL){
				this.idm = Integer.parseInt(argv.get(1));
				this.id_app = Integer.parseInt(argv.get(2));
				this.message_app = argv.get(3);
			}
		}
	}

	public String toString(){
		String mess = "";

		if(prefix == PrefixMsg.WELC){
			mess = String.format("%s %s %04d %s %04d\n",
				prefix,ip,port,ip_diff,port_diff);
		}
		else if(prefix == PrefixMsg.NEWC){
			mess = String.format("%s %s %04d\n",
				prefix,ip,port);
		}
		else if(prefix == PrefixMsg.ACKC){
			mess = String.format("%s\n",
				prefix);
		}
		else if(prefix == PrefixMsg.APPL){
			mess = String.format("%s %08d %08d %s\n",
				prefix,idm,id_app,message_app);
		}

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

	public void setPrefix(PrefixMsg pref){
		this.prefix = pref;
	}
	public void setIp(String ip){
		this.ip = ip;
	}
	public void setIp_succ(String ip){
		this.ip_succ = ip;
	}
	public void setPort(short p){
		this.port = p;
	}
	public void setIp_diff(String ip_diff){
		this.ip_diff = ip_diff;
	}
	public void setPort_diff(short port_diff){
		this.port_diff = port_diff;
	}
}



