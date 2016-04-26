import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools{

	/**
	 * Retourne l'ip correspondant avec des 0 en plus
	 * @param ip Ip a convertir
	 * @return ip convertie
	 */
	public static String addZerosToIp(String ip){
		String newIP = "";

		Pattern p = Pattern.compile("(\\d{1,3}).(\\d{1,3}).(\\d{1,3}).(\\d{1,3})");
		Matcher m = p.matcher(ip);

		if(m.find()){
			newIP = String.format("%s.%s.%s.%s",
				String.format("%03d", Integer.parseInt(m.group(1))),
				String.format("%03d", Integer.parseInt(m.group(2))),
				String.format("%03d", Integer.parseInt(m.group(3))),
				String.format("%03d", Integer.parseInt(m.group(4)))
				);
		}
		return newIP;
	}

	/**
	 * Retourne l'ip correspondant sous la forme d'une String sans 0
	 * @param ip Ip a convertir
	 * @return ip convertie
	 */
	public static String removeZerosFromIp(String ip){
		String newIP = "";

		Pattern p = Pattern.compile("(\\d{1,3}).(\\d{1,3}).(\\d{1,3}).(\\d{1,3})");
		Matcher m = p.matcher(ip);

		if(m.find()){
			newIP = String.format("%s.%s.%s.%s",
				String.format("%d", Integer.parseInt(m.group(1))),
				String.format("%d", Integer.parseInt(m.group(2))),
				String.format("%d", Integer.parseInt(m.group(3))),
				String.format("%d", Integer.parseInt(m.group(4)))
				);
		}
		return newIP;
	}

	/**
	 * Retourne les 8 derniers chiffres d'un int
	 * @param n Nombre a convertir
	 * @return Nombre sous forme de chaine
	 */
	public static String intToStr8b(int n){
		return String.format("%08d", Math.abs(n % 100000000));
	}

	/**
	 * Retourne les 8 derniers chiffres d'un long
	 * @param l Nombre a convertir
	 * @return Nombre sous forme de chaine
	 */
	public static String longToStr8b(long l){
		return String.format("%08d", Math.abs(l % 100000000));
	}

}