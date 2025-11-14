package com.delivery.deliveryapi.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.deliveryapi.model.District;
import com.delivery.deliveryapi.model.Province;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.ProvinceRepository;

@ExtendWith(MockitoExtension.class)
class GeographicServiceTest {

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private DistrictRepository districtRepository;

    @InjectMocks
    private GeographicService geographicService;

    private Province testProvince;
    private District testDistrict;
    private UUID provinceId;
    private UUID districtId;

    @BeforeEach
    void setUp() {
        provinceId = UUID.randomUUID();
        districtId = UUID.randomUUID();

        testProvince = new Province("12", "Phnom Penh Capital", "រាជធានីភ្នំពេញ");
        setProvinceId(testProvince, provinceId);
        testProvince.setCapital("Phnom Penh");
        testProvince.setPopulation(2129371);
        testProvince.setAreaKm2(679);
        testProvince.setDistrictsKhan(14);
        testProvince.setCommunesSangkat(105);
        testProvince.setTotalVillages(953);
        testProvince.setReferenceNumber("ប្រកាសលេខ ៤៩៣ ប្រ.ក");
        testProvince.setReferenceYear(2008);
        testProvince.updateTotalDistricts();
        testProvince.updateTotalCommunes();

        testDistrict = new District("Chamkarmon", "12-001", testProvince);
        setDistrictId(testDistrict, districtId);
        testDistrict.setNameKh("ចំការមន");
        testDistrict.setPopulation(200000);
    }

    @Test
    void testGetAllActiveProvinces() {
        // Given
        List<Province> provinces = Arrays.asList(testProvince);
        when(provinceRepository.findByActiveTrueOrderByName()).thenReturn(provinces);

        // When
        List<Province> result = geographicService.getAllActiveProvinces();

        // Then
        assertEquals(1, result.size());
        assertEquals(testProvince, result.get(0));
        verify(provinceRepository).findByActiveTrueOrderByName();
    }

    @Test
    void testGetProvinceById() {
        // Given
        when(provinceRepository.findById(provinceId)).thenReturn(Optional.of(testProvince));

        // When
        Optional<Province> result = geographicService.getProvinceById(provinceId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testProvince, result.get());
        verify(provinceRepository).findById(provinceId);
    }

    @Test
    void testGetProvinceByCode() {
        // Given
        when(provinceRepository.findByCode("12")).thenReturn(Optional.of(testProvince));

        // When
        Optional<Province> result = geographicService.getProvinceByCode("12");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testProvince, result.get());
        verify(provinceRepository).findByCode("12");
    }

    @Test
    void testSearchProvinces() {
        // Given
        List<Province> provinces = Arrays.asList(testProvince);
        when(provinceRepository.searchByName("Phnom")).thenReturn(provinces);

        // When
        List<Province> result = geographicService.searchProvinces("Phnom");

        // Then
        assertEquals(1, result.size());
        assertEquals(testProvince, result.get(0));
        verify(provinceRepository).searchByName("Phnom");
    }

    @Test
    void testSearchProvincesEmpty() {
        // Given
        List<Province> provinces = Arrays.asList(testProvince);
        when(provinceRepository.findByActiveTrueOrderByName()).thenReturn(provinces);

        // When
        List<Province> result = geographicService.searchProvinces("");

        // Then
        assertEquals(1, result.size());
        verify(provinceRepository).findByActiveTrueOrderByName();
    }

    @Test
    void testGetDistrictsByProvince() {
        // Given
        List<District> districts = Arrays.asList(testDistrict);
        when(districtRepository.findByProvinceAndActiveTrueOrderByName(testProvince)).thenReturn(districts);

        // When
        List<District> result = geographicService.getDistrictsByProvince(testProvince);

        // Then
        assertEquals(1, result.size());
        assertEquals(testDistrict, result.get(0));
        verify(districtRepository).findByProvinceAndActiveTrueOrderByName(testProvince);
    }

    @Test
    void testGetDistrictsByProvinceId() {
        // Given
        List<District> districts = Arrays.asList(testDistrict);
        when(districtRepository.findActiveDistrictsByProvinceId(provinceId)).thenReturn(districts);

        // When
        List<District> result = geographicService.getDistrictsByProvinceId(provinceId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testDistrict, result.get(0));
        verify(districtRepository).findActiveDistrictsByProvinceId(provinceId);
    }

    @Test
    void testGetDistrictsByProvinceCode() {
        // Given
        List<District> districts = Arrays.asList(testDistrict);
        when(districtRepository.findActiveDistrictsByProvinceCode("12")).thenReturn(districts);

        // When
        List<District> result = geographicService.getDistrictsByProvinceCode("12");

        // Then
        assertEquals(1, result.size());
        assertEquals(testDistrict, result.get(0));
        verify(districtRepository).findActiveDistrictsByProvinceCode("12");
    }

    @Test
    void testGetAllProvincesWithDistricts() {
        // Given
        List<Province> provinces = Arrays.asList(testProvince);
        List<District> districts = Arrays.asList(testDistrict);

        when(provinceRepository.findByActiveTrueOrderByName()).thenReturn(provinces);
        when(districtRepository.findByProvinceAndActiveTrueOrderByName(testProvince)).thenReturn(districts);

        // When
        List<GeographicService.ProvinceWithDistricts> result = geographicService.getAllProvincesWithDistricts();

        // Then
        assertEquals(1, result.size());
        GeographicService.ProvinceWithDistricts provinceWithDistricts = result.get(0);
        assertEquals("Phnom Penh Capital", provinceWithDistricts.getProvince().getName());
        assertEquals(1, provinceWithDistricts.getDistricts().size());
        assertEquals("Chamkarmon", provinceWithDistricts.getDistricts().get(0).getName());
    }

    @Test
    void testProvinceSummary() {
        // When
        GeographicService.ProvinceSummary summary = new GeographicService.ProvinceSummary(testProvince);

        // Then
        assertEquals(testProvince.getId().toString(), summary.getId());
        assertEquals("Phnom Penh Capital", summary.getName());
        assertEquals("រាជធានីភ្នំពេញ", summary.getNameKh());
        assertEquals("12", summary.getCode());
        assertEquals("Phnom Penh", summary.getCapital());
        assertEquals(679, summary.getAreaKm2());
        assertEquals(2129371, summary.getPopulation());
        assertEquals(0, summary.getDistrictsKrong());
        assertEquals(0, summary.getDistrictsSrok());
        assertEquals(14, summary.getDistrictsKhan());
        assertEquals(14, summary.getTotalDistricts());
        assertEquals(0, summary.getCommunesCommune());
        assertEquals(105, summary.getCommunesSangkat());
        assertEquals(105, summary.getTotalCommunes());
        assertEquals(953, summary.getTotalVillages());
        assertEquals("ប្រកាសលេខ ៤៩៣ ប្រ.ក", summary.getReferenceNumber());
        assertEquals(2008, summary.getReferenceYear());
    }

    @Test
    void testDistrictSummary() {
        // When
        GeographicService.DistrictSummary summary = new GeographicService.DistrictSummary(testDistrict);

        // Then
        assertEquals(testDistrict.getId().toString(), summary.getId());
        assertEquals("Chamkarmon", summary.getName());
        assertEquals("ចំការមន", summary.getNameKh());
        assertEquals("12-001", summary.getCode());
        assertEquals("Phnom Penh Capital", summary.getProvinceName());
        assertEquals("12", summary.getProvinceCode());
        assertEquals(200000, summary.getPopulation());
    }

    private void setProvinceId(Province province, UUID id) {
        try {
            var field = Province.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(province, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set province ID", e);
        }
    }

    private void setDistrictId(District district, UUID id) {
        try {
            var field = District.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(district, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set district ID", e);
        }
    }
}