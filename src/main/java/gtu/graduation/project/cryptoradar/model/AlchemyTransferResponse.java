package gtu.graduation.project.cryptoradar.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlchemyTransferResponse {
    private String jsonrpc;
    private String id;
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Transfer> transfers;
        private String pageKey;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transfer {
        private String blockNum;
        private String uniqueId;
        private String hash;
        private String from;
        private String to;
        private Double value;
        private String asset;
        private String category;
        private RawContract rawContract;
        private Metadata metadata;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawContract {
        private String value;
        private String address;
        private String decimal;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String blockTimestamp;
    }
}
