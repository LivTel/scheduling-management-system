package ngat.sms.util;

import java.io.Serializable;

import ngat.sms.*;

public class PrescanEntry implements Serializable {

	public long start;
	public long end;

	public GroupItem group;

	public long interval;

	public String gname;

	public double execTime;

	public boolean[] feasible;

	public int nx = 0;

	public PrescanEntry(long start, long end, long interval) {
		this.start = start;
		this.end = end;
		this.interval = interval;

		int nn = (int) ((end - start) / interval) + 1;
		System.err.printf("PSE: Create buffer with %4d slots, start %tT, end %tT, interval %8d \n",nn, start, end, interval);
		feasible = new boolean[nn];
	}

	public void setFeasible(long time) {
		if ((time < start) || (time > end))
			return;
		int nn = (int) ((time - start) / interval);
		if ((nn < 0) || (nn >= feasible.length))
			return;
		feasible[nn] = true;
	}

	public boolean isFeasible(long time) {
		if (time < start || time > end)
			return false;
		int nn = (int) ((time - start) / interval);
		if ((nn < 0) || (nn >= feasible.length))
			return false;
		return feasible[nn];
	}

	public double feasibleTotal() {
		double tot = 0.0;
		for (int i = 0; i < feasible.length; i++) {
			if (feasible[i])
				tot += (double)interval;
		}
		return tot;
	}

	public String display() {

		StringBuffer buff = new StringBuffer();

		int nms = (int) ((end - start) / (10*interval)) + 1; 
		for (int i = 0; i < nms; i++) {
			int is = i * 10;
			int ie = Math.min(is + 10, feasible.length);
			int nm = 0;
			for (int j = is; j < ie; j++) {
				if (feasible[j])
					nm++;
			}
			if (nm == 0)
				buff.append("-");
			else
				buff.append("" + (nm - 1));
		}
		return buff.toString();

	}

	public String toString() {
		return String.format("PSE: %tF %tT -> %tF %tT %s %s", start, start, end, end, group.getName(), display());
	}

}