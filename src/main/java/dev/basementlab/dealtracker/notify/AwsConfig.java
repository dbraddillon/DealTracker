package dev.basementlab.dealtracker.notify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class AwsConfig {

    // Credentials are resolved via the AWS SDK's default provider chain (env vars,
    // ~/.aws/credentials, instance/container profile) - deliberately not configured here.
    // Nothing in this class ever sees an access key.
    @Bean
    public SesV2Client sesV2Client(@Value("${dealtracker.aws.region}") String region) {
        return SesV2Client.builder()
                .region(Region.of(region))
                .build();
    }
}
