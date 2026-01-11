package com.example.intelliwear;

public class AppUser {
    private String id;
    private String name;
    private String email;
    private boolean isApproved;
    private boolean isAdmin;

    public AppUser() {
        // Required empty constructor for Firebase
    }

    public AppUser(String id, String name, String email, boolean isApproved) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.isApproved = isApproved;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        this.isApproved = approved;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
