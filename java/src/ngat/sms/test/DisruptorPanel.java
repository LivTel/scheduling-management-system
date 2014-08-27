/**
 * 
 */
package ngat.sms.test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;

import ngat.phase2.ITimePeriod;
import ngat.sms.Disruptor;

/**
 * @author eng
 * 
 */
public class DisruptorPanel extends JPanel {

	long start;
	long end;

	List<Disruptor> dlist;

	/**
	 * 
	 */
	public DisruptorPanel() {
		super(true);
		dlist = new Vector<Disruptor>();
	}

	public void addDisruptor(Disruptor d) {
		dlist.add(d);
	}

	public void setTimeLimits(long start, long end) {
		this.start = start;
		this.end = end;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		g.setColor(Color.black);
		g.drawRect(0, 0, getSize().width, getSize().height);

		g.setColor(Color.pink);

		// calc timestep
		long step = (long) ((double) (end - start) / getSize().width);
		System.err.println("Steps size: "+(step/1000)+"S");
		long t = start;
		while (t < end) {

			int ic = 0; // count intersects

			// iterate over d-list
			for (int i = 0; i < dlist.size(); i++) {

				Disruptor d = dlist.get(i);

				if (d.getPeriod().contains(t))
					ic++;

				
			}
			System.err.println("Plot ic "+ic+" at "+t);
			double xscale = (double) (getSize().width) / (double) (end - start);
			double yscale = (double) (getSize().height) / 5.0;

			int x = (int) ((double) (t-start) * xscale);
			int y = getSize().height;
			int dy = (int) ((double) (5 - ic) * yscale);

			g.fillRect(x, 0, 1, dy);
//System.err.println("Fill: "+x+","+y+", 1, "+dy);
			
			t += step;
		}

	} // paint(g)

}
