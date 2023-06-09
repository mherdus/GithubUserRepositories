package com.mherdus.githubuserrepositories.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mherdus.githubuserrepositories.utils.Branch;
import com.mherdus.githubuserrepositories.utils.ErrorMessage;
import com.mherdus.githubuserrepositories.utils.Repository;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class RepositoriesController {
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String url = "https://api.github.com/";

    public RepositoriesController() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping(value = "/repositories")
    public ResponseEntity<?> getRepositories(
            @RequestParam("username") String username,
            @RequestHeader(HttpHeaders.ACCEPT) String acceptHeader
    ) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(url + "users/" + username + "/repos");


        if (!acceptHeader.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_ACCEPTABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorMessage(HttpStatus.NOT_ACCEPTABLE.value(), "Unsupported media type."));
        }

        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ErrorMessage(HttpStatus.NOT_FOUND.value(), "User not found."));
            } else if (statusCode != HttpStatus.OK.value()) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred."));
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
            List<Repository> repositories = new ArrayList<>();

            for (JsonNode repoNode : jsonResponse) {
                if (!repoNode.get("fork").asBoolean()) {
                    String repoName = repoNode.get("name").asText();
                    String ownerLogin = repoNode.get("owner").get("login").asText();
                    List<Branch> branches = getBranchesForRepository(repoName, username);
                    repositories.add(new Repository(repoName, ownerLogin, branches));
                }
            }

            return ResponseEntity.ok(repositories);
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred."));
        }
    }

    private List<Branch> getBranchesForRepository(String repoName, String username) throws IOException {
        HttpGet request = new HttpGet(url + "repos/" + username + "/" + repoName + "/branches");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.OK.value()) {
                return new ArrayList<>(); // Empty branches list if unable to fetch
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
            List<Branch> branches = new ArrayList<>();

            for (JsonNode branchNode : jsonResponse) {
                String branchName = branchNode.get("name").asText();
                String lastCommitSha = branchNode.get("commit").get("sha").asText();
                branches.add(new Branch(branchName, lastCommitSha));
            }

            return branches;
        }
    }
}