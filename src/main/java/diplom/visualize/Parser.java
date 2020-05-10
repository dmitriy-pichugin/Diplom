package diplom.visualize;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Выполняет синтаксический анализ, преобразует входные запросы в объекты
 */
class Parser {
    /**
     * Объект типа session для текущего запуска
     */
    private Session session;
    /**
     * Модуль логирования программы
     */
    private Logger logger;
    /**
     * Массив путей к CTAS скриптам
     */
    private ArrayList<String> createAsSelectPaths;

    /**
     * Конструктор. Инициализирует модуль логирования
     *
     * @param logger модуль логирования
     */
    Parser(Logger logger) {
        this.session = new Session();
        this.createAsSelectPaths = new ArrayList<>();
        this.logger = logger;

    }

    /**
     * Преобразует входной массив в объекты Statement
     *
     * @param args пути к файлам с SQL DDL скриптами
     * @throws JSQLParserException исключение при ошибке, связанной с JSQLParser
     */
    void parse(String[] args) throws JSQLParserException {
        for (String filename : args) {
            if (checkCAS(filename))
                continue;
            CCJSqlParserManager ccjSqlParserManager = new CCJSqlParserManager();
            try {
                Statement statement = ccjSqlParserManager.parse(new InputStreamReader(
                        new FileInputStream(filename), StandardCharsets.UTF_8));
                parse(statement);
            } catch (FileNotFoundException e) {
                logger.severe("File not found: " + filename);
                e.printStackTrace();
            }
        }
        if (!this.createAsSelectPaths.isEmpty())
            parseCAS();
    }

    /**
     * Преобразует объект Statement в MyTable, добавляет его в session
     *
     * @param statement преобразуемый объект типа Statement
     */
    private void parse(Statement statement) {
        if (statement instanceof CreateTable) {
            /* Проверка дубликатов запросов во входных данных */
            if (checkExistence(((CreateTable) statement).getTable())) {
                /* Извлечение зависимостей из запроса */
                List relations = checkForeignKeys((CreateTable) statement);
                if (!relations.isEmpty()) {
                    for (int i = 0; i < relations.size(); i++) {
                        relations.set(i, ((CreateTable) statement).getTable().getSchemaName() + relations.get(i));
                    }
                    this.session.setTable((CreateTable) statement, relations);
                } else
                    this.session.setTable((CreateTable) statement);
                logger.info("Parsed successfully: " + ((CreateTable) statement).getTable().getWholeTableName());
            }
        } else if (statement instanceof Select) {
            logger.info("anal");
        } else logger.info("Script is not DDL:" + statement.toString());
    }

    /**
     * Проверяет существование таблицы в текущем session
     *
     * @param table таблица, существование которой проверяется
     * @return true, если существует, false, если нет
     */
    private boolean checkExistence(Table table) {
        boolean notExist = true;
        for (MyTable myTable : this.session.getTables()) {
            if (myTable.getTable().getWholeTableName().equals(table.getWholeTableName())) {
                notExist = false;
                break;
            }
        }
        return notExist;
    }

    /**
     * Проверяет, является ли скрипт SQL CTAS
     *
     * @param path путь к проверяемому скрипту
     * @return true, если SQL CTAS, false, если нет
     */
    private boolean checkCAS(String path) {
        try {
            /* Форматирование запроса */
            String script = new String(Files.readAllBytes(Paths.get(path)))
                    .replace("\r", "").replace("\n", "");
            /* Проверка соответствия типа запроса типу CREATE TABLE AS SELECT
            * Если соответствует - запрос форматируется и сохраняется во временную директорию.
            */
            Matcher asSelectMatcher = Pattern.compile("^CREATE TABLE .*(AS)? ([( ]?)SELECT").matcher(script.toUpperCase());
            while (asSelectMatcher.find()) {
                Matcher tableNameRegex = Pattern.compile("(?<=CREATE TABLE )[^\\s]+").matcher(script);
                while (tableNameRegex.find()) {
                    String createAsSelectNewFile = "C:/Diplom/Tmp/CreateAsSelect/" + tableNameRegex.group()
                            .replace(" ", "");
                    Matcher replacer = Pattern.compile("(?=SELECT).*[^);]").matcher(script);
                    while (replacer.find())
                        Files.write(Paths.get(createAsSelectNewFile), replacer.group().getBytes());
                    this.createAsSelectPaths.add(createAsSelectNewFile);
                    logger.info("Found CREATE AS SELECT in " + createAsSelectNewFile + ". Copied to temporary directory.");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Преобразует SQL CTAS скрипты в объекты MyTable, добавляет в session
     */
    private void parseCAS() {
        for (String query : createAsSelectPaths) {
            CCJSqlParserManager ccjSqlParserManager = new CCJSqlParserManager();
            try {
                /* Запрос преобразуется в Statement */
                Statement statement = ccjSqlParserManager.parse(new InputStreamReader(
                        new FileInputStream(query), StandardCharsets.UTF_8));
                if (statement instanceof Select) {
                    /* Из запроса извлекаются join соединения */
                    List<String> joins = (List<String>) ((PlainSelect) ((Select) statement)
                            .getSelectBody())
                            .getJoins()
                            .stream()
                            .map(object -> Objects.toString(object, null)).collect(Collectors.toList());
                    /* Join соединения форматируются, извлекаются зависимые таблицы */
                    for (int i = 0; i < joins.size(); i++) {
                        Matcher m = Pattern.compile("(?<=JOIN ).*(?= ON)").matcher(joins.get(i).toString());
                        while (m.find()) {
                            joins.set(i, m.group().replaceAll("(?= ).*", ""));
                        }
                    }
                    joins.add(((PlainSelect) ((Select) statement).getSelectBody()).getFromItem().toString().replaceAll("(?= ).*", ""));
                    Table newTable = new Table();
                    /* Извлекается название создаваемой таблицы и схемы */
                    String fullTableName = query.replaceAll(".*([\\/])", "");
                    newTable.setSchemaName(fullTableName.replaceAll("[.].*", ""));
                    newTable.setName(fullTableName.replaceAll(".*[.]", ""));
                    MyTable newMyTable = new MyTable();
                    newMyTable.setTable(newTable);
                    newMyTable.setRelations(joins);
                    /* Из запроса извлекаются атрибуты созданной таблицы */
                    ArrayList<ColumnDefinition> newTablesColumns = parseColumns(joins, ((PlainSelect) ((Select) statement).getSelectBody()).getSelectItems());
                    newMyTable.setColumns(newTablesColumns);
                    this.session.setTable(newMyTable);
                    logger.info("Parsed successfully: " + fullTableName);
                }
            } catch (JSQLParserException e) {
                logger.severe("Can't parse " + query);
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                logger.severe("File not found: " + query);
                e.printStackTrace();
            }
        }
    }

    /**
     * Анализирует SQL скрипт, возвращает атрибуты таблицы
     *
     * @param relations   список отношений таблицы
     * @param selectItems список атрибутов в группе SELECT запроса
     * @return Атрибуты таблицы
     */
    private ArrayList<ColumnDefinition> parseColumns(List<String> relations, List selectItems) {
        ArrayList<ColumnDefinition> columnsArrayList = new ArrayList<>();
        for (Object obj : selectItems) {
            ColumnDefinition newColumn = new ColumnDefinition();
            String unparsedColumn = obj.toString();
            Matcher asMatcher = Pattern.compile("(?<=AS ).*").matcher(unparsedColumn);
            if (asMatcher.find()) {
                newColumn.setColumnName(asMatcher.group());
            } else {
                newColumn.setColumnName(unparsedColumn.replaceAll("([^.]+$)", ""));
            }
            ColDataType dataType = getColumnDatatype(relations, unparsedColumn);
            if (dataType != null)
                newColumn.setColDataType(dataType);
            columnsArrayList.add(newColumn);
        }
        return columnsArrayList;
    }

    /**
     * Выполняет поиск атрибута в проанализированных таблицах, возвращает его тип
     *
     * @param relations список отношений таблицы
     * @param column    название атрибута
     * @return тип данных атрибута
     */
    private ColDataType getColumnDatatype(List<String> relations, String column) {
        String colTableName = column.replaceAll("[.].*", "");
        MyTable relatedTable = findTable(relations, colTableName);
        ColDataType dataType = findCol(relatedTable, column.replaceAll("(.*[.])|( .*)", ""));
        return dataType;
    }

    /**
     * Ищет таблицу из relations, имеющую атрибут colTableName
     *
     * @param relations    список отношений таблицы
     * @param colTableName название атрибута
     * @return таблица, в которой найден атрибут
     */
    private MyTable findTable(List<String> relations, String colTableName) {
        MyTable table = null;
        for (String tableName : relations) {
            MyTable relatedTable = findTable(tableName);
            if (relatedTable.getTable().getName().equals(colTableName))
                table = relatedTable;
        }
        return table;
    }

    /**
     * Ищет таблицу в session по названию
     *
     * @param tableName название таблицы
     * @return найденная таблица
     */
    private MyTable findTable(String tableName) {
        MyTable foundTable = null;
        List<MyTable> existingTables = this.session.getTables();
        for (MyTable table : existingTables) {
            if (table.getTable().getWholeTableName().equals(tableName)) {
                foundTable = table;
                break;
            }
        }
        return foundTable;
    }

    /**
     * Ищет атрибут в таблице по названию, возвращает его тип
     *
     * @param table          таблица
     * @param searchedColumn искомый атрибут
     * @return тип данных атрибута
     */
    private ColDataType findCol(MyTable table, String searchedColumn) {
        ColDataType dataType = null;
        for (ColumnDefinition column : table.getColumns()) {
            if (column.getColumnName().equals(searchedColumn)) {
                dataType = column.getColDataType();
                break;
            }
        }
        return dataType;
    }

    /**
     * Проверяет наличие внешних ключей в скрипте, возвращает список зависимостей
     *
     * @param createTable SQL запрос типа create table
     * @return зависимости таблиц, найденные в запросе
     */
    private List<String> checkForeignKeys(CreateTable createTable) {
        List<String> relations = new ArrayList();
        for (Object col : createTable.getColumnDefinitions()) {
            String newRelation = checkForeignKey(col);
            try {
                if (!newRelation.isEmpty())
                    relations.add(newRelation);
            } catch (NullPointerException e) {
            }
        }
        return relations;
    }

    /**
     * Проверяет наличие внешних ключей в скрипте. Возвращает одну зависимость
     *
     * @param col строка с объявлением атрибута
     * @return зависимая таблица
     */
    private String checkForeignKey(Object col) {
        String fkList = "";
        String colDefinitionString = ((ColumnDefinition) col).toString();
        Matcher fkMatcher = Pattern.compile("(?<=FOREIGN KEY [(]).*").matcher(colDefinitionString);
        while (fkMatcher.find()) {
            fkList = getRelatedTable(fkMatcher.group());
        }
        return fkList;
    }

    /**
     * Извлекает из строки название зависимой таблицы
     *
     * @param columnDefinition строка с объявлением атрибута
     * @return название зависимой таблицы
     */
    private String getRelatedTable(String columnDefinition) {
        String relatedTable = "";
        Matcher matcher = Pattern.compile("(?<=REFERENCES ).*(?= )").matcher(columnDefinition);
        while (matcher.find()) {
            relatedTable = matcher.group();
        }
        return relatedTable;
    }

    /**
     * Getter для session.
     *
     * @return session
     */
    Session getSession() {
        return session;
    }

}
