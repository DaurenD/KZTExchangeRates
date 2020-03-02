package any.exchange.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class QuoteUtils {
    public static final String BASE_URL = "https://nationalbank.kz/";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", new Locale("KZ"));

    public static String getCurrentDate(){
        Calendar date = Calendar.getInstance();
        return DATE_FORMAT.format(date.getTime());
    }

    public static long getCurrentDateLong() {
        try {
            return DATE_FORMAT.parse(getCurrentDate()).getTime();
        } catch (ParseException | NullPointerException e){
            e.printStackTrace();
        }
        return 0;
    }
}
