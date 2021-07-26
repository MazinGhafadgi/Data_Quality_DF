package io.cdap.plugin.json.parser;

public class DQConfig {
    private String sourcePath;
    private String sinkPath;
    private String dqConfigPath;
    private String repoType; // type of data quality rules repo , for example GS, BiG Query or Collibra
    private String sendToError; // failed rules

    public String getSinkPath() {
        return sinkPath;
    }

    public String getDqConfigPath() {
        return dqConfigPath;
    }

    public DQConfig() {

    }

    public String getSourcePath() {
        return sourcePath;
    }
}
