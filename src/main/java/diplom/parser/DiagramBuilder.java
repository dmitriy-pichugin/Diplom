package diplom.parser;

import net.sf.jsqlparser.JSQLParserException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main class for visualization. Runs syntax analysis and visualization.
 */
public class DiagramBuilder {
    private Logger logger;

    public DiagramBuilder() {
        this.logger = Logger.getLogger("DIPLOM");
        this.logger.setLevel(Level.INFO);
        try {
            DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss");
            Date currentDate = new Date();
            FileHandler fh = new FileHandler("C:/Diplom/Log/" + dateFormat.format(currentDate) + ".log");
            fh.setFormatter(new SimpleFormatter());
            this.logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File build(String[] args) throws JSQLParserException {
        Parser parser = new Parser(logger);
        parser.parse(args);
        logger.info("All tables parsed. Start building diagram.");
        Session session = parser.getSession();
        Formatter formatter = new Formatter(logger);
        formatter.addln(formatter.start_graph());
        formatter.add(formatter.toDotFormat(session));
        formatter.addln(formatter.end_graph());
        String type = "pdf";
        File out = new File("diplom" + "." + type);
        logger.info(formatter.getDotSource());
        formatter.writeGraphToFile(formatter.getGraph(formatter.getDotSource(), type), out);
        return out;
    }
}
