package com.example.androideatit.Model;

public class User {
    private String Name;
    private String Password;
    private String Phone;
    private String isStaff;
    private String secureCode;
    private String homeAddress;

    public User() {
    }

    public User(String name, String password, String secureCode) {
        Name = name;
        Password = password;
        isStaff = "false";
        this.secureCode = secureCode;
    }

    //Present Alt + Insert


    public String getSecureCode() {
        return secureCode;
    }

    public void setSecureCode(String secureCode) {
        this.secureCode = secureCode;
    }

    public String getIsStaff() {
        return isStaff;
    }

    public void setIsStaff(String isStaff) {
        this.isStaff = isStaff;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }
}
