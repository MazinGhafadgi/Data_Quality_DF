package io.cdap.plugin;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Joiner;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.PipelineConfigurer;
import io.cdap.cdap.etl.api.StageConfigurer;
import io.cdap.cdap.etl.api.action.Action;
import io.cdap.cdap.etl.api.action.ActionContext;
import io.cdap.plugin.json.parser.DQConfig;
import io.cdap.plugin.json.parser.DQRules;
import io.cdap.plugin.json.parser.Rule;
import io.cdap.plugin.util.GCPUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class <code>GCSArgumentSetter</code> get json file configuration from GCS.
 *
 * <p>The plugin provides the ability to map json properties as pipeline arguments name and columns
 * values as pipeline arguments <code>
 *   {
 *     "arguments" : [
 *        { "name" : "input.path", "type" : "string", "value" : "/data/sunny_feeds/master"},
 *        { "name" : "parse.schema, "type" : "schema", "value" : [
 *             { "name" : "fname", "type" : "string", "nullable" : true },
 *             { "name" : "age", "type" : "int", "nullable" : true},
 *             { "name" : "salary", "type" : "float", "nullable" : false}
 *          ]},
 *        { "name" : "directives", "type" : "array", "value" : [
 *             "parse-as-json body",
 *             "columns-replace s/body_//g",
 *             "keep f1,f2"
 *          ]}
 *     ]
 *   }
 *
 *   In case when `nullable` is not specified inside field json, then it defaults to true.
 * </code>
 */
@Plugin(type = Action.PLUGIN_TYPE)
@Name(MazinGCSArgumentSetter.NAME)
@Description("Mazin Argument setter for dynamically configuring pipeline from GCS test")
public final class MazinGCSArgumentSetter extends Action {

    public static final String NAME = "MazinGCSArgumentSetter";
    private GCSArgumentSetterConfig config;

    @Override
    public void configurePipeline(PipelineConfigurer configurer) {
        super.configurePipeline(configurer);
        StageConfigurer stageConfigurer = configurer.getStageConfigurer();
        FailureCollector collector = stageConfigurer.getFailureCollector();
        config.validate(collector);
    }

    @Override
    public void run(ActionContext context) throws Exception {
        config.validateProperties(context.getFailureCollector());
        String fileContent = MazinGCSArgumentSetter.getContent(config);

        try {
            DQConfig dqConfig = new GsonBuilder().create().fromJson(fileContent, DQConfig.class);
            //set sourcePath
            context.getArguments().set("sourcePath", dqConfig.getSourcePath());
            //set sink
            context.getArguments().set("sinkPath", dqConfig.getSinkPath());

            //yyyy-MM-dd-HH-mm
            context.getArguments().set("partition", "yyyy-MM-dd-HH-mm");


            Storage storage = getStorage(config);
            GCSPath path = GCSPath.from(dqConfig.getDqConfigPath());
            Blob blob = storage.get(path.getBucket(), path.getName());
            String dqRulesContent =  new String(blob.getContent());
            DQRules dqRules = new GsonBuilder().create().fromJson(dqRulesContent, DQRules.class);

             String rules = "parse-as-csv body , true \n drop body\n";

             //TODO
            //make sure the plugin works with bigquery or other repositories , like collibra.
            //you need to configure a falg in configuration.json for the source type of the configuration example GS or bigQuery or http.
            //TODO
            // need to develop new directive called send-to-bigquery-error -- done
            //follow the instruction here on how to write to big query table
            //https://cloud.google.com/bigquery/streaming-data-into-bigquery

           context.getArguments().set("inputSchema", "{\"type\":\"record\",\"name\":\"etlSchemaBody\",\"fields\":[{\"name\":\"body\",\"type\":\"string\"}]}");
           context.getArguments().set("outputSchema", "{\"type\":\"record\",\"name\":\"etlSchemaBody\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"}]}");
             for (Rule rule : dqRules.getRules()) {
                String name = rule.getRuleName();
                String column = rule.getColumn();
                //TODO make sure the metric namespace, error message and projectId and datset comes from the configuration.
                String error = " mazinMetric ' Mazin is a great programmer' 'eng-mechanism-316510:employee'";
                //"send-to-error exp:{ dq:isempty(C)}" + error,
                String action = rule.getAction(); // example send-to-error
                if (column != null) {
                    String dq = name.contains("!") ? "!dq:" : "dq:";
                    rules = rules + action + " " + "exp:{" + dq + name.replace("!", "") + "(" + column + ")" + "}" + error + "\n";
                } else {
                    throw new RuntimeException(
                            "Configuration '" + name + "' is null. Cannot set argument to null.");
                }
            }
             //set directives
            context.getArguments().set("directives", rules);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(
                    String.format(
                            "Could not parse response from '%s': %s", config.getPath(), e.getMessage()));
        }
    }

    private static Storage getStorage(GCSArgumentSetterConfig config) throws IOException {
        String serviceAccount = config.getServiceAccount();
        StorageOptions.Builder builder = StorageOptions.newBuilder().setProjectId(config.getProject());
        if (serviceAccount != null) {
            builder.setCredentials(GCPUtils.loadServiceAccountCredentials(serviceAccount, config.isServiceAccountFilePath()));
        }
        return builder.build().getService();
    }

    public static String getContent(GCSArgumentSetterConfig config) throws IOException {
        Storage storage = getStorage(config);
        GCSPath path = config.getPath();
        Blob blob = storage.get(path.getBucket(), path.getName());
        return new String(blob.getContent());
    }

    private static final class Configuration {

        private List<Argument> arguments;

        public List<Argument> getArguments() {
            return arguments;
        }
    }

    private static final class Argument {

        private String name;
        private String type;
        private JsonElement value;

        Argument() {
            type = "string";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            if (value == null) {
                throw new IllegalArgumentException("Null Argument value for name '" + name + "'");
            }
            if (type.equalsIgnoreCase("schema")) {
                return createSchema(value).toString();
            } else if (type.equalsIgnoreCase("int")) {
                return Integer.toString(value.getAsInt());
            } else if (type.equalsIgnoreCase("float")) {
                return Float.toString(value.getAsFloat());
            } else if (type.equalsIgnoreCase("double")) {
                return Double.toString(value.getAsDouble());
            } else if (type.equalsIgnoreCase("short")) {
                return Short.toString(value.getAsShort());
            } else if (type.equalsIgnoreCase("string")) {
                return value.getAsString();
            } else if (type.equalsIgnoreCase("char")) {
                return Character.toString(value.getAsCharacter());
            } else if (type.equalsIgnoreCase("array")) {
                List<String> values = new ArrayList<>();
                for (JsonElement v : value.getAsJsonArray()) {
                    values.add(v.getAsString());
                }
                return Joiner.on("\n").join(values);
            } else if (type.equalsIgnoreCase("map")) {
                List<String> values = new ArrayList<>();
                for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                    values.add(String.format("%s=%s", entry.getKey(), entry.getValue().getAsString()));
                }
                return Joiner.on(";").join(values);
            }

            throw new IllegalArgumentException("Invalid argument value '" + value + "'");
        }

        private Schema createSchema(JsonElement array) {
            List<Schema.Field> fields = new ArrayList<>();
            for (JsonElement field : array.getAsJsonArray()) {
                Schema.Type type = Schema.Type.STRING;

                JsonObject object = field.getAsJsonObject();
                String fieldType = object.get("type").getAsString().toLowerCase();

                boolean isNullable = true;
                if (object.get("nullable") != null) {
                    isNullable = object.get("nullable").getAsBoolean();
                }

                String name = object.get("name").getAsString();

                if (fieldType.equals("double")) {
                    type = Schema.Type.DOUBLE;
                } else if (fieldType.equals("float")) {
                    type = Schema.Type.FLOAT;
                } else if (fieldType.equals("long")) {
                    type = Schema.Type.LONG;
                } else if (fieldType.equals("int")) {
                    type = Schema.Type.INT;
                } else if (fieldType.equals("short")) {
                    type = Schema.Type.INT;
                } else if (fieldType.equals("string")) {
                    type = Schema.Type.STRING;
                }

                Schema schema;
                if (isNullable) {
                    schema = Schema.nullableOf(Schema.of(type));
                } else {
                    schema = Schema.of(type);
                }
                Schema.Field fld = Schema.Field.of(name, schema);
                fields.add(fld);
            }

            return Schema.recordOf("record", fields);
        }
    }
}
