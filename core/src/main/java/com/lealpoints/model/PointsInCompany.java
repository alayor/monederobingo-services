package com.lealpoints.model;

import org.json.JSONObject;

public class PointsInCompany {
    private long companyId;
    private String name;
    private String urlImageLogo;
    private float points;

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlImageLogo() {
        return urlImageLogo;
    }

    public void setUrlImageLogo(String urlImageLogo) {
        this.urlImageLogo = urlImageLogo;
    }

    public float getPoints() {
        return points;
    }

    public void setPoints(float points) {
        this.points = points;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("company_id", companyId);
        jsonObject.put("name", name);
        jsonObject.put("url_image_logo", urlImageLogo);
        jsonObject.put("points", points);
        return jsonObject;
    }
}
