package time.datatypes;
import java.util.Comparator;

public class LogoTimeComparator implements Comparator<LogoTime> {
	public int compare(LogoTime a, LogoTime b) {
		return a.compareTo(b);
	}
}