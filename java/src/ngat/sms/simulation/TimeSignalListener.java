package ngat.sms.simulation;

/** Classes which wish to receive timing signals should implement this interface.*/
public interface TimeSignalListener {


    /** Implementors should act on the timing signal.*/
    public void timingSignal(long time);

}
