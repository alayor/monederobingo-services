package com.lealpoints.repository.fixtures;

public class CompanyRepositoryFixture {

    public static final String INSERT_CLIENT_TWO_COMPANIES_AND_MAPPING = "INSERT INTO client(client_id, phone, can_receive_promo_sms) VALUES(1, 'A', TRUE );\n" +
                "INSERT INTO company(company_id, name, url_image_logo) VALUES(1, 'A','B');\n" +
                "INSERT INTO company(company_id, name, url_image_logo) VALUES(2, 'X','Y');\n" +
                "INSERT INTO company_client_mapping(company_id, client_id) VALUES(1, 1);\n" +
                "INSERT INTO company_client_mapping(company_id, client_id) VALUES(2, 1);";

    public static final String INSERT_COMPANY = "INSERT INTO company (company_id, name, url_image_logo) VALUES (1, 'A', 'B');";
}
