import java.io.*;

/**
 * ProgressBar Barre de progression dans la console	
 *
 * @author Lefranc Joaquim, Plat Guillaume, Skoda Jérôme
 */

public class ProgressBar{

	private int total;
	private int current;
	private int nb_bar;
	private String progress_bar;

	public ProgressBar(int total){
		this.total = total;
		this.current = 0;
		this.progress_bar = "";
	}

	public void update(int current) throws IOException{
		this.current = current;
		this.nb_bar = (int)Math.ceil(((double)(current * 40) / total));
		this.progress_bar = new String(new char[nb_bar]).replace("\0", "#");

		System.out.write(this.toString().getBytes());
	}

	public String toString(){
		String data = String.format("\r |%-40s| %d%% (%.1f/%.1fKo)",
                   progress_bar, (int)Math.ceil(((double)(current * 100) / total)), 
                   (double)(current) / 1000,  
                   (double)(total) / 1000);
                  
    return data;
	}
}