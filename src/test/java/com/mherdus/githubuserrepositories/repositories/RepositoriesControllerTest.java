package com.mherdus.githubuserrepositories.repositories;

import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class RepositoriesControllerTest {

    @Mock
    private CloseableHttpClient httpClient;

    private RepositoriesController controller;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new RepositoriesController();
    }

    @Test
    public void testGetRepositoriesValidUsernameWithJsonAcceptHeaderReturnsRepositoriesInJson() throws IOException {
        CloseableHttpResponse httpResponse = createMockResponse(HttpStatus.OK, createSampleResponseJson());
        when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpResponse);

        ResponseEntity<?> response = controller.getRepositories("johndoe", MediaType.APPLICATION_JSON_VALUE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetRepositoriesValidUsernameWithXmlAcceptHeaderReturnsErrorInJson() throws IOException {
        CloseableHttpResponse httpResponse = createMockResponse(HttpStatus.NOT_FOUND, "");
        when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpResponse);

        ResponseEntity<?> response = controller.getRepositories("johndoe", MediaType.APPLICATION_XML_VALUE);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());

    }

    @Test
    public void testGetRepositoriesInvalidUsernameReturnsNotFound() throws IOException {
        CloseableHttpResponse httpResponse = createMockResponse(HttpStatus.NOT_FOUND, "");
        when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpResponse);

        ResponseEntity<?> response = controller.getRepositories("invaliduser123412345098", MediaType.APPLICATION_JSON_VALUE);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    }

    private CloseableHttpResponse createMockResponse(HttpStatus status, String content) throws IOException {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, status.value(), ""));

        if (content != null) {
            HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
            when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(content.getBytes()));
            when(response.getEntity()).thenReturn(httpEntity);
        }

        return response;
    }

    private String createSampleResponseJson() {
        return "[\n" +
                "    {\n" +
                "        \"repositoryName\": \"example-repo\",\n" +
                "        \"ownerLogin\": \"johndoe\",\n" +
                "        \"branches\": [\n" +
                "            {\n" +
                "                \"branchName\": \"main\",\n" +
                "                \"lastCommitSha\": \"13131313\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"branchName\": \"feature-branch\",\n" +
                "                \"lastCommitSha\": \"12121212\"\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    {\n" +
                "        \"repositoryName\": \"another-repo\",\n" +
                "        \"ownerLogin\": \"johndoe\",\n" +
                "        \"branches\": [\n" +
                "            {\n" +
                "                \"branchName\": \"main\",\n" +
                "                \"lastCommitSha\": \"22222222\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"branchName\": \"bug-fix\",\n" +
                "                \"lastCommitSha\": \"11111111\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "]";
    }
}
