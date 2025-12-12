package com.delivery.deliveryapi.controller;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyController companyController;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DistrictRepository districtRepository;

    @MockBean
    private SecurityContext securityContext;

    @MockBean
    private Authentication authentication;

    private User testUser;
    private Company testCompany;
    private UUID userId;
    private UUID companyId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        // Create test province and district
        Province testProvince = new Province();
        Field provinceIdField = Province.class.getDeclaredField("id");
        provinceIdField.setAccessible(true);
        provinceIdField.set(testProvince, UUID.randomUUID());
        testProvince.setName("Bangkok");
        testProvince.setNameKh("ភ្នំពេញ");

        District testDistrict = new District();
        Field districtIdField = District.class.getDeclaredField("id");
        districtIdField.setAccessible(true);
        districtIdField.set(testDistrict, UUID.randomUUID());
        testDistrict.setName("Siam");
        testDistrict.setNameKh("សៀម");
        testDistrict.setProvince(testProvince);

        testCompany = new Company();
        Field companyIdField = Company.class.getDeclaredField("id");
        companyIdField.setAccessible(true);
        companyIdField.set(testCompany, companyId);
        testCompany.setName("Test Company");
        testCompany.setAddress("123 Test St");
        testCompany.setDistrictId(testDistrict.getId());
        testCompany.setActive(true);

        testUser = new User();
        Field userIdField = User.class.getDeclaredField("id");
        userIdField.setAccessible(true);
        userIdField.set(testUser, userId);
        testUser.setUsername("testuser");
        testUser.setCompany(testCompany);
        testUser.setUserRole(UserRole.OWNER);

        // Mock security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetMyCompany_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<Object> response = companyController.getMyCompany();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof CompanyController.CompanyResponse);
        CompanyController.CompanyResponse companyResponse = (CompanyController.CompanyResponse) response.getBody();
        assertEquals(companyId, companyResponse.id());
        assertEquals("Test Company", companyResponse.name());
        assertEquals("123 Test St", companyResponse.address());
        assertNotNull(companyResponse.district());
        assertEquals("Siam", companyResponse.district().name());
        assertEquals("Bangkok", companyResponse.district().province().name());
        assertTrue(companyResponse.active());
    }

    @Test
    void testGetMyCompany_UserNotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Object> response = companyController.getMyCompany();

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetMyCompany_NoCompany() {
        // Arrange
        testUser.setCompany(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<Object> response = companyController.getMyCompany();

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("User is not part of any company", body.get("error"));
    }

    @Test
    void testUpdateMyCompany_Success() {
        // Arrange
        UUID districtId = UUID.randomUUID();
        UUID provinceId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String phoneNumber = "0123456789";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);
        when(districtRepository.findById(districtId)).thenReturn(Optional.of(new District()));

        CompanyController.UpdateCompanyRequest request = new CompanyController.UpdateCompanyRequest(
            "Updated Company",
            "456 New St",
            phoneNumber,
            districtId,
            provinceId,
            categoryId
        );

        // Act
        ResponseEntity<Object> response = companyController.updateMyCompany(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Company information updated successfully", body.get("message"));
        verify(companyRepository).save(testCompany);
    }

    @Test
    void testUpdateMyCompany_NotOwner() {
        // Arrange
        testUser.setUserRole(UserRole.MANAGER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        CompanyController.UpdateCompanyRequest request = new CompanyController.UpdateCompanyRequest(
            "Updated Company",
            "456 New St",
            "0123456789",
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // Act
        ResponseEntity<Object> response = companyController.updateMyCompany(request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Only company owners can update company information", body.get("error"));
    }

    @Test
    void testUpdateMyCompany_NoCompany() {
        // Arrange
        testUser.setCompany(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        CompanyController.UpdateCompanyRequest request = new CompanyController.UpdateCompanyRequest(
            "Updated Company",
            "456 New St",
            "0123456789",
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // Act
        ResponseEntity<Object> response = companyController.updateMyCompany(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("User is not part of any company", body.get("error"));
    }
}