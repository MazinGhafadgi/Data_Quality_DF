package io.cdap.plugin.json.parser;

import com.google.gson.Gson;
import io.cdap.plugin.GCSPath;
import io.cdap.plugin.MazinGCSArgumentSetter;
import io.cdap.plugin.proto.Configuration;


/*
{
  "arguments" : [
    {
      "name": "input.path",
      "value": "gs://reusable-pipeline-tutorial/user-emails.txt"
    },
    {
      "name": "directives",
      "value": "send-to-error !dq:isEmail(body)"
    }
  ]
}
 */
public class JsonParserApp {
    private static final Gson gson = new Gson();
    public static void main(String[] args) {
        // "directives": "split-to-rows body \\n\nparse-as-json :body 1\nparse-as-json :body__id 1\nparse-as-json :body_weeks_on_list 1\nparse-as-json :body_rank_last_week 1\nparse-as-json :body_rank 1\nparse-as-json :body_price 1\nparse-as-json :body_published_date 2\nparse-as-json :body_bestsellers_date 2\ncolumns-replace s/^body_//g\ncleanse-column-names\nrename _id__oid id\nrename weeks_on_list__numberint weeks_on_list\nrename rank_last_week__numberint rank_last_week\nrename rank__numberint rank\nfill-null-or-empty :price__numberint '0'\nrename price__numberint price\nrename published_date__date__numberlong published_date\nrename bestsellers_date__date__numberlong bestsellers_date\nfill-null-or-empty :price__numberdouble '0'\nrename price__numberdouble price_other\nfill-null-or-empty :price '0'\nfill-null-or-empty :price_other '0'\nset-type :price float\nset-type :price_other float\nset-column :price_final price+price_other\ndrop price\ndrop price_other\nset-type :published_date long\nset-type :bestsellers_date long\nset-type :weeks_on_list integer\nset-type :rank_last_week integer\nset-type :rank long\nfilter-rows-on regex-not-match rank_last_week ^0$\ndrop rank_last_week\ndrop weeks_on_list\ndrop published_date\ndrop bestsellers_date\nfilter-rows-on condition-false rank <=3\nfilter-rows-on regex-match price_final ^0.0$\nfilter-rows-on condition-false price_final <25.0",

        String body = "{\n" +
                "  \"dqConfig\" : [\n" +
                "    {\n" +
                "      \"name\": \"input.path\",\n" +
                "      \"value\": \"gs://reusable-pipeline-tutorial/user-emails.txt\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"directives\",\n" +
                "      \"value\": \"send-to-error !dq:isEmail(body)\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String dqConfig = "{\"sourcePath\":\"gs://sourcePath\", \"sinkPath\": \"gs://sinkPath\", \"dqConfigPath\":\"gs://dqConfigPath\"}";
        String rules = "{\n" +
                "  \"rules\" : [\n" +
                "    {\n" +
                "      \"ruleName\": \"isempty\",\n" +
                "      \"column\": \"name\",\n" +
                "      \"action\": \"send-to-error\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        System.out.println(rules);
        DQConfig configuration = gson.fromJson(dqConfig, DQConfig.class);
        System.out.println(configuration);

        System.out.println(configuration.getSourcePath());
        System.out.println(configuration.getSinkPath());
        System.out.println(configuration.getDqConfigPath());

        //Get configuration.getDqConfigPath()
        //pars the configuration and assign it to string var
        //parse the string content using gson lib
        //set key value into settable object.

        DQRules dqRules = gson.fromJson(rules, DQRules.class);
        System.out.println(dqRules);

        String ruleBuilder = "";
        String directives = "parse-as-csv body , true \n drop body\n";
        for (Rule rule : dqRules.getRules()) {
            String name = rule.getRuleName();
            String column = rule.getColumn();
            String error = " mazinMetric ' Mazin is a great programmer' 'eng-mechanism-316510:employee'";
            //"send-to-error exp:{ dq:isempty(C)}" + error,
            String action = rule.getAction();
            if (column != null) {
                String dq = name.contains("!") ? "!dq:" : "dq:";
                directives = directives + action + " " + "exp:{" + dq + name.replace("!", "") + "(" + column + ")" + "}" + error + "\n";
            } else {
                throw new RuntimeException(
                        "Configuration '" + name + "' is null. Cannot set argument to null.");
            }
        }
        System.out.println(directives);

        GCSPath path = GCSPath.from("gs://reuable-pipeline-bucket/dqRules.json");
        System.out.println("path=" + path.getBucket());
        System.out.println("" + path.getName());

        //TODO
        // build data fusion dq rules from dqRules and set it into settable argument , set("dqRules", rules)
        // set source path into settable argument, set("sourcePath", value)
        // set sink path into settable argument, set("sinkPath", value)



}


}
