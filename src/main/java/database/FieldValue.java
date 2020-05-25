package database;

public class FieldValue{
    private String field;
    private Object value;
    private ValueType type;

    public FieldValue(String field, Object value, ValueType type){
        this.field = field;
        this.value = value;
        this.type = type;
    }

    public String getField(){
        return this.field;
    }

    public Object getValue(){
        return this.value;
    }

    public ValueType getType() { return type; }
}
