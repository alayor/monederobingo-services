package com.monederobingo.controller.api.v1;

import com.lealpoints.service.CompanyService;
import com.monederobingo.controller.base.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.annotation.MultipartConfig;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/api/v1/points_in_company")
@MultipartConfig
public class PointsInCompanyController extends BaseController {

    private CompanyService _companyService;

    @Autowired
    public PointsInCompanyController(CompanyService companyService) {
        _companyService = companyService;
    }

    @RequestMapping(value = "/{phone}", method = GET)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseEntity<xyz.greatapp.libs.service.ServiceResult> getByClientId(@PathVariable("phone") String phone) {
        xyz.greatapp.libs.service.ServiceResult serviceResult = _companyService.getPointsInCompanyByPhone(phone);
        return new ResponseEntity<>(serviceResult, HttpStatus.OK);
    }
}
