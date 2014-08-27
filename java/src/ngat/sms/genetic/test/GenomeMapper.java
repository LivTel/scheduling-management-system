package ngat.sms.genetic.test;

import java.util.*;

public class GenomeMapper {

    public static final char[] kon = new char[]{'b','c','d','f','g','h','j','k','l','m','n','p','q','r','s','t','v','x','z'};
    public static final char[] xkon = new char[]{'b','c','d','f','g','h','j','k','l','m','n','p','q','r','s','t','v','w','x','y','z'};
    
    public static final char[] vow = new char[]{'a','e','i','o','u'};
    public static final char[] xvow = new char[]{'a','e','i','o','u','y','w'};

    public static List<String> genomes = new Vector<String>();
    
    public static String newName() {

	String genome = null;
	boolean done = false;
	while (! done) {
	    
	    StringBuffer sequence = new StringBuffer();
	    
	    char[] g = new char[3];
	    
	    // make a genome
	    g[0] = select(xkon);
	    if (g[0] == 'y' || g[0] == 'w') {
		g[1] = select(vow);
		g[2] = select(kon);
	    } else {
		g[1] = select(xvow);
		if (g[1] == 'y' || g[1] == 'w')
		    g[2] = select(kon);
		else
		    g[2] = select(xkon);
	    }
	    
	    g[0] = Character.toUpperCase(g[0]);
	    g[2] = Character.toUpperCase(g[2]);
	    genome = new String(g);
	
	    if (! genomes.contains(genome)) {
		genomes.add(genome);
		done = true;
	    }

	}

	return genome;

    }


    private static char select(char[] list) {

        int k = (int)(Math.random()*2383.756) % list.length;

        return list[k];

    }

}