package com.monederobingo.controller.api.v1;

import com.lealpoints.i18n.Message;
import com.lealpoints.model.Company;
import com.lealpoints.service.implementations.CompanyServiceImpl;
import com.lealpoints.service.response.ServiceMessage;
import com.lealpoints.service.response.ServiceResult;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class CompanyControllerTest {

    @Test
    public void testUpdateLogo() throws FileUploadException {
        CompanyServiceImpl companyService = createCompanyService(new ServiceResult<Boolean>(true,
                new ServiceMessage(Message.YOUR_LOGO_WAS_UPDATED.name())));
        final ServletFileUpload servletFileUpload = createMock(ServletFileUpload.class);
        expect(servletFileUpload.parseRequest((HttpServletRequest) anyObject())).andReturn(new ArrayList<FileItem>());
        replay(servletFileUpload);
        CompanyController companyController = new CompanyController(companyService) {
            @Override
            ServletFileUpload getServletFileUpload() {
                return servletFileUpload;
            }
        };

        ResponseEntity<ServiceResult> responseEntity = companyController.updateLogo(1, createMock(HttpServletRequest.class));
        assertNotNull(responseEntity);
        ServiceResult serviceResult = responseEntity.getBody();
        assertNotNull(serviceResult);
        assertTrue(serviceResult.isSuccess());
        assertEquals(Message.YOUR_LOGO_WAS_UPDATED.name(), serviceResult.getMessage());
        verify(companyService);
    }

    @Test
    public void testGet() throws Exception {
        //given
        Company company = new Company();
        company.setName("name");
        company.setUrlImageLogo("logo.png");
        xyz.greatapp.libs.service.ServiceResult serviceResult = new xyz.greatapp.libs.service.ServiceResult(true, "", company.toJSONObject().toString());
        CompanyServiceImpl companyService = createCompanyServiceForGet(serviceResult);
        CompanyController companyController = new CompanyController(companyService);

        //when
        ResponseEntity<xyz.greatapp.libs.service.ServiceResult> responseEntity = companyController.get(1);

        //then
        assertNotNull(responseEntity);
        xyz.greatapp.libs.service.ServiceResult actualServiceResult = responseEntity.getBody();
        assertNotNull(actualServiceResult);
        assertTrue(actualServiceResult.isSuccess());
        assertNotNull(actualServiceResult.getObject());
        JSONObject actualCompany = new JSONObject(serviceResult.getObject());
        assertEquals("name", actualCompany.get("name"));
        assertEquals("logo.png", actualCompany.get("url_image_logo"));
        verify(companyService);
    }

    private CompanyServiceImpl createCompanyService(ServiceResult<Boolean> serviceResult) {
        CompanyServiceImpl companyService = createMock(CompanyServiceImpl.class);
        expect(companyService.updateLogo(anyObject(), anyLong())).andReturn(serviceResult);
        replay(companyService);
        return companyService;
    }

    private CompanyServiceImpl createCompanyServiceForGet(xyz.greatapp.libs.service.ServiceResult company) throws Exception {
        final CompanyServiceImpl companyService = EasyMock.createMock(CompanyServiceImpl.class);
        expect(companyService.getByCompanyId(anyLong())).andReturn(company).times(1);
        replay(companyService);
        return companyService;
    }
}
