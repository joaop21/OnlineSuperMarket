package database;

import java.util.List;

class FieldValue{
    private String field;
    private Object value;

    public FieldValue(String field, Object value){
        this.field = field;
        this.value = value;
    }

    public String getField(){
        return this.field;
    }

    public Object getValue(){
        return this.value;
    }
}

public class DatabaseModification {
    private int type; // 0: INSERT, 1: UPDATE; 2: DELETE
    private String table;
    private List<FieldValue> mods;
    private List<FieldValue> where;

    public DatabaseModification(int type, String table, List<FieldValue> mods, List<FieldValue> where){
        this.type = type;
        this.table = table;
        this.mods = mods;
        this.where = where;
    }

    public int getType(){
        return this.type;
    }

    public String getTable(){
        return this.table;
    }

    public List<FieldValue> getMods(){
        return this.mods;
    }

    public List<FieldValue> getWhere(){
        return this.where;
    }
}
