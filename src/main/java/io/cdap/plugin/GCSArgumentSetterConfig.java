package io.cdap.plugin;


import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.etl.api.FailureCollector;

/** Holds configuration required for configuring {@link MazinGCSArgumentSetter}. */
public final class GCSArgumentSetterConfig extends GCPConfig {

    public static final String NAME_PATH = "path";

    @Name(NAME_PATH)
    @Macro
    @Description("GCS Path to the file containing the arguments")
    private String path;

    public void validate(FailureCollector collector) {
        validateProperties(collector);

        if (canConnect()) {
            try {
                MazinGCSArgumentSetter.getContent(this);
            } catch (Exception e) {
                collector.addFailure("Can not get content from GCP!", null);
            }
        }
        collector.getOrThrowException();
    }

    public void validateProperties(FailureCollector collector) {
        if (!containsMacro(NAME_PATH)) {
            try {
                getPath();
            } catch (IllegalArgumentException e) {
                collector.addFailure(e.getMessage(), null).withConfigProperty(NAME_PATH);
            }
        }

        Boolean isServiceAccountJson = isServiceAccountJson();
        if (isServiceAccountJson != null && isServiceAccountJson
                && !containsMacro(NAME_SERVICE_ACCOUNT_JSON)
                && Strings.isNullOrEmpty(getServiceAccountJson())) {
            collector
                    .addFailure("Required property 'Service Account JSON' has no value.", "")
                    .withConfigProperty(NAME_SERVICE_ACCOUNT_JSON);
        }
    }

    private boolean canConnect() {
        boolean canConnect =
                !containsMacro(NAME_PATH)
                        && !(containsMacro(NAME_PROJECT) || AUTO_DETECT.equals(project))
                        && !(containsMacro(NAME_SERVICE_ACCOUNT_TYPE));

        if (!canConnect) {
            return false;
        }

        if (!isServiceAccountJson()) {
            return !containsMacro(NAME_SERVICE_ACCOUNT_FILE_PATH)
                    && !Strings.isNullOrEmpty(getServiceAccountFilePath());
        }
        return !containsMacro(NAME_SERVICE_ACCOUNT_JSON)
                && !Strings.isNullOrEmpty(getServiceAccountJson());
    }

    public GCSPath getPath() {
        return GCSPath.from(path);
    }
}

