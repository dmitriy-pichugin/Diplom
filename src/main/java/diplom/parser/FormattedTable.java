package diplom.parser;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;

class FormattedTable {
    private Table table;
    private ArrayList<ColumnDefinition> columns;
    private List relations;

    void setTable(Table table) {
        this.table = table;
    }

    void setColumns(ArrayList<ColumnDefinition> columns) {
        this.columns = columns;
    }

    public void setRelations(List relations) {
        this.relations = relations;
    }

    Table getTable() {
        return table;
    }

    ArrayList<ColumnDefinition> getColumns() {
        return columns;
    }

    public List getRelations() {
        return relations;
    }
}
