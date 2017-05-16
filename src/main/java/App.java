import org.quickfixj.*;
import quickfix.*;
import quickfix.DataDictionary;
import quickfix.DataDictionary.Exception;
import quickfix.DataDictionary.GroupInfo;
import quickfix.fix44.Advertisement;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class App {
    public String getGreeting() {
        return "Hello world.";
    }

    public static boolean isArray(String fixType) {
        if (fixType == "MULTIPLEVALUESTRING")
            return true;
        return false;
    }

    public static String decodeType(String fixType) {
        switch (fixType) {
        case "PERCENTAGE":
        case "CURRENCY":
        case "FLOAT":
        case "PRICEOFFSET":
        case "QTY":
        case "PRICE":
        case "AMT":
            return "double";
        case "INT":
        case "LENGTH":
            return "long";
        case "BOOLEAN":
            return "boolean";
        case "UTCTIMESTAMP":
        case "LOCALMKTDATE":
            return "string";
        case "DATA":
            return "bytes";
        case "STRING":
        case "CHAR":
        case "MONTHYEAR":
        case "COUNTRY":
        case "MULTIPLEVALUESTRING":
            return "string";
        default:
            return "#ERR " + fixType;
        }
    }

    public static String fieldLine(String fieldName, String fieldType, boolean optional, boolean array) {
        ArrayList<String> fields = new ArrayList<>();
        fields.add("\"name\": \"" + fieldType.charAt(0) + fieldType.substring(2) + "\" ");
        String type = "\"" + decodeType(fieldType) + "\"";
        if (array) {
            fields.add("\"items\": " + type);
            type = "array";
        }
        if (optional) {
            type = "[null," + type + "]";
        }
        fields.add("\"type\": " + type);
        return "\t{ " + String.join(", ", fields) + " }";
    }

    private static java.util.List<String> createType(String name, quickfix.DataDictionary dict, int[] fields) {
        java.util.List<String> output = new java.util.LinkedList<>();
        java.util.List<String> dependencies = new java.util.LinkedList<>();
        output.add("{\n\t\"type\": \"record\",\n\t\"name\": \"" + name + "\",\n\t\"fields\": [\n");
        for (int i : fields) {
            String fieldName = dict.getFieldName(i);
            // int fieldType = dict.getFieldType(i);
            Boolean required = dict.isRequiredField("D", i);
            FieldType ee = dict.getFieldTypeEnum(i);

            Boolean group = dict.isGroup("D", i);
            String fieldTypeName = ee == null ? "UNKNOWN" : decodeType(ee.getName());
            boolean isArrayField = isArray(fieldTypeName);
            output.add(fieldLine(fieldName, fieldTypeName, !required, group || isArrayField));

            if (group) {
                GroupInfo groupInfo = dict.getGroup("D", i);
                DataDictionary ddd = groupInfo.getDataDictionary();
                dependencies.addAll(createType(fieldName, dict, ddd.getOrderedFields()));
            }
        }
        output.add("\t]\n},");

        dependencies.addAll(output);
        return dependencies;
    }

    public static void main(String[] args) {
        try {
            // quickfix.fix50sp2.NewOrderSingle dd = new quickfix.fix50sp2.NewOrderSingle();
            quickfix.DataDictionary dict = new DataDictionary("FIX44.xml");
            IntStream orderedFields = IntStream.of(dict.getOrderedFields());
            int[] filtered = orderedFields.filter(i -> dict.isMsgField("D", i)).toArray();
            List<String> output = createType("NewOrder", dict, filtered);
            for (String o : output) {
                System.out.format("%s\n", o);
            }
        } catch (ConfigError e) {
            System.out.format("Error " + e.getMessage());
        }

    }
}
