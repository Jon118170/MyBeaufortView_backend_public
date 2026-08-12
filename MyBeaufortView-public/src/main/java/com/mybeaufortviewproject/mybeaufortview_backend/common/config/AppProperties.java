package com.mybeaufortviewproject.mybeaufortview_backend.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Aws aws = new Aws();

    private Openai openai = new Openai();

    public Openai getOpenai() {
        return openai;
    }

    public void setOpenai(Openai openai) {
        this.openai = openai;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Aws getAws() {
        return aws;
    }

    public static class Openai {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4.1-mini";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Jwt {

        @NotBlank
        private String secretKey;

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    public static class Aws {

        private final S3 s3 = new S3();

        public S3 getS3() {
            return s3;
        }

        public static class S3 {

            @NotBlank
            private String bucketName;

            @NotBlank
            private String region = "us-east-1";

            public String getBucketName() {
                return bucketName;
            }

            public void setBucketName(String bucketName) {
                this.bucketName = bucketName;
            }

            public String getRegion() {
                return region;
            }

            public void setRegion(String region) {
                this.region = region;
            }
        }
    }
}
