import java.io.*;
import java.util.concurrent.*;

/**
 *  A simple interface representing a unit of work that can be done in parallel.
 *  Created by <a href="http://www.people.fas.harvard.edu/~ekwan/">Eugene E. Kwan</a>.  
 */
public interface WorkUnit extends Callable<Result>, Serializable
{
    /**
     *  The {@link Result} that you get when you call this work unit.  
     */
    public Result call();
    public String toString();
}
