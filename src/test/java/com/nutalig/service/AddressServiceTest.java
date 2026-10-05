package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.dto.DistrictDto;
import com.nutalig.dto.ProvinceDto;
import com.nutalig.dto.SubDistrictDto;
import com.nutalig.entity.DistrictEntity;
import com.nutalig.entity.ProvinceEntity;
import com.nutalig.entity.SubDistrictEntity;
import com.nutalig.mapper.AddressMapper;
import com.nutalig.repository.DistrictRepository;
import com.nutalig.repository.ProvinceRepository;
import com.nutalig.repository.SubDistrictRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AddressServiceTest {

    private ProvinceRepository provinceRepository;
    private DistrictRepository districtRepository;
    private SubDistrictRepository subDistrictRepository;
    private AddressMapper addressMapper;
    private MockRestServiceServer server;
    private AddressService service;

    @BeforeEach
    void setUp() {
        provinceRepository = mock(ProvinceRepository.class);
        districtRepository = mock(DistrictRepository.class);
        subDistrictRepository = mock(SubDistrictRepository.class);
        addressMapper = mock(AddressMapper.class);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new AddressService(provinceRepository, districtRepository,
                subDistrictRepository, addressMapper, new ObjectMapper(), builder.build());
        ReflectionTestUtils.setField(service, "geothaiBaseUrl", "https://example.test/api/");
    }

    @Test
    void enabledUsesConfiguredUrlAndConvertsNumericIdsToStrings() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/provinces/all"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":2,"nameTh":"สมุทรปราการ","nameEn":"Samut Prakan"},
                         {"id":1,"nameTh":"กรุงเทพมหานคร","nameEn":"Bangkok"}]
                        """, MediaType.APPLICATION_JSON));

        List<ProvinceDto> provinces = service.getAllProvince();

        assertEquals(List.of("1", "2"), provinces.stream().map(ProvinceDto::getId).toList());
        assertEquals("กรุงเทพมหานคร", provinces.getFirst().getNameTh());
        assertEquals("Bangkok", provinces.getFirst().getNameEn());
        verifyNoInteractions(provinceRepository, addressMapper);
        server.verify();
    }

    @Test
    void disabledReadsDatabaseAndKeepsThaiNameSorting() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", false);
        ProvinceEntity bangkok = new ProvinceEntity();
        ProvinceEntity samutPrakan = new ProvinceEntity();
        bangkok.setId("1");
        samutPrakan.setId("2");
        ProvinceDto bangkokDto = new ProvinceDto();
        bangkokDto.setId("1");
        bangkokDto.setNameTh("กรุงเทพมหานคร");
        ProvinceDto samutPrakanDto = new ProvinceDto();
        samutPrakanDto.setId("2");
        samutPrakanDto.setNameTh("สมุทรปราการ");
        when(provinceRepository.findAll()).thenReturn(List.of(samutPrakan, bangkok));
        when(addressMapper.toProvinceDto(bangkok)).thenReturn(bangkokDto);
        when(addressMapper.toProvinceDto(samutPrakan)).thenReturn(samutPrakanDto);

        assertEquals(List.of(bangkokDto, samutPrakanDto), service.getAllProvince());

        verify(provinceRepository).findAll();
        server.verify();
    }

    @Test
    void enabledRejectsHttpErrorsWithoutSwitchingToDatabase() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/provinces/all"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        IllegalStateException error = assertThrows(IllegalStateException.class, service::getAllProvince);

        assertEquals("Failed to load provinces from GeoThai: HTTP 503", error.getMessage());
        verifyNoInteractions(provinceRepository);
        server.verify();
    }

    @Test
    void enabledRejectsMissingResponseBody() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/provinces/all"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        IllegalStateException error = assertThrows(IllegalStateException.class, service::getAllProvince);

        assertEquals("Failed to load provinces from GeoThai: empty response", error.getMessage());
        verifyNoInteractions(provinceRepository);
        server.verify();
    }

    @Test
    void enabledReadsNestedDistrictsAndFillsProvinceId() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/provinces-with-districts/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":1,"nameTh":"กรุงเทพมหานคร","nameEn":"Bangkok","geographyId":2,
                         "districts":[{"id":1001,"nameTh":"เขตพระนคร","nameEn":"Khet Phra Nakhon"},
                                      {"id":1002,"nameTh":"เขตดุสิต","nameEn":"Khet Dusit"}]}
                        """, MediaType.APPLICATION_JSON));

        List<DistrictDto> districts = service.getDistrictByProvince(" 1 ");

        assertEquals(List.of("1001", "1002"), districts.stream().map(DistrictDto::getId).toList());
        assertEquals(List.of("1", "1"), districts.stream().map(DistrictDto::getProvinceId).toList());
        assertEquals("เขตพระนคร", districts.getFirst().getNameTh());
        assertEquals("Khet Phra Nakhon", districts.getFirst().getNameEn());
        verifyNoInteractions(districtRepository, provinceRepository, addressMapper);
        server.verify();
    }

    @Test
    void enabledReadsAllDistrictsWhenProvinceIsBlank() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/provinces-with-districts/all"))
                .andRespond(withSuccess("""
                        [{"id":1,"districts":[{"id":1001,"nameTh":"เขตพระนคร","nameEn":"Khet Phra Nakhon"}]},
                         {"id":2,"districts":[{"id":1101,"nameTh":"เมืองสมุทรปราการ","nameEn":"Mueang Samut Prakan"}]}]
                        """, MediaType.APPLICATION_JSON));

        List<DistrictDto> districts = service.getDistrictByProvince(" ");

        assertEquals(List.of("1001", "1101"), districts.stream().map(DistrictDto::getId).toList());
        assertEquals(List.of("1", "2"), districts.stream().map(DistrictDto::getProvinceId).toList());
        verifyNoInteractions(districtRepository, provinceRepository, addressMapper);
        server.verify();
    }

    @Test
    void disabledReadsDistrictsFromDatabaseByProvince() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", false);
        DistrictEntity entity = new DistrictEntity();
        DistrictDto dto = new DistrictDto();
        dto.setId("1001");
        dto.setProvinceId("1");
        when(districtRepository.findByProvinceId("1")).thenReturn(List.of(entity));
        when(addressMapper.toDistrictDto(entity)).thenReturn(dto);

        assertEquals(List.of(dto), service.getDistrictByProvince("1"));

        verify(districtRepository).findByProvinceId("1");
        server.verify();
    }

    @Test
    void disabledReadsAllDistrictsFromDatabaseWhenProvinceIsEmpty() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", false);
        when(districtRepository.findAll()).thenReturn(List.of());

        assertEquals(List.of(), service.getDistrictByProvince(""));

        verify(districtRepository).findAll();
        server.verify();
    }

    @Test
    void enabledRejectsDistrictHttpErrorsWithoutSwitchingToDatabase() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/provinces-with-districts/1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getDistrictByProvince("1"));

        assertEquals("Failed to load districts from GeoThai: HTTP 503", error.getMessage());
        verifyNoInteractions(districtRepository);
        server.verify();
    }

    @Test
    void enabledRejectsProvinceResponseWithoutDistricts() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/provinces-with-districts/1"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getDistrictByProvince("1"));

        assertEquals("Failed to load districts from GeoThai: missing districts", error.getMessage());
        verifyNoInteractions(districtRepository);
        server.verify();
    }

    @Test
    void enabledReadsNestedSubdistrictsAndConvertsIdsAndZipCodesToStrings() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/districts-with-subdistricts/1001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":1001,"nameTh":"เขตพระนคร","nameEn":"Khet Phra Nakhon","provinceId":1,
                         "subdistricts":[{"id":100101,"zipCode":10200,"nameTh":"พระบรมมหาราชวัง",
                                          "nameEn":"Phra Borom Maha Ratchawang","lat":null,"long":null},
                                         {"id":100102,"zipCode":10200,"nameTh":"วังบูรพาภิรมย์",
                                          "nameEn":"Wang Burapha Phirom","lat":13.75,"long":100.5}]}
                        """, MediaType.APPLICATION_JSON));

        List<SubDistrictDto> subdistricts = service.getSubDistrictByDistrict(" 1001 ");

        assertEquals(List.of("100101", "100102"), subdistricts.stream().map(SubDistrictDto::getId).toList());
        assertEquals(List.of("1001", "1001"), subdistricts.stream().map(SubDistrictDto::getDistrictId).toList());
        assertEquals(List.of("10200", "10200"), subdistricts.stream().map(SubDistrictDto::getZipCode).toList());
        assertEquals("พระบรมมหาราชวัง", subdistricts.getFirst().getNameTh());
        assertEquals("Phra Borom Maha Ratchawang", subdistricts.getFirst().getNameEn());
        verifyNoInteractions(subDistrictRepository, districtRepository, provinceRepository, addressMapper);
        server.verify();
    }

    @Test
    void enabledReadsAllSubdistrictsWhenDistrictIsBlank() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/districts-with-subdistricts/all"))
                .andRespond(withSuccess("""
                        [{"id":1001,"subdistricts":[{"id":100101,"zipCode":10200,
                          "nameTh":"พระบรมมหาราชวัง","nameEn":"Phra Borom Maha Ratchawang","lat":null,"long":null}]},
                         {"id":1002,"subdistricts":[{"id":100201,"zipCode":10300,
                          "nameTh":"ดุสิต","nameEn":"Dusit","lat":null,"long":null}]}]
                        """, MediaType.APPLICATION_JSON));

        List<SubDistrictDto> subdistricts = service.getSubDistrictByDistrict(" ");

        assertEquals(List.of("100101", "100201"), subdistricts.stream().map(SubDistrictDto::getId).toList());
        assertEquals(List.of("1001", "1002"), subdistricts.stream().map(SubDistrictDto::getDistrictId).toList());
        assertEquals(List.of("10200", "10300"), subdistricts.stream().map(SubDistrictDto::getZipCode).toList());
        verifyNoInteractions(subDistrictRepository, districtRepository, provinceRepository, addressMapper);
        server.verify();
    }

    @Test
    void disabledReadsSubdistrictsFromDatabaseByDistrict() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", false);
        SubDistrictEntity entity = new SubDistrictEntity();
        SubDistrictDto dto = new SubDistrictDto();
        dto.setId("100101");
        dto.setDistrictId("1001");
        dto.setZipCode("10200");
        when(subDistrictRepository.findByDistrictId("1001")).thenReturn(List.of(entity));
        when(addressMapper.toSubDistrictDto(entity)).thenReturn(dto);

        assertEquals(List.of(dto), service.getSubDistrictByDistrict("1001"));

        verify(subDistrictRepository).findByDistrictId("1001");
        server.verify();
    }

    @Test
    void disabledReadsAllSubdistrictsFromDatabaseWhenDistrictIsEmpty() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", false);
        when(subDistrictRepository.findAll()).thenReturn(List.of());

        assertEquals(List.of(), service.getSubDistrictByDistrict(""));

        verify(subDistrictRepository).findAll();
        server.verify();
    }

    @Test
    void enabledRejectsSubdistrictHttpErrorsWithoutSwitchingToDatabase() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/districts-with-subdistricts/1001"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getSubDistrictByDistrict("1001"));

        assertEquals("Failed to load subdistricts from GeoThai: HTTP 503", error.getMessage());
        verifyNoInteractions(subDistrictRepository);
        server.verify();
    }

    @Test
    void enabledRejectsDistrictResponseWithoutSubdistricts() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/districts-with-subdistricts/1001"))
                .andRespond(withSuccess("{\"id\":1001}", MediaType.APPLICATION_JSON));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getSubDistrictByDistrict("1001"));

        assertEquals("Failed to load subdistricts from GeoThai: missing subdistricts", error.getMessage());
        verifyNoInteractions(subDistrictRepository);
        server.verify();
    }

    @Test
    void enabledRejectsMissingSubdistrictResponseBody() {
        ReflectionTestUtils.setField(service, "geothaiEnabled", true);
        server.expect(requestTo("https://example.test/api/districts-with-subdistricts/1001"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getSubDistrictByDistrict("1001"));

        assertEquals("Failed to load subdistricts from GeoThai: empty response", error.getMessage());
        verifyNoInteractions(subDistrictRepository);
        server.verify();
    }
}
