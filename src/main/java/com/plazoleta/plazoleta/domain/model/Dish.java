package com.plazoleta.plazoleta.domain.model;

public class Dish {

    private Long id;
    private String name;
    private Integer price;
    private String description;
    private String urlImage;
    private Long categoryId;
    private Long restaurantId;
    private Boolean active;

    public Dish() {
    }

    public Dish(Long id, String name, Integer price, String description, String urlImage,
                Long categoryId, Long restaurantId, Boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.urlImage = urlImage;
        this.categoryId = categoryId;
        this.restaurantId = restaurantId;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrlImage() { return urlImage; }
    public void setUrlImage(String urlImage) { this.urlImage = urlImage; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
