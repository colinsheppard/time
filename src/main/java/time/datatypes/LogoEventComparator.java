package time.datatypes;
import java.util.Comparator;

/*
 * The LogoEventComparator first compares based on tick (which is a Double) and then on id 
 * so if there is a tie for tick, the event that was created first get's executed first allowing
 * for a more intuitive execution.
 */
public class LogoEventComparator implements Comparator<LogoEvent> {
	public int compare(LogoEvent a, LogoEvent b) {
		if(a.tick < b.tick){
			return -1;
		}else if(a.tick > b.tick){
			return 1;
		}else if(a.id < b.id){
			return -1;
		}else if(a.id > b.id){
			return 1;
		}else{
			return 0;
		}
	}
}