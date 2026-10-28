package com.plazoleta.plazoleta.domain.model;

public class Restaurant {

    private Long id;
    private String name;
    private String nit;
    private String address;
    private String phone;
    private String urlLogo;
    private Long ownerId;

    public Restaurant() {
    }

    public Restaurant(Long id, String name, String nit, String address, String phone, String urlLogo, Long ownerId) {
        this.id = id;
        this.name = name;
        this.nit = nit;
        this.address = address;
        this.phone = phone;
        this.urlLogo = urlLogo;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUrlLogo() { return urlLogo; }
    public void setUrlLogo(String urlLogo) { this.urlLogo = urlLogo; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
