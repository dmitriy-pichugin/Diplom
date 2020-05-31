package diplom.visualize;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Экземпляры класса – преобразованные таблицы
 */
class MyTable extends Table {
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
        super.setSchemaName(table.getSchemaName());
        super.setName(table.getName());
    }

    /**
     * Setter для columns
     *
     * @param columns new columns
     */
    void setColumns(ArrayList<ColumnDefinition> columns) {
        for (int i = 0; i < columns.size(); i++)
            columns.removeIf(columnDefinition -> columnDefinition.getColumnName().toUpperCase().equals("CONSTRAINT"));
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
        return (Table) this;
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
