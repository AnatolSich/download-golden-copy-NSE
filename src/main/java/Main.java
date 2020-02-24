import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import download.ApacheHttpClient;
import download.CloudStorageClient;
import download.RedisClient;
import jsonUtilities.JsonUtils;
import model.Entity;
import model.ReportRecord;
import org.apache.log4j.Logger;
import service.BotLoadService;
import service.DateTimeService;
import service.EntityService;
import service.MatchService;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@SuppressWarnings("WeakerAccess")

public class Main {

    private static final Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) {

        try {
            Properties appProps = loadProperties();
            DateTimeService dateTimeService = new DateTimeService(appProps);
            ApacheHttpClient apacheHttpClient = new ApacheHttpClient();
            BotLoadService botLoadService = new BotLoadService(apacheHttpClient, appProps);
            MatchService matchService = new MatchService(botLoadService.downloadBodJson());
            RedisClient redisClient = new RedisClient(matchService, appProps);
            JsonUtils jsonService = new JsonUtils(matchService);
            EntityService entityService = new EntityService(appProps);
            CloudStorageClient cloudStorageClient = loadAmazonClient(appProps, dateTimeService);
            ObjectMapper objectMapper =  getObjectMapper();

            Map<String, List<Entity>> mapRedisEntities = getRedisData(entityService, redisClient);
            Map<String, List<Entity>> mapNiftyEntities = getNiftyData(appProps, apacheHttpClient, jsonService, cloudStorageClient);

            List<ReportRecord> report = entityService.getReportRecords(mapNiftyEntities, mapRedisEntities);
            cloudStorageClient.uploadString(appProps.getProperty("aws.s3.report.key") + " " + dateTimeService.getFormattedNiftyCurrentLocalDateTime(), objectMapper.writeValueAsString(report));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private static CloudStorageClient loadAmazonClient(Properties appProps, DateTimeService dateTimeService) {
        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(appProps.getProperty("aws.access.key"), appProps.getProperty("aws.secret.key"))))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(appProps.getProperty("aws.access.endpoint"), Regions.US_EAST_1.name()))
                .build();
        return new CloudStorageClient(appProps.getProperty("aws.s3.bucket.name"), s3client, dateTimeService);
    }

    private static Map<String, List<Entity>> getRedisData(EntityService entityService, RedisClient redisClient) throws IOException {
        Map<String, List<String>> map = redisClient.getMapFromRedisData();
        return entityService.parseFromRedisMap(map);
    }

    private static Map<String, List<Entity>> getNiftyData(Properties appProps, ApacheHttpClient apacheHttpClient, JsonUtils jsonService, CloudStorageClient cloudStorageClient) throws IOException {
        String response = apacheHttpClient.sendGet(appProps.getProperty("upstox.goldencopy.NSE.url"));
        cloudStorageClient.uploadNiftyData(response);
        return jsonService.parseFromNiftyMap(jsonService.parseStringToJson(response));
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("dd/MM/yyyy hh.mm a")));
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    private static Properties loadProperties() throws IOException {
        String appPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("application.properties")).getPath();

        Properties appProps = new Properties();
        appProps.load(new FileInputStream(appPath));
        log.info("Props ready");
        return appProps;
    }


}
