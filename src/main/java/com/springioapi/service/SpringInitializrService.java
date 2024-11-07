package com.springioapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SpringInitializrService {
	private static final Logger logger = LoggerFactory.getLogger(SpringInitializrService.class);
	public String baseDir = "my-spring-boot-project";
	public String artifactId = "GenerateSpringBoot";
	String bootVersion = "3.2.0";
	String groupId = "com.example";
	String name = "Demo Application";
	String description = "A demo application for Spring Boot";
	String packageName = "com.example.demo";
	private final RestTemplate restTemplate;

	@Autowired
	public SpringInitializrService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void generateProject(String dependencies, String type, String language, String javaVersion,
			String tmfStandard, String dbConfig) {
		try {
			URI uri = UriComponentsBuilder.fromUriString("https://start.spring.io/starter.zip")
					.queryParam("dependencies", dependencies).queryParam("type", type).queryParam("dbConfig", dbConfig)
					.queryParam("bootVersion", bootVersion).queryParam("language", language)
					.queryParam("groupId", groupId).queryParam("artifactId", artifactId).queryParam("name", name)
					.queryParam("description", description).queryParam("packageName", packageName)
					.queryParam("javaVersion", javaVersion).build().toUri();

			Resource resource = restTemplate.getForObject(uri, Resource.class);
			logger.info("Project ZIP received from Spring Initializr.");

			Path basePath = Paths.get(baseDir);
			createDirectoryIfNotExists(basePath);
			Path projectDir = extractProject(resource, basePath);

			if (tmfStandard != null && !tmfStandard.isEmpty()) {
				logger.info("Generating TMF-specific project for standard: {}", tmfStandard);
				generateTmfClasses(projectDir, packageName, tmfStandard);
				addApplicationProperties(projectDir, dbConfig, tmfStandard);
				addRestConfig(projectDir, packageName);
			} else {
				logger.info("Generating basic Spring Boot project without TMF customization.");
				addApplicationProperties(projectDir, dbConfig, tmfStandard);
				addRestConfig(projectDir, packageName);
			}

			repackageProject(projectDir, basePath.resolve(artifactId + ".zip"));
			logger.info("Project generated successfully at {}", projectDir.toAbsolutePath());

		} catch (Exception e) {
			logger.error("Error generating project", e);
		}
	}

	private Path extractProject(Resource resource, Path basePath) throws IOException {
		Path zipPath = basePath.resolve(artifactId + ".zip");
		Files.copy(resource.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);

		Path projectDir = basePath.resolve(artifactId);
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				Path filePath = projectDir.resolve(entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(filePath);
				} else {
					Files.createDirectories(filePath.getParent());
					Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}

		return projectDir;
	}

	private void generateTmfClasses(Path projectDir, String packageName, String tmff) throws IOException {
		Map<String, List<String>> controllersAndMethods = getTmfControllersAndMethods(tmff);

		String controllerTemplate = getTemplateContent("tmf-controller.txt");
		String serviceTemplate = getTemplateContent("tmf-service-template.txt");
//		String notificationListenerTemplate = getTemplateContent("notification-listener-template.txt");

		Path srcPath = projectDir.resolve("src/main/java/" + packageName.replace('.', '/'));
		Path controllerPath = srcPath.resolve("controller");
		Path servicePath = srcPath.resolve("service");

		Files.createDirectories(controllerPath);
		Files.createDirectories(servicePath);

		for (Map.Entry<String, List<String>> entry : controllersAndMethods.entrySet()) {
			String controllerName = entry.getKey();
			List<String> methods = entry.getValue();
//			String templateToUse = controllerName.equals("NotificationListener") ? notificationListenerTemplate
//					: controllerTemplate;
//
			String controllerContent = processTemplate( packageName, packageName,controllerName, methods, tmff);
			Files.write(controllerPath.resolve(controllerName + "Controller.java"), controllerContent.getBytes());

			String serviceContent = processTemplate(serviceTemplate, packageName, controllerName, methods, tmff);
			Files.write(servicePath.resolve(controllerName + "Service.java"), serviceContent.getBytes());
		}
	}

	private String getTemplateContent(String templateName) throws IOException {
		Resource resource = new ClassPathResource("templates/" + templateName);
		return new String(resource.getInputStream().readAllBytes());
	}

	private String processTemplate(String template, String packageName, String controllerName, List<String> methods, String tmff) {
		String controllerNameLower = controllerName.toLowerCase();
		String tmfStandardLower=tmff.toLowerCase();
		String processedTemplate = template
				.replace("{{packageName}}", packageName)
				.replace("{{artifactId}}", artifactId)
				.replace("{{controllerName}}", controllerName)
				.replace("{{serviceName}}", controllerName + "Service")
				.replace("{{tmfStandard}}", tmff)
				.replace("{{controllerName.toLowerCase()}}", controllerNameLower)
		        .replace("{{tmfStandard.toLowerCase()}}", tmfStandardLower);

//		if (controllerName.equals("NotificationListener")) {
//			StringBuilder methodsBuilder = new StringBuilder();
//			for (String method : methods) {
//				methodsBuilder.append(method).append(",");
//			}
//			if (methodsBuilder.length() > 0) {
//				methodsBuilder.setLength(methodsBuilder.length() - 1);
//			}
//			processedTemplate = processedTemplate.replace("{{notificationMethods}}", methodsBuilder.toString());
//		} else {
			
		    processedTemplate = processedTemplate.replace("{% if methods.contains(\"getAll\") %}", methods.contains("getAll") ? "" : "/*").replace("{% getAll endif %}", methods.contains("getAll") ? "" : "*/");
			processedTemplate = processedTemplate.replace("{% if methods.contains(\"get\") %}", methods.contains("get") ? "" : "/*").replace("{%  get endif  %}", methods.contains("get") ? "" : "*/");
			processedTemplate = processedTemplate.replace("{% if methods.contains(\"post\") %}", methods.contains("post") ? "" : "/*").replace("{% post endif %}", methods.contains("post") ? "" : "*/");
			processedTemplate = processedTemplate.replace("{% if methods.contains(\"put\") %}", methods.contains("put") ? "" : "/*").replace("{% put endif %}", methods.contains("put") ? "" : "*/");
			processedTemplate = processedTemplate.replace("{% if methods.contains(\"patch\") %}", methods.contains("patch") ? "" : "/*").replace("{% patch endif %}", methods.contains("patch") ? "" : "*/");
			processedTemplate = processedTemplate.replace("{% if methods.contains(\"delete\") %}", methods.contains("delete") ? "" : "/*").replace("{% delete endif %}", methods.contains("delete") ? "" : "*/");



		return processedTemplate;
	}

	private void addApplicationProperties(Path projectDir, String dbType, String tmfStandard) throws IOException {
		Path resourcePath = projectDir.resolve("src/main/resources");
		Files.createDirectories(resourcePath);

		String tmfProperties;
		switch (tmfStandard.toLowerCase()) {
		case "tmf620":
			tmfProperties = getTemplateContent("application-tmf620.properties");
			break;
		case "tmf622":
			tmfProperties = getTemplateContent("application-tmf622.properties");
			break;
		case "tmf632":
			tmfProperties = getTemplateContent("application-tmf632.properties");
			break;
		case "tmf641":
			tmfProperties = getTemplateContent("application-tmf641.properties");
			break;
		case "tmf666":
			tmfProperties = getTemplateContent("application-tmf666.properties");
			break;
		default:
			throw new IllegalArgumentException("Unsupported TMF standard: " + tmfStandard);
		}

		String dbConfig;
		if ("h2".equalsIgnoreCase(dbType)) {
			dbConfig = getTemplateContent("application-h2.properties");
		} else if ("mysql".equalsIgnoreCase(dbType)) {
			dbConfig = getTemplateContent("application-mysql.properties");
		} else {
			throw new IllegalArgumentException("Unsupported database type: " + dbType);
		}

		String finalProperties = dbConfig + "\n" + tmfProperties;
		Files.write(resourcePath.resolve("application.properties"), finalProperties.getBytes());
	}

	private void addRestConfig(Path projectDir, String packageName) throws IOException {
		String restConfigTemplate = getTemplateContent("rest-template-config.txt");
		String restConfigContent = restConfigTemplate.replace("{{packageName}}", packageName);

		Path configPath = projectDir.resolve("src/main/java/" + packageName.replace('.', '/') + "/config");
		Files.createDirectories(configPath);
		Files.write(configPath.resolve("RestTemplateConfig.java"), restConfigContent.getBytes());
	}

	private Map<String, List<String>> getTmfControllersAndMethods(String tmf) {
		Map<String, List<String>> controllersAndMethods = new HashMap<>();

		switch (tmf.toLowerCase()) {
		case "tmf620":
			controllersAndMethods.put("Catalog", Arrays.asList("getAll", "get", "post", "put", "patch", "delete"));
			controllersAndMethods.put("Category", Arrays.asList("getAll", "get", "post", "put", "patch", "delete"));
			controllersAndMethods.put("ProductOffering",
					Arrays.asList("getAll", "get", "post", "put", "patch", "delete"));
			controllersAndMethods.put("ProductOfferingPrice",
					Arrays.asList("getAll", "get", "post", "put", "patch", "delete"));
			controllersAndMethods.put("ProductSpec", Arrays.asList("getAll", "get", "post", "put", "patch", "delete"));
			controllersAndMethods.put("ImportJob", Arrays.asList("getAll", "get", "post", "delete"));
			controllersAndMethods.put("ExportJob", Arrays.asList("getAll", "get", "post", "delete"));
			controllersAndMethods.put("NotificationListener", Arrays.asList("post"));
			controllersAndMethods.put("EventSubscription", Arrays.asList("post", "delete"));
			break;
		case "tmf622":
			controllersAndMethods.put("ProductOrder", Arrays.asList("getAll", "get", "post", "patch", "delete"));
			controllersAndMethods.put("EventSubscription", Arrays.asList("post", "delete"));
//			controllersAndMethods.put("NotificationListener", Arrays.asList("post"));
			controllersAndMethods.put("CancelOrderProduct", Arrays.asList("getAll", "get", "post"));
			break;
		case "tmf632":
			controllersAndMethods.put("EventSubscription", Arrays.asList("post", "delete"));
			controllersAndMethods.put("Organization", Arrays.asList("getAll", "get", "post", "patch", "delete"));
//			controllersAndMethods.put("NotificationListener",
//					Arrays.asList("organizationStateChangeEvent", "organizationDeleteEvent", "organizationCreateEvent",
//							"organizationAttributeValueChangeEvent", "individualStateChangeEvent",
//							"individualDeleteEvent", "individualCreateEvent", "individualAttributeValueChangeEvent"));
			controllersAndMethods.put("Individual", Arrays.asList("getAll", "get", "post", "patch", "delete"));
			break;
		case "tmf641":
			controllersAndMethods.put("ServiceOrder", Arrays.asList("getAll", "get", "post", "patch", "delete"));
			controllersAndMethods.put("EventSubscription", Arrays.asList("post", "delete"));
			controllersAndMethods.put("NotificationListener", Arrays.asList("post"));
			controllersAndMethods.put("CancelServiceOrder", Arrays.asList("post", "get", "getAll"));
			break;
		case "tmf666":
			controllersAndMethods.put("Account", Arrays.asList("getAll", "get", "post", "put", "patch", "delete"));
			controllersAndMethods.put("BillFormat", Arrays.asList("getAll", "get", "post", "patch", "delete"));
			controllersAndMethods.put("BillingAccount", Arrays.asList("getAll", "get", "post", "patch", "delete"));
			controllersAndMethods.put("BillingCycleSpecification",
					Arrays.asList("getAll", "get", "post", "put", "delete"));
			controllersAndMethods.put("EventSubscription", Arrays.asList("post", "delete", "getAll", "patch"));
			controllersAndMethods.put("NotificationListener", Arrays.asList("post"));
			controllersAndMethods.put("ApiProduct", Arrays.asList("post", "get", "getAll"));
			controllersAndMethods.put("ApplicationOwner", Arrays.asList("post", "get", "getAll", "patch"));
			controllersAndMethods.put("BillingMedia", Arrays.asList("post", "get", "getAll", "put", "delete"));
			controllersAndMethods.put("Financial", Arrays.asList("post", "get", "getAll", "patch", "delete"));
			controllersAndMethods.put("Hub", Arrays.asList("post", "delete"));
			controllersAndMethods.put("PartyAccount", Arrays.asList("post", "get", "getAll", "patch", "delete"));
			controllersAndMethods.put("Settlement", Arrays.asList("post", "get", "getAll", "patch", "delete"));
			break;
		default:
			logger.warn("Unknown TMF standard: {}", tmf);
		}

		return controllersAndMethods;
	}

	private void createDirectoryIfNotExists(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectories(path);
			logger.info("Created directory: {}", path);
		}
	}

	private void repackageProject(Path projectDir, Path zipPath) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
			Files.walk(projectDir).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(projectDir.relativize(path).toString());
				try {
					zos.putNextEntry(zipEntry);
					Files.copy(path, zos);
					zos.closeEntry();
				} catch (IOException e) {
					logger.error("Error adding file to zip: {}", path, e);
				}
			});
		}
	}
}