package com.delivery.deliveryapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.dto.ImageUploadResult;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class ProductImageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadImageAndCreateProductWithPhotos() throws Exception {
        // Prepare company and user
        Company company = new Company();
        company.setName("TestCo");
        company = companyRepository.save(company);

        User user = new User();
        user.setCompany(company);
        user.setUserRole(UserRole.OWNER);
        user = userRepository.save(user);

        // Set authentication with user id as principal (string) - matches controllers' expectations
        final String userId = user.getId().toString();
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Upload an image via the ImageController endpoint
        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "dummy-image-bytes".getBytes(StandardCharsets.UTF_8));

        MvcResult res = mockMvc.perform(multipart("/api/images/upload").file(file).with(authentication(auth)))
            .andExpect(status().isOk())
            .andReturn();

        String json = res.getResponse().getContentAsString();
        List<ImageUploadResult> results = objectMapper.readValue(json, new TypeReference<List<ImageUploadResult>>(){});
        assertThat(results).isNotEmpty();
        ImageUploadResult uploaded = results.get(0);
        assertThat(uploaded.getId()).isNotEmpty();

        // Create product using a direct POST to /products endpoint, passing productPhotos
        String payload = "{\"name\": \"IntegrationProduct\", \"productPhotos\": [\"" + uploaded.getId() + "\"], \"buyingPrice\": 20.00, \"sellingPrice\": 25.00 }";

        MvcResult cres = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
            .content(payload).with(authentication(auth)))
            .andExpect(status().isCreated())
            .andReturn();

        String prodJson = cres.getResponse().getContentAsString();
        // Parse and assert productPhotos present
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(prodJson);
        assertThat(node.get("productPhotos")).isNotNull();
        assertThat(node.get("productPhotos").get(0).get("id").asText()).isEqualTo(uploaded.getId());
    }
}
