package ngat.sms.simulation;

import ngat.sms.*;

/** Classes which wish to provide timing signals should implement this interface.*/
public interface TimeSignalGenerator extends TimeModel {

    /** Implementors should register the listener for a signal at the speicified time.*/
    public void awaitTimingSignal(TimeSignalListener tsl, long time);

}
