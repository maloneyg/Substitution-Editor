import java.io.*;

/**
 *  Constants that {@link WorkUnit}s can return.  
 */
public interface Result extends Serializable
{
    public static final Result JOB_FAILED = new Result() {
        public String toString() { return "job failed"; } };
    public static final Result JOB_INTERRUPTED = new Result() {
        public String toString() { return "job was interrupted"; } };
    public static final Result JOB_UNAVAILABLE = new Result() {
        public String toString() { return "job result not available"; } };
    public static final Result JOB_COMPLETE = new Result() {
        public String toString() { return "job complete"; } };

    public String toString();
}
