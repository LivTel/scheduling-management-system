package ngat.sms;

import java.io.Serializable;
import java.util.*;

/** A snapshot of an actual or predicted environmental state. */
public class EnvironmentSnapshot implements Serializable {

	/** Indicates excellent seeing. */
	public static final int SEEING_EXCELLENT = 0;

	/** Indicates average seeing. */
	public static final int SEEING_AVERAGE = 1;

	/** Indicates poor seeing. */
	public static final int SEEING_POOR = 2;

	/** Indicates just-usable seeing. */
	public static final int SEEING_USABLE = 3;

    /** Indicates unusable.bad seeing. */
    public static final int SEEING_BAD = 4;

	/** Indicates photometric extinction level. */
	public static final int EXTINCTION_PHOTOM = 2;

	/** Indicates spectrometric extinction level. */
	public static final int EXTINCTION_SPECTRO = 1;

	/** Time for which this snapshot is valid. */
	private long timeStamp;

	/** Seeing state. */
	//private int seeingState;

    /** Value of corrected seeing at zenith in r-band (nm).*/
    private double correctedSeeing;

	/** Extinction state. */
	private int extinctionState;

	//public EnvironmentSnapshot(long timeStamp, int se, int extinction) {
		//this.timeStamp = timeStamp;
		//this.seeingState = seeing;
		//this.extinctionState = extinction;
	//}

    public EnvironmentSnapshot(long timeStamp, double correctedSeeing, int extinction) {
	this.timeStamp = timeStamp;
	this.correctedSeeing = correctedSeeing;
	//this.seeingState = seeing;
	this.extinctionState = extinction;
    }

	public long getTimeStamp() {
		return timeStamp;
	}

    public double getCorrectedSeeing() {
	return correctedSeeing;
    }

/*	public int getSeeingState() {
		return seeingState;
	}*/

	public int getExtinctionState() {
		return extinctionState;
	}

    /** Calculates which seeing band the specified seeing is in. */
    public static int getSeeingCategory(double seeing) {

	if (seeing < 0.8)
	    return EnvironmentSnapshot.SEEING_EXCELLENT;
	else if (seeing < 1.3)
	    return EnvironmentSnapshot.SEEING_AVERAGE;
	else if (seeing < 3.0)
	    return EnvironmentSnapshot.SEEING_POOR;
	else if (seeing < 5.0)
	    return EnvironmentSnapshot.SEEING_USABLE;
	else
	    return EnvironmentSnapshot.SEEING_BAD;

    }

    /** Calculates which extinction band the specified extinction is in. */
    public static int getExtinctionCategory(double extinction) {

	if (extinction < 0.5)
	    return EnvironmentSnapshot.EXTINCTION_PHOTOM;
	else
	    return EnvironmentSnapshot.EXTINCTION_SPECTRO;
    }

    public static String getSeeingCategoryName(int seeingState) {
	switch (seeingState) {
	case SEEING_EXCELLENT:
	  return "EXCELLENT";
	case SEEING_AVERAGE:
	    return "AVERAGE";
	case SEEING_POOR:
	    return "POOR";
	case SEEING_USABLE:
	    return "USABLE";
	}
	return "UNKNOWN";
    }

    public static String getExtinctionCategoryName(int extinctionState) {
	switch (extinctionState) {
	case EXTINCTION_PHOTOM:
	    return "PHOTOM";
	case EXTINCTION_SPECTRO:
	    return "SPECTRO";
	}
	return "UNKNOWN";
    }

	public String toString() {
		String seeStr = "UNKNOWN";
		int seeingState = getSeeingCategory(correctedSeeing);
		switch (seeingState) {
		case SEEING_EXCELLENT:
			seeStr = "EXCELLENT";
			break;
		case SEEING_AVERAGE:
			seeStr = "AVERAGE";
			break;
		case SEEING_POOR:
			seeStr = "POOR";
			break;
		case SEEING_USABLE:
			seeStr = "USABLE";
			break;
		}
		String extStr = "UNKNOWN";
		switch (extinctionState) {
		case EXTINCTION_PHOTOM:
			extStr = "PHOTOM";
			break;
		case EXTINCTION_SPECTRO:
			extStr = "SPECTRO";
			break;
		}
		return "[Env:t=" + (new Date(timeStamp)) + ", Seeing=" + seeStr + ", Ext=" + extStr + "]";
	}

}