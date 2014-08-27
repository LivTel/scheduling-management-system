package ngat.sms;

import java.util.*;

public interface DisruptorModel {

    /** @return a list of bookings which intersect time.*/ 
    public List<Disruptor> listBookings(long t);

    /** @return the one and only possible merged booking which intersects time.*/
    public Disruptor getMergedBooking(long t);

    /** @return a list of bookings which fall between t1, t2 (including overlaps).*/
    public List<Disruptor> listBookings(long t1, long t2);

    /** @return a list of merged bookings which fall between t1, t2 (including overlaps).*/
    public List<Disruptor> listMergedBookings(long t1, long t2);

    /** @return the next available booking period strictly AFTER time.*/
    public Disruptor nextBooking(long t);

    /** @return the next available merged booking period strictly AFTER time.*/
    public Disruptor nextMergedBooking(long t);

    /** @return true if there are any bookings for time.*/
    public boolean hasBookings(long t);

    /** @return true if there is a merged booking for time.*/
    public boolean hasMergedBooking(long t);

}