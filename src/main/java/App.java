import quickfix.DataDictionary;
// import quickfix.DataDictionary.Exception;
import quickfix.DataDictionary.GroupInfo;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.Map;
import java.util.HashMap;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class App {

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
        case "NUMINGROUP":
        case "SEQNUM":
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
        case "EXCHANGE":
        case "COUNTRY":
        case "MULTIPLEVALUESTRING":
            return "string";
        default:
            return fixType;
        }
    }

    public static String getTabs(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < indent; n++) {
            sb.append("\t");
        }
        return sb.toString();
    }

    /** Returns an avro field definition for a specified Fix Field and Fix Type (converting to avro primitives) */
    public static String fieldLine(String fieldName, int fixTag, String fieldType, String subType, boolean optional,
            boolean array) {
        ArrayList<String> fields = new ArrayList<>();
        fields.add("\"name\": \"" + fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1) + "\" ");
        String type = subType != null ? subType : ("\"" + decodeType(fieldType) + "\"");
        fields.add("\"fixTag\": " + fixTag);
        fields.add("\"fixType\": \"" + fieldType + "\"");
        if (array) {
            // fields.add("\"items\": " + type);
            type = "{ \"type\":\"array\",\"items\": " + type + "}";
        }
        if (optional) {
            type = "[ \"null\", " + type + " ]";
            fields.add("\"default\": null");
        }
        fields.add("\"type\": " + type);
        return "{ " + String.join(", ", fields) + " }";
    }

    /** returns an Avro Schema Type for the supplied set of fields in the data dictionary */
    private static String createType(String name, quickfix.DataDictionary dict, quickfix.DataDictionary dictFixt,
            int[] fields, String messageId, HashMap<String, String> typeCache, int indent) {
        java.util.List<String> output = new java.util.LinkedList<>();
        for (int i : fields) {
            boolean isHeader = dictFixt.isHeaderField(i);
            boolean isTrailer = dictFixt.isTrailerField(i);
            DataDictionary useDict = (isHeader || isTrailer) ? dictFixt : dict;

            String fieldName = useDict.getFieldName(i);
            Boolean required = isHeader ? useDict.isRequiredHeaderField(i)
                    : isTrailer ? useDict.isRequiredTrailerField(i) : useDict.isRequiredField(messageId, i);
            quickfix.FieldType ee = useDict.getFieldTypeEnum(i);

            Boolean group = isHeader ? useDict.isHeaderGroup(i) : useDict.isGroup(messageId, i);
            String fieldTypeName = (ee == null) ? ("NOTFOUND:" + useDict.getFieldType(i)) : ee.getName();
            String subType = null;
            boolean isArrayField = isArray(fieldTypeName);
            if (ee.getName() == "UNKNOWN") {
                fieldTypeName = "string";
            }
            if (group) {
                GroupInfo groupInfo = isHeader ? useDict.getGroup(DataDictionary.HEADER_ID, i)
                        : isTrailer ? useDict.getGroup(DataDictionary.TRAILER_ID, i) : useDict.getGroup(messageId, i);
                fieldTypeName = fieldName.substring(2) + "Type";
                if (!typeCache.containsKey(fieldTypeName)) {
                    DataDictionary ddd = groupInfo.getDataDictionary();
                    subType = createType(fieldTypeName, dict, dictFixt, ddd.getOrderedFields(), messageId, typeCache,
                            indent + 2);
                    typeCache.put(fieldTypeName, subType);
                }
            }
            if (group && fieldName.startsWith("No")) {
                fieldName = fieldName.substring(2,3).toLowerCase() + fieldName.substring(3);
            }
            String fieldLineDef = fieldLine(fieldName, i, fieldTypeName, subType, !required, group || isArrayField);
            output.add(getTabs(indent + 1) + fieldLineDef);
        }
        return "{\n" + getTabs(indent) + "\"type\": \"record\",\n" + getTabs(indent) + "\"name\": \"" + name + "\",\n"
                + getTabs(indent) + "\"fields\": [\n" + String.join(",\n", output) + "\n" + getTabs(indent) + "]\n"
                + getTabs(indent - 1) + "}";
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0 || args[0] == "--help") {
                System.out.println("Use fix-avro <message-name> [FIX44|FIX50]");
                // return;
            }
            final String vers = (args.length > 1) ? args[1] : "FIX50SP2";
            final String messageName = (args.length > 0) ? args[0] : "TradeCaptureReport";

            quickfix.DataDictionary dict = new DataDictionary(vers + ".xml");
            quickfix.DataDictionary fixtDict = new DataDictionary("FIXT11.xml");

            HashMap<String, String> typeCache = new HashMap<>();
            final String messageId = dict.getMsgType(messageName);

            int[] filtered = IntStream.of(dict.getOrderedFields()).filter(
                    i -> dict.isMsgField(messageId, i) || fixtDict.isHeaderField(i) || fixtDict.isTrailerField(i))
                    .toArray();

            String output = createType(messageName, dict, fixtDict, filtered, messageId, typeCache, 1);
            System.out.println(output);
        } catch (quickfix.ConfigError e) {
            System.out.format("Error " + e.getMessage());
        }

    }
}
