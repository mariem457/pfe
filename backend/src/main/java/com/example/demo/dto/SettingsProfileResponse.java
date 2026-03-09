package com.example.demo.dto;

public class SettingsProfileResponse {

    private String firstName;
    private String lastName;
    private String email;
    private String function;
    private String organization;

    public SettingsProfileResponse() {
    }

    public SettingsProfileResponse(String firstName, String lastName, String email, String function, String organization) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.function = function;
        this.organization = organization;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}