package vizualize.builder;

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
 * Инициализирует модуль логирования, запускает синтаксический анализ и построение диаграммы
 */
public class DiagramBuilder {
    /**
     * Модуль логирования
     */
    private Logger logger;

    /**
     * Конструктор класса. Инициализирует модуль логирования
     */
    public DiagramBuilder() {
        this.logger = Logger.getLogger("DIPLOM");
        this.logger.setLevel(Level.INFO);
        try {
            /* Логи хранятся по адресу C:/Diplom/Log
            *  Название файла соответствует дате и времени запуска программы
            */
            DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss");
            Date currentDate = new Date();
            FileHandler fh = new FileHandler("C:/Diplom/Log/" + dateFormat.format(currentDate) + ".log");
            fh.setFormatter(new SimpleFormatter());
            this.logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Запускает синтаксический анализ и построение диаграммы
     * @param args входной массив путей к файлам с SQL DDL
     * @return файл с диаграммой
     * @throws JSQLParserException
     */
    public File build(String[] args) throws JSQLParserException {
        /* Синтаксический анализ */
        Parser parser = new Parser(logger);
        parser.parse(args);
        logger.info("All tables parsed. Start building diagram.");
        Session session = parser.getSession();
        /* Построение диаграммы */
        Formatter formatter = new Formatter(logger);
        formatter.addln(formatter.start_graph());
        formatter.add(formatter.toDotFormat(session));
        formatter.addln(formatter.end_graph());
        String type = "pdf";
        File out = new File("DIAGRAM" + "." + type);
        /* Удаляет старую диаграмму, если она существует */
        if (out.delete())
            logger.info("Deleted old diagram");
        logger.info("digraph created:\n" + formatter.getDotSource() + "\nwriting to file..");
        formatter.writeGraphToFile(formatter.getGraph(formatter.getDotSource(), type), out);
        return out;
    }
}
