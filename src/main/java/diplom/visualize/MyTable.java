package diplom.visualize;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Экземпляры класса – преобразованные таблицы
 */
class MyTable {
    /**
     * Объект, хранящий имя таблицы и схемы
     */
    private Table table;
    /**
     * Множество атрибутов таблицы, их типов и свойств
     */
    private ArrayList<ColumnDefinition> columns;
    /**
     * Список связей таблицы
     */
    private List<String> relations;

    /**
     * Setter для tables
     *
     * @param table new table
     */
    void setTable(Table table) {
        this.table = table;
    }

    /**
     * Setter для columns
     *
     * @param columns new columns
     */
    void setColumns(ArrayList<ColumnDefinition> columns) {
        this.columns = columns;
    }

    /**
     * Setter для relations
     *
     * @param relations new relations
     */
    void setRelations(List relations) {
        this.relations = relations;
    }

    /**
     * Getter для tables
     *
     * @return table
     */
    Table getTable() {
        return table;
    }

    /**
     * Getter для columns
     *
     * @return columns
     */
    ArrayList<ColumnDefinition> getColumns() {
        return columns;
    }

    /**
     * Getter для relations
     *
     * @return relations
     */
    List<String> getRelations() {
        return relations;
    }
}
