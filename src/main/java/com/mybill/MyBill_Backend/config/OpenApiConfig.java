package com.mybill.MyBill_Backend.config;

import com.mybill.MyBill_Backend.dto.ApiErrorResponse;
import com.mybill.MyBill_Backend.dto.ValidationErrorResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

@Configuration
@ConditionalOnProperty(prefix = "springdoc.api-docs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    @Bean
    public OpenAPI myBillOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyBill Backend API")
                        .version("1.0.0")
                        .description("""
                                Complete OpenAPI 3 documentation for the MyBill backend.

                                Authentication: pass the application JWT as `Authorization: Bearer <token>`.
                                Validation rules are generated from Jakarta Bean Validation annotations on request DTOs.
                                File uploads use `multipart/form-data`; PDF and image downloads stream binary content.
                                """)
                        .license(new License().name("Private")))
                .addServersItem(new Server().url("/").description("Current server"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(
                        BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT returned by `/api/auth/firebase-login`.")
                )
                .addSchemas("ApiErrorResponse", apiErrorSchema())
                .addSchemas("ValidationErrorResponse", validationErrorSchema()));
    }

    @Bean
    public GroupedOpenApi publicAndMobileApi() {
        return GroupedOpenApi.builder()
                .group("mybill-api")
                .pathsToMatch("/api/**", "/ping")
                .build();
    }

    @Bean
    public OperationCustomizer myBillOperationCustomizer() {
        return (operation, handlerMethod) -> {
            applyDefaultOperationText(operation, handlerMethod);
            applyCommonParameters(operation);
            applyCommonResponses(operation);
            applyEndpointSpecificDocumentation(operation);
            return operation;
        };
    }

    private void applyDefaultOperationText(Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) {
        if (operation.getSummary() == null || operation.getSummary().isBlank()) {
            operation.setSummary(defaultSummary(handlerMethod));
        }
        if (operation.getDescription() == null || operation.getDescription().isBlank()) {
            operation.setDescription("Generated from the Spring controller method `" + handlerMethod.getMethod().getName() + "`.");
        }
    }

    private String defaultSummary(org.springframework.web.method.HandlerMethod handlerMethod) {
        String methodName = handlerMethod.getMethod().getName();
        String verb = httpVerb(handlerMethod);
        String spaced = methodName.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return verb + " " + spaced;
    }

    private String httpVerb(org.springframework.web.method.HandlerMethod handlerMethod) {
        if (AnnotationUtils.findAnnotation(handlerMethod.getMethod(), GetMapping.class) != null) return "Get";
        if (AnnotationUtils.findAnnotation(handlerMethod.getMethod(), PostMapping.class) != null) return "Create";
        if (AnnotationUtils.findAnnotation(handlerMethod.getMethod(), PutMapping.class) != null) return "Update";
        if (AnnotationUtils.findAnnotation(handlerMethod.getMethod(), PatchMapping.class) != null) return "Patch";
        if (AnnotationUtils.findAnnotation(handlerMethod.getMethod(), DeleteMapping.class) != null) return "Delete";
        return "Call";
    }

    private void applyCommonParameters(Operation operation) {
        if (operation.getParameters() == null) return;
        for (Parameter parameter : operation.getParameters()) {
            if ("page".equals(parameter.getName())) {
                parameter.setDescription("Zero-based page index.");
                parameter.setExample(0);
            } else if ("size".equals(parameter.getName())) {
                parameter.setDescription("Page size.");
                parameter.setExample(20);
            } else if ("sort".equals(parameter.getName())) {
                parameter.setDescription("Sort expression, for example `createdDate,desc`.");
                parameter.setExample("createdDate,desc");
            } else if ("since".equals(parameter.getName())) {
                parameter.setDescription("ISO-8601 timestamp used by incremental sync endpoints.");
                parameter.setExample("2026-07-14T10:15:30");
            }
        }
    }

    private void applyCommonResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        responses.addApiResponse("400", response("Bad request or validation failure", "#/components/schemas/ValidationErrorResponse", validationExample()));
        responses.addApiResponse("401", response("Missing, expired, or invalid JWT token", "#/components/schemas/ApiErrorResponse", errorExample(401, "Unauthorized", "Authentication token is missing or invalid")));
        responses.addApiResponse("403", response("Authenticated user is not allowed to access this resource", "#/components/schemas/ApiErrorResponse", errorExample(403, "Forbidden", "You do not have permission to perform this action")));
        responses.addApiResponse("404", response("Requested resource was not found", "#/components/schemas/ApiErrorResponse", errorExample(404, "Not Found", "Resource not found")));
        responses.addApiResponse("409", response("Conflict, duplicate, or stale sync change", "#/components/schemas/ApiErrorResponse", errorExample(409, "Conflict", "A conflicting record already exists. Refresh and try again")));
        responses.addApiResponse("413", response("Upload payload is too large", "#/components/schemas/ApiErrorResponse", errorExample(413, "Payload Too Large", "Image must be 5 MB or smaller.")));
        responses.addApiResponse("415", response("Unsupported content type or content encoding", "#/components/schemas/ApiErrorResponse", errorExample(415, "Unsupported Media Type", "Please upload a JPG or PNG image.")));
        responses.addApiResponse("429", response("Rate limit exceeded", "#/components/schemas/ApiErrorResponse", errorExample(429, "Too Many Requests", "Too many requests")));
        responses.addApiResponse("500", response("Unexpected server error", "#/components/schemas/ApiErrorResponse", errorExample(500, "Internal Server Error", "Something went wrong. Please try again later")));
    }

    private void applyEndpointSpecificDocumentation(Operation operation) {
        String operationId = operation.getOperationId() != null ? operation.getOperationId().toLowerCase() : "";

        if (operationId.contains("upload")) {
            operation.setDescription("""
                    Uploads and normalizes a business image.

                    Validation rules: `file` is required, content type must be `image/jpeg` or `image/png`,
                    size must be 5 MB or smaller, and decoded image dimensions must not exceed 20,000,000 pixels.
                    Response body contains the stored `/uploads/...` path used by the business profile.
                    """);
            operation.getResponses().addApiResponse("200", new ApiResponse()
                    .description("Upload accepted")
                    .content(new Content().addMediaType(JSON, new MediaType()
                            .example(java.util.Map.of("path", "/uploads/logo_123e4567-e89b-12d3-a456-426614174000.png")))));
        }

        if (operationId.contains("downloadinvoice")) {
            operation.setDescription("Streams the invoice PDF as `application/pdf` with an attachment filename based on the invoice number.");
            operation.getResponses().addApiResponse("200", new ApiResponse()
                    .description("PDF stream")
                    .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_PDF_VALUE, new MediaType()
                            .schema(new Schema<>().type("string").format("binary")))));
        }

        if (operationId.contains("servefile")) {
            operation.setDescription("Streams an authenticated business upload owned by the current user. Valid filenames match `logo|qr|signature_<uuid>.png|jpg`.");
            operation.getResponses().addApiResponse("200", new ApiResponse()
                    .description("Image stream")
                    .content(new Content()
                            .addMediaType(org.springframework.http.MediaType.IMAGE_PNG_VALUE, new MediaType().schema(new Schema<>().type("string").format("binary")))
                            .addMediaType(org.springframework.http.MediaType.IMAGE_JPEG_VALUE, new MediaType().schema(new Schema<>().type("string").format("binary")))));
        }

        if (operationId.contains("sync")) {
            operation.setDescription("""
                    Synchronizes offline mobile changes with the server.

                    `/api/sync` accepts normal JSON and gzip-compressed JSON when `Content-Encoding: gzip` is sent.
                    `/api/sync/background` marks the request as a background sync. `pageSize` must be 1..500,
                    `changes` may include at most 100 entries, and conflict policy is `CLIENT_WINS` or `SERVER_WINS`.
                    """);
        }
    }

    private ApiResponse response(String description, String schemaRef, Object example) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON, new MediaType()
                        .schema(new Schema<>().$ref(schemaRef))
                        .addExamples("example", new Example().value(example))));
    }

    private Object errorExample(int status, String error, String message) {
        return java.util.Map.of(
                "timestamp", "2026-07-14T10:15:30",
                "status", status,
                "error", error,
                "message", message,
                "path", "/api/example",
                "requestId", "8d3f67f7b0f64f6d"
        );
    }

    private Object validationExample() {
        return java.util.Map.of(
                "timestamp", "2026-07-14T10:15:30",
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "message", "Validation failed",
                "path", "/api/invoice/generate",
                "requestId", "8d3f67f7b0f64f6d",
                "fieldErrors", java.util.Map.of(
                        "clientId", "Client is required",
                        "workIds", "Select at least one work item"
                )
        );
    }

    private Schema<?> apiErrorSchema() {
        return new ObjectSchema()
                .description(ApiErrorResponse.class.getSimpleName())
                .addProperty("timestamp", new StringSchema().format("date-time").example("2026-07-14T10:15:30"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("error", new StringSchema().example("Not Found"))
                .addProperty("message", new StringSchema().example("Resource not found"))
                .addProperty("path", new StringSchema().example("/api/invoice/123e4567-e89b-12d3-a456-426614174000"))
                .addProperty("requestId", new StringSchema().example("8d3f67f7b0f64f6d"));
    }

    private Schema<?> validationErrorSchema() {
        return new ObjectSchema()
                .description(ValidationErrorResponse.class.getSimpleName())
                .addProperty("timestamp", new StringSchema().format("date-time").example("2026-07-14T10:15:30"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("error", new StringSchema().example("Bad Request"))
                .addProperty("message", new StringSchema().example("Validation failed"))
                .addProperty("path", new StringSchema().example("/api/invoice/generate"))
                .addProperty("requestId", new StringSchema().example("8d3f67f7b0f64f6d"))
                .addProperty("fieldErrors", new MapSchema()
                        .additionalProperties(new StringSchema())
                        .example(java.util.Map.of(
                                "clientId", "Client is required",
                                "workIds", "Select at least one work item"
                        )));
    }
}
