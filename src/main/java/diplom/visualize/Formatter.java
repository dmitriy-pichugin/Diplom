package diplom.visualize;

import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Отвечает за построение диаграммы
 */
public class Formatter {
    /**
     * Хранит DOT запрос для создания диаграммы
     */
    private StringBuilder graph = new StringBuilder();
    /**
     * Путь к конфигурационному файлу
     */
    private final static String cfgProp = "C:/Diplom/GraphVizApi/Data/config.properties";
    /**
     * Конфигурация
     */
    private final static Properties configFile = new Properties() {
        private final static long serialVersionUID = 1L;

        {
            try {
                load(new FileInputStream(cfgProp));
            } catch (Exception e) {
            }
        }
    };
    /**
     * Путь к директории для временных файлов
     */
    private static String TEMP_DIR = "C:/Diplom/Tmp/GraphVizTmp";
    /**
     * Путь к dot.exe
     */
    private static String DOT = configFile.getProperty("dotForWindows");
    /**
     * Значения dpi для dot
     */
    private int[] dpiSizes = {46, 51, 57, 63, 70, 78, 86, 96, 106, 116, 128, 141, 155, 170, 187, 206, 226, 249};
    /**
     * Параметр Gdpi для dot
     */
    private int currentDpiPos = 7;
    /**
     * Модуль логирования программы
     */
    private Logger logger;

    /**
     * Конструктор. Инициализирует модуль логирования
     *
     * @param logger модуль логирования
     */
    Formatter(Logger logger) {
        this.logger = logger;
    }

    /**
     * Добавляет строку в graph
     *
     * @param line добавляемая строка
     */
    void add(String line) {
        this.graph.append(line);
    }

    /**
     * Добавляет строку и переход на новую строку в graph
     *
     * @param line добавляемая строка
     */
    void addln(String line) {
        this.graph.append(line + "\n");
    }

    /**
     * Добавляет переход на новую строку в graph
     */
    public void addln() {
        this.graph.append('\n');
    }

    /**
     * Добавляет начало dot запроса
     *
     * @return начало запроса
     */
    String start_graph() {
        return "digraph G {";
    }

    /**
     * Добавляет завершение dot запроса
     *
     * @return завершение запроса
     */
    String end_graph() {
        return "}";
    }

    /**
     * Возвращает dot запрос
     *
     * @return
     */
    String getDotSource() {
        return this.graph.toString();
    }

    /**
     * Возвращает диаграмму в виде массива байт
     *
     * @param dot_source dot запрос
     * @param type тип файла
     * @return диаграмма
     */
    byte[] getGraph(String dot_source, String type) {
        File dot;
        byte[] img_stream = null;
        try {
            dot = writeDotSourceToFile(dot_source);
            if (dot != null) {
                img_stream = get_img_stream(dot, type);
                if (dot.delete() == false)
                    logger.severe("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                return img_stream;
            }
            return null;
        } catch (java.io.IOException ioe) {
            return null;
        }
    }

    /**
     * Запись dot запроса в файл
     *
     * @param str dot запрос
     * @return файл
     * @throws java.io.IOException ошибка при записи
     */
    private File writeDotSourceToFile(String str) throws java.io.IOException {
        File temp;
        try {
            temp = File.createTempFile("dorrr", ".dot", new File(TEMP_DIR));
            FileWriter fout = new FileWriter(temp);
            fout.write(str);
            BufferedWriter br = new BufferedWriter(new FileWriter("dotsource.dot"));
            br.write(str);
            br.flush();
            br.close();
            fout.close();
        } catch (Exception e) {
            logger.severe("Error: I/O error while writing the dot source to temp file!");
            return null;
        }
        return temp;
    }

    /**
     * Запись диаграммы в файл
     *
     * @param img диаграмма
     * @param to файл
     * @return 1, если успешно, иначе -1
     */
    int writeGraphToFile(byte[] img, File to) {
        try {
            FileOutputStream fos = new FileOutputStream(to);
            fos.write(img);
            fos.close();
        } catch (java.io.IOException ioe) {
            return -1;
        }
        return 1;
    }

    /**
     * Построение dot запроса
     *
     * @param thisSession текущий session, по которому строится запрос
     * @return dot запрос
     */
    String toDotFormat(Session thisSession) {
        StringBuilder formattedTables = new StringBuilder();
        StringBuilder formattedRelations = new StringBuilder();
        for (MyTable table : thisSession.getTables()) {
            formattedTables.append(tableToDotFormat(table));
            try {
                formattedRelations.append(relationToDotFormat(table));
            } catch (NullPointerException e) {
            }
        }
        return formattedTables.append(formattedRelations).toString();
    }

    /**
     * Представление таблицы в виде части dot запроса
     *
     * @param table таблица
     * @return строка в формате dot
     */
    private String tableToDotFormat(MyTable table) {
        String fullTableName = table.getTable().getWholeTableName();
        StringBuilder tableInDot = new StringBuilder();
        tableInDot.append(fullTableName.replace(".", ""))
                .append(" [style=filled, fillcolor=\"#BFC9CA\", shape=none, margin=0, label=<" + "<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">" + "<TR><TD COLSPAN=\"2\">")
                .append(fullTableName)
                .append("</TD></TR>");
        for (ColumnDefinition col : table.getColumns()) {
            tableInDot.append("<TR><TD>").append(col.getColumnName()).append("</TD><TD>").append(col.getColDataType()).append("</TD></TR>");
        }
        tableInDot.append("</TABLE>>];\n");
        return tableInDot.toString();
    }

    /**
     * Представление отношения в виде части dot запроса
     *
     * @param table таблица, у которой проверяются зависимости
     * @return строка в формате dot
     */
    private String relationToDotFormat(MyTable table) {
        String fullTableName = table.getTable().getWholeTableName().replace(".", "");
        StringBuilder relationInDot = new StringBuilder();
        for (String relation : table.getRelations()) {
            relationInDot.append(fullTableName).append("->").append(relation.replace(".", "")).append(";");
        }
        return relationInDot.toString();
    }

    /**
     * Выполнение dot запроса
     *
     * @param dot файл с dot запросом
     * @param type расширение диаграммы (png)
     * @return диаграмма
     */
    private byte[] get_img_stream(File dot, String type) {
        File img;
        byte[] img_stream = null;

        try {
            img = File.createTempFile("graph_", "." + type, new File(TEMP_DIR));
            Runtime rt = Runtime.getRuntime();

            String[] args = {DOT, "-T" + type, "-Gdpi=" + dpiSizes[this.currentDpiPos], dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
            Process p = rt.exec(args);

            p.waitFor();

            FileInputStream in = new FileInputStream(img.getAbsolutePath());
            img_stream = new byte[in.available()];
            in.read(img_stream);
            // Close it if we need to
            if (in != null) in.close();

            if (!img.delete())
                logger.severe("Warning: " + img.getAbsolutePath() + " could not be deleted!");
        } catch (java.io.IOException ioe) {
            logger.severe("Error:    in I/O processing of tempfile in dir " + TEMP_DIR + "\n");
            logger.severe("       or in calling external command");
            ioe.printStackTrace();
        } catch (java.lang.InterruptedException ie) {
            logger.severe("Error: the execution of the external program was interrupted");
            ie.printStackTrace();
        }

        return img_stream;
    }

}
