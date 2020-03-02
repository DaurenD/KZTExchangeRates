package any.exchange.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false)
public class Rates {

    @ElementList(entry = "item", inline = true)
    public List<Item> items;

    @Element
    public String date;

    @Root(strict = false)
    public static class Item {
        @Element(required = false)
        public String fullname;
        @Element(required = false)
        public String title;
        @Element(required = false)
        public String description;
        @Element(required = false)
        public String quant;
        @Element(required = false)
        public String index;
        @Element(required = false)
        public String change;

        public void fromQuote(Quote quote){
            fullname = quote.getFullName();
            title = quote.getTicker();
            description = String.valueOf(quote.getPrice());
            quant = String.valueOf(quote.getQuantity());
            change = String.valueOf(quote.getChange());
        }
        public String getFullname() {
            return fullname;
        }

        public void setFullname(String fullname) {
            System.out.println("setFullname: " + fullname);
            this.fullname = fullname;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getQuant() {
            return quant;
        }

        public void setQuant(String quant) {
            this.quant = quant;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getChange() {
            return change;
        }

        public void setChange(String change) {
            this.change = change;
        }
    }
}

